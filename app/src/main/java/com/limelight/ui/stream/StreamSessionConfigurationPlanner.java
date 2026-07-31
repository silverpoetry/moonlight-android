package com.limelight.ui.stream;

import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamFramePacingPolicy;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.transfer.TransferSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Produces the immutable transport configuration for one stream launch.
 *
 * <p>Android discovery is completed before this boundary. The planner owns
 * cross-domain startup policy: decoder format advertisement, final HDR
 * eligibility, gamepad persistence, frame-pacing fallback, and translation
 * from typed settings to the common-c configuration document.</p>
 */
public final class StreamSessionConfigurationPlanner {
    public enum Warning {
        HDR_DECODER_UNSUPPORTED,
        FORCED_HEVC_DECODER_UNAVAILABLE,
        FORCED_AV1_DECODER_UNAVAILABLE
    }

    public enum NetworkMode {
        AUTO
    }

    public static final class SettingsSnapshot {
        private final StreamDecoderSettings decoder;
        private final StreamVideoSettings video;
        private final StreamAudioSettings audio;
        private final ControllerSettings controller;
        private final InputSettings input;
        private final TransferSettings transfer;

        public SettingsSnapshot(
                StreamDecoderSettings decoder,
                StreamVideoSettings video,
                StreamAudioSettings audio,
                ControllerSettings controller,
                InputSettings input,
                TransferSettings transfer) {
            this.decoder = Objects.requireNonNull(decoder, "decoder");
            this.video = Objects.requireNonNull(video, "video");
            this.audio = Objects.requireNonNull(audio, "audio");
            this.controller = Objects.requireNonNull(
                    controller,
                    "controller");
            this.input = Objects.requireNonNull(input, "input");
            this.transfer = Objects.requireNonNull(transfer, "transfer");
        }
    }

    public static final class Environment {
        private final NvApp app;
        private final StreamDecoderCapabilities decoderCapabilities;
        private final int discoveredGamepadMask;
        private final float displayRefreshRate;
        private final int pixelsPerInch;
        private final boolean hdrRequested;

        public Environment(
                NvApp app,
                StreamDecoderCapabilities decoderCapabilities,
                int discoveredGamepadMask,
                float displayRefreshRate,
                int pixelsPerInch,
                boolean hdrRequested) {
            this.app = Objects.requireNonNull(app, "app");
            this.decoderCapabilities = Objects.requireNonNull(
                    decoderCapabilities,
                    "decoderCapabilities");
            this.discoveredGamepadMask = discoveredGamepadMask;
            this.displayRefreshRate = displayRefreshRate;
            this.pixelsPerInch = pixelsPerInch;
            this.hdrRequested = hdrRequested;
        }
    }

    public static final class Plan {
        private final ConfigurationDocument configuration;
        private final StreamDecoderSettings.FramePacing
                effectiveFramePacing;
        private final boolean hdrEnabled;
        private final List<Warning> warnings;

        private Plan(
                ConfigurationDocument configuration,
                StreamDecoderSettings.FramePacing effectiveFramePacing,
                boolean hdrEnabled,
                List<Warning> warnings) {
            this.configuration = Objects.requireNonNull(
                    configuration,
                    "configuration");
            this.effectiveFramePacing = Objects.requireNonNull(
                    effectiveFramePacing,
                    "effectiveFramePacing");
            this.hdrEnabled = hdrEnabled;
            this.warnings = Collections.unmodifiableList(
                    new ArrayList<>(warnings));
        }

        public ConfigurationDocument getConfigurationDocument() {
            return configuration;
        }

        public StreamDecoderSettings.FramePacing
                getEffectiveFramePacing() {
            return effectiveFramePacing;
        }

        public boolean isHdrEnabled() {
            return hdrEnabled;
        }

        public List<Warning> getWarnings() {
            return warnings;
        }
    }

    /** Immutable, native-library-free input to the transport adapter. */
    public static final class ConfigurationDocument {
        private final NvApp app;
        private final int width;
        private final int height;
        private final int launchRefreshRate;
        private final int refreshRate;
        private final int bitrateKbps;
        private final boolean optimizeGameSettings;
        private final boolean playHostAudio;
        private final int maxPacketSize;
        private final NetworkMode networkMode;
        private final int supportedVideoFormats;
        private final int attachedGamepadMask;
        private final int clientRefreshRateX100;
        private final StreamAudioSettings.ChannelConfiguration
                audioConfiguration;
        private final int colorSpace;
        private final int colorRange;
        private final int pixelsPerInch;
        private final int virtualDisplayMode;
        private final boolean persistGamepadsAfterDisconnect;
        private final boolean nativeCursorEnabled;
        private final boolean clipboardSyncEnabled;
        private final boolean adaptiveInputThrottlingDisabled;

        private ConfigurationDocument(
                NvApp app,
                int width,
                int height,
                int launchRefreshRate,
                int refreshRate,
                int bitrateKbps,
                boolean optimizeGameSettings,
                boolean playHostAudio,
                int supportedVideoFormats,
                int attachedGamepadMask,
                int clientRefreshRateX100,
                StreamAudioSettings.ChannelConfiguration
                        audioConfiguration,
                int colorSpace,
                int colorRange,
                int pixelsPerInch,
                int virtualDisplayMode,
                boolean persistGamepadsAfterDisconnect,
                boolean nativeCursorEnabled,
                boolean clipboardSyncEnabled,
                boolean adaptiveInputThrottlingDisabled) {
            this.app = app;
            this.width = width;
            this.height = height;
            this.launchRefreshRate = launchRefreshRate;
            this.refreshRate = refreshRate;
            this.bitrateKbps = bitrateKbps;
            this.optimizeGameSettings = optimizeGameSettings;
            this.playHostAudio = playHostAudio;
            maxPacketSize = 1392;
            networkMode = NetworkMode.AUTO;
            this.supportedVideoFormats = supportedVideoFormats;
            this.attachedGamepadMask = attachedGamepadMask;
            this.clientRefreshRateX100 = clientRefreshRateX100;
            this.audioConfiguration = audioConfiguration;
            this.colorSpace = colorSpace;
            this.colorRange = colorRange;
            this.pixelsPerInch = pixelsPerInch;
            this.virtualDisplayMode = virtualDisplayMode;
            this.persistGamepadsAfterDisconnect =
                    persistGamepadsAfterDisconnect;
            this.nativeCursorEnabled = nativeCursorEnabled;
            this.clipboardSyncEnabled = clipboardSyncEnabled;
            this.adaptiveInputThrottlingDisabled =
                    adaptiveInputThrottlingDisabled;
        }

        public NvApp getApp() {
            return app;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getLaunchRefreshRate() {
            return launchRefreshRate;
        }

        public int getRefreshRate() {
            return refreshRate;
        }

        public int getBitrateKbps() {
            return bitrateKbps;
        }

        public boolean shouldOptimizeGameSettings() {
            return optimizeGameSettings;
        }

        public boolean shouldPlayHostAudio() {
            return playHostAudio;
        }

        public int getMaxPacketSize() {
            return maxPacketSize;
        }

        public NetworkMode getNetworkMode() {
            return networkMode;
        }

        public int getSupportedVideoFormats() {
            return supportedVideoFormats;
        }

        public int getAttachedGamepadMask() {
            return attachedGamepadMask;
        }

        public int getClientRefreshRateX100() {
            return clientRefreshRateX100;
        }

        public StreamAudioSettings.ChannelConfiguration
                getAudioConfiguration() {
            return audioConfiguration;
        }

        public int getColorSpace() {
            return colorSpace;
        }

        public int getColorRange() {
            return colorRange;
        }

        public int getPixelsPerInch() {
            return pixelsPerInch;
        }

        public int getVirtualDisplayMode() {
            return virtualDisplayMode;
        }

        public boolean shouldPersistGamepadsAfterDisconnect() {
            return persistGamepadsAfterDisconnect;
        }

        public boolean isNativeCursorEnabled() {
            return nativeCursorEnabled;
        }

        public boolean isClipboardSyncEnabled() {
            return clipboardSyncEnabled;
        }

        public boolean isAdaptiveInputThrottlingDisabled() {
            return adaptiveInputThrottlingDisabled;
        }
    }

    private StreamSessionConfigurationPlanner() {
    }

    public static Plan plan(
            SettingsSnapshot settings,
            Environment environment) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(environment, "environment");

        StreamDecoderCapabilities capabilities =
                environment.decoderCapabilities;
        boolean hdrEnabled = environment.hdrRequested &&
                (capabilities.isHevcMain10Hdr10Supported() ||
                        capabilities.isAv1Main10Supported());

        List<Warning> warnings = new ArrayList<>(3);
        if (environment.hdrRequested && !hdrEnabled) {
            warnings.add(Warning.HDR_DECODER_UNSUPPORTED);
        }
        if (settings.decoder.getVideoFormat() ==
                StreamDecoderSettings.VideoFormat.FORCE_HEVC &&
                !capabilities.isHevcSupported()) {
            warnings.add(
                    Warning.FORCED_HEVC_DECODER_UNAVAILABLE);
        }
        if (settings.decoder.getVideoFormat() ==
                StreamDecoderSettings.VideoFormat.FORCE_AV1 &&
                !capabilities.isAv1Supported()) {
            warnings.add(
                    Warning.FORCED_AV1_DECODER_UNAVAILABLE);
        }

        StreamFramePacingPolicy.Decision pacing =
                StreamFramePacingPolicy.resolve(
                        settings.decoder.getFramePacing(),
                        settings.decoder.getFps(),
                        environment.displayRefreshRate);
        int gamepadMask = resolveGamepadMask(
                settings.controller,
                environment.discoveredGamepadMask);
        int supportedVideoFormats = resolveSupportedVideoFormats(
                capabilities,
                hdrEnabled);

        ConfigurationDocument configuration =
                new ConfigurationDocument(
                        environment.app,
                        settings.decoder.getWidth(),
                        settings.decoder.getHeight(),
                        settings.decoder.getFps(),
                        pacing.getTargetFps(),
                        settings.decoder.getBitrateKbps(),
                        settings.video.shouldOptimizeGameSettings(),
                        settings.audio.shouldPlayHostAudio(),
                        supportedVideoFormats,
                        gamepadMask,
                        (int) (environment.displayRefreshRate * 100),
                        settings.audio.getChannelConfiguration(),
                        capabilities.getPreferredColorSpace(),
                        capabilities.getPreferredColorRange(),
                        environment.pixelsPerInch,
                        settings.video
                                .getVirtualDisplayMode()
                                .getStorageValue(),
                        !settings.controller.isMultiControllerEnabled(),
                        settings.input.isAbsoluteMouseMode(),
                        settings.transfer.isClipboardSyncEnabled(),
                        settings.input
                                .isAdaptiveInputThrottlingDisabled());

        return new Plan(
                configuration,
                pacing.getEffectiveMode(),
                hdrEnabled,
                warnings);
    }

    private static int resolveGamepadMask(
            ControllerSettings settings,
            int discoveredMask) {
        int gamepadMask = settings.isMultiControllerEnabled()
                ? discoveredMask
                : 1;
        if (settings.isOnscreenControllerEnabled()) {
            gamepadMask |= 1;
        }
        return gamepadMask;
    }

    private static int resolveSupportedVideoFormats(
            StreamDecoderCapabilities capabilities,
            boolean hdrEnabled) {
        int formats = MoonBridge.VIDEO_FORMAT_H264;
        if (capabilities.isHevcSupported()) {
            formats |= MoonBridge.VIDEO_FORMAT_H265;
            if (hdrEnabled &&
                    capabilities.isHevcMain10Hdr10Supported()) {
                formats |= MoonBridge.VIDEO_FORMAT_H265_MAIN10;
            }
        }
        if (capabilities.isAv1Supported()) {
            formats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;
            if (hdrEnabled && capabilities.isAv1Main10Supported()) {
                formats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN10;
            }
        }
        return formats;
    }

}
