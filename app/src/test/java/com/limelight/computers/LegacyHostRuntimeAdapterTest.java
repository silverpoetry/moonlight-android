package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRuntimeMergePolicy;
import com.limelight.computers.model.HostRuntimeObservation;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager;

import org.junit.Test;

import java.security.cert.X509Certificate;

public final class LegacyHostRuntimeAdapterTest {
    @Test
    public void runtimeRoundTripPreservesEveryServiceField() {
        ComputerDetails source = details();
        X509Certificate certificate =
                new TestX509Certificate("pinned");
        source.serverCert = certificate;
        source.state = ComputerDetails.State.ONLINE;
        source.activeAddress = source.localAddress;
        source.httpsPort = 47984;
        source.pairState = PairingManager.PairState.PAIRED;
        source.runningGameId = 42;
        source.rawAppList = "<AppList/>";
        source.nvidiaServer = true;

        HostRuntimeSnapshot snapshot =
                LegacyHostRuntimeAdapter.fromComputerDetails(
                        source,
                        "Living room");
        source.name = "Mutated";
        source.state = ComputerDetails.State.OFFLINE;
        source.rawAppList = null;
        ComputerDetails restored =
                LegacyHostRuntimeAdapter.toComputerDetails(snapshot);

        assertEquals("host-id", restored.uuid);
        assertEquals("Host", restored.name);
        assertEquals(source.localAddress, restored.localAddress);
        assertEquals(source.remoteAddress, restored.remoteAddress);
        assertEquals(source.manualAddress, restored.manualAddress);
        assertEquals(source.ipv6Address, restored.ipv6Address);
        assertEquals("00:11:22:33:44:55", restored.macAddress);
        assertSame(certificate, restored.serverCert);
        assertEquals(ComputerDetails.State.ONLINE, restored.state);
        assertEquals(tuple("192.0.2.1", 47989), restored.activeAddress);
        assertEquals(47984, restored.httpsPort);
        assertEquals(48000, restored.externalPort);
        assertEquals(PairingManager.PairState.PAIRED, restored.pairState);
        assertEquals(42, restored.runningGameId);
        assertEquals("<AppList/>", restored.rawAppList);
        assertEquals(true, restored.nvidiaServer);
        assertEquals(
                "Living room",
                snapshot.getRecord().getIdentity().getUserAlias());
    }

    @Test
    public void observationMergePreservesCredentialAndUnobservedData() {
        ComputerDetails existingDetails = details();
        X509Certificate certificate =
                new TestX509Certificate("pinned");
        existingDetails.serverCert = certificate;
        existingDetails.rawAppList = "<Cached/>";
        HostRuntimeSnapshot existing =
                LegacyHostRuntimeAdapter.fromComputerDetails(
                        existingDetails,
                        "Alias");

        ComputerDetails observed = new ComputerDetails();
        observed.uuid = "HOST-ID";
        observed.name = "Renamed";
        observed.state = ComputerDetails.State.ONLINE;
        observed.localAddress = tuple("127.0.0.1", 47989);
        observed.externalPort = 48001;
        observed.macAddress = "00:00:00:00:00:00";
        observed.httpsPort = 47984;
        observed.pairState = PairingManager.PairState.PAIRED;
        observed.runningGameId = 7;
        observed.serverCert = new TestX509Certificate("untrusted");
        HostEndpoint activeEndpoint = new HostEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.0.2.1",
                47989);
        HostRuntimeObservation observation =
                LegacyHostRuntimeAdapter.toObservation(
                        observed,
                        activeEndpoint);

        HostRuntimeSnapshot merged = HostRuntimeMergePolicy.merge(
                existing,
                observation);

        assertSame(
                certificate,
                merged.getPersistedHost().getPinnedCertificate());
        assertEquals("Alias", merged.getRecord()
                .getIdentity().getUserAlias());
        assertEquals("Renamed", merged.getRecord()
                .getIdentity().getAdvertisedName());
        assertEquals("192.0.2.1", merged.getRecord()
                .getEndpoint(HostEndpoint.Kind.LOCAL_IPV4)
                .getAddress());
        assertEquals(48001, merged.getRecord()
                .getEndpoint(HostEndpoint.Kind.REMOTE)
                .getPort());
        assertEquals("manual.test", merged.getRecord()
                .getEndpoint(HostEndpoint.Kind.MANUAL)
                .getAddress());
        assertEquals("00:11:22:33:44:55",
                merged.getRecord().getMacAddress());
        assertEquals("<Cached/>", merged.getRawAppList());
        assertEquals(activeEndpoint,
                merged.getConnectionState().getActiveEndpoint());
        assertEquals(HostConnectionState.Reachability.ONLINE,
                merged.getConnectionState().getReachability());
        assertEquals(HostConnectionState.PairingStatus.PAIRED,
                merged.getConnectionState().getPairingStatus());
        assertEquals(47984,
                merged.getConnectionState().getHttpsPort());
        assertEquals(7,
                merged.getConnectionState().getRunningAppId());
    }

    @Test
    public void persistedHostStartsWithUnknownRuntimeState() {
        HostRuntimeSnapshot source =
                LegacyHostRuntimeAdapter.fromComputerDetails(
                        details(),
                        null);
        HostRuntimeSnapshot restored =
                LegacyHostRuntimeAdapter.fromPersistedHost(
                        source.getPersistedHost());

        assertEquals(HostConnectionState.Reachability.UNKNOWN,
                restored.getConnectionState().getReachability());
        assertEquals(HostConnectionState.PairingStatus.UNKNOWN,
                restored.getConnectionState().getPairingStatus());
        assertNull(restored.getConnectionState().getActiveEndpoint());
        assertNull(restored.getRawAppList());
    }

    @Test
    public void candidateRebaseRetainsRepositoryOwnedState() {
        ComputerDetails persistedDetails = details();
        X509Certificate certificate =
                new TestX509Certificate("pinned");
        persistedDetails.serverCert = certificate;
        HostRuntimeSnapshot persistedRuntime =
                LegacyHostRuntimeAdapter.fromComputerDetails(
                        persistedDetails,
                        "Alias");

        ComputerDetails candidateDetails = new ComputerDetails();
        candidateDetails.uuid = "host-id";
        candidateDetails.name = "Renamed";
        candidateDetails.localAddress = tuple("192.0.2.99", 47989);
        candidateDetails.state = ComputerDetails.State.ONLINE;
        candidateDetails.activeAddress = candidateDetails.localAddress;
        HostRuntimeSnapshot candidate =
                LegacyHostRuntimeAdapter.fromComputerDetails(
                        candidateDetails,
                        null);

        HostRuntimeSnapshot rebased =
                HostRuntimeMergePolicy.rebaseOnPersistedHost(
                        candidate,
                        persistedRuntime.getPersistedHost());

        assertSame(certificate,
                rebased.getPersistedHost().getPinnedCertificate());
        assertEquals("Alias", rebased.getRecord()
                .getIdentity().getUserAlias());
        assertEquals("Renamed", rebased.getRecord()
                .getIdentity().getAdvertisedName());
        assertEquals("192.0.2.99", rebased.getRecord()
                .getEndpoint(HostEndpoint.Kind.LOCAL_IPV4)
                .getAddress());
        assertEquals("remote.test", rebased.getRecord()
                .getEndpoint(HostEndpoint.Kind.REMOTE)
                .getAddress());
        assertEquals(HostConnectionState.Reachability.ONLINE,
                rebased.getConnectionState().getReachability());
    }

    private static ComputerDetails details() {
        ComputerDetails details = new ComputerDetails();
        details.uuid = "Host-ID";
        details.name = "Host";
        details.localAddress = tuple("192.0.2.1", 47989);
        details.remoteAddress = tuple("remote.test", 48000);
        details.manualAddress = tuple("manual.test", 47989);
        details.ipv6Address = tuple("2001:db8::1", 47989);
        details.macAddress = "00:11:22:33:44:55";
        return details;
    }

    private static ComputerDetails.AddressTuple tuple(
            String address,
            int port) {
        return new ComputerDetails.AddressTuple(address, port);
    }
}
