package com.limelight.settings.stream;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure codec for the string-backed stream resolution and frame-rate settings.
 */
public final class StreamResolutionCodec {
    public static final String RESOLUTION_360P = "640x360";
    public static final String RESOLUTION_480P = "854x480";
    public static final String RESOLUTION_720P = "1280x720";
    public static final String RESOLUTION_1080P = "1920x1080";
    public static final String RESOLUTION_1440P = "2560x1440";
    public static final String RESOLUTION_4K = "3840x2160";
    public static final String DEFAULT_RESOLUTION = RESOLUTION_720P;
    public static final String DEFAULT_FPS = "60";
    public static final String SELECTION_PRESET = "preset";
    public static final String SELECTION_CUSTOM_OR_NATIVE =
            "custom_or_native";
    public static final String ASPECT_RATIO_16_9 = "16_9";
    public static final String ASPECT_RATIO_NATIVE = "native";

    private StreamResolutionCodec() {
    }

    public static Result decode(
            String resolutionValue,
            String selectionValue,
            String aspectRatioValue,
            String fpsValue,
            DisplayAspect displayAspect) {
        Objects.requireNonNull(displayAspect, "displayAspect");

        String originalResolution = resolutionValue;
        NormalizedResolution normalizedResolution =
                normalizeResolutionValue(resolutionValue);
        String canonicalResolution =
                normalizedResolution.canonicalValue;

        Selection selection;
        AspectRatio aspectRatio;
        if (normalizedResolution.forcePreset) {
            selection = Selection.PRESET;
            aspectRatio = AspectRatio.VIDEO_16_9;
        }
        else {
            selection = Selection.fromStorageValue(selectionValue);
            aspectRatio = AspectRatio.fromStorageValue(aspectRatioValue);
        }

        Dimensions dimensions = parseDimensions(canonicalResolution);
        if (dimensions == null) {
            canonicalResolution = DEFAULT_RESOLUTION;
            dimensions = requireDefaultDimensions();
            selection = Selection.PRESET;
            aspectRatio = AspectRatio.VIDEO_16_9;
        }

        int resolvedHeight = dimensions.height;
        if (selection == Selection.PRESET &&
                isStandardResolutionPreset(canonicalResolution) &&
                aspectRatio == AspectRatio.NATIVE_DISPLAY) {
            resolvedHeight = displayAspect.getEvenHeightForWidth(
                    dimensions.width);
        }

        int fps = parsePositiveInt(fpsValue, 60);
        String canonicalFps = Integer.toString(fps);
        String canonicalSelection = selection.storageValue;
        String canonicalAspectRatio = aspectRatio.storageValue;

        boolean repairRequired =
                !canonicalResolution.equals(originalResolution) ||
                !canonicalFps.equals(fpsValue) ||
                !canonicalSelection.equals(selectionValue) ||
                !canonicalAspectRatio.equals(aspectRatioValue);

        return new Result(
                dimensions.width,
                resolvedHeight,
                fps,
                selection,
                aspectRatio,
                canonicalResolution,
                canonicalFps,
                canonicalSelection,
                canonicalAspectRatio,
                repairRequired);
    }

    public static boolean isStandardResolutionPreset(String value) {
        return RESOLUTION_360P.equals(value) ||
                RESOLUTION_480P.equals(value) ||
                RESOLUTION_720P.equals(value) ||
                RESOLUTION_1080P.equals(value) ||
                RESOLUTION_1440P.equals(value) ||
                RESOLUTION_4K.equals(value);
    }

    private static NormalizedResolution normalizeResolutionValue(
            String value) {
        if (value == null) {
            return NormalizedResolution.repairedDefault();
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('×', 'x');
        switch (normalized) {
            case "360p":
                return NormalizedResolution.legacy(
                        RESOLUTION_360P);
            case "480p":
                return NormalizedResolution.legacy(
                        RESOLUTION_480P);
            case "720p":
                return NormalizedResolution.legacy(
                        RESOLUTION_720P);
            case "1080p":
                return NormalizedResolution.legacy(
                        RESOLUTION_1080P);
            case "1440p":
                return NormalizedResolution.legacy(
                        RESOLUTION_1440P);
            case "4k":
                return NormalizedResolution.legacy(
                        RESOLUTION_4K);
            default:
                Dimensions parsed = parseDimensions(normalized);
                if (parsed == null) {
                    return NormalizedResolution.repairedDefault();
                }
                return NormalizedResolution.valid(
                        parsed.width + "x" + parsed.height);
        }
    }

    private static Dimensions parseDimensions(String value) {
        if (value == null) {
            return null;
        }
        int separator = value.indexOf('x');
        if (separator <= 0 ||
                separator != value.lastIndexOf('x') ||
                separator == value.length() - 1) {
            return null;
        }

        int width = parsePositiveInt(
                value.substring(0, separator),
                -1);
        int height = parsePositiveInt(
                value.substring(separator + 1),
                -1);
        return width > 0 && height > 0
                ? new Dimensions(width, height)
                : null;
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        }
        catch (NumberFormatException invalidValue) {
            return fallback;
        }
    }

    private static Dimensions requireDefaultDimensions() {
        Dimensions dimensions = parseDimensions(DEFAULT_RESOLUTION);
        if (dimensions == null) {
            throw new AssertionError("Invalid default resolution");
        }
        return dimensions;
    }

    public enum Selection {
        PRESET(SELECTION_PRESET),
        CUSTOM_OR_NATIVE(SELECTION_CUSTOM_OR_NATIVE);

        private final String storageValue;

        Selection(String storageValue) {
            this.storageValue = storageValue;
        }

        static Selection fromStorageValue(String value) {
            return SELECTION_PRESET.equals(value)
                    ? PRESET
                    : CUSTOM_OR_NATIVE;
        }
    }

    public enum AspectRatio {
        VIDEO_16_9(ASPECT_RATIO_16_9),
        NATIVE_DISPLAY(ASPECT_RATIO_NATIVE);

        private final String storageValue;

        AspectRatio(String storageValue) {
            this.storageValue = storageValue;
        }

        static AspectRatio fromStorageValue(String value) {
            return ASPECT_RATIO_NATIVE.equals(value)
                    ? NATIVE_DISPLAY
                    : VIDEO_16_9;
        }
    }

    public static final class DisplayAspect {
        private final int longSide;
        private final int shortSide;

        public DisplayAspect(int width, int height) {
            if (width <= 0 || height <= 0) {
                longSide = 16;
                shortSide = 9;
            }
            else {
                longSide = Math.max(width, height);
                shortSide = Math.min(width, height);
            }
        }

        int getEvenHeightForWidth(int width) {
            long scaledHeight =
                    Math.round((double) width * shortSide / longSide);
            int boundedHeight = (int) Math.max(
                    2,
                    Math.min(Integer.MAX_VALUE, scaledHeight));
            return boundedHeight & ~1;
        }
    }

    public static final class Result {
        private final int width;
        private final int height;
        private final int fps;
        private final Selection selection;
        private final AspectRatio aspectRatio;
        private final String canonicalResolution;
        private final String canonicalFps;
        private final String canonicalSelection;
        private final String canonicalAspectRatio;
        private final boolean repairRequired;

        Result(
                int width,
                int height,
                int fps,
                Selection selection,
                AspectRatio aspectRatio,
                String canonicalResolution,
                String canonicalFps,
                String canonicalSelection,
                String canonicalAspectRatio,
                boolean repairRequired) {
            this.width = width;
            this.height = height;
            this.fps = fps;
            this.selection = selection;
            this.aspectRatio = aspectRatio;
            this.canonicalResolution = canonicalResolution;
            this.canonicalFps = canonicalFps;
            this.canonicalSelection = canonicalSelection;
            this.canonicalAspectRatio = canonicalAspectRatio;
            this.repairRequired = repairRequired;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getFps() {
            return fps;
        }

        public Selection getSelection() {
            return selection;
        }

        public AspectRatio getAspectRatio() {
            return aspectRatio;
        }

        public String getCanonicalResolution() {
            return canonicalResolution;
        }

        public String getCanonicalFps() {
            return canonicalFps;
        }

        public String getCanonicalSelection() {
            return canonicalSelection;
        }

        public String getCanonicalAspectRatio() {
            return canonicalAspectRatio;
        }

        public boolean isRepairRequired() {
            return repairRequired;
        }

        /**
         * Matches the historical native-resolution policy without coupling
         * consumers to the legacy preference aggregate.
         */
        public boolean isNativeResolution() {
            return selection != Selection.PRESET &&
                    !isStandardResolutionPreset(
                            canonicalResolution);
        }
    }

    private static final class Dimensions {
        final int width;
        final int height;

        Dimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final class NormalizedResolution {
        final String canonicalValue;
        final boolean forcePreset;

        private NormalizedResolution(
                String canonicalValue,
                boolean forcePreset) {
            this.canonicalValue = canonicalValue;
            this.forcePreset = forcePreset;
        }

        static NormalizedResolution valid(String canonicalValue) {
            return new NormalizedResolution(canonicalValue, false);
        }

        static NormalizedResolution legacy(String canonicalValue) {
            return new NormalizedResolution(canonicalValue, true);
        }

        static NormalizedResolution repairedDefault() {
            return new NormalizedResolution(DEFAULT_RESOLUTION, true);
        }
    }
}
