package com.limelight.binding.input;

import java.util.Objects;

/**
 * Owns the desired LED state for one controller without owning an Android
 * input-device identity.
 */
final class ControllerLedSession {
    interface Target {
        boolean isAvailable();

        void applyArgb(int argb);

        void close();
    }

    static final class DesiredState {
        private final boolean hasColor;
        private final int argb;

        private DesiredState(boolean hasColor, int argb) {
            this.hasColor = hasColor;
            this.argb = argb;
        }
    }

    private static final Target UNAVAILABLE_TARGET = new Target() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void applyArgb(int argb) {
        }

        @Override
        public void close() {
        }
    };

    private final Target target;
    private boolean hasDesiredColor;
    private int desiredArgb;
    private boolean destroyed;

    ControllerLedSession(Target target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    static ControllerLedSession unavailable() {
        return new ControllerLedSession(UNAVAILABLE_TARGET);
    }

    static Target unavailableTarget() {
        return UNAVAILABLE_TARGET;
    }

    synchronized boolean isAvailable() {
        return !destroyed && target.isAvailable();
    }

    synchronized void setColor(byte red, byte green, byte blue) {
        if (destroyed || !target.isAvailable()) {
            return;
        }

        desiredArgb = toArgb(red, green, blue);
        hasDesiredColor = true;
        target.applyArgb(desiredArgb);
    }

    synchronized DesiredState snapshotDesiredState() {
        return new DesiredState(hasDesiredColor, desiredArgb);
    }

    synchronized void restoreDesiredState(DesiredState state) {
        Objects.requireNonNull(state, "state");
        if (destroyed || !state.hasColor || !target.isAvailable()) {
            return;
        }

        hasDesiredColor = true;
        desiredArgb = state.argb;
        target.applyArgb(desiredArgb);
    }

    synchronized void destroy() {
        if (destroyed) {
            return;
        }

        destroyed = true;
        target.close();
    }

    private static int toArgb(byte red, byte green, byte blue) {
        return 0xFF000000 |
                ((red & 0xFF) << 16) |
                ((green & 0xFF) << 8) |
                (blue & 0xFF);
    }
}
