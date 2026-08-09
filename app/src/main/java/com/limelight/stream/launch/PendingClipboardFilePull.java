package com.limelight.stream.launch;

/** Immutable clipboard file pull retained across a stream reconnect. */
public final class PendingClipboardFilePull {
    private final String hostId;
    private final int appId;
    private final String destinationUri;

    public PendingClipboardFilePull(
            String hostId,
            int appId,
            String destinationUri) {
        if (hostId == null || hostId.trim().isEmpty()) {
            throw new IllegalArgumentException("hostId is required");
        }
        if (appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        if (destinationUri == null || destinationUri.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "destinationUri is required");
        }
        this.hostId = hostId;
        this.appId = appId;
        this.destinationUri = destinationUri;
    }

    public String getHostId() {
        return hostId;
    }

    public int getAppId() {
        return appId;
    }

    public String getDestinationUri() {
        return destinationUri;
    }

    public boolean matches(String candidateHostId, int candidateAppId) {
        return appId == candidateAppId &&
                candidateHostId != null &&
                hostId.equalsIgnoreCase(candidateHostId);
    }
}
