package com.limelight.nvstream;

/**
 * Lifecycle state of a single stream session.
 *
 * <p>A session is single-use. A new connection requires a new
 * {@link StreamSessionController} and {@link NvConnection}.</p>
 */
public enum SessionState {
    CREATED,
    STARTING,
    STREAMING,
    TERMINATED,
    STOPPING,
    STOPPED,
    FAILED;

    public boolean hasStarted() {
        return this != CREATED;
    }

    public boolean isStreaming() {
        return this == STREAMING;
    }

    public boolean needsStop() {
        return this == STARTING ||
                this == STREAMING ||
                this == TERMINATED ||
                this == FAILED;
    }
}
