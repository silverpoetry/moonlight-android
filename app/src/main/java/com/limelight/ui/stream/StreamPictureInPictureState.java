package com.limelight.ui.stream;

/** Mutable session state for deterministic picture-in-picture auto-entry. */
public final class StreamPictureInPictureState {
    private boolean enabled;
    private boolean connected;
    private int suppressionCount;

    public StreamPictureInPictureState(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void acquireSuppression() {
        if (suppressionCount < Integer.MAX_VALUE) {
            suppressionCount++;
        }
    }

    public void releaseSuppression() {
        if (suppressionCount > 0) {
            suppressionCount--;
        }
    }

    public boolean shouldAutoEnter() {
        return enabled && connected && suppressionCount == 0;
    }

    int getSuppressionCount() {
        return suppressionCount;
    }
}
