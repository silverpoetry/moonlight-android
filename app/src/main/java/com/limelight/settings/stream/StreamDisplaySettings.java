package com.limelight.settings.stream;

import java.util.Objects;

/**
 * Immutable settings snapshot for stream layout and FSR presentation.
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

    public enum FsrTarget {
        OFF("off", 0, 0),
        OUTPUT_2K("2k", 1440, 2560),
        OUTPUT_4K("4k", 2160, 3840),
        NATIVE_HEIGHT("native_height", 0, 0),
        /**
         * Preserves the established fallback for a syntactically valid but
         * unrecognized stored target: FSR remains enabled at 1440p without a
         * minimum width. This is distinct from a missing or wrong-type value,
         * which the schema resolves to {@link #OFF}.
         */
        UNKNOWN("", 1440, 0);

        private final String storageValue;
        private final int outputHeight;
        private final int minimumOutputWidth;

        FsrTarget(
                String storageValue,
                int outputHeight,
                int minimumOutputWidth) {
            this.storageValue = storageValue;
            this.outputHeight = outputHeight;
            this.minimumOutputWidth = minimumOutputWidth;
        }

        public static FsrTarget fromStorageValue(String value) {
            for (FsrTarget target : values()) {
                if (target != UNKNOWN &&
                        target.storageValue.equalsIgnoreCase(value)) {
                    return target;
                }
            }
            return UNKNOWN;
        }

        public int getOutputHeight() {
            return outputHeight;
        }

        public int getMinimumOutputWidth() {
            return minimumOutputWidth;
        }
    }

    public enum FsrSharpness {
        SOFT("soft", 0.55f),
        STANDARD("standard", 0.85f),
        STRONG("strong", 1.45f),
        MAXIMUM("max", 1.85f);

        private final String storageValue;
        private final float factor;

        FsrSharpness(String storageValue, float factor) {
            this.storageValue = storageValue;
            this.factor = factor;
        }

        public static FsrSharpness fromStorageValue(String value) {
            for (FsrSharpness sharpness : values()) {
                if (sharpness.storageValue.equalsIgnoreCase(value)) {
                    return sharpness;
                }
            }
            return STANDARD;
        }

        public float getFactor() {
            return factor;
        }
    }

    public enum FsrHdrOutput {
        SDR("sdr"),
        NATIVE("native");

        private final String storageValue;

        FsrHdrOutput(String storageValue) {
            this.storageValue = storageValue;
        }

        public static FsrHdrOutput fromStorageValue(String value) {
            for (FsrHdrOutput output : values()) {
                if (output.storageValue.equalsIgnoreCase(value)) {
                    return output;
                }
            }
            return SDR;
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
    private final FsrTarget fsrTarget;
    private final FsrSharpness fsrSharpness;
    private final FsrHdrOutput fsrHdrOutput;

    public StreamDisplaySettings(
            int streamWidth,
            int streamHeight,
            boolean nativeResolution,
            boolean stretchVideo,
            boolean displayCutoutEnabled,
            boolean externalDisplayEnabled,
            boolean hdrEnabled,
            Gravity gravity,
            FsrTarget fsrTarget,
            FsrSharpness fsrSharpness,
            FsrHdrOutput fsrHdrOutput) {
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
        this.fsrTarget = Objects.requireNonNull(
                fsrTarget,
                "fsrTarget");
        this.fsrSharpness = Objects.requireNonNull(
                fsrSharpness,
                "fsrSharpness");
        this.fsrHdrOutput = Objects.requireNonNull(
                fsrHdrOutput,
                "fsrHdrOutput");
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

    public Gravity getGravity() {
        return gravity;
    }

    public FsrTarget getFsrTarget() {
        return fsrTarget;
    }

    public FsrSharpness getFsrSharpness() {
        return fsrSharpness;
    }

    public boolean isFsrEnabled() {
        return !externalDisplayEnabled &&
                fsrTarget != FsrTarget.OFF;
    }

    public boolean isNativeHeightFsrTarget() {
        return fsrTarget == FsrTarget.NATIVE_HEIGHT;
    }

    public boolean isNativeHdrOutputEnabled() {
        return hdrEnabled &&
                fsrHdrOutput == FsrHdrOutput.NATIVE;
    }
}
