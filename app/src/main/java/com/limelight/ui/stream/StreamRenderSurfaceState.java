package com.limelight.ui.stream;

/**
 * Platform-independent lifecycle state for the stream render Surface.
 */
final class StreamRenderSurfaceState {
    private boolean created;
    private boolean valid;

    void onCreated() {
        created = true;
        valid = false;
    }

    void onChanged(boolean surfaceValid) {
        requireCreated("changed");
        valid = surfaceValid;
    }

    boolean canStart(boolean sessionCanStart) {
        return created && valid && sessionCanStart;
    }

    void onDestroyed() {
        requireCreated("destroyed");
        created = false;
        valid = false;
    }

    void reset() {
        created = false;
        valid = false;
    }

    private void requireCreated(String event) {
        if (!created) {
            throw new IllegalStateException(
                    "Surface " + event + " before creation");
        }
    }
}
