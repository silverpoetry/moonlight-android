package com.limelight.settings.virtualcontrols;

import java.util.Objects;

/**
 * Atomically publishes virtual-control settings to active overlay views.
 */
public final class VirtualControlSettingsState {
    private volatile VirtualControlSettings current;

    public VirtualControlSettingsState(
            VirtualControlSettings initialSettings) {
        current = Objects.requireNonNull(
                initialSettings,
                "initialSettings");
    }

    public VirtualControlSettings get() {
        return current;
    }

    public void replace(VirtualControlSettings settings) {
        current = Objects.requireNonNull(settings, "settings");
    }
}
