package com.limelight.settings.input;

import java.util.Objects;

/**
 * Single owner of the active immutable input-settings snapshot.
 */
public final class InputSettingsState {
    private volatile InputSettings current;

    public InputSettingsState(InputSettings initialSettings) {
        current = Objects.requireNonNull(
                initialSettings,
                "initialSettings");
    }

    public InputSettings get() {
        return current;
    }

    public void replace(InputSettings settings) {
        current = Objects.requireNonNull(settings, "settings");
    }
}
