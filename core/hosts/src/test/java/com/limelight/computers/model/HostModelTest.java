package com.limelight.computers.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class HostModelTest {
    @Test
    public void hostIdIsCanonicalAndCaseInsensitive() {
        assertEquals(HostId.of(" HOST-ID "), HostId.of("host-id"));
        assertThrows(
                IllegalArgumentException.class,
                () -> HostId.of("host id"));
        assertThrows(
                IllegalArgumentException.class,
                () -> HostId.of("  "));
    }

    @Test
    public void endpointNormalizesAddressAndValidatesPort() {
        HostEndpoint endpoint = new HostEndpoint(
                HostEndpoint.Kind.LOCAL_IPV6,
                " [FE80::1] ",
                47989);

        assertEquals("fe80::1", endpoint.getAddress());
        assertEquals(
                "LOCAL_IPV6:[fe80::1]:47989",
                endpoint.toString());
        assertThrows(
                IllegalArgumentException.class,
                () -> new HostEndpoint(
                        HostEndpoint.Kind.REMOTE,
                        "example.test",
                        0));
    }

    @Test
    public void recordDefensivelyCopiesAndIndexesEndpoints() {
        List<HostEndpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint(HostEndpoint.Kind.LOCAL_IPV4, "192.0.2.1"));
        HostRecord record = record("Host", "Alias", endpoints, "00:00:00:00:00:00");
        endpoints.clear();

        assertEquals(1, record.getEndpoints().size());
        assertNull(record.getMacAddress());
        assertEquals("Alias", record.getIdentity().getDisplayName());
        assertThrows(
                UnsupportedOperationException.class,
                () -> record.getEndpoints().clear());
        assertThrows(
                IllegalArgumentException.class,
                () -> record(
                        "Host",
                        null,
                        Arrays.asList(
                                endpoint(HostEndpoint.Kind.REMOTE, "one.test"),
                                endpoint(HostEndpoint.Kind.REMOTE, "two.test")),
                        null));
    }

    @Test
    public void connectionStateKeepsRuntimeEndpointOutOfPersistentRecord() {
        HostRecord record = record(
                "Host",
                null,
                Collections.singletonList(
                        endpoint(HostEndpoint.Kind.REMOTE, "remote.test")),
                null);
        HostEndpoint active = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.0.2.4");
        HostConnectionState state = new HostConnectionState(
                record.getIdentity().getId(),
                HostConnectionState.Reachability.ONLINE,
                HostConnectionState.PairingStatus.PAIRED,
                active,
                47984,
                12);

        assertEquals(active, state.getActiveEndpoint());
        assertNull(record.getEndpoint(HostEndpoint.Kind.LOCAL_IPV4));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HostConnectionState(
                        record.getIdentity().getId(),
                        HostConnectionState.Reachability.UNKNOWN,
                        HostConnectionState.PairingStatus.UNKNOWN,
                        null,
                        65536,
                        0));
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address) {
        return new HostEndpoint(kind, address, 47989);
    }

    private static HostRecord record(
            String advertisedName,
            String userAlias,
            List<HostEndpoint> endpoints,
            String macAddress) {
        return new HostRecord(
                new HostIdentity(
                        HostId.of("host-id"),
                        advertisedName,
                        userAlias),
                endpoints,
                macAddress);
    }
}
