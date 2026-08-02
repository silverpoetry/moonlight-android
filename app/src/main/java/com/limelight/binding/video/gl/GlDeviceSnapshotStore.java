package com.limelight.binding.video.gl;

/** Persistence port for the atomic build-fingerprint and GL-renderer pair. */
public interface GlDeviceSnapshotStore {
    GlDeviceSnapshot read();

    /**
     * Atomically replaces the stored pair. An unavailable snapshot clears a
     * stale or partially valid pair.
     */
    void replace(GlDeviceSnapshot snapshot);
}
