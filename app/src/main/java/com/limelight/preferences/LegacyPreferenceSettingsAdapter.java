package com.limelight.preferences;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamDisplaySettingKeys;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.stream.StreamDecoderSettings;

import java.util.Objects;

/**
 * Temporary migration adapter from the legacy mutable preference bag to
 * immutable domain snapshots.
 *
 * <p>This adapter is removed within the typed-settings phase after every field
 * is decoded directly by the settings repository.</p>
 */
public final class LegacyPreferenceSettingsAdapter {
    private LegacyPreferenceSettingsAdapter() {
    }

    public static StreamDisplaySettings loadStreamDisplaySettings(
            PreferenceConfiguration legacy,
            SettingsRepository repository) {
        Objects.requireNonNull(legacy, "legacy");
        Objects.requireNonNull(repository, "repository");

        return new StreamDisplaySettings(
                legacy.width,
                legacy.height,
                legacy.isNativeResolution(),
                legacy.stretchVideo,
                legacy.enableCutoutModeVideo,
                legacy.enableExDisplay,
                legacy.enableHdr,
                StreamDisplaySettings.Gravity.fromStorageValue(
                        repository.get(
                                StreamDisplaySettingKeys.GRAVITY)),
                StreamDisplaySettings.FsrTarget.fromStorageValue(
                        repository.get(
                                StreamDisplaySettingKeys.FSR_TARGET)),
                StreamDisplaySettings.FsrSharpness.fromStorageValue(
                        repository.get(
                                StreamDisplaySettingKeys.FSR_SHARPNESS)),
                StreamDisplaySettings.FsrHdrOutput.fromStorageValue(
                        repository.get(
                                StreamDisplaySettingKeys.FSR_HDR_OUTPUT)));
    }

    public static StreamDecoderSettings loadStreamDecoderSettings(
            PreferenceConfiguration legacy) {
        Objects.requireNonNull(legacy, "legacy");
        if (legacy.audioConfiguration == null) {
            throw new IllegalArgumentException(
                    "Legacy audio configuration is missing");
        }

        return new StreamDecoderSettings(
                legacy.width,
                legacy.height,
                legacy.fps,
                legacy.bitrate,
                mapVideoFormat(legacy.videoFormat),
                mapFramePacing(legacy.framePacing),
                legacy.fullRange,
                legacy.lowLatencyExperiment,
                legacy.enablePerfOverlay,
                legacy.audioConfiguration.channelCount);
    }

    private static StreamDecoderSettings.VideoFormat mapVideoFormat(
            PreferenceConfiguration.FormatOption value) {
        if (value == null) {
            return StreamDecoderSettings.VideoFormat.AUTO;
        }
        switch (value) {
            case FORCE_AV1:
                return StreamDecoderSettings.VideoFormat.FORCE_AV1;
            case FORCE_HEVC:
                return StreamDecoderSettings.VideoFormat.FORCE_HEVC;
            case FORCE_H264:
                return StreamDecoderSettings.VideoFormat.FORCE_H264;
            case AUTO:
            default:
                return StreamDecoderSettings.VideoFormat.AUTO;
        }
    }

    private static StreamDecoderSettings.FramePacing mapFramePacing(
            int value) {
        switch (value) {
            case PreferenceConfiguration.FRAME_PACING_BALANCED:
                return StreamDecoderSettings.FramePacing.BALANCED;
            case PreferenceConfiguration.FRAME_PACING_CAP_FPS:
                return StreamDecoderSettings.FramePacing.CAP_FPS;
            case PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS:
                return StreamDecoderSettings.FramePacing
                        .MAXIMUM_SMOOTHNESS;
            case PreferenceConfiguration.FRAME_PACING_MIN_LATENCY:
            default:
                return StreamDecoderSettings.FramePacing
                        .MINIMUM_LATENCY;
        }
    }
}
