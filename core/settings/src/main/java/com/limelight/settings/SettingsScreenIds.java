package com.limelight.settings;

/**
 * Stable identifiers for non-persisted settings-screen elements.
 *
 * <p>Persisted rows use {@link SettingKey} names instead. Keeping actions,
 * sections, and editors here prevents presentation identifiers from being
 * mistaken for stored preferences.</p>
 */
public final class SettingsScreenIds {
    public static final String SECTION_VIDEO_DISPLAY =
            "section_video_display";
    public static final String SECTION_AUDIO = "section_audio";
    public static final String SECTION_TOUCH_MOUSE =
            "section_touch_mouse";
    public static final String SECTION_GAMEPAD = "section_gamepad";
    public static final String SECTION_VIRTUAL_CONTROLS =
            "section_virtual_controls";
    public static final String SECTION_CLIPBOARD_FILES =
            "section_clipboard_files";
    public static final String SECTION_STREAM_INTERFACE =
            "section_stream_interface";
    public static final String SECTION_APP_APPEARANCE =
            "section_app_appearance";
    public static final String SECTION_SYSTEM_ACCESSIBILITY =
            "section_system_accessibility";
    public static final String SECTION_BACKUP_RESTORE =
            "section_backup_restore";
    public static final String SECTION_ABOUT = "section_about";

    public static final String EDITOR_VIDEO_BITRATE_MBPS =
            "editor_video_bitrate_mbps";

    public static final String ACTION_VIRTUAL_GAMEPAD_IMPORT =
            "action_virtual_gamepad_import";
    public static final String ACTION_VIRTUAL_GAMEPAD_EXPORT =
            "action_virtual_gamepad_export";
    public static final String ACTION_VIRTUAL_KEYBOARD_IMPORT =
            "action_virtual_keyboard_import";
    public static final String ACTION_VIRTUAL_KEYBOARD_EXPORT =
            "action_virtual_keyboard_export";
    public static final String ACTION_ACCESSIBILITY_CONFIG_IMPORT =
            "action_accessibility_config_import";
    public static final String ACTION_CONFIGURATION_EXPORT =
            "action_configuration_export";
    public static final String ACTION_CONFIGURATION_IMPORT =
            "action_configuration_import";

    private SettingsScreenIds() {
    }
}
