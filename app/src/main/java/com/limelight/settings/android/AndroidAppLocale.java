package com.limelight.settings.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.app.AppPresentationSettings;

import java.util.Locale;
import java.util.Objects;

/**
 * Creates the pre-Android 13 localized Activity context and migrates the
 * legacy language setting to Android's per-app locale API when available.
 */
public final class AndroidAppLocale {
    private AndroidAppLocale() {
    }

    /**
     * Lint 32.3 does not recognize the AGP 9 bundle-language DSL in Debug
     * variants. The finalized Android DSL is independently release-gated to
     * require {@code bundle.language.enableSplit == false}.
     */
    @SuppressLint("AppBundleLocaleChanges")
    public static Context wrapBaseContext(Context context) {
        Objects.requireNonNull(context, "context");
        AppPresentationSettings settings =
                AndroidAppPresentationSettingsLoader.load(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                settings.usesSystemLanguage()) {
            return context;
        }

        Configuration configuration = new Configuration(
                context.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(
                settings.getLanguage()));
        return context.createConfigurationContext(configuration);
    }

    public static void migrateToPlatformLocales(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        AppPresentationSettings settings =
                AndroidAppPresentationSettingsLoader.load(activity);
        if (settings.usesSystemLanguage()) {
            return;
        }
        String language = settings.getLanguage();
        LocaleManager localeManager =
                activity.getSystemService(LocaleManager.class);
        if (localeManager == null) {
            return;
        }
        localeManager.setApplicationLocales(
                LocaleList.forLanguageTags(language));
        SettingsRepository repository =
                AndroidSettingsRepository.create(activity);
        repository.edit()
                .put(
                        AppPresentationSettingKeys.LANGUAGE,
                        AppPresentationSettingKeys.SYSTEM_LANGUAGE)
                .apply();
    }
}
