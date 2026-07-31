package com.limelight.preferences;

import com.limelight.settings.stream.StreamDisplayGeometry;
import com.limelight.settings.stream.StreamResolutionCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Pure policy that turns device capabilities into settings-list changes. */
final class SettingsDisplayPolicy {
    private SettingsDisplayPolicy() {
    }

    static Result evaluate(
            SettingsDisplayCapabilities capabilities,
            String customResolution,
            boolean unlockFrameRates) {
        LinkedHashMap<String, ResolutionOption> resolutions =
                new LinkedHashMap<>();
        boolean invalidCustomResolution = !appendCustomResolution(
                resolutions,
                customResolution);
        for (SettingsDisplayCapabilities.NativeResolution candidate :
                capabilities.getNativeResolutions()) {
            appendResolutionPair(
                    resolutions,
                    candidate.getWidth(),
                    candidate.getHeight(),
                    candidate.isFullscreen());
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

    private static boolean appendCustomResolution(
            LinkedHashMap<String, ResolutionOption> resolutions,
            String customResolution) {
        if (customResolution == null || customResolution.isEmpty()) {
            return true;
        }
        String[] dimensions = customResolution.split("x");
        if (dimensions.length != 2) {
            return false;
        }
        try {
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            if (width <= 0 || height <= 0) {
                return false;
            }
            appendResolutionPair(
                    resolutions,
                    width,
                    height,
                    false);
            return true;
        }
        catch (NumberFormatException error) {
            return false;
        }
    }

    private static void appendResolutionPair(
            LinkedHashMap<String, ResolutionOption> resolutions,
            int width,
            int height,
            boolean fullscreen) {
        if (StreamDisplayGeometry.isSquarish(width, height)) {
            appendResolution(
                    resolutions,
                    height,
                    width,
                    fullscreen,
                    OrientationLabel.PORTRAIT);
            appendResolution(
                    resolutions,
                    width,
                    height,
                    fullscreen,
                    OrientationLabel.LANDSCAPE);
        }
        else {
            appendResolution(
                    resolutions,
                    width,
                    height,
                    fullscreen,
                    OrientationLabel.NONE);
        }
    }

    private static void appendResolution(
            LinkedHashMap<String, ResolutionOption> resolutions,
            int width,
            int height,
            boolean fullscreen,
            OrientationLabel orientationLabel) {
        ResolutionOption option = new ResolutionOption(
                width,
                height,
                fullscreen,
                orientationLabel);
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

        private ResolutionOption(
                int width,
                int height,
                boolean fullscreen,
                OrientationLabel orientationLabel) {
            this.width = width;
            this.height = height;
            this.fullscreen = fullscreen;
            this.orientationLabel = orientationLabel;
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
        private final List<ResolutionOption> nativeResolutions;
        private final List<ValueRemoval> resolutionRemovals;
        private final List<ValueRemoval> frameRateRemovals;
        private final int nativeFrameRate;
        private final SettingsDisplayCapabilities.HdrState hdrState;
        private final boolean invalidCustomResolution;

        private Result(
                List<ResolutionOption> nativeResolutions,
                List<ValueRemoval> resolutionRemovals,
                List<ValueRemoval> frameRateRemovals,
                int nativeFrameRate,
                SettingsDisplayCapabilities.HdrState hdrState,
                boolean invalidCustomResolution) {
            this.nativeResolutions = Collections.unmodifiableList(
                    new ArrayList<>(nativeResolutions));
            this.resolutionRemovals = Collections.unmodifiableList(
                    new ArrayList<>(resolutionRemovals));
            this.frameRateRemovals = Collections.unmodifiableList(
                    new ArrayList<>(frameRateRemovals));
            this.nativeFrameRate = nativeFrameRate;
            this.hdrState = hdrState;
            this.invalidCustomResolution = invalidCustomResolution;
        }

        List<ResolutionOption> getNativeResolutions() {
            return nativeResolutions;
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
