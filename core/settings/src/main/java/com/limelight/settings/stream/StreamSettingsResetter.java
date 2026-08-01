package com.limelight.settings.stream;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Resets only decoder-crash-sensitive stream settings.
 */
public final class StreamSettingsResetter {
    private StreamSettingsResetter() {
    }

    public static void resetAfterDecoderCrashes(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");
        repository.edit()
                .remove(StreamVideoSettingKeys.BITRATE_KBPS)
                .remove(
                        StreamVideoSettingKeys
                                .LEGACY_BITRATE_MBPS)
                .remove(
                        StreamResolutionSettingKeys
                                .LEGACY_RESOLUTION_AND_FPS)
                .remove(StreamResolutionSettingKeys.RESOLUTION)
                .remove(StreamResolutionSettingKeys.FPS)
                .remove(StreamVideoSettingKeys.VIDEO_FORMAT)
                .remove(StreamVideoSettingKeys.HDR_ENABLED)
                .remove(StreamVideoSettingKeys.UNLOCK_FPS)
                .remove(StreamDecoderSettingKeys.FULL_RANGE)
                .apply();
    }
}
