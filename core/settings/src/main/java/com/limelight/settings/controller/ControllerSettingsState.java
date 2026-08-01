package com.limelight.settings.controller;

import java.util.Objects;

/**
 * Atomically publishes controller-policy updates to callback threads.
 */
public final class ControllerSettingsState {
    private volatile ControllerSettings current;

    public ControllerSettingsState(ControllerSettings initialSettings) {
        current = Objects.requireNonNull(
                initialSettings,
                "initialSettings");
    }

    public ControllerSettings get() {
        return current;
    }

    public void replace(ControllerSettings settings) {
        current = Objects.requireNonNull(settings, "settings");
    }
}
