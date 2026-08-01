package com.limelight.stream.launch;

/** Immutable stream identity retained while navigation returns to host UI. */
public final class PendingStreamReconnect {
    private final String hostId;
    private final String appName;
    private final int appId;
    private final boolean appSupportsHdr;

    public PendingStreamReconnect(
            String hostId,
            String appName,
            int appId,
            boolean appSupportsHdr) {
        if (hostId == null || hostId.trim().isEmpty()) {
            throw new IllegalArgumentException("hostId is required");
        }
        this.hostId = hostId;
        this.appName = appName;
        this.appId = appId;
        this.appSupportsHdr = appSupportsHdr;
    }

    public String getHostId() {
        return hostId;
    }

    public String getAppName() {
        return appName;
    }

    public int getAppId() {
        return appId;
    }

    public boolean supportsHdr() {
        return appSupportsHdr;
    }
}
