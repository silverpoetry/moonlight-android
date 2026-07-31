package com.limelight.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable display and decoder capabilities consumed by settings policy. */
final class SettingsDisplayCapabilities {
    enum HdrState {
        UNAVAILABLE,
        AVAILABLE,
        BLOCKED_BY_FIRMWARE
    }

    private final List<NativeResolution> nativeResolutions;
    private final int maximumSupportedPresetWidth;
    private final float maximumRefreshRate;
    private final HdrState hdrState;

    private SettingsDisplayCapabilities(Builder builder) {
        nativeResolutions = Collections.unmodifiableList(
                new ArrayList<>(builder.nativeResolutions));
        maximumSupportedPresetWidth =
                builder.maximumSupportedPresetWidth;
        maximumRefreshRate = builder.maximumRefreshRate;
        hdrState = builder.hdrState;
    }

    static Builder builder() {
        return new Builder();
    }

    List<NativeResolution> getNativeResolutions() {
        return nativeResolutions;
    }

    int getMaximumSupportedPresetWidth() {
        return maximumSupportedPresetWidth;
    }

    float getMaximumRefreshRate() {
        return maximumRefreshRate;
    }

    HdrState getHdrState() {
        return hdrState;
    }

    static final class NativeResolution {
        private final int width;
        private final int height;
        private final boolean fullscreen;

        private NativeResolution(
                int width,
                int height,
                boolean fullscreen) {
            this.width = width;
            this.height = height;
            this.fullscreen = fullscreen;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }

        boolean isFullscreen() {
            return fullscreen;
        }
    }

    static final class Builder {
        private final ArrayList<NativeResolution> nativeResolutions =
                new ArrayList<>();
        private int maximumSupportedPresetWidth;
        private float maximumRefreshRate;
        private HdrState hdrState = HdrState.UNAVAILABLE;

        Builder addNativeResolution(
                int width,
                int height,
                boolean fullscreen) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException(
                        "Native resolution must be positive");
            }
            nativeResolutions.add(new NativeResolution(
                    width,
                    height,
                    fullscreen));
            return this;
        }

        Builder maximumSupportedPresetWidth(int value) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Maximum width must not be negative");
            }
            maximumSupportedPresetWidth = value;
            return this;
        }

        Builder maximumRefreshRate(float value) {
            if (!Float.isFinite(value) || value < 0) {
                throw new IllegalArgumentException(
                        "Maximum refresh rate must be finite and non-negative");
            }
            maximumRefreshRate = value;
            return this;
        }

        Builder hdrState(HdrState value) {
            if (value == null) {
                throw new NullPointerException("hdrState");
            }
            hdrState = value;
            return this;
        }

        SettingsDisplayCapabilities build() {
            return new SettingsDisplayCapabilities(this);
        }
    }
}
