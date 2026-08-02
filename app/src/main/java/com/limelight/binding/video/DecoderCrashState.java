package com.limelight.binding.video;

/** Immutable persisted decoder-crash accounting state. */
public final class DecoderCrashState {
    private final int crashCount;
    private final int acknowledgedCrashCount;

    public DecoderCrashState(
            int crashCount,
            int acknowledgedCrashCount) {
        this.crashCount = Math.max(0, crashCount);
        this.acknowledgedCrashCount = Math.max(
                0, acknowledgedCrashCount);
    }

    public int getCrashCount() {
        return crashCount;
    }

    public int getAcknowledgedCrashCount() {
        return acknowledgedCrashCount;
    }
}
