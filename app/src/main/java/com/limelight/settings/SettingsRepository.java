package com.limelight.settings;

/**
 * Type-safe persistence boundary for application settings.
 */
public interface SettingsRepository {
    <T> T get(SettingKey<T> key);

    Editor edit();

    interface Editor {
        <T> Editor put(SettingKey<T> key, T value);

        Editor remove(SettingKey<?> key);

        void apply();

        boolean commit();
    }
}
