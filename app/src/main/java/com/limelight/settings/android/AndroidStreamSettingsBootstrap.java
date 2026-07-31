package com.limelight.settings.android;

import android.os.Build;

import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.controller.ControllerSettingKeys;

import java.util.Objects;

/**
 * Runs schema migration and device-specific defaults before a stream snapshot
 * is composed.
 */
public final class AndroidStreamSettingsBootstrap {
    private AndroidStreamSettingsBootstrap() {
    }

    public static void prepare(SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");
        SettingsMigrationRunner.migrate(repository);

        if (shouldDisableMotionSensorsByDefault(
                Build.VERSION.SDK_INT,
                repository.contains(
                        ControllerSettingKeys.MOTION_SENSORS))) {
            repository.edit()
                    .put(
                            ControllerSettingKeys.MOTION_SENSORS,
                            false)
                    .apply();
        }
    }

    static boolean shouldDisableMotionSensorsByDefault(
            int sdkInt,
            boolean hasStoredPreference) {
        // Android 12 can crash merely by obtaining an InputDevice sensor
        // manager. Preserve an explicit user choice, but make fresh installs
        // safe on that release.
        return sdkInt == Build.VERSION_CODES.S &&
                !hasStoredPreference;
    }
}
