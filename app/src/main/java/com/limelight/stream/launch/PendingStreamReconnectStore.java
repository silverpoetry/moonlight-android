package com.limelight.stream.launch;

import java.util.Objects;

/** Application-scoped, compare-and-clear store for one reconnect handoff. */
public final class PendingStreamReconnectStore {
    private PendingStreamReconnect pending;

    public synchronized void save(PendingStreamReconnect reconnect) {
        pending = Objects.requireNonNull(reconnect, "reconnect");
    }

    public synchronized PendingStreamReconnect get() {
        return pending;
    }

    public synchronized boolean clearIfCurrent(
            PendingStreamReconnect expected) {
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
