package com.limelight.computers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InterfaceAddress;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.limelight.LimeLog;
import com.limelight.binding.PlatformBinding;
import com.limelight.computers.discovery.AndroidMdnsDiscoverySource;
import com.limelight.computers.discovery.HostDiscoveryCandidate;
import com.limelight.computers.discovery.HostDiscoverySource;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.NetHelper;
import com.limelight.utils.ServerHelper;

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

    private final ComputerManagerBinder binder = new ComputerManagerBinder();

    private ComputerDatabaseManager dbManager;
    private final AtomicInteger dbRefCount = new AtomicInteger(0);

    private IdentityManager idManager;
    private final LinkedList<PollingTuple> pollingTuples = new LinkedList<>();
    private ComputerManagerListener listener = null;
    private final AtomicInteger activePolls = new AtomicInteger(0);
    private boolean pollingActive = false;
    private final Lock defaultNetworkLock = new ReentrantLock();

    private ConnectivityManager.NetworkCallback networkCallback;

    private HostDiscoverySource discoverySource;

    // Returns true if the details object was modified
    private boolean runPoll(ComputerDetails details, boolean newPc, int offlineCount) throws InterruptedException {
        if (!getLocalDatabaseReference()) {
            return false;
        }

        final int pollTriesBeforeOffline = details.state == ComputerDetails.State.UNKNOWN ?
                INITIAL_POLL_TRIES : OFFLINE_POLL_TRIES;

        activePolls.incrementAndGet();

        // Poll the machine
        try {
            if (!pollComputer(details)) {
                if (!newPc && offlineCount < pollTriesBeforeOffline) {
                    // Return without calling the listener
                    releaseLocalDatabaseReference();
                    return false;
                }

                details.state = ComputerDetails.State.OFFLINE;
            }
        } catch (InterruptedException e) {
            releaseLocalDatabaseReference();
            throw e;
        } finally {
            activePolls.decrementAndGet();
        }

        // If it's online, update our persistent state
        if (details.state == ComputerDetails.State.ONLINE) {
            ComputerDetails existingComputer = dbManager.getComputerByUUID(details.uuid);

            // Check if it's in the database because it could have been
            // removed after this was issued
            if (!newPc && existingComputer == null) {
                // It's gone
                releaseLocalDatabaseReference();
                return false;
            }

            // If we already have an entry for this computer in the DB, we must
            // combine the existing data with this new data (which may be partially available
            // due to detecting the PC via mDNS) without the saved external address. If we
            // write to the DB without doing this first, we can overwrite our existing data.
            if (existingComputer != null) {
                LegacyComputerDetailsMergePolicy.mergeObservation(
                        existingComputer,
                        details);
                dbManager.updateComputerMetadata(existingComputer);
            }
            else {
                try {
                    // If the active address is a site-local address (RFC 1918),
                    // then use STUN to populate the external address field if
                    // it's not set already.
                    if (details.remoteAddress == null) {
                        InetAddress addr = InetAddress.getByName(details.activeAddress.address);
                        if (addr.isSiteLocalAddress()) {
                            populateExternalAddress(details);
                        }
                    }
                } catch (UnknownHostException ignored) {}

                dbManager.updateComputerMetadata(details);
            }
        }

        // Don't call the listener if this is a failed lookup of a new PC
        if ((!newPc || details.state == ComputerDetails.State.ONLINE) && listener != null) {
            listener.notifyComputerUpdated(details);
        }

        releaseLocalDatabaseReference();
        return true;
    }

    private Thread createPollingThread(final PollingTuple tuple) {
        Thread t = new Thread() {
            @Override
            public void run() {

                int offlineCount = 0;
                while (!isInterrupted() && pollingActive && tuple.thread == this) {
                    try {
                        // Only allow one request to the machine at a time
                        synchronized (tuple.networkLock) {
                            // Check if this poll has modified the details
                            if (!runPoll(tuple.computer, false, offlineCount)) {
                                LimeLog.warning("Host is offline (attempt " + offlineCount + ")");
                                offlineCount++;
                            } else {
                                tuple.lastSuccessfulPollMs = SystemClock.elapsedRealtime();
                                offlineCount = 0;
                            }
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
        public void startPolling(ComputerManagerListener listener) {
            // Polling is active
            pollingActive = true;

            // Set the listener
            ComputerManagerService.this.listener = listener;

            // Start mDNS autodiscovery too
            discoverySource.start(MDNS_QUERY_PERIOD_MS);

            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    // Enforce the poll data TTL
                    if (SystemClock.elapsedRealtime() - tuple.lastSuccessfulPollMs > POLL_DATA_TTL_MS) {
                        LimeLog.info("Timing out stale host state");
                        tuple.computer.state = ComputerDetails.State.UNKNOWN;
                    }

                    // Report this computer initially
                    listener.notifyComputerUpdated(tuple.computer);

                    // This polling thread might already be there
                    if (tuple.thread == null) {
                        tuple.thread = createPollingThread(tuple);
                        tuple.thread.start();
                    }
                }
            }
        }

        public void waitForReady() {
            try {
                discoverySource.awaitReady();
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }

        public void waitForPollingStopped() {
            while (activePolls.get() != 0) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // InterruptedException clears the thread's interrupt status. Since we can't
                    // handle that here, we will re-interrupt the thread to set the interrupt
                    // status back to true.
                    Thread.currentThread().interrupt();
                }
            }
        }

        public boolean addComputerBlocking(ComputerDetails fakeDetails) throws InterruptedException {
            return ComputerManagerService.this.addComputerBlocking(fakeDetails);
        }

        public void removeComputer(ComputerDetails computer) {
            ComputerManagerService.this.removeComputer(computer);
        }

        public void stopPolling() {
            // Just call the unbind handler to cleanup
            ComputerManagerService.this.onUnbind(null);
        }

        public ApplistPoller createAppListPoller(ComputerDetails computer) {
            return new ApplistPoller(computer);
        }

        public String getUniqueId() {
            return idManager.getUniqueId();
        }

        public ComputerDetails getComputer(String uuid) {
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (uuid.equals(tuple.computer.uuid)) {
                        return tuple.computer;
                    }
                }
            }

            return null;
        }

        public boolean updatePinnedCertificate(
                String uuid,
                java.security.cert.X509Certificate certificate) {
            if (!getLocalDatabaseReference()) {
                return false;
            }
            try {
                synchronized (pollingTuples) {
                    for (PollingTuple tuple : pollingTuples) {
                        if (!uuid.equals(tuple.computer.uuid)) {
                            continue;
                        }
                        synchronized (tuple.networkLock) {
                            dbManager.updatePinnedCertificate(
                                    uuid,
                                    certificate);
                            tuple.computer.serverCert = certificate;
                            return true;
                        }
                    }
                }
                return false;
            }
            finally {
                releaseLocalDatabaseReference();
            }
        }

        public int getComputerCount() {
            synchronized (pollingTuples) {
                return pollingTuples.size();
            }
        }

        public void invalidateStateForComputer(String uuid) {
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (uuid.equals(tuple.computer.uuid)) {
                        // We need the network lock to prevent a concurrent poll
                        // from wiping this change out
                        synchronized (tuple.networkLock) {
                            tuple.computer.state = ComputerDetails.State.UNKNOWN;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        discoverySource.stop();

        // Stop polling
        pollingActive = false;
        synchronized (pollingTuples) {
            for (PollingTuple tuple : pollingTuples) {
                if (tuple.thread != null) {
                    // Interrupt and remove the thread
                    tuple.thread.interrupt();
                    tuple.thread = null;
                }
            }
        }

        // Remove the listener
        listener = null;

        return false;
    }

    private void populateExternalAddress(ComputerDetails details) {
        boolean boundToNetwork = false;
        boolean activeNetworkIsVpn = NetHelper.isActiveNetworkVpn(this);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check if we're currently connected to a VPN which may send our
        // STUN request from an unexpected interface
        if (activeNetworkIsVpn) {
            // Acquire the default network lock since we could be changing global process state
            defaultNetworkLock.lock();

            // On Lollipop or later, we can bind our process to the underlying interface
            // to ensure our STUN request goes out on that interface or not at all (which is
            // preferable to getting a VPN endpoint address back).
            Network[] networks = connMgr.getAllNetworks();
            for (Network net : networks) {
                NetworkCapabilities netCaps = connMgr.getNetworkCapabilities(net);
                if (netCaps != null) {
                    if (!netCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                            !netCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        // This network looks like an underlying multicast-capable transport,
                        // so let's guess that it's probably where our mDNS response came from.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (connMgr.bindProcessToNetwork(net)) {
                                boundToNetwork = true;
                                break;
                            }
                        } else if (ConnectivityManager.setProcessDefaultNetwork(net)) {
                            boundToNetwork = true;
                            break;
                        }
                    }
                }
            }
        }

        // Perform the STUN request if we're not on a VPN or if we bound to a network
        if (!activeNetworkIsVpn || boundToNetwork) {
            String stunResolvedAddress = NvConnection.findExternalAddressForMdns("stun.moonlight-stream.org", 3478);
            if (stunResolvedAddress != null) {
                // We don't know for sure what the external port is, so we will have to guess.
                // When we contact the PC (if we haven't already), it will update the port.
                details.remoteAddress = new ComputerDetails.AddressTuple(stunResolvedAddress, details.guessExternalPort());
            }
        }

        // Unbind from the network
        if (boundToNetwork) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connMgr.bindProcessToNetwork(null);
            } else {
                ConnectivityManager.setProcessDefaultNetwork(null);
            }
        }

        // Unlock the network state
        if (activeNetworkIsVpn) {
            defaultNetworkLock.unlock();
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

    private void addTuple(ComputerDetails details) {
        synchronized (pollingTuples) {
            for (PollingTuple tuple : pollingTuples) {
                // Check if this is the same computer
                if (tuple.computer.uuid.equals(details.uuid)) {
                    // Update the saved computer with potentially new details
                    LegacyComputerDetailsMergePolicy.mergeObservation(
                            tuple.computer,
                            details);

                    // Start a polling thread if polling is active
                    if (pollingActive && tuple.thread == null) {
                        tuple.thread = createPollingThread(tuple);
                        tuple.thread.start();
                    }

                    // Found an entry so we're done
                    return;
                }
            }

            // If we got here, we didn't find an entry
            PollingTuple tuple = new PollingTuple(details, null);
            if (pollingActive) {
                tuple.thread = createPollingThread(tuple);
            }
            pollingTuples.add(tuple);
            if (tuple.thread != null) {
                tuple.thread.start();
            }
        }
    }

    public boolean addComputerBlocking(ComputerDetails fakeDetails) throws InterruptedException {
        // Block while we try to fill the details

        // We cannot use runPoll() here because it will attempt to persist the state of the machine
        // in the database, which would be bad because we don't have our pinned cert loaded yet.
        if (pollComputer(fakeDetails)) {
            // See if we have record of this PC to pull its pinned cert
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (tuple.computer.uuid.equals(fakeDetails.uuid)) {
                        fakeDetails.serverCert = tuple.computer.serverCert;
                        break;
                    }
                }
            }

            // Poll again, possibly with the pinned cert, to get accurate pairing information.
            // This will insert the host into the database too.
            runPoll(fakeDetails, true, 0);
        }

        // If the machine is reachable, it was successful
        if (fakeDetails.state == ComputerDetails.State.ONLINE) {
            LimeLog.info("New host added");

            // Start a polling thread for this machine
            addTuple(fakeDetails);
            return true;
        }
        else {
            return false;
        }
    }

    public void removeComputer(ComputerDetails computer) {
        if (!getLocalDatabaseReference()) {
            return;
        }

        // Remove it from the database
        dbManager.deleteComputer(computer);

        synchronized (pollingTuples) {
            // Remove the computer from the computer list
            for (PollingTuple tuple : pollingTuples) {
                if (tuple.computer.uuid.equals(computer.uuid)) {
                    if (tuple.thread != null) {
                        // Interrupt the thread on this entry
                        tuple.thread.interrupt();
                        tuple.thread = null;
                    }
                    pollingTuples.remove(tuple);
                    break;
                }
            }
        }

        releaseLocalDatabaseReference();
    }

    private boolean getLocalDatabaseReference() {
        if (dbRefCount.get() == 0) {
            return false;
        }

        dbRefCount.incrementAndGet();
        return true;
    }

    private void releaseLocalDatabaseReference() {
        if (dbRefCount.decrementAndGet() == 0) {
            dbManager.close();
        }
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
            else if (details.uuid != null && !details.uuid.equals(newDetails.uuid)) {
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

    private static class ParallelPollTuple {
        public ComputerDetails.AddressTuple address;
        public ComputerDetails existingDetails;

        public volatile boolean complete;
        public Thread pollingThread;
        public volatile ComputerDetails returnedDetails;

        public ParallelPollTuple(ComputerDetails.AddressTuple address, ComputerDetails existingDetails) {
            this.address = address;
            this.existingDetails = existingDetails;
        }

        public void interrupt() {
            if (pollingThread != null) {
                pollingThread.interrupt();
            }
        }
    }

    private void startParallelPollThread(ParallelPollTuple tuple, HashSet<ComputerDetails.AddressTuple> uniqueAddresses,
                                         final Object completionLock) {
        // Don't bother starting a polling thread for an address that doesn't exist
        // or if the address has already been polled with an earlier tuple
        if (tuple.address == null || !uniqueAddresses.add(tuple.address)) {
            tuple.complete = true;
            tuple.returnedDetails = null;
            synchronized (completionLock) {
                completionLock.notifyAll();
            }
            return;
        }

        tuple.pollingThread = new Thread() {
            @Override
            public void run() {
                ComputerDetails details = tryPollIp(tuple.existingDetails, tuple.address);

                synchronized (tuple) {
                    tuple.complete = true; // Done
                    tuple.returnedDetails = details; // Polling result
                }

                synchronized (completionLock) {
                    completionLock.notifyAll();
                }
            }
        };
        tuple.pollingThread.setName("Host endpoint poll");
        tuple.pollingThread.start();
    }

    private static ComputerDetails getReturnedDetails(ParallelPollTuple tuple) {
        ComputerDetails details = tuple.returnedDetails;
        if (details != null) {
            details.activeAddress = tuple.address;
        }
        return details;
    }

    private static boolean hasIncompleteHigherPriorityPoll(ParallelPollTuple[] pollOrder, int bestIndex) {
        for (int i = 0; i < bestIndex; i++) {
            if (!pollOrder[i].complete) {
                return true;
            }
        }
        return false;
    }

    private static boolean areAllPollsComplete(ParallelPollTuple[] pollOrder) {
        for (ParallelPollTuple tuple : pollOrder) {
            if (!tuple.complete) {
                return false;
            }
        }
        return true;
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

                    byte[] targetAddrBytes = targetAddress.getAddress();
                    byte[] ifaceAddrBytes = ifaceAddress.getAddress().getAddress();

                    boolean addressMatches = true;
                    for (int i = 0; i < ifaceAddress.getNetworkPrefixLength(); i++) {
                        if ((ifaceAddrBytes[i / 8] & (1 << (i % 8))) !=
                                (targetAddrBytes[i / 8] & (1 << (i % 8)))) {
                            addressMatches = false;
                            break;
                        }
                    }

                    if (addressMatches) {
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

    private ComputerDetails parallelPollPc(ComputerDetails details) throws InterruptedException {
        ParallelPollTuple localInfo = new ParallelPollTuple(details.localAddress, details);
        ParallelPollTuple manualInfo = new ParallelPollTuple(details.manualAddress, details);
        ParallelPollTuple remoteInfo = new ParallelPollTuple(details.remoteAddress, details);
        ParallelPollTuple ipv6Info = new ParallelPollTuple(details.ipv6Address, details);

        boolean preferExternalAddress =
                details.manualAddress != null && isWrongSubnetSiteLocalAddress(details.localAddress);

        Object completionLock = new Object();

        ParallelPollTuple[] pollOrder = preferExternalAddress
                ? new ParallelPollTuple[] { manualInfo, remoteInfo, ipv6Info, localInfo }
                : new ParallelPollTuple[] { localInfo, manualInfo, remoteInfo, ipv6Info };

        // These must be started in order of precedence for the deduplication algorithm
        // to result in the correct behavior.
        HashSet<ComputerDetails.AddressTuple> uniqueAddresses = new HashSet<>();
        for (ParallelPollTuple tuple : pollOrder) {
            startParallelPollThread(tuple, uniqueAddresses, completionLock);
        }

        try {
            ComputerDetails bestDetails = null;
            int bestIndex = Integer.MAX_VALUE;
            long upgradeDeadlineMs = 0;

            synchronized (completionLock) {
                while (true) {
                    for (int i = 0; i < pollOrder.length; i++) {
                        if (pollOrder[i].complete && pollOrder[i].returnedDetails != null && i < bestIndex) {
                            bestDetails = getReturnedDetails(pollOrder[i]);
                            bestIndex = i;

                            if (bestIndex == 0) {
                                return bestDetails;
                            }
                            if (upgradeDeadlineMs == 0) {
                                upgradeDeadlineMs = SystemClock.elapsedRealtime() + ADDRESS_UPGRADE_GRACE_MS;
                            }
                        }
                    }

                    if (bestDetails != null) {
                        if (!hasIncompleteHigherPriorityPoll(pollOrder, bestIndex)) {
                            return bestDetails;
                        }

                        long remainingMs = upgradeDeadlineMs - SystemClock.elapsedRealtime();
                        if (remainingMs <= 0) {
                            return bestDetails;
                        }

                        completionLock.wait(remainingMs);
                    }
                    else {
                        if (areAllPollsComplete(pollOrder)) {
                            return null;
                        }

                        completionLock.wait();
                    }
                }
            }
        } finally {
            // Stop any further polling if we've found a working address or we've been
            // interrupted by an attempt to stop polling.
            localInfo.interrupt();
            manualInfo.interrupt();
            remoteInfo.interrupt();
            ipv6Info.interrupt();
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
        dbRefCount.set(1);

        // Grab known machines into our computer list
        if (!getLocalDatabaseReference()) {
            return;
        }

        for (ComputerDetails computer : dbManager.getAllComputers()) {
            // Add tuples for each computer
            addTuple(computer);
        }

        releaseLocalDatabaseReference();

        // Monitor for network changes to invalidate our PC state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    LimeLog.info("Resetting PC state for new available network");
                    synchronized (pollingTuples) {
                        for (PollingTuple tuple : pollingTuples) {
                            tuple.computer.state = ComputerDetails.State.UNKNOWN;
                            if (listener != null) {
                                listener.notifyComputerUpdated(tuple.computer);
                            }
                        }
                    }
                }

                @Override
                public void onLost(Network network) {
                    LimeLog.info("Offlining PCs due to network loss");
                    synchronized (pollingTuples) {
                        for (PollingTuple tuple : pollingTuples) {
                            tuple.computer.state = ComputerDetails.State.OFFLINE;
                            if (listener != null) {
                                listener.notifyComputerUpdated(tuple.computer);
                            }
                        }
                    }
                }
            };

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connMgr.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connMgr.unregisterNetworkCallback(networkCallback);
        }

        discoverySource.close();

        // FIXME: Should await termination here but we have timeout issues in HttpURLConnection

        // Remove the initial DB reference
        releaseLocalDatabaseReference();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class ApplistPoller {
        private Thread thread;
        private final ComputerDetails computer;
        private final Object pollEvent = new Object();
        private boolean receivedAppList = false;

        public ApplistPoller(ComputerDetails computer) {
            this.computer = computer;
        }

        public void pollNow() {
            synchronized (pollEvent) {
                pollEvent.notify();
            }
        }

        private boolean waitPollingDelay() {
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

            return thread != null && !thread.isInterrupted();
        }

        private PollingTuple getPollingTuple(ComputerDetails details) {
            synchronized (pollingTuples) {
                for (PollingTuple tuple : pollingTuples) {
                    if (details.uuid.equals(tuple.computer.uuid)) {
                        return tuple;
                    }
                }
            }

            return null;
        }

        public void start() {
            thread = new Thread() {
                @Override
                public void run() {
                    int emptyAppListResponses = 0;
                    do {
                        // Can't poll if it's not online or paired
                        if (computer.state != ComputerDetails.State.ONLINE ||
                                computer.pairState != PairingManager.PairState.PAIRED) {
                            if (listener != null) {
                                listener.notifyComputerUpdated(computer);
                            }
                            continue;
                        }

                        // Can't poll if there's no UUID yet
                        if (computer.uuid == null) {
                            continue;
                        }

                        PollingTuple tuple = getPollingTuple(computer);

                        try {
                            NvHTTP http = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer), computer.httpsPort, idManager.getUniqueId(),
                                    computer.serverCert, PlatformBinding.getCryptoProvider(ComputerManagerService.this));

                            String appList;
                            if (tuple != null) {
                                // If we're polling this machine too, grab the network lock
                                // while doing the app list request to prevent other requests
                                // from being issued in the meantime.
                                synchronized (tuple.networkLock) {
                                    appList = http.getAppListRaw();
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
                                        getCacheDir(), "applist", computer.uuid)
                                ) {
                                    CacheHelper.writeStringToOutputStream(cacheOut, appList);
                                } catch (IOException e) {
                                    LimeLog.warning("Unable to cache host app list");
                                }

                                // Reset empty count if it wasn't empty this time
                                if (!list.isEmpty()) {
                                    emptyAppListResponses = 0;
                                }

                                // Update the computer
                                computer.rawAppList = appList;
                                receivedAppList = true;

                                // Notify that the app list has been updated
                                // and ensure that the thread is still active
                                if (listener != null && thread != null) {
                                    listener.notifyComputerUpdated(computer);
                                }
                            }
                            else if (appList.isEmpty()) {
                                LimeLog.warning("Null app list received from host");
                            }
                        } catch (IOException e) {
                            LimeLog.warning("Unable to retrieve host app list");
                        } catch (XmlPullParserException e) {
                            LimeLog.warning("Host returned an invalid app list");
                        }
                    } while (waitPollingDelay());
                }
            };
            thread.setName("Host app-list polling");
            thread.start();
        }

        public void stop() {
            if (thread != null) {
                thread.interrupt();

                // Don't join here because we might be blocked on network I/O

                thread = null;
            }
        }
    }
}

class PollingTuple {
    public Thread thread;
    public final ComputerDetails computer;
    public final Object networkLock;
    public long lastSuccessfulPollMs;

    public PollingTuple(ComputerDetails computer, Thread thread) {
        this.computer = computer;
        this.thread = thread;
        this.networkLock = new Object();
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
