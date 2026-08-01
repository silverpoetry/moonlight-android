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
        X509Certificate pinnedCertificate = new TestCertificate("pinned");
        destination.serverCert = pinnedCertificate;
        ComputerDetails observation = observation();
        observation.serverCert = new TestCertificate("untrusted observation");

        LegacyComputerDetailsMergePolicy.mergeObservation(
                destination,
                observation);

        assertSame(pinnedCertificate, destination.serverCert);
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

    private static final class TestCertificate extends X509Certificate {
        private final String id;

        private TestCertificate(String id) {
            this.id = id;
        }

        @Override public void checkValidity() {}
        @Override public void checkValidity(java.util.Date date) {}
        @Override public int getVersion() { return 3; }
        @Override public java.math.BigInteger getSerialNumber() { return java.math.BigInteger.ONE; }
        @Override public java.security.Principal getIssuerDN() { return () -> id; }
        @Override public java.security.Principal getSubjectDN() { return () -> id; }
        @Override public java.util.Date getNotBefore() { return new java.util.Date(0); }
        @Override public java.util.Date getNotAfter() { return new java.util.Date(Long.MAX_VALUE); }
        @Override public byte[] getTBSCertificate() { return new byte[0]; }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public String getSigAlgName() { return "none"; }
        @Override public String getSigAlgOID() { return "0"; }
        @Override public byte[] getSigAlgParams() { return null; }
        @Override public boolean[] getIssuerUniqueID() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return null; }
        @Override public boolean[] getKeyUsage() { return null; }
        @Override public int getBasicConstraints() { return -1; }
        @Override public byte[] getEncoded() { return id.getBytes(java.nio.charset.StandardCharsets.UTF_8); }
        @Override public void verify(java.security.PublicKey key) {}
        @Override public void verify(java.security.PublicKey key, String sigProvider) {}
        @Override public String toString() { return id; }
        @Override public java.security.PublicKey getPublicKey() { return null; }
        @Override public boolean hasUnsupportedCriticalExtension() { return false; }
        @Override public java.util.Set<String> getCriticalExtensionOIDs() { return null; }
        @Override public java.util.Set<String> getNonCriticalExtensionOIDs() { return null; }
        @Override public byte[] getExtensionValue(String oid) { return null; }
    }
}
