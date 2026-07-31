package com.limelight.preferences;

/** Read-only values consumed by settings-screen presentation policy. */
interface SettingsValueReader {
    boolean getBoolean(SettingsItem item);

    int getInt(SettingsItem item);

    String getString(SettingsItem item);
}
