package com.limelight.computers.model;

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Immutable service-owned view of one host's persistent and transient state.
 */
public final class HostRuntimeSnapshot {
    private final PersistedHost persistedHost;
    private final HostConnectionState connectionState;
    private final String rawAppList;
    private final boolean nvidiaServer;

    public HostRuntimeSnapshot(
            PersistedHost persistedHost,
            HostConnectionState connectionState,
            String rawAppList,
            boolean nvidiaServer) {
        this.persistedHost = Objects.requireNonNull(
                persistedHost,
                "persistedHost");
        this.connectionState = Objects.requireNonNull(
                connectionState,
                "connectionState");
        if (!persistedHost.getRecord().getIdentity().getId().equals(
                connectionState.getHostId())) {
            throw new IllegalArgumentException(
                    "Persistent and connection state belong to different hosts");
        }
        this.rawAppList = rawAppList;
        this.nvidiaServer = nvidiaServer;
    }

    public PersistedHost getPersistedHost() {
        return persistedHost;
    }

    public HostRecord getRecord() {
        return persistedHost.getRecord();
    }

    public HostConnectionState getConnectionState() {
        return connectionState;
    }

    public String getRawAppList() {
        return rawAppList;
    }

    public boolean isNvidiaServer() {
        return nvidiaServer;
    }

    public HostRuntimeSnapshot withReachability(
            HostConnectionState.Reachability reachability) {
        HostConnectionState current = connectionState;
        return new HostRuntimeSnapshot(
                persistedHost,
                new HostConnectionState(
                        current.getHostId(),
                        Objects.requireNonNull(
                                reachability,
                                "reachability"),
                        current.getPairingStatus(),
                        current.getActiveEndpoint(),
                        current.getHttpsPort(),
                        current.getRunningAppId()),
                rawAppList,
                nvidiaServer);
    }

    public HostRuntimeSnapshot withPinnedCertificate(
            X509Certificate certificate) {
        return new HostRuntimeSnapshot(
                new PersistedHost(
                        persistedHost.getRecord(),
                        Objects.requireNonNull(
                                certificate,
                                "certificate")),
                connectionState,
                rawAppList,
                nvidiaServer);
    }

    public HostRuntimeSnapshot withRawAppList(String appList) {
        return new HostRuntimeSnapshot(
                persistedHost,
                connectionState,
                appList,
                nvidiaServer);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HostRuntimeSnapshot)) {
            return false;
        }
        HostRuntimeSnapshot snapshot = (HostRuntimeSnapshot) other;
        return persistedHost.equals(snapshot.persistedHost) &&
                connectionState.equals(snapshot.connectionState) &&
                Objects.equals(rawAppList, snapshot.rawAppList) &&
                nvidiaServer == snapshot.nvidiaServer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                persistedHost,
                connectionState,
                rawAppList,
                nvidiaServer);
    }
}
