package com.limelight.computers.model;

import java.util.Objects;

/**
 * Immutable result of one server-info probe. Credentials are absent by
 * construction and therefore cannot be replaced by network input.
 */
public final class HostRuntimeObservation {
    private final HostObservation hostObservation;
    private final HostConnectionState connectionState;
    private final int reportedExternalPort;
    private final String rawAppList;
    private final boolean nvidiaServer;

    public HostRuntimeObservation(
            HostObservation hostObservation,
            HostConnectionState connectionState,
            int reportedExternalPort,
            String rawAppList,
            boolean nvidiaServer) {
        this.hostObservation = Objects.requireNonNull(
                hostObservation,
                "hostObservation");
        this.connectionState = Objects.requireNonNull(
                connectionState,
                "connectionState");
        if (!hostObservation.asRecord().getIdentity().getId().equals(
                connectionState.getHostId())) {
            throw new IllegalArgumentException(
                    "Metadata and connection observation belong to different hosts");
        }
        if (reportedExternalPort < 0 || reportedExternalPort > 65535) {
            throw new IllegalArgumentException(
                    "Invalid reported external port");
        }
        this.reportedExternalPort = reportedExternalPort;
        this.rawAppList = rawAppList;
        this.nvidiaServer = nvidiaServer;
    }

    HostObservation getHostObservation() {
        return hostObservation;
    }

    HostConnectionState getConnectionState() {
        return connectionState;
    }

    int getReportedExternalPort() {
        return reportedExternalPort;
    }

    String getRawAppList() {
        return rawAppList;
    }

    boolean isNvidiaServer() {
        return nvidiaServer;
    }
}
