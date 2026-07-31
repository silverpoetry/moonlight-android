package com.limelight.settings.android;

import android.content.Context;
import android.preference.PreferenceManager;

import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamBitratePolicy;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingsLoader;
import com.limelight.settings.stream.StreamSettingsResetter;

import java.util.Objects;

/**
 * Android adapter for display-dependent stream defaults and recovery.
 */
public final class AndroidStreamDefaults {
    private AndroidStreamDefaults() {
    }

    public static int getDefaultBitrateKbps(Context context) {
        SettingsRepository repository = repository(context);
        SettingsMigrationRunner.migrate(repository);
        StreamResolutionCodec.Result resolution =
                StreamResolutionSettingsLoader.load(
                        repository,
                        AndroidDisplayAspectProvider.get(context));
        return StreamBitratePolicy.calculateDefaultBitrateKbps(
                resolution.getWidth(),
                resolution.getHeight(),
                resolution.getFps());
    }

    public static void resetAfterDecoderCrashes(Context context) {
        StreamSettingsResetter.resetAfterDecoderCrashes(
                repository(context));
    }

    private static SettingsRepository repository(Context context) {
        Objects.requireNonNull(context, "context");
        return new SharedPreferencesSettingsRepository(
                PreferenceManager.getDefaultSharedPreferences(
                        context));
    }
}
