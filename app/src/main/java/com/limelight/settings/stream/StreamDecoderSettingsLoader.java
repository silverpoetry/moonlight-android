package com.limelight.settings.stream;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.ui.StreamUiSettings;

import java.util.Objects;

/**
 * Builds the immutable decoder/runtime timing contract from typed domain
 * snapshots. No decoder consumer needs to know persisted key names.
 */
public final class StreamDecoderSettingsLoader {
    private StreamDecoderSettingsLoader() {
    }

    public static StreamDecoderSettings load(
            SettingsRepository repository,
            StreamVideoSettings videoSettings,
            StreamAudioSettings audioSettings,
            StreamUiSettings uiSettings) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(videoSettings, "videoSettings");
        Objects.requireNonNull(audioSettings, "audioSettings");
        Objects.requireNonNull(uiSettings, "uiSettings");

        return new StreamDecoderSettings(
                videoSettings.getWidth(),
                videoSettings.getHeight(),
                videoSettings.getFps(),
                videoSettings.getBitrateKbps(),
                videoSettings.getVideoFormat(),
                decodeFramePacing(repository.get(
                        StreamDecoderSettingKeys.FRAME_PACING)),
                repository.get(
                        StreamDecoderSettingKeys.FULL_RANGE),
                videoSettings.isLowLatencyExperimentEnabled(),
                uiSettings.isPerformanceOverlayEnabled(),
                getChannelCount(
                        audioSettings.getChannelConfiguration()),
                repository.get(
                        StreamDecoderSettingKeys
                                .REDUCE_REFRESH_RATE));
    }

    static StreamDecoderSettings.FramePacing decodeFramePacing(
            String value) {
        if (StreamDecoderSettingKeys.FRAME_PACING_BALANCED
                .equals(value)) {
            return StreamDecoderSettings.FramePacing.BALANCED;
        }
        if (StreamDecoderSettingKeys.FRAME_PACING_CAP_FPS
                .equals(value)) {
            return StreamDecoderSettings.FramePacing.CAP_FPS;
        }
        if (StreamDecoderSettingKeys.FRAME_PACING_MAX_SMOOTHNESS
                .equals(value)) {
            return StreamDecoderSettings.FramePacing
                    .MAXIMUM_SMOOTHNESS;
        }
        return StreamDecoderSettings.FramePacing
                .MINIMUM_LATENCY;
    }

    private static int getChannelCount(
            StreamAudioSettings.ChannelConfiguration value) {
        switch (value) {
            case SURROUND_7_1:
                return 8;
            case SURROUND_5_1:
                return 6;
            case STEREO:
            default:
                return 2;
        }
    }
}
