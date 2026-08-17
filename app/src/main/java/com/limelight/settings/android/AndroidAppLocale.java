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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context;
        }

        String language =
                AndroidAppLanguageSettingsLoader.load(context);
        if (AppPresentationSettingKeys.SYSTEM_LANGUAGE.equals(language)) {
            return context;
        }

        Configuration configuration = new Configuration(
                context.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(
                language));
        return context.createConfigurationContext(configuration);
    }

    public static void migrateToPlatformLocales(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        String language =
                AndroidAppLanguageSettingsLoader.load(activity);
        if (AppPresentationSettingKeys.SYSTEM_LANGUAGE.equals(language)) {
            return;
        }
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

    /** Returns the effective user-selected language in portable schema form. */
    public static String configuredLanguage(Context context) {
        Objects.requireNonNull(context, "context");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return AndroidAppLanguageSettingsLoader.load(context);
        }
        LocaleManager localeManager =
                context.getSystemService(LocaleManager.class);
        if (localeManager == null ||
                localeManager.getApplicationLocales().isEmpty()) {
            return AppPresentationSettingKeys.SYSTEM_LANGUAGE;
        }
        return AppPresentationSettingKeys.LANGUAGE.normalizeValue(
                localeManager.getApplicationLocales().toLanguageTags());
    }

    /** Applies a language restored from a portable configuration archive. */
    public static void applyImportedLanguage(
            Context context,
            String language) {
        Objects.requireNonNull(context, "context");
        String normalized = AppPresentationSettingKeys.LANGUAGE
                .normalizeValue(language);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        LocaleManager localeManager =
                context.getSystemService(LocaleManager.class);
        if (localeManager == null) {
            return;
        }
        localeManager.setApplicationLocales(
                AppPresentationSettingKeys.SYSTEM_LANGUAGE.equals(normalized)
                        ? LocaleList.getEmptyLocaleList()
                        : LocaleList.forLanguageTags(normalized));
        // Android 13+ owns the effective language. Keep the legacy repository
        // at its neutral value so a later migration cannot overwrite the
        // platform selection.
        AndroidSettingsRepository.create(context)
                .edit()
                .put(
                        AppPresentationSettingKeys.LANGUAGE,
                        AppPresentationSettingKeys.SYSTEM_LANGUAGE)
                .apply();
    }
}
