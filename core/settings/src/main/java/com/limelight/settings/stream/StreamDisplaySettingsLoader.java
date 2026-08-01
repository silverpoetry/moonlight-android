package com.limelight.settings.stream;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Projects persisted stream-video policy into the immutable display contract
 * consumed by layout and presentation code.
 */
public final class StreamDisplaySettingsLoader {
    private StreamDisplaySettingsLoader() {
    }

    public static StreamDisplaySettings load(
            SettingsRepository repository,
            StreamVideoSettings videoSettings) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(videoSettings, "videoSettings");

        return new StreamDisplaySettings(
                videoSettings.getWidth(),
                videoSettings.getHeight(),
                videoSettings.isNativeResolution(),
                repository.get(
                        StreamDisplaySettingKeys.STRETCH_VIDEO),
                repository.get(
                        StreamDisplaySettingKeys.DISPLAY_CUTOUT),
                videoSettings.isExternalDisplay(),
                videoSettings.isHdrEnabled(),
                StreamDisplaySettings.Gravity.fromStorageValue(
                        repository.get(
                                StreamDisplaySettingKeys.GRAVITY)));
    }
}
