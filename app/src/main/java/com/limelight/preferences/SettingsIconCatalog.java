package com.limelight.preferences;

import com.limelight.R;
import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.ui.StreamUiSettingKeys;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;

/** Stable icon mapping for settings sections and canonical setting IDs. */
final class SettingsIconCatalog {
    private SettingsIconCatalog() {
    }

    static int forSection(String key) {
        switch (key) {
            case SettingsScreenIds.SECTION_VIDEO_DISPLAY:
                return R.drawable.ic_screen;
            case SettingsScreenIds.SECTION_AUDIO:
                return R.drawable.ic_mic;
            case SettingsScreenIds.SECTION_TOUCH_MOUSE:
                return R.drawable.ic_touch_all;
            case SettingsScreenIds.SECTION_GAMEPAD:
                return R.drawable.ic_gamepad;
            case SettingsScreenIds.SECTION_VIRTUAL_CONTROLS:
                return R.drawable.ic_gamepad_dpad;
            case SettingsScreenIds.SECTION_CLIPBOARD_FILES:
                return R.drawable.ic_clipboard_send;
            case SettingsScreenIds.SECTION_STREAM_INTERFACE:
                return R.drawable.ic_window;
            case SettingsScreenIds.SECTION_APP_APPEARANCE:
                return R.drawable.ic_app_setting;
            case SettingsScreenIds.SECTION_SYSTEM_ACCESSIBILITY:
                return R.drawable.ic_other_settings;
            case SettingsScreenIds.SECTION_BACKUP_RESTORE:
                return R.drawable.ic_down;
            case SettingsScreenIds.SECTION_ABOUT:
                return R.drawable.ic_app_about;
            default:
                throw new IllegalArgumentException(
                        "Unknown settings section: " + key);
        }
    }

    static int forItem(String key) {
        if (key == null) {
            return R.drawable.ic_options;
        }
        int exactIcon = exactItem(key);
        if (exactIcon != 0) {
            return exactIcon;
        }
        if (key.contains("resolution")) {
            return R.drawable.ic_gamepad_display;
        }
        if (key.contains("frame_rate")) {
            return R.drawable.ic_gamepad_fps;
        }
        if (key.contains("bitrate")) {
            return R.drawable.ic_gamepad_bitrate;
        }
        if (key.contains("hdr")) {
            return R.drawable.ic_hdr;
        }
        if (key.contains("audio")) {
            return R.drawable.ic_mic;
        }
        if (key.contains("haptics")) {
            return R.drawable.ic_vibrate;
        }
        if (key.contains("rumble") || key.contains("vibrate")) {
            return R.drawable.ic_vibrate;
        }
        if (key.contains("gamepad") ||
                key.contains("controller")) {
            return R.drawable.ic_gamepad;
        }
        if (key.contains("mouse") || key.contains("pointer")) {
            return R.drawable.ic_mouse_left;
        }
        if (key.contains("touch")) {
            return R.drawable.ic_touch;
        }
        if (key.contains("keyboard")) {
            return R.drawable.ic_keyboard;
        }
        if (key.contains("import")) {
            return R.drawable.ic_down;
        }
        if (key.contains("export")) {
            return R.drawable.ic_clipboard_send;
        }
        if (key.contains("language")) {
            return R.drawable.ic_app_setting;
        }
        if (key.contains("delete") || key.contains("disable")) {
            return R.drawable.ic_delete;
        }
        if (key.contains("clipboard")) {
            return R.drawable.ic_clipboard_send;
        }
        if (key.contains("pip") || key.contains("window")) {
            return R.drawable.ic_window;
        }
        if (key.contains("host") || key.contains("computer")) {
            return R.drawable.ic_host_computer;
        }
        if (key.contains("screen")) {
            return R.drawable.ic_desktop;
        }
        return R.drawable.ic_options;
    }

    private static int exactItem(String key) {
        if (StreamResolutionSettingKeys.RESOLUTION.getName()
                .equals(key)) {
            return R.drawable.ic_gamepad_display;
        }
        if (StreamResolutionSettingKeys.ASPECT_RATIO.getName()
                .equals(key)) {
            return R.drawable.ic_gamepad_zoom;
        }
        if (StreamResolutionSettingKeys.FPS.getName().equals(key)) {
            return R.drawable.ic_gamepad_fps;
        }
        if (StreamVideoSettingKeys.BITRATE_KBPS.getName().equals(key) ||
                SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS
                        .equals(key)) {
            return R.drawable.ic_gamepad_bitrate;
        }
        if (key.endsWith("frame_pacing") ||
                key.endsWith("low_latency_decode") ||
                key.endsWith("optimize_game_settings") ||
                key.contains("performance_overlay")) {
            return R.drawable.ic_performance;
        }
        if (key.endsWith("stretch_video") ||
                key.endsWith("gravity")) {
            return R.drawable.ic_mobility_center;
        }
        if (key.endsWith("use_cutout_area")) {
            return R.drawable.ic_projection_mode;
        }
        if (key.endsWith("automatic_orientation") ||
                key.endsWith("portrait")) {
            return R.drawable.ic_switch_screen;
        }
        if (key.endsWith("stick_deadzone_percent") ||
                key.endsWith("trigger_deadzone_disabled")) {
            return R.drawable.ic_joystick;
        }
        if (key.contains("usb_driver")) {
            return R.drawable.ic_gamepad_xbox;
        }
        if (key.endsWith("scrolling_stick")) {
            return R.drawable.ic_mouse_down;
        }
        if (key.endsWith("flip_face_buttons")) {
            return R.drawable.ic_gamepad_move;
        }
        if (key.endsWith("touchpad_as_mouse")) {
            return R.drawable.ic_touch;
        }
        if (key.contains("motion")) {
            return R.drawable.ic_gamepad_sensor;
        }
        if (InputSettingKeys.TOUCH_MODE.getName().equals(key)) {
            return R.drawable.ic_touch_all;
        }
        if (key.endsWith("local_system_cursor")) {
            return R.drawable.ic_mouse_left_s;
        }
        if (key.endsWith("navigation_buttons")) {
            return R.drawable.ic_mouse_right;
        }
        if (key.endsWith("absolute_mouse")) {
            return R.drawable.ic_touch_center;
        }
        if (key.contains("force_press") ||
                key.contains("long_press") ||
                key.contains("opacity_percent")) {
            return R.drawable.ic_touch_sensitivity;
        }
        if (TransferSettingKeys.CLIPBOARD_SYNC.getName().equals(key) ||
                key.contains("clipboard")) {
            return R.drawable.ic_clipboard_send;
        }
        if (ControllerSettingKeys.ONSCREEN_CONTROLLER.getName()
                .equals(key)) {
            return R.drawable.ic_gamepad_dpad;
        }
        if (VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID.getName()
                .equals(key)) {
            return R.drawable.ic_gamepad_active;
        }
        if (key.endsWith("joycon_compatibility")) {
            return R.drawable.ic_joycon_compatibility;
        }
        if (key.endsWith("battery_reporting")) {
            return R.drawable.ic_gamepad_battery;
        }
        if (StreamUiSettingKeys.FLOATING_CONTROL_ENABLED.getName()
                .equals(key)) {
            return R.drawable.ic_quick_actions;
        }
        if (key.endsWith("external_display")) {
            return R.drawable.ic_desktop;
        }
        if (key.endsWith("game_mode_integration_disabled")) {
            return R.drawable.ic_gamepad_disable;
        }
        if (key.endsWith("key_logging") ||
                key.endsWith("combination_mode")) {
            return R.drawable.ic_keyboard_list;
        }
        if (key.endsWith("picture_in_picture")) {
            return R.drawable.ic_window;
        }
        if (key.endsWith("connection_warnings_disabled")) {
            return R.drawable.ic_delete;
        }
        if (key.endsWith("latency_toast")) {
            return R.drawable.ic_app_about;
        }
        if (key.endsWith("audio.output_target")) {
            return R.drawable.ic_gamepad_device;
        }
        if (key.endsWith("keep_controller_rumble")) {
            return R.drawable.ic_virtual_gamepad_rumble;
        }
        if (key.startsWith("action_backup_hosts_")) {
            return R.drawable.ic_host_computer;
        }
        if (key.startsWith("action_backup_certificate_") ||
                key.startsWith("action_backup_private_key_")) {
            return R.drawable.ic_lock_screen;
        }
        if (key.startsWith("action_") && key.endsWith("_import")) {
            return R.drawable.ic_down;
        }
        if (key.startsWith("action_") && key.endsWith("_export")) {
            return R.drawable.ic_clipboard_send;
        }
        if (key.endsWith("background.enabled") ||
                SettingsScreenIds.ACTION_APP_BACKGROUND_SELECT
                        .equals(key)) {
            return R.drawable.ic_desktop;
        }
        if (key.endsWith("background.blur")) {
            return R.drawable.ic_zoom;
        }
        if (key.endsWith("host_list_label")) {
            return R.drawable.ic_keyboard;
        }
        return 0;
    }
}
