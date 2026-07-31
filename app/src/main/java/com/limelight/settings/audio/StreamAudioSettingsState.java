package com.limelight.settings.audio;

import java.util.Objects;

/**
 * Atomically publishes live stream-audio policy to callback threads.
 */
public final class StreamAudioSettingsState {
    private volatile StreamAudioSettings current;

    public StreamAudioSettingsState(
            StreamAudioSettings initialSettings) {
        current = Objects.requireNonNull(
                initialSettings,
                "initialSettings");
    }

    public StreamAudioSettings get() {
        return current;
    }

    public void replace(StreamAudioSettings settings) {
        current = Objects.requireNonNull(settings, "settings");
    }
}
