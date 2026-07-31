package com.limelight.settings.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/** Single composition entry point for the application's default settings. */
public final class AndroidSettingsRepository {
    private AndroidSettingsRepository() {
    }

    public static SettingsRepository create(Context context) {
        return new SharedPreferencesSettingsRepository(
                defaultPreferences(context));
    }

    static SharedPreferences defaultPreferences(Context context) {
        Context checkedContext = Objects.requireNonNull(
                context,
                "context");
        Context applicationContext =
                checkedContext.getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(
                applicationContext == null
                        ? checkedContext
                        : applicationContext);
    }
}
