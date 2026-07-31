package com.limelight.settings.android;

import android.content.Context;
import android.preference.PreferenceManager;

import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.app.AppPresentationSettings;
import com.limelight.settings.app.AppPresentationSettingsLoader;

import java.util.Objects;

/**
 * Android composition adapter for application presentation settings.
 */
public final class AndroidAppPresentationSettingsLoader {
    private AndroidAppPresentationSettingsLoader() {
    }

    public static AppPresentationSettings load(Context context) {
        Objects.requireNonNull(context, "context");
        SettingsRepository repository =
                new SharedPreferencesSettingsRepository(
                        PreferenceManager
                                .getDefaultSharedPreferences(context));
        prepare(
                repository,
                AndroidAppPresentationDefaults
                        .shouldUseSmallAppIcons(context));
        return AppPresentationSettingsLoader.load(repository);
    }

    static void prepare(
            SettingsRepository repository,
            boolean defaultSmallAppIcons) {
        Objects.requireNonNull(repository, "repository");
        SettingsMigrationRunner.migrate(repository);
        if (!repository.contains(
                AppPresentationSettingKeys.SMALL_APP_ICONS)) {
            repository.edit()
                    .put(
                            AppPresentationSettingKeys
                                    .SMALL_APP_ICONS,
                            defaultSmallAppIcons)
                    .apply();
        }
    }
}
