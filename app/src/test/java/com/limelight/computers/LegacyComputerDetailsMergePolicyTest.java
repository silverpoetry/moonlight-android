package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager;

import org.junit.Test;

import java.security.cert.X509Certificate;

public final class LegacyComputerDetailsMergePolicyTest {
    @Test
    public void observationCannotReplacePinnedCredential() {
        ComputerDetails destination = persistentDetails();
        X509Certificate pinnedCertificate =
                new TestX509Certificate("pinned");
        destination.serverCert = pinnedCertificate;
        destination.rawAppList = "<cached-app-list/>";
        ComputerDetails observation = observation();
        observation.serverCert =
                new TestX509Certificate("untrusted observation");

        LegacyComputerDetailsMergePolicy.mergeObservation(
                destination,
                observation);

        assertSame(pinnedCertificate, destination.serverCert);
        assertEquals("<cached-app-list/>", destination.rawAppList);
    }

    @Test
    public void partialObservationPreservesUnobservedEndpoints() {
        ComputerDetails destination = persistentDetails();
        ComputerDetails.AddressTuple originalRemote = destination.remoteAddress;
        ComputerDetails observation = observation();
        observation.localAddress = tuple("127.0.0.1", 47989);
        observation.remoteAddress = null;
        observation.manualAddress = null;
        observation.ipv6Address = null;
        observation.externalPort = 48000;
        observation.macAddress = "00:00:00:00:00:00";

        LegacyComputerDetailsMergePolicy.mergeObservation(
                destination,
                observation);

        assertEquals("192.0.2.1", destination.localAddress.address);
        assertEquals("remote.test", destination.remoteAddress.address);
        assertEquals(48000, destination.remoteAddress.port);
        assertNotSame(originalRemote, destination.remoteAddress);
        assertEquals(47989, originalRemote.port);
        assertEquals("manual.test", destination.manualAddress.address);
        assertEquals("2001:db8::1", destination.ipv6Address.address);
        assertEquals("00:11:22:33:44:55", destination.macAddress);
    }

    @Test
    public void observationUpdatesRuntimeStateExplicitly() {
        ComputerDetails destination = persistentDetails();
        ComputerDetails observation = observation();

        LegacyComputerDetailsMergePolicy.mergeObservation(
                destination,
                observation);

        assertEquals(ComputerDetails.State.ONLINE, destination.state);
        assertEquals("New name", destination.name);
        assertEquals("host-id", destination.uuid);
        assertEquals("192.0.2.99", destination.activeAddress.address);
        assertEquals(47984, destination.httpsPort);
        assertEquals(PairingManager.PairState.PAIRED, destination.pairState);
        assertEquals(42, destination.runningGameId);
    }

    private static ComputerDetails persistentDetails() {
        ComputerDetails details = new ComputerDetails();
        details.uuid = "host-id";
        details.name = "Old name";
        details.localAddress = tuple("192.0.2.1", 47989);
        details.remoteAddress = tuple("remote.test", 47989);
        details.manualAddress = tuple("manual.test", 47989);
        details.ipv6Address = tuple("2001:db8::1", 47989);
        details.macAddress = "00:11:22:33:44:55";
        return details;
    }

    private static ComputerDetails observation() {
        ComputerDetails details = new ComputerDetails();
        details.uuid = "host-id";
        details.name = "New name";
        details.state = ComputerDetails.State.ONLINE;
        details.activeAddress = tuple("192.0.2.99", 47989);
        details.localAddress = tuple("192.0.2.99", 47989);
        details.httpsPort = 47984;
        details.pairState = PairingManager.PairState.PAIRED;
        details.runningGameId = 42;
        return details;
    }

    private static ComputerDetails.AddressTuple tuple(
            String address,
            int port) {
        return new ComputerDetails.AddressTuple(address, port);
    }

}
