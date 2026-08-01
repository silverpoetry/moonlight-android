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
    public static final SettingKey<Boolean> BACKGROUND_ENABLED =
            SettingKey.booleanKey(
                    "app.appearance.background.enabled",
                    false)
                    .renamedFrom("checkbox_enable_screen_bg");
    public static final SettingKey<Boolean> BACKGROUND_BLUR_ENABLED =
            SettingKey.booleanKey(
                    "app.appearance.background.blur",
                    true)
                    .renamedFrom("checkbox_enable_screen_obscure");
    public static final SettingKey<String> BACKGROUND_FILE =
            SettingKey.boundedStringKey(
                    "app.appearance.background.file",
                    DEFAULT_BACKGROUND_FILE,
                    255)
                    .renamedFrom("screen_bg_file_name");
    public static final SettingKey<String> HOST_LIST_LABEL =
            SettingKey.boundedStringKey(
                    "app.appearance.host_list_label",
                    "",
                    256)
                    .renamedFrom("change_screen_label_key");

    private AppPresentationSettingKeys() {
    }
}
