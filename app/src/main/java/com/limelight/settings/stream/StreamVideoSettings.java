package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;
import com.limelight.settings.stream.StreamDisplaySettings.FsrHdrOutput;
import com.limelight.settings.stream.StreamDisplaySettings.FsrSharpness;
import com.limelight.settings.stream.StreamDisplaySettings.FsrTarget;

import java.util.Objects;

/**
 * Immutable persisted policy for composing the next stream's video session.
 */
public final class StreamVideoSettings {
    public enum ScreenOnPolicy {
        DISABLED(0),
        CURRENT_SESSION(1),
        ALWAYS(2);

        private final int storageValue;

        ScreenOnPolicy(int storageValue) {
            this.storageValue = storageValue;
        }

        public int getStorageValue() {
            return storageValue;
        }

        static ScreenOnPolicy fromStorageValue(int value) {
            if (value == 1) {
                return CURRENT_SESSION;
            }
            if (value == 2) {
                return ALWAYS;
            }
            return DISABLED;
        }
    }

    public enum VirtualDisplayMode {
        DISABLED(0),
        EXTENDED(1),
        VIRTUAL_ONLY(2);

        private final int storageValue;

        VirtualDisplayMode(int storageValue) {
            this.storageValue = storageValue;
        }

        public int getStorageValue() {
            return storageValue;
        }

        static VirtualDisplayMode fromStorageValue(int value) {
            if (value == 1) {
                return EXTENDED;
            }
            if (value == 2) {
                return VIRTUAL_ONLY;
            }
            return DISABLED;
        }
    }

    private final int width;
    private final int height;
    private final int fps;
    private final int bitrateKbps;
    private final VideoFormat videoFormat;
    private final boolean hdrEnabled;
    private final boolean hdrHighBrightness;
    private final boolean ignoreHdrCapability;
    private final boolean lowLatencyExperimentEnabled;
    private final boolean fpsUnlocked;
    private final boolean portrait;
    private final boolean externalDisplay;
    private final boolean nativeResolution;
    private final boolean stretchVideo;
    private final boolean displayCutoutEnabled;
    private final boolean optimizeGameSettings;
    private final VirtualDisplayMode virtualDisplayMode;
    private final boolean enforceDisplayMode;
    private final ScreenOnPolicy screenOnPolicy;
    private final String fsrTargetStorageValue;
    private final String fsrSharpnessStorageValue;
    private final String fsrHdrOutputStorageValue;

    private StreamVideoSettings(Builder builder) {
        if (builder.width <= 0 || builder.height <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions must be positive");
        }
        if (builder.fps <= 0) {
            throw new IllegalArgumentException(
                    "Stream FPS must be positive");
        }
        if (builder.bitrateKbps <= 0) {
            throw new IllegalArgumentException(
                    "Stream bitrate must be positive");
        }
        width = builder.width;
        height = builder.height;
        fps = builder.fps;
        bitrateKbps = StreamVideoSettingKeys.BITRATE_KBPS
                .normalizeValue(builder.bitrateKbps);
        videoFormat = Objects.requireNonNull(
                builder.videoFormat,
                "videoFormat");
        hdrEnabled = builder.hdrEnabled;
        hdrHighBrightness = builder.hdrHighBrightness;
        ignoreHdrCapability = builder.ignoreHdrCapability;
        lowLatencyExperimentEnabled =
                builder.lowLatencyExperimentEnabled;
        fpsUnlocked = builder.fpsUnlocked;
        portrait = builder.portrait;
        externalDisplay = builder.externalDisplay;
        nativeResolution = builder.nativeResolution;
        stretchVideo = builder.stretchVideo;
        displayCutoutEnabled =
                builder.displayCutoutEnabled;
        optimizeGameSettings =
                builder.optimizeGameSettings;
        virtualDisplayMode = Objects.requireNonNull(
                builder.virtualDisplayMode,
                "virtualDisplayMode");
        enforceDisplayMode = builder.enforceDisplayMode;
        screenOnPolicy = Objects.requireNonNull(
                builder.screenOnPolicy,
                "screenOnPolicy");
        fsrTargetStorageValue =
                normalizeFsrValue(
                        StreamDisplaySettingKeys.FSR_TARGET,
                        builder.fsrTargetStorageValue);
        fsrSharpnessStorageValue =
                normalizeFsrValue(
                        StreamDisplaySettingKeys.FSR_SHARPNESS,
                        builder.fsrSharpnessStorageValue);
        fsrHdrOutputStorageValue =
                normalizeFsrValue(
                        StreamDisplaySettingKeys.FSR_HDR_OUTPUT,
                        builder.fsrHdrOutputStorageValue);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
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

    public int getBitrateKbps() {
        return bitrateKbps;
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public boolean isHdrEnabled() {
        return hdrEnabled;
    }

    public boolean isHdrHighBrightnessEnabled() {
        return hdrHighBrightness;
    }

    public boolean shouldIgnoreHdrCapability() {
        return ignoreHdrCapability;
    }

    public boolean isLowLatencyExperimentEnabled() {
        return lowLatencyExperimentEnabled;
    }

    public boolean isFpsUnlocked() {
        return fpsUnlocked;
    }

    public boolean isPortrait() {
        return portrait;
    }

    public boolean isExternalDisplay() {
        return externalDisplay;
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

    public boolean shouldOptimizeGameSettings() {
        return optimizeGameSettings;
    }

    public VirtualDisplayMode getVirtualDisplayMode() {
        return virtualDisplayMode;
    }

    public boolean shouldEnforceDisplayMode() {
        return enforceDisplayMode;
    }

    public ScreenOnPolicy getScreenOnPolicy() {
        return screenOnPolicy;
    }

    public FsrTarget getFsrTarget() {
        return FsrTarget.fromStorageValue(
                fsrTargetStorageValue);
    }

    public FsrSharpness getFsrSharpness() {
        return FsrSharpness.fromStorageValue(
                fsrSharpnessStorageValue);
    }

    public FsrHdrOutput getFsrHdrOutput() {
        return FsrHdrOutput.fromStorageValue(
                fsrHdrOutputStorageValue);
    }

    String getFsrTargetStorageValue() {
        return fsrTargetStorageValue;
    }

    String getFsrSharpnessStorageValue() {
        return fsrSharpnessStorageValue;
    }

    String getFsrHdrOutputStorageValue() {
        return fsrHdrOutputStorageValue;
    }

    private static String normalizeFsrValue(
            SettingKey<String> key,
            String value) {
        return key.normalizeValue(value);
    }

    public static final class Builder {
        private int width = 1280;
        private int height = 720;
        private int fps = 60;
        private int bitrateKbps = 10_000;
        private VideoFormat videoFormat = VideoFormat.AUTO;
        private boolean hdrEnabled;
        private boolean hdrHighBrightness;
        private boolean ignoreHdrCapability;
        private boolean lowLatencyExperimentEnabled = true;
        private boolean fpsUnlocked;
        private boolean portrait;
        private boolean externalDisplay;
        private boolean nativeResolution;
        private boolean stretchVideo;
        private boolean displayCutoutEnabled;
        private boolean optimizeGameSettings = true;
        private VirtualDisplayMode virtualDisplayMode =
                VirtualDisplayMode.DISABLED;
        private boolean enforceDisplayMode;
        private ScreenOnPolicy screenOnPolicy =
                ScreenOnPolicy.DISABLED;
        private String fsrTargetStorageValue = "off";
        private String fsrSharpnessStorageValue = "standard";
        private String fsrHdrOutputStorageValue = "native";

        private Builder() {
        }

        private Builder(StreamVideoSettings settings) {
            width = settings.width;
            height = settings.height;
            fps = settings.fps;
            bitrateKbps = settings.bitrateKbps;
            videoFormat = settings.videoFormat;
            hdrEnabled = settings.hdrEnabled;
            hdrHighBrightness =
                    settings.hdrHighBrightness;
            ignoreHdrCapability =
                    settings.ignoreHdrCapability;
            lowLatencyExperimentEnabled =
                    settings.lowLatencyExperimentEnabled;
            fpsUnlocked = settings.fpsUnlocked;
            portrait = settings.portrait;
            externalDisplay = settings.externalDisplay;
            nativeResolution = settings.nativeResolution;
            stretchVideo = settings.stretchVideo;
            displayCutoutEnabled =
                    settings.displayCutoutEnabled;
            optimizeGameSettings =
                    settings.optimizeGameSettings;
            virtualDisplayMode =
                    settings.virtualDisplayMode;
            enforceDisplayMode =
                    settings.enforceDisplayMode;
            screenOnPolicy = settings.screenOnPolicy;
            fsrTargetStorageValue =
                    settings.fsrTargetStorageValue;
            fsrSharpnessStorageValue =
                    settings.fsrSharpnessStorageValue;
            fsrHdrOutputStorageValue =
                    settings.fsrHdrOutputStorageValue;
        }

        public Builder setDimensions(int width, int height) {
            this.width = width;
            this.height = height;
            nativeResolution =
                    !StreamResolutionCodec
                            .isStandardResolutionPreset(
                                    width + "x" + height);
            return this;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder setBitrateKbps(int bitrateKbps) {
            this.bitrateKbps = bitrateKbps;
            return this;
        }

        public Builder setVideoFormat(VideoFormat videoFormat) {
            this.videoFormat = videoFormat;
            return this;
        }

        public Builder setHdrEnabled(boolean hdrEnabled) {
            this.hdrEnabled = hdrEnabled;
            return this;
        }

        public Builder setHdrHighBrightness(
                boolean enabled) {
            hdrHighBrightness = enabled;
            return this;
        }

        public Builder setIgnoreHdrCapability(boolean ignore) {
            ignoreHdrCapability = ignore;
            return this;
        }

        public Builder setLowLatencyExperimentEnabled(
                boolean enabled) {
            lowLatencyExperimentEnabled = enabled;
            return this;
        }

        public Builder setFpsUnlocked(boolean unlocked) {
            fpsUnlocked = unlocked;
            return this;
        }

        public Builder setPortrait(boolean portrait) {
            this.portrait = portrait;
            return this;
        }

        public Builder setExternalDisplay(boolean external) {
            externalDisplay = external;
            return this;
        }

        public Builder setNativeResolution(boolean nativeResolution) {
            this.nativeResolution = nativeResolution;
            return this;
        }

        public Builder setStretchVideo(boolean stretch) {
            stretchVideo = stretch;
            return this;
        }

        public Builder setDisplayCutoutEnabled(boolean enabled) {
            displayCutoutEnabled = enabled;
            return this;
        }

        public Builder setOptimizeGameSettings(boolean optimize) {
            optimizeGameSettings = optimize;
            return this;
        }

        public Builder setVirtualDisplayMode(
                VirtualDisplayMode mode) {
            virtualDisplayMode = mode;
            return this;
        }

        public Builder setEnforceDisplayMode(boolean enforce) {
            enforceDisplayMode = enforce;
            return this;
        }

        public Builder setScreenOnPolicy(
                ScreenOnPolicy policy) {
            screenOnPolicy = policy;
            return this;
        }

        public Builder setFsrTarget(FsrTarget target) {
            fsrTargetStorageValue =
                    StreamVideoSettingsCodec
                            .encodeFsrTarget(target);
            return this;
        }

        public Builder setFsrSharpness(
                FsrSharpness sharpness) {
            fsrSharpnessStorageValue =
                    StreamVideoSettingsCodec
                            .encodeFsrSharpness(sharpness);
            return this;
        }

        public Builder setFsrHdrOutput(
                FsrHdrOutput output) {
            fsrHdrOutputStorageValue =
                    StreamVideoSettingsCodec
                            .encodeFsrHdrOutput(output);
            return this;
        }

        Builder setPersistedFsrValues(
                String target,
                String sharpness,
                String hdrOutput) {
            fsrTargetStorageValue = target;
            fsrSharpnessStorageValue = sharpness;
            fsrHdrOutputStorageValue = hdrOutput;
            return this;
        }

        public StreamVideoSettings build() {
            return new StreamVideoSettings(this);
        }
    }
}
