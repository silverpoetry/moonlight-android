package com.limelight.ui.filepush;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FilePushTargetFactoryTest {
    @Test
    public void selectsManualEndpointAndPinnedCredential() {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.168.1.2");
        HostEndpoint manual = endpoint(
                HostEndpoint.Kind.MANUAL,
                "manual.example");
        TestCertificate certificate = new TestCertificate("pinned");
        PersistedHost host = new PersistedHost(
                new HostRecord(
                        identity(),
                        Arrays.asList(local, manual),
                        "00:11:22:33:44:55"),
                certificate);

        FilePushTarget target = FilePushTargetFactory.create(host)
                .getTarget();

        assertEquals("Host", target.getDisplayName());
        assertSame(manual, target.getEndpoint());
        assertSame(certificate, target.getPinnedCertificate());
    }

    @Test
    public void rejectsUnpairedOrUnaddressableHosts() {
        PersistedHost unpaired = new PersistedHost(
                new HostRecord(
                        identity(),
                        Collections.singletonList(endpoint(
                                HostEndpoint.Kind.LOCAL_IPV4,
                                "192.168.1.2")),
                        null),
                null);
        PersistedHost unaddressable = new PersistedHost(
                new HostRecord(
                        identity(),
                        Collections.emptyList(),
                        null),
                new TestCertificate("pinned"));

        assertFalse(FilePushTargetFactory.create(unpaired).isFound());
        assertFalse(FilePushTargetFactory.create(
                unaddressable).isFound());
    }

    @Test
    public void preservesLocalIpv4Ipv6RemoteFallbackOrder() {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.168.1.2");
        HostEndpoint ipv6 = endpoint(
                HostEndpoint.Kind.LOCAL_IPV6,
                "2001:db8::1");
        HostEndpoint remote = endpoint(
                HostEndpoint.Kind.REMOTE,
                "remote.example");

        assertSame(local, targetFor(Arrays.asList(
                remote, ipv6, local)).getEndpoint());
        assertSame(ipv6, targetFor(Arrays.asList(
                remote, ipv6)).getEndpoint());
        assertSame(remote, targetFor(Collections.singletonList(
                remote)).getEndpoint());
    }

    private static FilePushTarget targetFor(
            List<HostEndpoint> endpoints) {
        return FilePushTargetFactory.create(new PersistedHost(
                new HostRecord(identity(), endpoints, null),
                new TestCertificate("pinned"))).getTarget();
    }

    private static HostIdentity identity() {
        return new HostIdentity(
                HostId.of("host-id"),
                "Host",
                null);
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address) {
        return new HostEndpoint(kind, address, 47989);
    }
}
