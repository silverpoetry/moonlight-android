package com.limelight.settings.android;

import android.content.Context;
import android.preference.PreferenceManager;

import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsLoader;

import java.util.Objects;

/**
 * Android composition adapter for non-session entry points that must honor
 * stream-related platform integration policy.
 */
public final class AndroidStreamUiSettingsLoader {
    private AndroidStreamUiSettingsLoader() {
    }

    public static StreamUiSettings load(Context context) {
        Objects.requireNonNull(context, "context");
        SettingsRepository repository =
                new SharedPreferencesSettingsRepository(
                        PreferenceManager
                                .getDefaultSharedPreferences(context));
        SettingsMigrationRunner.migrate(repository);
        return StreamUiSettingsLoader.load(repository);
    }
}
