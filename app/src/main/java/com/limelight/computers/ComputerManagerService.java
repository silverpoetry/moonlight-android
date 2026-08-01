package com.limelight.computers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InterfaceAddress;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.limelight.LimeLog;
import com.limelight.binding.PlatformBinding;
import com.limelight.computers.discovery.AndroidMdnsDiscoverySource;
import com.limelight.computers.discovery.HostDiscoveryCandidate;
import com.limelight.computers.discovery.HostDiscoverySource;
import com.limelight.computers.http.android.AndroidNvHttpClientFactory;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.reachability.HostReachabilityCoordinator;
import com.limelight.computers.reachability.HostReachabilityPlan;
import com.limelight.computers.reachability.Ipv4SubnetMatcher;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.NetHelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import org.xmlpull.v1.XmlPullParserException;

public class ComputerManagerService extends Service {
    private static final int SERVERINFO_POLLING_PERIOD_MS = 1500;
    private static final int APPLIST_POLLING_PERIOD_MS = 30000;
    private static final int APPLIST_FAILED_POLLING_RETRY_MS = 2000;
    private static final int MDNS_QUERY_PERIOD_MS = 1000;
    private static final int OFFLINE_POLL_TRIES = 3;
    private static final int INITIAL_POLL_TRIES = 2;
    private static final int EMPTY_LIST_THRESHOLD = 3;
    private static final int POLL_DATA_TTL_MS = 30000;
    private static final int ADDRESS_UPGRADE_GRACE_MS = 200;
    private static final int ENDPOINT_PROBE_THREADS = 8;
    private static final int ENDPOINT_PROBE_QUEUE_CAPACITY = 32;
    private static final AtomicInteger endpointProbeThreadId =
            new AtomicInteger();

    private final ComputerManagerBinder binder = new ComputerManagerBinder();

    private ComputerDatabaseManager dbManager;
    private HostRepositoryLeaseManager repositoryLeases;

    private IdentityManager idManager;
    // Lock order for host state is networkLock -> stateLock -> pollingTuples.
    // Code holding pollingTuples must never acquire either tuple lock.
    private final LinkedList<PollingTuple> pollingTuples = new LinkedList<>();
    private final Object pollingLifecycleLock = new Object();
    private final HostPollingOwnership<ComputerManagerListener>
            pollingOwnership = new HostPollingOwnership<>();
    private final InFlightOperationTracker activePolls =
            new InFlightOperationTracker();
    private final HostAdmissionGate hostAdmissionGate =
            new HostAdmissionGate();
    private volatile boolean pollingActive;
    private volatile boolean serviceDestroyed;
    private final Lock defaultNetworkLock = new ReentrantLock();

    private ConnectivityManager.NetworkCallback networkCallback;

    private HostDiscoverySource discoverySource;
    private ExecutorService endpointProbeExecutor;
    private HostReachabilityCoordinator reachabilityCoordinator;

    private boolean runPoll(
            ComputerDetails target,
            PollingTuple registeredTuple,
            boolean newPc,
            int offlineCount) throws InterruptedException {
        HostRepositoryLeaseManager.Lease repositoryLease =
                acquireRepositoryLease();
        if (repositoryLease == null) {
            return false;
        }

        try (InFlightOperationTracker.Lease ignoredOperation =
                        activePolls.begin();
                HostRepositoryLeaseManager.Lease ignoredRepository =
                        repositoryLease) {
            ComputerDetails observation;
            int pollTriesBeforeOffline;
            if (registeredTuple == null) {
                observation = ComputerDetailsSnapshot.copyOf(target);
                pollTriesBeforeOffline =
                        target.state == ComputerDetails.State.UNKNOWN ?
                                INITIAL_POLL_TRIES : OFFLINE_POLL_TRIES;
            }
            else {
                synchronized (registeredTuple.stateLock) {
                    if (!isRegistered(registeredTuple)) {
                        return false;
                    }
                    observation = ComputerDetailsSnapshot.copyOf(target);
                    pollTriesBeforeOffline =
                            target.state == ComputerDetails.State.UNKNOWN ?
                                    INITIAL_POLL_TRIES : OFFLINE_POLL_TRIES;
                }
            }

            // Poll the machine
            if (!pollComputer(observation)) {
                if (!newPc && offlineCount < pollTriesBeforeOffline) {
                    // Return without calling the listener
                    return false;
                }

                observation.state = ComputerDetails.State.OFFLINE;
            }

            Object stateLock = registeredTuple == null ?
                    target : registeredTuple.stateLock;
            ComputerDetails published;
            synchronized (stateLock) {
                if (registeredTuple != null &&
                        !isRegistered(registeredTuple)) {
                    return false;
                }

                // If it's online, update our persistent state
                if (observation.state == ComputerDetails.State.ONLINE) {
                    ComputerDetails existingComputer =
                            dbManager.getComputerByUUID(observation.uuid);

                    // Check if it's in the database because it could have been
                    // removed after this was issued
                    if (!newPc && existingComputer == null) {
                        return false;
                    }

                    // Preserve endpoints absent from a partial observation.
                    if (existingComputer != null) {
                        LegacyComputerDetailsMergePolicy.mergeObservation(
                                existingComputer,
                                observation);
                        dbManager.updateComputerMetadata(existingComputer);
                        LegacyComputerDetailsMergePolicy.mergeObservation(
                                target,
                                existingComputer);
                    }
                    else {
                        try {
                            // Populate a guessed external endpoint only for a
                            // newly discovered local host.
                            if (observation.remoteAddress == null) {
                                InetAddress addr = InetAddress.getByName(
                                        observation.activeAddress.address);
                                if (addr.isSiteLocalAddress()) {
                                    populateExternalAddress(observation);
                                }
                            }
                        }
                        catch (UnknownHostException ignoredError) {
                        }

                        dbManager.updateComputerMetadata(observation);
                        LegacyComputerDetailsMergePolicy.mergeObservation(
                                target,
                                observation);
                    }
                }
                else {
                    LegacyComputerDetailsMergePolicy.mergeObservation(
                            target,
                            observation);
                }

                published = ComputerDetailsSnapshot.copyOf(target);
            }

            // Don't call the listener if this is a failed lookup of a new PC
            if (!newPc || published.state == ComputerDetails.State.ONLINE) {
                notifyComputerUpdated(published);
            }

            return true;
        }
    }

    private boolean runPoll(
            PollingTuple tuple,
            int offlineCount) throws InterruptedException {
        return runPoll(
                tuple.computer,
                tuple,
                false,
                offlineCount);
    }

    private boolean runNewComputerPoll(
            ComputerDetails details) throws InterruptedException {
        return runPoll(details, null, true, 0);
    }

    private Thread createPollingThread(final PollingTuple tuple) {
        Thread t = new Thread() {
            @Override
            public void run() {

                int offlineCount = 0;
                while (!isInterrupted() && pollingActive && tuple.thread == this) {
                    try {
                        // Only allow one request to the machine at a time
                        tuple.networkLock.lockInterruptibly();
                        try {
                            // stopPolling() may have invalidated this worker
                            // while it was waiting for an app-list request.
                            if (!isPollingThreadCurrent(tuple, this)) {
                                break;
                            }
                            // Check if this poll has modified the details
                            if (!runPoll(tuple, offlineCount)) {
                                LimeLog.warning("Host is offline (attempt " + offlineCount + ")");
                                offlineCount++;
                            } else {
                                tuple.lastSuccessfulPollMs = SystemClock.elapsedRealtime();
                                offlineCount = 0;
                            }
                        }
                        finally {
                            tuple.networkLock.unlock();
                        }

                        // Wait until the next polling interval
                        Thread.sleep(SERVERINFO_POLLING_PERIOD_MS);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        t.setName("Host polling");
        return t;
    }

    public class ComputerManagerBinder extends Binder {
        /**
         * Pins the repository for one pairing transaction. The returned
         * session must be closed after the remote pair/persist boundary.
         */
        public HostCredentialWriteSession openHostCredentialWriteSession(
                HostId hostId) {
            HostRepositoryLeaseManager.Lease repositoryLease =
                    acquireRepositoryLease();
            return repositoryLease == null ? null :
                    new HostCredentialWriteSession(hostId, repositoryLease);
        }

        public HostPollingSubscription startPolling(
                ComputerManagerListener listener) {
            HostPollingOwnership.Token ownerToken;
            synchronized (pollingLifecycleLock) {
                if (serviceDestroyed) {
                    throw new IllegalStateException(
                            "Computer manager service is destroyed");
                }
                ownerToken = pollingOwnership.replace(listener);
                if (!pollingActive) {
                    try {
                        discoverySource.start(MDNS_QUERY_PERIOD_MS);
                        pollingActive = true;
                    }
                    catch (RuntimeException | Error error) {
                        pollingOwnership.release(ownerToken);
                        throw error;
                    }
                }
            }

            try {
                initializePolling(ownerToken);
                return new HostPollingSubscription(ownerToken);
            }
            catch (RuntimeException | Error error) {
                stopPolling(ownerToken);
                throw error;
            }
        }

        public boolean waitForReady() {
            try {
                discoverySource.awaitReady();
                return true;
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return false;
            }
            catch (IllegalStateException error) {
                // Service destruction closes the discovery source and wakes
                // any client still waiting for its binding.
                return false;
            }
        }

        public void waitForPollingStopped() {
            try {
                activePolls.awaitIdle();
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }

        public boolean addComputerBlocking(ComputerDetails fakeDetails) throws InterruptedException {
            return ComputerManagerService.this.addComputerBlocking(fakeDetails);
        }

        public void removeComputer(ComputerDetails computer) {
            ComputerManagerService.this.removeComputer(computer);
        }

        public String getUniqueId() {
            return idManager.getUniqueId();
        }

        public ComputerDetails getComputer(String uuid) {
            PollingTuple match = null;
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (sameHostIdentity(uuid, tuple.computer.uuid)) {
                        match = tuple;
                        break;
                    }
                }
            }
            if (match == null) {
                return null;
            }
            synchronized (match.stateLock) {
                return ComputerDetailsSnapshot.copyOf(match.computer);
            }
        }

        public ComputerDetails getComputerByName(String name) {
            PollingTuple match = null;
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (Objects.equals(name, tuple.computer.name)) {
                        match = tuple;
                        break;
                    }
                }
            }
            if (match == null) {
                return null;
            }
            synchronized (match.stateLock) {
                return ComputerDetailsSnapshot.copyOf(match.computer);
            }
        }

        public int getComputerCount() {
            synchronized (pollingTuples) {
                return pollingTuples.size();
            }
        }

        public void invalidateStateForComputer(String uuid) {
            invalidateStateForComputer(HostId.of(uuid));
        }

        public void invalidateStateForComputer(HostId hostId) {
            PollingTuple match = null;
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (hostId.equals(HostId.of(
                            tuple.computer.uuid))) {
                        match = tuple;
                        break;
                    }
                }
            }
            if (match != null) {
                // Order invalidation after any request already using this
                // host. Otherwise a late server-info result can immediately
                // overwrite UNKNOWN and suppress the requested refresh.
                try {
                    match.networkLock.lockInterruptibly();
                }
                catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    synchronized (match.stateLock) {
                        if (isRegistered(match)) {
                            match.computer.state =
                                    ComputerDetails.State.UNKNOWN;
                        }
                    }
                }
                finally {
                    match.networkLock.unlock();
                }
            }
        }
    }

    private void initializePolling(
            HostPollingOwnership.Token ownerToken) {
        List<PollingTuple> tuples;
        synchronized (pollingTuples) {
            tuples = new ArrayList<>(pollingTuples);
        }
        for (PollingTuple tuple : tuples) {
            ComputerDetails initialSnapshot;
            synchronized (tuple.stateLock) {
                if (SystemClock.elapsedRealtime() -
                        tuple.lastSuccessfulPollMs > POLL_DATA_TTL_MS) {
                    LimeLog.info("Timing out stale host state");
                    tuple.computer.state = ComputerDetails.State.UNKNOWN;
                }
                initialSnapshot = ComputerDetailsSnapshot.copyOf(
                        tuple.computer);
            }

            notifyComputerUpdated(ownerToken, initialSnapshot);

            synchronized (pollingTuples) {
                // This tuple or owner may have changed during its callback.
                if (pollingOwnership.owns(ownerToken) &&
                        pollingTuples.contains(tuple) &&
                        tuple.thread == null) {
                    tuple.thread = createPollingThread(tuple);
                    tuple.thread.start();
                }
            }
        }
    }

    public final class HostPollingSubscription implements AutoCloseable {
        private final HostPollingOwnership.Token ownerToken;
        private ApplistPoller appListPoller;
        private boolean closed;

        private HostPollingSubscription(
                HostPollingOwnership.Token ownerToken) {
            this.ownerToken = ownerToken;
        }

        /**
         * Creates the optional app-list worker as a child of this host
         * subscription. Closing the parent always closes the child first.
         */
        public synchronized ApplistPoller startAppListPolling(
                ComputerDetails computer) {
            if (closed) {
                throw new IllegalStateException(
                        "Host polling subscription is closed");
            }
            if (appListPoller != null) {
                throw new IllegalStateException(
                        "App-list polling is already active");
            }
            synchronized (pollingLifecycleLock) {
                if (!pollingOwnership.owns(ownerToken)) {
                    return null;
                }
                ApplistPoller poller = new ApplistPoller(computer);
                appListPoller = poller;
                poller.start();
                return poller;
            }
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (appListPoller != null) {
                appListPoller.stop();
                appListPoller = null;
            }
            stopPolling(ownerToken);
        }
    }

    private void stopPolling(HostPollingOwnership.Token ownerToken) {
        synchronized (pollingLifecycleLock) {
            if (!pollingOwnership.release(ownerToken)) {
                return;
            }
            stopPollingInfrastructure();
        }
    }

    private void stopPollingInfrastructure() {
        pollingActive = false;
        synchronized (pollingTuples) {
            for (PollingTuple tuple : pollingTuples) {
                if (tuple.thread != null) {
                    tuple.thread.interrupt();
                    tuple.thread = null;
                }
            }
        }
        try {
            discoverySource.stop();
        }
        catch (RuntimeException error) {
            LimeLog.warning("Unable to stop host discovery cleanly");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopAllPolling();

        return false;
    }

    private void stopAllPolling() {
        synchronized (pollingLifecycleLock) {
            pollingOwnership.clear();
            stopPollingInfrastructure();
        }
    }

    private void populateExternalAddress(ComputerDetails details) {
        boolean boundToNetwork = false;
        boolean activeNetworkIsVpn = NetHelper.isActiveNetworkVpn(this);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (activeNetworkIsVpn) {
            defaultNetworkLock.lock();
        }

        try {
            // When a VPN is active, bind STUN to a non-VPN network or skip it.
            if (activeNetworkIsVpn) {
                Network[] networks = connMgr.getAllNetworks();
                for (Network net : networks) {
                    NetworkCapabilities netCaps =
                            connMgr.getNetworkCapabilities(net);
                    if (netCaps != null &&
                            !netCaps.hasTransport(
                                    NetworkCapabilities.TRANSPORT_CELLULAR) &&
                            !netCaps.hasTransport(
                                    NetworkCapabilities.TRANSPORT_VPN)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (connMgr.bindProcessToNetwork(net)) {
                                boundToNetwork = true;
                                break;
                            }
                        }
                        else if (ConnectivityManager
                                .setProcessDefaultNetwork(net)) {
                            boundToNetwork = true;
                            break;
                        }
                    }
                }
            }

            if (!activeNetworkIsVpn || boundToNetwork) {
                String resolvedAddress =
                        NvConnection.findExternalAddressForMdns(
                                "stun.moonlight-stream.org",
                                3478);
                if (resolvedAddress != null) {
                    details.remoteAddress =
                            new ComputerDetails.AddressTuple(
                                    resolvedAddress,
                                    details.guessExternalPort());
                }
            }
        }
        finally {
            if (boundToNetwork) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connMgr.bindProcessToNetwork(null);
                }
                else {
                    ConnectivityManager.setProcessDefaultNetwork(null);
                }
            }
            if (activeNetworkIsVpn) {
                defaultNetworkLock.unlock();
            }
        }
    }

    private void handleDiscoveredHost(HostDiscoveryCandidate candidate) {
        ComputerDetails details = new ComputerDetails();
        HostEndpoint localIpv4 = candidate.getEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4);
        if (localIpv4 != null) {
            details.localAddress = new ComputerDetails.AddressTuple(
                    localIpv4.getAddress(),
                    localIpv4.getPort());
            populateExternalAddress(details);
        }
        HostEndpoint localIpv6 = candidate.getEndpoint(
                HostEndpoint.Kind.LOCAL_IPV6);
        if (localIpv6 != null) {
            details.ipv6Address = new ComputerDetails.AddressTuple(
                    localIpv6.getAddress(),
                    localIpv6.getPort());
        }

        try {
            if (!addComputerBlocking(details)) {
                LimeLog.warning("Auto-discovered host failed to respond");
            }
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    public final class HostCredentialWriteSession implements AutoCloseable {
        private final Object stateLock = new Object();
        private final HostId hostId;
        private HostRepositoryLeaseManager.Lease repositoryLease;

        private HostCredentialWriteSession(
                HostId hostId,
                HostRepositoryLeaseManager.Lease repositoryLease) {
            this.hostId = Objects.requireNonNull(
                    hostId,
                    "hostId");
            this.repositoryLease = Objects.requireNonNull(
                    repositoryLease,
                    "repositoryLease");
        }

        public boolean updatePinnedCertificate(
                X509Certificate certificate) {
            synchronized (stateLock) {
                if (repositoryLease == null) {
                    return false;
                }
                PollingTuple match = null;
                synchronized (pollingTuples) {
                    for (PollingTuple tuple : pollingTuples) {
                        if (!hostId.equals(HostId.of(
                                tuple.computer.uuid))) {
                            continue;
                        }
                        match = tuple;
                        break;
                    }
                }
                if (match == null) {
                    return false;
                }
                synchronized (match.stateLock) {
                    if (!isRegistered(match)) {
                        return false;
                    }
                    dbManager.updatePinnedCertificate(
                            match.computer.uuid,
                            certificate);
                    match.computer.serverCert = certificate;
                    return true;
                }
            }
        }

        @Override
        public void close() {
            synchronized (stateLock) {
                if (repositoryLease == null) {
                    return;
                }
                repositoryLease.close();
                repositoryLease = null;
            }
        }
    }

    private boolean isRegistered(PollingTuple tuple) {
        synchronized (pollingTuples) {
            return pollingTuples.contains(tuple);
        }
    }

    private PollingTuple findPollingTuple(String uuid) {
        if (uuid == null) {
            return null;
        }
        synchronized (pollingTuples) {
            for (PollingTuple tuple : pollingTuples) {
                if (sameHostIdentity(uuid, tuple.computer.uuid)) {
                    return tuple;
                }
            }
        }
        return null;
    }

    private boolean isPollingThreadCurrent(
            PollingTuple tuple,
            Thread worker) {
        return pollingActive &&
                tuple.thread == worker &&
                isRegistered(tuple);
    }

    private void notifyComputerUpdated(ComputerDetails details) {
        ComputerManagerListener currentListener =
                pollingOwnership.getListener();
        if (currentListener != null) {
            currentListener.notifyComputerUpdated(
                    ComputerDetailsSnapshot.copyOf(details));
        }
    }

    private void notifyComputerUpdated(
            HostPollingOwnership.Token ownerToken,
            ComputerDetails details) {
        ComputerManagerListener currentListener =
                pollingOwnership.getListener(ownerToken);
        if (currentListener != null) {
            currentListener.notifyComputerUpdated(
                    ComputerDetailsSnapshot.copyOf(details));
        }
    }

    private void addTuple(ComputerDetails details) {
        PollingTuple tuple = null;
        Thread threadToStart = null;
        boolean created = false;
        synchronized (pollingTuples) {
            for (PollingTuple candidate : pollingTuples) {
                // Check if this is the same computer
                if (sameHostIdentity(
                        candidate.computer.uuid,
                        details.uuid)) {
                    tuple = candidate;
                    break;
                }
            }

            if (tuple == null) {
                tuple = new PollingTuple(
                        ComputerDetailsSnapshot.copyOf(details),
                        null);
                pollingTuples.add(tuple);
                created = true;
            }
        }

        if (!created) {
            synchronized (tuple.stateLock) {
                if (!isRegistered(tuple)) {
                    return;
                }
                LegacyComputerDetailsMergePolicy.mergeObservation(
                        tuple.computer,
                        details);
            }
        }

        synchronized (pollingTuples) {
            if (pollingActive &&
                    pollingTuples.contains(tuple) &&
                    tuple.thread == null) {
                tuple.thread = createPollingThread(tuple);
                threadToStart = tuple.thread;
            }
        }
        if (threadToStart != null) {
            threadToStart.start();
        }
    }

    public boolean addComputerBlocking(ComputerDetails candidate)
            throws InterruptedException {
        Objects.requireNonNull(candidate, "candidate");
        HostAdmissionGate.Lease admission;
        try {
            admission = hostAdmissionGate.acquire();
        }
        catch (IllegalStateException error) {
            // Service teardown closes the admission owner before disposing its
            // discovery and repository dependencies.
            return false;
        }
        try (HostAdmissionGate.Lease ignoredAdmission = admission;
                InFlightOperationTracker.Lease ignoredOperation =
                        activePolls.begin()) {
            if (serviceDestroyed) {
                return false;
            }
            return addComputerSerialized(candidate);
        }
    }

    private boolean addComputerSerialized(ComputerDetails candidate)
            throws InterruptedException {
        // The first probe resolves stable identity without persisting anything;
        // no pinned certificate is available at this boundary yet.
        if (!pollComputer(candidate)) {
            return false;
        }

        PollingTuple match = findPollingTuple(candidate.uuid);
        if (match == null) {
            return finishComputerAdmission(candidate);
        }

        // Once identity is known, serialize the credential-aware probe with
        // all server-info and app-list traffic for the existing host.
        match.networkLock.lockInterruptibly();
        try {
            synchronized (match.stateLock) {
                if (!isRegistered(match)) {
                    return finishComputerAdmission(candidate);
                }
                candidate.serverCert = match.computer.serverCert;
            }
            return finishComputerAdmission(candidate);
        }
        finally {
            match.networkLock.unlock();
        }
    }

    private boolean finishComputerAdmission(ComputerDetails candidate)
            throws InterruptedException {
        // Probe again with the pinned certificate, if one was found, to obtain
        // authoritative pairing state before committing the host record.
        runNewComputerPoll(candidate);
        if (candidate.state != ComputerDetails.State.ONLINE) {
            return false;
        }

        LimeLog.info("New host added");
        addTuple(candidate);
        return true;
    }

    public void removeComputer(ComputerDetails computer) {
        HostRepositoryLeaseManager.Lease repositoryLease =
                acquireRepositoryLease();
        if (repositoryLease == null) {
            return;
        }

        try (HostRepositoryLeaseManager.Lease ignored = repositoryLease) {
            PollingTuple removed = null;
            synchronized (pollingTuples) {
                Iterator<PollingTuple> iterator =
                        pollingTuples.iterator();
                while (iterator.hasNext()) {
                    PollingTuple tuple = iterator.next();
                    if (sameHostIdentity(
                            tuple.computer.uuid,
                            computer.uuid)) {
                        if (tuple.thread != null) {
                            tuple.thread.interrupt();
                            tuple.thread = null;
                        }
                        iterator.remove();
                        removed = tuple;
                        break;
                    }
                }
            }

            if (removed == null) {
                dbManager.deleteComputer(computer);
                return;
            }
            // A poll already committing wins before this delete. A poll that
            // completes later observes that the tuple is no longer registered.
            synchronized (removed.stateLock) {
                dbManager.deleteComputer(removed.computer);
            }
        }
    }

    private HostRepositoryLeaseManager.Lease acquireRepositoryLease() {
        HostRepositoryLeaseManager leases = repositoryLeases;
        return leases == null ? null : leases.tryAcquire();
    }

    private ComputerDetails tryPollIp(ComputerDetails details, ComputerDetails.AddressTuple address) {
        try {
            // If the current address's port number matches the active address's port number, we can also assume
            // the HTTPS port will also match. This assumption is currently safe because Sunshine sets all ports
            // as offsets from the base HTTP port and doesn't allow custom HttpsPort responses for WAN vs LAN.
            boolean portMatchesActiveAddress = details.state == ComputerDetails.State.ONLINE &&
                    details.activeAddress != null && address.port == details.activeAddress.port;

            NvHTTP http = new NvHTTP(address, portMatchesActiveAddress ? details.httpsPort : 0, idManager.getUniqueId(), details.serverCert,
                    PlatformBinding.getCryptoProvider(ComputerManagerService.this));

            // If this PC is currently online at this address, extend the timeouts to allow more time for the PC to respond.
            boolean isLikelyOnline = details.state == ComputerDetails.State.ONLINE && address.equals(details.activeAddress);

            ComputerDetails newDetails = http.getComputerDetails(isLikelyOnline);

            // Check if this is the PC we expected
            if (newDetails.uuid == null) {
                LimeLog.severe("Polling returned no UUID!");
                return null;
            }
            // details.uuid can be null on initial PC add
            else if (details.uuid != null &&
                    !sameHostIdentity(
                            details.uuid,
                            newDetails.uuid)) {
                // We got the wrong PC!
                LimeLog.info("Polling returned the wrong PC!");
                return null;
            }

            return newDetails;
        } catch (XmlPullParserException e) {
            LimeLog.warning("Host poll returned invalid server information");
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isWrongSubnetSiteLocalAddress(ComputerDetails.AddressTuple address) {
        if (address == null) {
            return false;
        }

        try {
            InetAddress targetAddress = InetAddress.getByName(address.address);
            if (!(targetAddress instanceof Inet4Address) || !targetAddress.isSiteLocalAddress()) {
                return false;
            }

            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress ifaceAddress : iface.getInterfaceAddresses()) {
                    if (!(ifaceAddress.getAddress() instanceof Inet4Address) ||
                            !ifaceAddress.getAddress().isSiteLocalAddress()) {
                        continue;
                    }

                    if (Ipv4SubnetMatcher.isSameSubnet(
                            targetAddress.getAddress(),
                            ifaceAddress.getAddress().getAddress(),
                            ifaceAddress.getNetworkPrefixLength())) {
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            // Some Android builds throw unexpected exceptions while enumerating interfaces.
            LimeLog.warning("Unable to evaluate local host subnet");
            return false;
        }
    }

    private static boolean sameHostIdentity(
            String expected,
            String actual) {
        try {
            return HostId.of(expected).equals(HostId.of(actual));
        }
        catch (IllegalArgumentException error) {
            // Preserve exact matching for malformed identifiers already in a
            // legacy database without allowing them to match another host.
            return Objects.equals(expected, actual);
        }
    }

    private ComputerDetails parallelPollPc(ComputerDetails details) throws InterruptedException {
        boolean preferExternalAddress =
                details.manualAddress != null && isWrongSubnetSiteLocalAddress(details.localAddress);
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                toHostEndpoint(
                        HostEndpoint.Kind.LOCAL_IPV4,
                        details.localAddress),
                toHostEndpoint(
                        HostEndpoint.Kind.MANUAL,
                        details.manualAddress),
                toHostEndpoint(
                        HostEndpoint.Kind.REMOTE,
                        details.remoteAddress),
                toHostEndpoint(
                        HostEndpoint.Kind.LOCAL_IPV6,
                        details.ipv6Address),
                preferExternalAddress);
        HostReachabilityCoordinator.Result<ComputerDetails> result =
                reachabilityCoordinator.probe(
                        plan,
                        endpoint -> pollEndpoint(details, endpoint));
        return result == null ? null : result.getValue();
    }

    private ComputerDetails pollEndpoint(
            ComputerDetails existingDetails,
            HostEndpoint endpoint) {
        ComputerDetails.AddressTuple address = getLegacyAddress(
                existingDetails,
                endpoint.getKind());
        if (address == null) {
            return null;
        }
        ComputerDetails returnedDetails = tryPollIp(
                existingDetails,
                address);
        if (returnedDetails != null) {
            returnedDetails.activeAddress = address;
        }
        return returnedDetails;
    }

    private static HostEndpoint toHostEndpoint(
            HostEndpoint.Kind kind,
            ComputerDetails.AddressTuple address) {
        if (address == null) {
            return null;
        }
        try {
            return new HostEndpoint(
                    kind,
                    address.address,
                    address.port);
        }
        catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static ComputerDetails.AddressTuple getLegacyAddress(
            ComputerDetails details,
            HostEndpoint.Kind kind) {
        switch (kind) {
            case LOCAL_IPV4:
                return details.localAddress;
            case LOCAL_IPV6:
                return details.ipv6Address;
            case REMOTE:
                return details.remoteAddress;
            case MANUAL:
                return details.manualAddress;
            default:
                throw new AssertionError("Unhandled endpoint kind");
        }
    }

    private boolean pollComputer(ComputerDetails details) throws InterruptedException {
        // Poll all addresses in parallel to speed up the process
        LimeLog.info("Starting host reachability poll");
        ComputerDetails polledDetails = parallelPollPc(details);
        LimeLog.info("Host reachability poll completed: " +
                (polledDetails == null ? "offline" : "online"));

        if (polledDetails != null) {
            LegacyComputerDetailsMergePolicy.mergeObservation(
                    details,
                    polledDetails);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        endpointProbeExecutor = createEndpointProbeExecutor();
        reachabilityCoordinator = new HostReachabilityCoordinator(
                endpointProbeExecutor,
                SystemClock::elapsedRealtime,
                ADDRESS_UPGRADE_GRACE_MS);
        discoverySource = new AndroidMdnsDiscoverySource(
                this,
                new HostDiscoverySource.Listener() {
                    @Override
                    public void onHostDiscovered(
                            HostDiscoveryCandidate candidate) {
                        handleDiscoveredHost(candidate);
                    }

                    @Override
                    public void onDiscoveryFailure() {
                        LimeLog.severe("mDNS discovery failed");
                    }
                });

        // Lookup or generate this device's UID
        idManager = new IdentityManager(this);

        // Initialize the DB
        dbManager = new ComputerDatabaseManager(this);
        repositoryLeases = new HostRepositoryLeaseManager(dbManager::close);

        // Grab known machines into our computer list
        HostRepositoryLeaseManager.Lease repositoryLease =
                acquireRepositoryLease();
        if (repositoryLease == null) {
            return;
        }
        try (HostRepositoryLeaseManager.Lease ignored = repositoryLease) {
            for (ComputerDetails computer : dbManager.getAllComputers()) {
                // Add tuples for each computer
                addTuple(computer);
            }
        }

        // Monitor for network changes to invalidate our PC state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    LimeLog.info("Resetting PC state for new available network");
                    updateAllHostStates(ComputerDetails.State.UNKNOWN);
                }

                @Override
                public void onLost(Network network) {
                    LimeLog.info("Offlining PCs due to network loss");
                    updateAllHostStates(ComputerDetails.State.OFFLINE);
                }
            };

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connMgr.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    public void onDestroy() {
        serviceDestroyed = true;
        hostAdmissionGate.close();
        try {
            stopAllPolling();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    networkCallback != null) {
                ConnectivityManager connMgr =
                        (ConnectivityManager) getSystemService(
                                Context.CONNECTIVITY_SERVICE);
                try {
                    connMgr.unregisterNetworkCallback(networkCallback);
                }
                catch (RuntimeException error) {
                    LimeLog.warning(
                            "Unable to unregister host network callback cleanly");
                }
                networkCallback = null;
            }

            if (discoverySource != null) {
                try {
                    discoverySource.close();
                }
                catch (RuntimeException error) {
                    LimeLog.warning(
                            "Unable to close host discovery cleanly");
                }
                discoverySource = null;
            }
            if (endpointProbeExecutor != null) {
                try {
                    endpointProbeExecutor.shutdownNow();
                }
                catch (RuntimeException error) {
                    LimeLog.warning(
                            "Unable to stop endpoint probing cleanly");
                }
                endpointProbeExecutor = null;
            }

            HostRepositoryLeaseManager leases = repositoryLeases;
            repositoryLeases = null;
            if (leases != null) {
                try {
                    leases.close();
                }
                catch (RuntimeException error) {
                    LimeLog.warning(
                            "Unable to close host repository cleanly");
                }
            }
        }
        finally {
            super.onDestroy();
        }
    }

    private void updateAllHostStates(ComputerDetails.State state) {
        List<PollingTuple> tuples;
        synchronized (pollingTuples) {
            tuples = new ArrayList<>(pollingTuples);
        }
        List<ComputerDetails> updates = new ArrayList<>(tuples.size());
        for (PollingTuple tuple : tuples) {
            synchronized (tuple.stateLock) {
                if (!isRegistered(tuple)) {
                    continue;
                }
                tuple.computer.state = state;
                updates.add(ComputerDetailsSnapshot.copyOf(
                        tuple.computer));
            }
        }
        for (ComputerDetails update : updates) {
            notifyComputerUpdated(update);
        }
    }

    private static ExecutorService createEndpointProbeExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                ENDPOINT_PROBE_THREADS,
                ENDPOINT_PROBE_THREADS,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(
                        ENDPOINT_PROBE_QUEUE_CAPACITY),
                command -> new Thread(
                        command,
                        "Host endpoint probe " +
                                endpointProbeThreadId.incrementAndGet()),
                (command, owner) -> {
                    if (owner.isShutdown()) {
                        throw new RejectedExecutionException(
                                "Host endpoint probing is shut down");
                    }
                    command.run();
                });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class ApplistPoller {
        private final Object lifecycleLock = new Object();
        private volatile Thread thread;
        private final ComputerDetails computer;
        private final Object pollEvent = new Object();
        private boolean receivedAppList = false;
        private boolean closed;

        private ApplistPoller(ComputerDetails computer) {
            this.computer = ComputerDetailsSnapshot.copyOf(computer);
        }

        public void pollNow() {
            synchronized (lifecycleLock) {
                if (closed) {
                    return;
                }
            }
            synchronized (pollEvent) {
                pollEvent.notify();
            }
        }

        private boolean waitPollingDelay(Thread ownerThread) {
            try {
                synchronized (pollEvent) {
                    if (receivedAppList) {
                        // If we've already reported an app list successfully,
                        // wait the full polling period
                        pollEvent.wait(APPLIST_POLLING_PERIOD_MS);
                    }
                    else {
                        // If we've failed to get an app list so far, retry much earlier
                        pollEvent.wait(APPLIST_FAILED_POLLING_RETRY_MS);
                    }
                }
            } catch (InterruptedException e) {
                return false;
            }

            return thread == ownerThread &&
                    !ownerThread.isInterrupted();
        }

        private PollingTuple getPollingTuple() {
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (sameHostIdentity(
                            computer.uuid,
                            tuple.computer.uuid)) {
                        return tuple;
                    }
                }
            }

            return null;
        }

        private void start() {
            Thread worker = new Thread() {
                @Override
                public void run() {
                    int emptyAppListResponses = 0;
                    do {
                        if (thread != this || isInterrupted()) {
                            break;
                        }
                        PollingTuple tuple = getPollingTuple();
                        if (tuple == null) {
                            continue;
                        }
                        ComputerDetails currentComputer;
                        synchronized (tuple.stateLock) {
                            if (!isRegistered(tuple)) {
                                continue;
                            }
                            currentComputer =
                                    ComputerDetailsSnapshot.copyOf(
                                            tuple.computer);
                        }

                        // Can't poll if it's not online or paired
                        if (currentComputer.state != ComputerDetails.State.ONLINE ||
                                currentComputer.pairState != PairingManager.PairState.PAIRED) {
                            notifyComputerUpdated(currentComputer);
                            continue;
                        }

                        // Can't poll if there's no UUID yet
                        if (currentComputer.uuid == null) {
                            continue;
                        }

                        try {
                            NvHTTP http =
                                    AndroidNvHttpClientFactory.create(
                                            ComputerManagerService.this,
                                            currentComputer,
                                            idManager.getUniqueId());

                            String appList;
                            if (tuple != null) {
                                // If we're polling this machine too, grab the network lock
                                // while doing the app list request to prevent other requests
                                // from being issued in the meantime.
                                tuple.networkLock.lockInterruptibly();
                                try {
                                    appList = http.getAppListRaw();
                                }
                                finally {
                                    tuple.networkLock.unlock();
                                }
                            }
                            else {
                                // No polling is happening now, so we just call it directly
                                appList = http.getAppListRaw();
                            }

                            List<NvApp> list = NvHTTP.getAppListByReader(new StringReader(appList));
                            if (list.isEmpty()) {
                                LimeLog.warning("Empty app list received from host");

                                // The app list might actually be empty, so if we get an empty response a few times
                                // in a row, we'll go ahead and believe it.
                                emptyAppListResponses++;
                            }
                            if (!appList.isEmpty() &&
                                    (!list.isEmpty() || emptyAppListResponses >= EMPTY_LIST_THRESHOLD)) {
                                // Open the cache file
                                try (final OutputStream cacheOut = CacheHelper.openCacheFileForOutput(
                                        getCacheDir(), "applist", currentComputer.uuid)
                                ) {
                                    CacheHelper.writeStringToOutputStream(cacheOut, appList);
                                } catch (IOException e) {
                                    LimeLog.warning("Unable to cache host app list");
                                }

                                // Reset empty count if it wasn't empty this time
                                if (!list.isEmpty()) {
                                    emptyAppListResponses = 0;
                                }

                                ComputerDetails update;
                                synchronized (tuple.stateLock) {
                                    if (!isRegistered(tuple)) {
                                        continue;
                                    }
                                    tuple.computer.rawAppList = appList;
                                    update = ComputerDetailsSnapshot.copyOf(
                                            tuple.computer);
                                }
                                receivedAppList = true;

                                // Notify that the app list has been updated
                                // and ensure that the thread is still active
                                if (thread == this) {
                                    notifyComputerUpdated(update);
                                }
                            }
                            else if (appList.isEmpty()) {
                                LimeLog.warning("Null app list received from host");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (IOException e) {
                            LimeLog.warning("Unable to retrieve host app list");
                        } catch (XmlPullParserException e) {
                            LimeLog.warning("Host returned an invalid app list");
                        }
                    } while (waitPollingDelay(this));
                }
            };
            worker.setName("Host app-list polling");
            synchronized (lifecycleLock) {
                if (thread != null || closed) {
                    return;
                }
                thread = worker;
                worker.start();
            }
        }

        public void stop() {
            Thread worker;
            synchronized (lifecycleLock) {
                closed = true;
                worker = thread;
                thread = null;
            }
            if (worker != null) {
                // Don't join here because we might be blocked on network I/O.
                worker.interrupt();
            }
        }
    }
}

class PollingTuple {
    public volatile Thread thread;
    public final ComputerDetails computer;
    public final Lock networkLock;
    public final Object stateLock;
    public volatile long lastSuccessfulPollMs;

    public PollingTuple(ComputerDetails computer, Thread thread) {
        this.computer = computer;
        this.thread = thread;
        this.networkLock = new ReentrantLock(true);
        this.stateLock = new Object();
    }
}

class ReachabilityTuple {
    public final String reachableAddress;
    public final ComputerDetails computer;

    public ReachabilityTuple(ComputerDetails computer, String reachableAddress) {
        this.computer = computer;
        this.reachableAddress = reachableAddress;
    }
}
