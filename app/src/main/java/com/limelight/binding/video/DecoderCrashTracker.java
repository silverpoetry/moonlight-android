package com.limelight.binding.video;

import java.util.Objects;

/**
 * Owns decoder-crash accounting for one playback attempt.
 *
 * <p>The initial count is frozen for decoder discovery. A crash is committed
 * synchronously at most once because the process may terminate immediately;
 * only a clean attempt may clear historical crash state.</p>
 */
public final class DecoderCrashTracker implements CrashListener {
    public interface Store {
        int getCrashCount();

        void recordCrashSynchronously();

        void clearCrashHistory();
    }

    private final Store store;
    private final int initialCrashCount;
    private boolean crashReported;
    private boolean completed;

    public DecoderCrashTracker(Store store) {
        this.store = Objects.requireNonNull(store, "store");
        initialCrashCount = Math.max(0, store.getCrashCount());
    }

    public int getInitialCrashCount() {
        return initialCrashCount;
    }

    @Override
    public synchronized void notifyCrash(Exception error) {
        if (crashReported) {
            return;
        }
        crashReported = true;
        store.recordCrashSynchronously();
    }

    public synchronized void completeCleanly() {
        if (completed) {
            return;
        }
        completed = true;
        if (!crashReported && store.getCrashCount() != 0) {
            store.clearCrashHistory();
        }
    }
}
