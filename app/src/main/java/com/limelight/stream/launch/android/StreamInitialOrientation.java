package com.limelight.stream.launch.android;

import com.limelight.settings.stream.StreamVideoSettings;

import java.util.Objects;

/** Immutable launch-time orientation derived from the prepared session. */
public enum StreamInitialOrientation {
    LANDSCAPE,
    PORTRAIT;

    public static StreamInitialOrientation from(
            StreamVideoSettings videoSettings) {
        return Objects.requireNonNull(
                videoSettings,
                "videoSettings").isPortrait()
                ? PORTRAIT
                : LANDSCAPE;
    }
}
