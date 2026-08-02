package com.limelight.stream.launch.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.StreamLaunchRequest;

import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public final class AndroidStreamLaunchRequestFactoryTest {
    @Test
    public void createsLaunchRequestFromImmutableRuntimeSnapshot()
            throws Exception {
        HostRuntimeSnapshot host = host(
                new HostEndpoint(
                        HostEndpoint.Kind.REMOTE,
                        "remote.example.test",
                        48000));

        StreamLaunchRequest request =
                AndroidStreamLaunchRequestFactory.create(
                        host,
                        new NvApp("Desktop", 7, true),
                        "client-id");

        assertEquals("remote.example.test", request.getHostAddress());
        assertEquals(48000, request.getHostPort());
        assertEquals(47984, request.getHttpsPort());
        assertEquals("Desktop", request.getAppName());
        assertEquals(7, request.getAppId());
        assertTrue(request.supportsHdr());
        assertEquals("client-id", request.getClientId());
        assertEquals("host-id", request.getHostId());
        assertEquals("Advertised host", request.getHostName());
        assertArrayEquals(
                "pinned".getBytes(StandardCharsets.UTF_8),
                request.getServerCertificate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSnapshotWithoutActiveEndpoint()
            throws Exception {
        AndroidStreamLaunchRequestFactory.create(
                host(null),
                new NvApp("Desktop", 7, false),
                "client-id");
    }

    private static HostRuntimeSnapshot host(
            HostEndpoint activeEndpoint) {
        HostId hostId = HostId.of("host-id");
        HostRecord record = new HostRecord(
                new HostIdentity(
                        hostId,
                        "Advertised host",
                        "User alias"),
                Collections.singletonList(
                        new HostEndpoint(
                                HostEndpoint.Kind.LOCAL_IPV4,
                                "192.0.2.10",
                                47989)),
                "00:11:22:33:44:55");
        return new HostRuntimeSnapshot(
                new PersistedHost(
                        record,
                        new TestCertificate("pinned")),
                new HostConnectionState(
                        hostId,
                        HostConnectionState.Reachability.ONLINE,
                        HostConnectionState.PairingStatus.PAIRED,
                        activeEndpoint,
                        47984,
                        0),
                "<AppList/>",
                false);
    }

    private static final class TestCertificate
            extends X509Certificate {
        private final String id;

        TestCertificate(String id) {
            this.id = id;
        }

        @Override public void checkValidity() { }
        @Override public void checkValidity(Date date) { }
        @Override public int getVersion() { return 3; }
        @Override public BigInteger getSerialNumber() {
            return BigInteger.ONE;
        }
        @Override public Principal getIssuerDN() { return () -> id; }
        @Override public Principal getSubjectDN() { return () -> id; }
        @Override public Date getNotBefore() { return new Date(0); }
        @Override public Date getNotAfter() {
            return new Date(Long.MAX_VALUE);
        }
        @Override public byte[] getTBSCertificate() {
            return new byte[0];
        }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public String getSigAlgName() { return "none"; }
        @Override public String getSigAlgOID() { return "0"; }
        @Override public byte[] getSigAlgParams() { return null; }
        @Override public boolean[] getIssuerUniqueID() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return null; }
        @Override public boolean[] getKeyUsage() { return null; }
        @Override public int getBasicConstraints() { return -1; }
        @Override public byte[] getEncoded() {
            return id.getBytes(StandardCharsets.UTF_8);
        }
        @Override public void verify(PublicKey key) { }
        @Override public void verify(
                PublicKey key,
                String provider) { }
        @Override public String toString() { return id; }
        @Override public PublicKey getPublicKey() { return null; }
        @Override public boolean hasUnsupportedCriticalExtension() {
            return false;
        }
        @Override public Set<String> getCriticalExtensionOIDs() {
            return null;
        }
        @Override public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }
        @Override public byte[] getExtensionValue(String oid) {
            return null;
        }
    }
}
