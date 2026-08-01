package com.limelight.computers.model;

import java.util.Objects;

/** Immutable transient connection state, never persisted as host identity. */
public final class HostConnectionState {
    public enum Reachability {
        UNKNOWN,
        ONLINE,
        OFFLINE
    }

    public enum PairingStatus {
        UNKNOWN,
        NOT_PAIRED,
        PAIRED
    }

    private final HostId hostId;
    private final Reachability reachability;
    private final PairingStatus pairingStatus;
    private final HostEndpoint activeEndpoint;
    private final int httpsPort;
    private final int runningAppId;

    public HostConnectionState(
            HostId hostId,
            Reachability reachability,
            PairingStatus pairingStatus,
            HostEndpoint activeEndpoint,
            int httpsPort,
            int runningAppId) {
        this.hostId = Objects.requireNonNull(hostId, "hostId");
        this.reachability = Objects.requireNonNull(
                reachability, "reachability");
        this.pairingStatus = Objects.requireNonNull(
                pairingStatus, "pairingStatus");
        this.activeEndpoint = activeEndpoint;
        if (httpsPort < 0 || httpsPort > 65535) {
            throw new IllegalArgumentException("Invalid HTTPS port");
        }
        if (runningAppId < 0) {
            throw new IllegalArgumentException("Invalid running app ID");
        }
        this.httpsPort = httpsPort;
        this.runningAppId = runningAppId;
    }

    public HostId getHostId() {
        return hostId;
    }

    public Reachability getReachability() {
        return reachability;
    }

    public PairingStatus getPairingStatus() {
        return pairingStatus;
    }

    public HostEndpoint getActiveEndpoint() {
        return activeEndpoint;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public int getRunningAppId() {
        return runningAppId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HostConnectionState)) {
            return false;
        }
        HostConnectionState state = (HostConnectionState) other;
        return hostId.equals(state.hostId) &&
                reachability == state.reachability &&
                pairingStatus == state.pairingStatus &&
                Objects.equals(activeEndpoint, state.activeEndpoint) &&
                httpsPort == state.httpsPort &&
                runningAppId == state.runningAppId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hostId,
                reachability,
                pairingStatus,
                activeEndpoint,
                httpsPort,
                runningAppId);
    }
}
