package com.limelight.settings.stream;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic owner of the persisted stream-video launch policy.
 */
public final class StreamVideoSettingsState {
    private final AtomicReference<StreamVideoSettings> current;

    public StreamVideoSettingsState(
            StreamVideoSettings initial) {
        current = new AtomicReference<>(
                Objects.requireNonNull(initial, "initial"));
    }

    public StreamVideoSettings get() {
        return current.get();
    }

    public void replace(StreamVideoSettings replacement) {
        current.set(
                Objects.requireNonNull(
                        replacement,
                        "replacement"));
    }
}
