package com.limelight.stream.launch;

import java.util.Objects;

/** Process-scoped, compare-and-clear store for one reconnect file pull. */
public final class PendingClipboardFilePullStore {
    private PendingClipboardFilePull pending;

    public synchronized void save(PendingClipboardFilePull filePull) {
        pending = Objects.requireNonNull(filePull, "filePull");
    }

    public synchronized PendingClipboardFilePull getFor(
            String hostId,
            int appId) {
        return pending != null && pending.matches(hostId, appId)
                ? pending
                : null;
    }

    public synchronized boolean hasFor(String hostId, int appId) {
        return pending != null && pending.matches(hostId, appId);
    }

    public synchronized boolean clearIfCurrent(
            PendingClipboardFilePull expected) {
        if (pending != expected) {
            return false;
        }
        pending = null;
        return true;
    }

    public synchronized void clear() {
        pending = null;
    }
}
