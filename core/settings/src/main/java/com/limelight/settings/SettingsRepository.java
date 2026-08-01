package com.limelight.settings;

/**
 * Type-safe persistence boundary for application settings.
 */
public interface SettingsRepository {
    /**
     * Returns whether the persistence layer contains any value for this key.
     * Presence is distinct from validity; {@link #get(SettingKey)} always
     * returns a schema-valid value.
     */
    boolean contains(SettingKey<?> key);

    <T> T get(SettingKey<T> key);

    Editor edit();

    interface Editor {
        <T> Editor put(SettingKey<T> key, T value);

        Editor remove(SettingKey<?> key);

        void apply();

        boolean commit();
    }
}
