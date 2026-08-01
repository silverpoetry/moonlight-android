package com.limelight.computers.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class HostRecordMergePolicyTest {
    @Test
    public void observationUpdatesOnlyObservedFields() {
        HostId id = HostId.of("host-id");
        HostRecord existing = new HostRecord(
                new HostIdentity(id, "Old server name", "Living room"),
                Arrays.asList(
                        endpoint(HostEndpoint.Kind.LOCAL_IPV4, "192.0.2.1"),
                        endpoint(HostEndpoint.Kind.REMOTE, "remote.test"),
                        endpoint(HostEndpoint.Kind.MANUAL, "manual.test")),
                "00:11:22:33:44:55");
        HostObservation observation = new HostObservation(
                id,
                "New server name",
                Collections.singletonList(
                        endpoint(HostEndpoint.Kind.LOCAL_IPV4, "192.0.2.2")),
                null);

        HostRecord merged = HostRecordMergePolicy.merge(existing, observation);

        assertEquals("New server name", merged.getIdentity().getAdvertisedName());
        assertEquals("Living room", merged.getIdentity().getUserAlias());
        assertEquals(
                "192.0.2.2",
                merged.getEndpoint(HostEndpoint.Kind.LOCAL_IPV4).getAddress());
        assertEquals(
                "remote.test",
                merged.getEndpoint(HostEndpoint.Kind.REMOTE).getAddress());
        assertEquals(
                "manual.test",
                merged.getEndpoint(HostEndpoint.Kind.MANUAL).getAddress());
        assertEquals("00:11:22:33:44:55", merged.getMacAddress());
    }

    @Test
    public void newObservationCreatesRecordWithoutAliasOrCredentials() {
        HostObservation observation = new HostObservation(
                HostId.of("new-host"),
                "New host",
                Collections.singletonList(
                        endpoint(HostEndpoint.Kind.LOCAL_IPV6, "2001:db8::1")),
                "AA:BB:CC:DD:EE:FF");

        HostRecord record = HostRecordMergePolicy.merge(null, observation);

        assertEquals("new-host", record.getIdentity().getId().getValue());
        assertNull(record.getIdentity().getUserAlias());
        assertEquals("AA:BB:CC:DD:EE:FF", record.getMacAddress());
    }

    @Test
    public void rejectsObservationForDifferentHost() {
        HostRecord existing = new HostRecord(
                new HostIdentity(HostId.of("one"), "One", null),
                Collections.emptyList(),
                null);
        HostObservation observation = new HostObservation(
                HostId.of("two"),
                "Two",
                Collections.emptyList(),
                null);

        assertThrows(
                IllegalArgumentException.class,
                () -> HostRecordMergePolicy.merge(existing, observation));
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address) {
        return new HostEndpoint(kind, address, 47989);
    }
}
