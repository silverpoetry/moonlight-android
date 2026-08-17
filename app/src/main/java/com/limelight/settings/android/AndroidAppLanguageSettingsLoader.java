package com.limelight.settings.android;

import android.content.Context;

import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;

import java.util.Objects;

/**
 * Loads the persisted application language without resolving presentation or
 * display-dependent defaults.
 *
 * <p>This adapter is safe to use while an Activity is attaching its base
 * context. Display-scoped Android services must not be introduced here.</p>
 */
final class AndroidAppLanguageSettingsLoader {
    private AndroidAppLanguageSettingsLoader() {
    }

    static String load(Context context) {
        Objects.requireNonNull(context, "context");
        return load(AndroidSettingsRepository.create(context));
    }

    static String load(SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");
        SettingsMigrationRunner.migrate(repository);
        return repository.get(AppPresentationSettingKeys.LANGUAGE);
    }
}
