package com.limelight.ui.stream;

import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.audio.StreamAudioSettings;

import java.util.Objects;

/** Converts a pure startup document into the common-c transport object. */
public final class StreamSessionConfigurationAdapter {
    private StreamSessionConfigurationAdapter() {
    }

    public static StreamConfiguration toTransportConfiguration(
            StreamSessionConfigurationPlanner.ConfigurationDocument
                    document) {
        Objects.requireNonNull(document, "document");
        return new StreamConfiguration.Builder()
                .setResolution(
                        document.getWidth(),
                        document.getHeight())
                .setLaunchRefreshRate(
                        document.getLaunchRefreshRate())
                .setRefreshRate(document.getRefreshRate())
                .setApp(document.getApp())
                .setBitrate(document.getBitrateKbps())
                .setEnableSops(
                        document.shouldOptimizeGameSettings())
                .enableLocalAudioPlayback(
                        document.shouldPlayHostAudio())
                .setMaxPacketSize(document.getMaxPacketSize())
                .setRemoteConfiguration(
                        toTransportNetworkMode(
                                document.getNetworkMode()))
                .setSupportedVideoFormats(
                        document.getSupportedVideoFormats())
                .setAttachedGamepadMask(
                        document.getAttachedGamepadMask())
                .setClientRefreshRateX100(
                        document.getClientRefreshRateX100())
                .setAudioConfiguration(toTransportAudioConfiguration(
                        document.getAudioConfiguration()))
                .setColorSpace(document.getColorSpace())
                .setColorRange(document.getColorRange())
                .setPPI(document.getPixelsPerInch())
                .setRazerVD(document.getVirtualDisplayMode())
                .setPersistGamepadsAfterDisconnect(
                        document.shouldPersistGamepadsAfterDisconnect())
                .enableNativeCursor(
                        document.isNativeCursorEnabled())
                .enableClipboardSync(
                        document.isClipboardSyncEnabled())
                .disableAdaptiveInputThrottling(
                        document.isAdaptiveInputThrottlingDisabled())
                .build();
    }

    private static int toTransportNetworkMode(
            StreamSessionConfigurationPlanner.NetworkMode mode) {
        switch (mode) {
            case AUTO:
                return StreamConfiguration.STREAM_CFG_AUTO;
            default:
                throw new IllegalArgumentException(
                        "Unsupported network mode: " + mode);
        }
    }

    private static MoonBridge.AudioConfiguration
            toTransportAudioConfiguration(
                    StreamAudioSettings.ChannelConfiguration
                            configuration) {
        switch (configuration) {
            case SURROUND_7_1:
                return MoonBridge.AUDIO_CONFIGURATION_71_SURROUND;
            case SURROUND_5_1:
                return MoonBridge.AUDIO_CONFIGURATION_51_SURROUND;
            case STEREO:
            default:
                return MoonBridge.AUDIO_CONFIGURATION_STEREO;
        }
    }
}
