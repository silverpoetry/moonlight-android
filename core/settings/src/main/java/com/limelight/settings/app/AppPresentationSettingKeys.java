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
    public static final SettingKey<Boolean> LIGHT_THEME =
            SettingKey.booleanKey(
                    "app.appearance.light_theme",
                    true)
                    .renamedFrom("checkbox_ui_theme_white");
    private AppPresentationSettingKeys() {
    }
}
