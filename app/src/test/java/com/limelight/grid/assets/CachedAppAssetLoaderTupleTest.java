package com.limelight.grid.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.NvApp;

import org.junit.Test;

import java.util.Collections;

public final class CachedAppAssetLoaderTupleTest {
    @Test
    public void cacheIdentityIgnoresRuntimeEndpointChanges() {
        CachedAppAssetLoader.LoaderTuple local =
                new CachedAppAssetLoader.LoaderTuple(
                        host("host-id", "192.0.2.10"),
                        new NvApp("Desktop", 7, false));
        CachedAppAssetLoader.LoaderTuple remote =
                new CachedAppAssetLoader.LoaderTuple(
                        host("host-id", "198.51.100.20"),
                        new NvApp("Renamed", 7, true));

        assertEquals(local, remote);
        assertEquals(local.hashCode(), remote.hashCode());
        assertEquals("host-id", local.getHostId());
    }

    @Test
    public void cacheIdentitySeparatesHostsAndApps() {
        CachedAppAssetLoader.LoaderTuple source =
                new CachedAppAssetLoader.LoaderTuple(
                        host("host-a", "192.0.2.10"),
                        new NvApp("Desktop", 7, false));

        assertNotEquals(
                source,
                new CachedAppAssetLoader.LoaderTuple(
                        host("host-b", "192.0.2.10"),
                        new NvApp("Desktop", 7, false)));
        assertNotEquals(
                source,
                new CachedAppAssetLoader.LoaderTuple(
                        host("host-a", "192.0.2.10"),
                        new NvApp("Desktop", 8, false)));
    }

    private static HostRuntimeSnapshot host(
            String id,
            String address) {
        HostId hostId = HostId.of(id);
        HostEndpoint endpoint = new HostEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                address,
                47989);
        return new HostRuntimeSnapshot(
                new PersistedHost(
                        new HostRecord(
                                new HostIdentity(
                                        hostId,
                                        "Host",
                                        null),
                                Collections.singletonList(endpoint),
                                null),
                        null),
                new HostConnectionState(
                        hostId,
                        HostConnectionState.Reachability.ONLINE,
                        HostConnectionState.PairingStatus.PAIRED,
                        endpoint,
                        0,
                        0),
                null,
                false);
    }
}
