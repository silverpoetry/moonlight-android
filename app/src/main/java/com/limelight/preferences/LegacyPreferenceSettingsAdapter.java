package com.limelight.preferences;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamDisplaySettingKeys;
import com.limelight.settings.stream.StreamDisplaySettings;

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
}
