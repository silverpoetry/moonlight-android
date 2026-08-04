package com.limelight.settings.app;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for application-level presentation settings.
 *
 * <p>These settings apply outside a streaming session. They intentionally
 * remain separate from stream UI policy.</p>
 */
public final class AppPresentationSettingKeys {
    public static final String SYSTEM_LANGUAGE = "default";
    public static final String THEME_MODE_SYSTEM = "system";
    public static final String THEME_MODE_LIGHT = "light";
    public static final String THEME_MODE_DARK = "dark";
    public static final SettingKey<String> LANGUAGE =
            SettingKey.boundedStringKey(
                    "app.language",
                    SYSTEM_LANGUAGE,
                    64)
                    .renamedFrom("list_languages");
    public static final SettingKey<Boolean> SMALL_APP_ICONS =
            SettingKey.booleanKey(
                    "app.appearance.small_icons",
                    false)
                    .renamedFrom("checkbox_small_icon_mode");
    public static final SettingKey<String> THEME_MODE =
            SettingKey.stringSetKey(
                    "app.appearance.theme_mode",
                    THEME_MODE_SYSTEM,
                    THEME_MODE_SYSTEM,
                    THEME_MODE_LIGHT,
                    THEME_MODE_DARK);

    /** Retained only so the schema migration can consume older installs. */
    public static final SettingKey<Boolean> LEGACY_LIGHT_THEME =
            SettingKey.booleanKey(
                    "app.appearance.light_theme",
                    true)
                    .renamedFrom("checkbox_ui_theme_white");
    private AppPresentationSettingKeys() {
    }
}
