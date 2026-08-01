package com.limelight.settings.ui;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic live owner of one stream's in-stream UI policy.
 */
public final class StreamUiSettingsState {
    private final AtomicReference<StreamUiSettings> current;

    public StreamUiSettingsState(StreamUiSettings initial) {
        current = new AtomicReference<>(
                Objects.requireNonNull(initial, "initial"));
    }

    public StreamUiSettings get() {
        return current.get();
    }

    public void replace(StreamUiSettings replacement) {
        current.set(
                Objects.requireNonNull(
                        replacement,
                        "replacement"));
    }
}
