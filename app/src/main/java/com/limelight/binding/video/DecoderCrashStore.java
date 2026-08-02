package com.limelight.binding.video;

/** Persistence port for decoder-crash recovery and notification state. */
public interface DecoderCrashStore {
    DecoderCrashState readState();

    void recordCrashSynchronously();

    void clearCrashHistory();

    void acknowledgeCrashCount(int crashCount);
}
