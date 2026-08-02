package com.limelight.computers.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.model.PersistedHost;

import org.junit.Test;

import java.util.Collections;

public final class HostHttpTargetTest {
    @Test
    public void snapshotsActiveEndpointAndHttpSessionIdentity() {
        HostId hostId = HostId.of("host-id");
        HostEndpoint endpoint = new HostEndpoint(
                HostEndpoint.Kind.REMOTE,
                "remote.example.test",
                48000);
        HostRuntimeSnapshot host = new HostRuntimeSnapshot(
                new PersistedHost(
                        new HostRecord(
                                new HostIdentity(hostId, "Host", null),
                                Collections.singletonList(endpoint),
                                null),
                        null),
                new HostConnectionState(
                        hostId,
                        HostConnectionState.Reachability.ONLINE,
                        HostConnectionState.PairingStatus.PAIRED,
                        endpoint,
                        47984,
                        0),
                null,
                false);

        HostHttpTarget target = HostHttpTarget.from(
                host,
                "client-id");

        assertEquals("remote.example.test", target.getAddress());
        assertEquals(48000, target.getPort());
        assertEquals(47984, target.getHttpsPort());
        assertEquals("client-id", target.getUniqueId());
        assertNull(target.getPinnedCertificate());
        assertEquals(
                target,
                new HostHttpTarget(
                        "remote.example.test",
                        48000,
                        47984,
                        "client-id",
                        null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSnapshotWithoutActiveEndpoint() {
        HostId hostId = HostId.of("host-id");
        HostHttpTarget.from(
                new HostRuntimeSnapshot(
                        new PersistedHost(
                                new HostRecord(
                                        new HostIdentity(
                                                hostId,
                                                "Host",
                                                null),
                                        Collections.emptyList(),
                                        null),
                                null),
                        new HostConnectionState(
                                hostId,
                                HostConnectionState.Reachability.UNKNOWN,
                                HostConnectionState.PairingStatus.UNKNOWN,
                                null,
                                0,
                                0),
                        null,
                        false),
                "client-id");
    }
}
