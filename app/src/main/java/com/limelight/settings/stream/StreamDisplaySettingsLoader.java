package com.limelight.settings.stream;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Projects persisted stream-video policy into the immutable display contract
 * consumed by layout and FSR code.
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
                videoSettings.isStretchVideo(),
                videoSettings.isDisplayCutoutEnabled(),
                videoSettings.isExternalDisplay(),
                videoSettings.isHdrEnabled(),
                StreamDisplaySettings.Gravity.fromStorageValue(
                        repository.get(
                                StreamDisplaySettingKeys.GRAVITY)),
                videoSettings.getFsrTarget(),
                videoSettings.getFsrSharpness(),
                videoSettings.getFsrHdrOutput());
    }
}
