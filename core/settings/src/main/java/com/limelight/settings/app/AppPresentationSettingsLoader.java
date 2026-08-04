package com.limelight.settings.app;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Builds one coherent application presentation snapshot.
 */
public final class AppPresentationSettingsLoader {
    private AppPresentationSettingsLoader() {
    }

    public static AppPresentationSettings load(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");
        return new AppPresentationSettings(
                repository.get(
                        AppPresentationSettingKeys.LANGUAGE),
                repository.get(
                        AppPresentationSettingKeys.SMALL_APP_ICONS),
                normalizeThemeMode(repository.get(
                        AppPresentationSettingKeys.THEME_MODE)));
    }

    private static String normalizeThemeMode(String value) {
        if (AppPresentationSettingKeys.THEME_MODE_LIGHT.equals(value) ||
                AppPresentationSettingKeys.THEME_MODE_DARK.equals(value)) {
            return value;
        }
        return AppPresentationSettingKeys.THEME_MODE_SYSTEM;
    }
}
