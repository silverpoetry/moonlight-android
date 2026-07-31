package com.limelight.settings.android;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.preference.PreferenceManager;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.app.AppPresentationSettings;

import java.util.Locale;
import java.util.Objects;

/**
 * Applies the legacy in-app language setting and migrates it to Android's
 * per-app locale API where available.
 */
public final class AndroidAppLocale {
    private AndroidAppLocale() {
    }

    public static void apply(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        apply(
                activity,
                AndroidAppPresentationSettingsLoader.load(activity));
    }

    static void apply(
            Activity activity,
            AppPresentationSettings settings) {
        if (settings.usesSystemLanguage()) {
            return;
        }

        String language = settings.getLanguage();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager localeManager =
                    activity.getSystemService(LocaleManager.class);
            if (localeManager == null) {
                return;
            }
            localeManager.setApplicationLocales(
                    LocaleList.forLanguageTags(language));
            SettingsRepository repository =
                    new SharedPreferencesSettingsRepository(
                            PreferenceManager
                                    .getDefaultSharedPreferences(
                                            activity));
            repository.edit()
                    .put(
                            AppPresentationSettingKeys.LANGUAGE,
                            AppPresentationSettingKeys
                                    .SYSTEM_LANGUAGE)
                    .apply();
            return;
        }

        Configuration configuration =
                new Configuration(
                        activity.getResources()
                                .getConfiguration());
        configuration.setLocale(
                Locale.forLanguageTag(language));
        activity.getResources().updateConfiguration(
                configuration,
                activity.getResources().getDisplayMetrics());
    }
}
