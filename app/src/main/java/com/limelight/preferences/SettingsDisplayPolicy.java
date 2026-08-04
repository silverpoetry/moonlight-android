package com.limelight.preferences;

import com.limelight.settings.stream.CustomResolution;
import com.limelight.settings.stream.StreamDisplayGeometry;
import com.limelight.settings.stream.StreamResolutionCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Pure policy that turns device capabilities into settings-list changes. */
final class SettingsDisplayPolicy {
    private SettingsDisplayPolicy() {
    }

    static Result evaluate(
            SettingsDisplayCapabilities capabilities,
            Set<String> customResolutions,
            boolean unlockFrameRates) {
        LinkedHashMap<String, ResolutionOption> resolutions =
                new LinkedHashMap<>();
        boolean invalidCustomResolution = !appendCustomResolutions(
                resolutions,
                customResolutions);
        for (SettingsDisplayCapabilities.NativeResolution candidate :
                capabilities.getNativeResolutions()) {
            appendResolutionPair(
                    resolutions,
                    candidate.getWidth(),
                    candidate.getHeight(),
                    candidate.isFullscreen(),
                    false);
        }

        ArrayList<ValueRemoval> resolutionRemovals = new ArrayList<>();
        int maximumWidth =
                capabilities.getMaximumSupportedPresetWidth();
        if (maximumWidth != 0) {
            if (maximumWidth < 3840) {
                resolutionRemovals.add(new ValueRemoval(
                        StreamResolutionCodec.RESOLUTION_4K,
                        StreamResolutionCodec.RESOLUTION_1440P));
            }
            if (maximumWidth < 2560) {
                resolutionRemovals.add(new ValueRemoval(
                        StreamResolutionCodec.RESOLUTION_1440P,
                        StreamResolutionCodec.RESOLUTION_1080P));
            }
            if (maximumWidth < 1920) {
                resolutionRemovals.add(new ValueRemoval(
                        StreamResolutionCodec.RESOLUTION_1080P,
                        StreamResolutionCodec.RESOLUTION_720P));
            }
        }

        ArrayList<ValueRemoval> frameRateRemovals = new ArrayList<>();
        float maximumRefreshRate = capabilities.getMaximumRefreshRate();
        if (!unlockFrameRates) {
            if (maximumRefreshRate < 118) {
                frameRateRemovals.add(new ValueRemoval("120", "90"));
            }
            if (maximumRefreshRate < 88) {
                frameRateRemovals.add(new ValueRemoval("90", "60"));
            }
        }

        return new Result(
                new ArrayList<>(resolutions.values()),
                resolutionRemovals,
                frameRateRemovals,
                Math.round(maximumRefreshRate),
                capabilities.getHdrState(),
                invalidCustomResolution);
    }

    private static boolean appendCustomResolutions(
            LinkedHashMap<String, ResolutionOption> resolutions,
            Set<String> customResolutions) {
        if (customResolutions == null || customResolutions.isEmpty()) {
            return true;
        }
        boolean valid = true;
        TreeSet<CustomResolution> sorted = new TreeSet<>();
        for (String value : customResolutions) {
            CustomResolution resolution = CustomResolution.parse(value);
            if (resolution == null) {
                valid = false;
            }
            else if (!StreamResolutionCodec.isStandardResolutionPreset(
                    resolution.toStorageValue())) {
                sorted.add(resolution);
            }
        }
        for (CustomResolution resolution : sorted) {
            appendResolution(
                    resolutions,
                    resolution.getWidth(),
                    resolution.getHeight(),
                    false,
                    OrientationLabel.NONE,
                    true);
        }
        return valid;
    }

    private static void appendResolutionPair(
            LinkedHashMap<String, ResolutionOption> resolutions,
            int width,
            int height,
            boolean fullscreen,
            boolean custom) {
        if (StreamDisplayGeometry.isSquarish(width, height)) {
            appendResolution(
                    resolutions,
                    height,
                    width,
                    fullscreen,
                    OrientationLabel.PORTRAIT,
                    custom);
            appendResolution(
                    resolutions,
                    width,
                    height,
                    fullscreen,
                    OrientationLabel.LANDSCAPE,
                    custom);
        }
        else {
            appendResolution(
                    resolutions,
                    width,
                    height,
                    fullscreen,
                    OrientationLabel.NONE,
                    custom);
        }
    }

    private static void appendResolution(
            LinkedHashMap<String, ResolutionOption> resolutions,
            int width,
            int height,
            boolean fullscreen,
            OrientationLabel orientationLabel,
            boolean custom) {
        ResolutionOption option = new ResolutionOption(
                width,
                height,
                fullscreen,
                orientationLabel,
                custom);
        if (!resolutions.containsKey(option.getValue())) {
            resolutions.put(option.getValue(), option);
        }
    }

    enum OrientationLabel {
        NONE,
        PORTRAIT,
        LANDSCAPE
    }

    static final class ResolutionOption {
        private final int width;
        private final int height;
        private final boolean fullscreen;
        private final OrientationLabel orientationLabel;
        private final boolean custom;

        private ResolutionOption(
                int width,
                int height,
                boolean fullscreen,
                OrientationLabel orientationLabel,
                boolean custom) {
            this.width = width;
            this.height = height;
            this.fullscreen = fullscreen;
            this.orientationLabel = orientationLabel;
            this.custom = custom;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }

        String getValue() {
            return width + "x" + height;
        }

        boolean isFullscreen() {
            return fullscreen;
        }

        OrientationLabel getOrientationLabel() {
            return orientationLabel;
        }

        boolean isCustom() {
            return custom;
        }
    }

    static final class ValueRemoval {
        private final String value;
        private final String fallbackValue;

        private ValueRemoval(String value, String fallbackValue) {
            this.value = value;
            this.fallbackValue = fallbackValue;
        }

        String getValue() {
            return value;
        }

        String getFallbackValue() {
            return fallbackValue;
        }
    }

    static final class Result {
        private final List<ResolutionOption> resolutionOptions;
        private final List<ValueRemoval> resolutionRemovals;
        private final List<ValueRemoval> frameRateRemovals;
        private final int nativeFrameRate;
        private final SettingsDisplayCapabilities.HdrState hdrState;
        private final boolean invalidCustomResolution;

        private Result(
                List<ResolutionOption> resolutionOptions,
                List<ValueRemoval> resolutionRemovals,
                List<ValueRemoval> frameRateRemovals,
                int nativeFrameRate,
                SettingsDisplayCapabilities.HdrState hdrState,
                boolean invalidCustomResolution) {
            this.resolutionOptions = Collections.unmodifiableList(
                    new ArrayList<>(resolutionOptions));
            this.resolutionRemovals = Collections.unmodifiableList(
                    new ArrayList<>(resolutionRemovals));
            this.frameRateRemovals = Collections.unmodifiableList(
                    new ArrayList<>(frameRateRemovals));
            this.nativeFrameRate = nativeFrameRate;
            this.hdrState = hdrState;
            this.invalidCustomResolution = invalidCustomResolution;
        }

        List<ResolutionOption> getResolutionOptions() {
            return resolutionOptions;
        }

        List<ValueRemoval> getResolutionRemovals() {
            return resolutionRemovals;
        }

        List<ValueRemoval> getFrameRateRemovals() {
            return frameRateRemovals;
        }

        int getNativeFrameRate() {
            return nativeFrameRate;
        }

        SettingsDisplayCapabilities.HdrState getHdrState() {
            return hdrState;
        }

        boolean hasInvalidCustomResolution() {
            return invalidCustomResolution;
        }
    }
}
