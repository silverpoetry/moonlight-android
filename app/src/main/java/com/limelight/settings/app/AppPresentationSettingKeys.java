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
    public static final String DEFAULT_BACKGROUND_FILE =
            "axi_screen_bg.png";

    public static final SettingKey<String> LANGUAGE =
            SettingKey.boundedStringKey(
                    "list_languages",
                    SYSTEM_LANGUAGE,
                    64);
    public static final SettingKey<Boolean> SMALL_APP_ICONS =
            SettingKey.booleanKey(
                    "checkbox_small_icon_mode",
                    false);
    public static final SettingKey<Boolean> LIGHT_THEME =
            SettingKey.booleanKey(
                    "checkbox_ui_theme_white",
                    true);
    public static final SettingKey<Boolean> BACKGROUND_ENABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_screen_bg",
                    false);
    public static final SettingKey<Boolean> BACKGROUND_BLUR_ENABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_screen_obscure",
                    true);
    public static final SettingKey<String> BACKGROUND_FILE =
            SettingKey.boundedStringKey(
                    "screen_bg_file_name",
                    DEFAULT_BACKGROUND_FILE,
                    255);
    public static final SettingKey<String> HOST_LIST_LABEL =
            SettingKey.boundedStringKey(
                    "change_screen_label_key",
                    "",
                    256);

    private AppPresentationSettingKeys() {
    }
}
