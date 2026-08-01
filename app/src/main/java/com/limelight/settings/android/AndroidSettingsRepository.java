package com.limelight.settings.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/** Single composition entry point for the application's default settings. */
public final class AndroidSettingsRepository {
    private static final String DEFAULT_PREFERENCES_SUFFIX =
            "_preferences";

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
        Context storageContext = applicationContext == null
                ? checkedContext
                : applicationContext;
        return storageContext.getSharedPreferences(
                defaultPreferencesName(storageContext.getPackageName()),
                Context.MODE_PRIVATE);
    }

    /**
     * Preserves Android's historical default-preferences filename without
     * retaining a dependency on the removed platform Preference UI stack.
     */
    static String defaultPreferencesName(String packageName) {
        return Objects.requireNonNull(packageName, "packageName") +
                DEFAULT_PREFERENCES_SUFFIX;
    }
}
