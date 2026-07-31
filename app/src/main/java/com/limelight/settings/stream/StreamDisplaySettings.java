package com.limelight.settings.stream;

import java.util.Objects;

/**
 * Immutable settings snapshot for stream layout and presentation.
 *
 * <p>This object is created once during stream composition. Render and input
 * paths never reread persistent storage.</p>
 */
public final class StreamDisplaySettings {
    public enum Gravity {
        DEFAULT("0"),
        TOP_CENTER("1"),
        TOP_LEFT("2"),
        TOP_RIGHT("3"),
        BOTTOM_CENTER("4"),
        BOTTOM_LEFT("5"),
        BOTTOM_RIGHT("6");

        private final String storageValue;

        Gravity(String storageValue) {
            this.storageValue = storageValue;
        }

        public static Gravity fromStorageValue(String value) {
            for (Gravity gravity : values()) {
                if (gravity.storageValue.equals(value)) {
                    return gravity;
                }
            }
            return DEFAULT;
        }
    }

    private final int streamWidth;
    private final int streamHeight;
    private final boolean nativeResolution;
    private final boolean stretchVideo;
    private final boolean displayCutoutEnabled;
    private final boolean externalDisplayEnabled;
    private final boolean hdrEnabled;
    private final Gravity gravity;

    public StreamDisplaySettings(
            int streamWidth,
            int streamHeight,
            boolean nativeResolution,
            boolean stretchVideo,
            boolean displayCutoutEnabled,
            boolean externalDisplayEnabled,
            boolean hdrEnabled,
            Gravity gravity) {
        if (streamWidth <= 0 || streamHeight <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions must be positive");
        }
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        this.nativeResolution = nativeResolution;
        this.stretchVideo = stretchVideo;
        this.displayCutoutEnabled = displayCutoutEnabled;
        this.externalDisplayEnabled = externalDisplayEnabled;
        this.hdrEnabled = hdrEnabled;
        this.gravity = Objects.requireNonNull(gravity, "gravity");
    }

    public int getStreamWidth() {
        return streamWidth;
    }

    public int getStreamHeight() {
        return streamHeight;
    }

    public boolean isNativeResolution() {
        return nativeResolution;
    }

    public boolean isStretchVideo() {
        return stretchVideo;
    }

    public boolean isDisplayCutoutEnabled() {
        return displayCutoutEnabled;
    }

    public boolean isExternalDisplayEnabled() {
        return externalDisplayEnabled;
    }

    public boolean isHdrEnabled() {
        return hdrEnabled;
    }

    public Gravity getGravity() {
        return gravity;
    }

}
