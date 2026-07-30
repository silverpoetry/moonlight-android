package com.limelight.settings;

/**
 * Versioned schema metadata for default application preferences.
 */
public final class SettingsSchema {
    public static final int CURRENT_VERSION = 1;

    public static final SettingKey<Integer> VERSION =
            SettingKey.integerKey(
                    "settings_schema_version",
                    0,
                    0,
                    Integer.MAX_VALUE);

    private SettingsSchema() {
    }
}
