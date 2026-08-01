package com.limelight.stream.launch;

import java.util.Objects;

/** Immutable app identity used to resume a host's most recent stream. */
public final class RecentStreamSession {
    private final String appName;
    private final int appId;
    private final boolean appSupportsHdr;

    public RecentStreamSession(
            String appName,
            int appId,
            boolean appSupportsHdr) {
        if (appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        this.appName = appName == null ? "app" : appName;
        this.appId = appId;
        this.appSupportsHdr = appSupportsHdr;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecentStreamSession)) {
            return false;
        }
        RecentStreamSession session = (RecentStreamSession) other;
        return appId == session.appId &&
                appSupportsHdr == session.appSupportsHdr &&
                appName.equals(session.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, appId, appSupportsHdr);
    }
}
