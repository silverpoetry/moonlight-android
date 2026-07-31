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
                repository.get(
                        AppPresentationSettingKeys.LIGHT_THEME),
                repository.get(
                        AppPresentationSettingKeys
                                .BACKGROUND_ENABLED),
                repository.get(
                        AppPresentationSettingKeys
                                .BACKGROUND_BLUR_ENABLED),
                repository.get(
                        AppPresentationSettingKeys.BACKGROUND_FILE),
                repository.get(
                        AppPresentationSettingKeys.HOST_LIST_LABEL));
    }
}
