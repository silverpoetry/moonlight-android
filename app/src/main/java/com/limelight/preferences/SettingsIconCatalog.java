package com.limelight.preferences;

import com.limelight.R;
import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.audio.StreamAudioSettingKeys;
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
                return R.drawable.ic_m3_display;
            case SettingsScreenIds.SECTION_AUDIO:
                return R.drawable.ic_m3_volume_up;
            case SettingsScreenIds.SECTION_TOUCH_MOUSE:
                return R.drawable.ic_m3_mouse;
            case SettingsScreenIds.SECTION_GAMEPAD:
                return R.drawable.ic_m3_gamepad;
            case SettingsScreenIds.SECTION_VIRTUAL_CONTROLS:
                return R.drawable.ic_m3_grid_view;
            case SettingsScreenIds.SECTION_CLIPBOARD_FILES:
                return R.drawable.ic_m3_download;
            case SettingsScreenIds.SECTION_STREAM_INTERFACE:
                return R.drawable.ic_m3_cast;
            case SettingsScreenIds.SECTION_APP_APPEARANCE:
                return R.drawable.ic_m3_palette;
            case SettingsScreenIds.SECTION_SYSTEM_ACCESSIBILITY:
                return R.drawable.ic_m3_security;
            case SettingsScreenIds.SECTION_BACKUP_RESTORE:
                return R.drawable.ic_m3_backup;
            case SettingsScreenIds.SECTION_ABOUT:
                return R.drawable.ic_m3_info;
            default:
                throw new IllegalArgumentException(
                        "Unknown settings section: " + key);
        }
    }

    static int forItem(String key) {
        if (key == null) {
            return R.drawable.ic_m3_tune;
        }
        int exactIcon = exactItem(key);
        if (exactIcon != 0) {
            return exactIcon;
        }
        if (key.contains("resolution")) {
            return R.drawable.ic_m3_display;
        }
        if (key.contains("frame_rate")) {
            return R.drawable.ic_m3_fps;
        }
        if (key.contains("bitrate")) {
            return R.drawable.ic_m3_speed;
        }
        if (key.contains("hdr")) {
            return R.drawable.ic_m3_hdr;
        }
        if (key.contains("audio")) {
            return R.drawable.ic_m3_volume_up;
        }
        if (key.contains("haptics")) {
            return R.drawable.ic_m3_vibration;
        }
        if (key.contains("rumble") || key.contains("vibrate")) {
            return R.drawable.ic_m3_vibration;
        }
        if (key.contains("gamepad") ||
                key.contains("controller")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (key.contains("mouse") || key.contains("pointer")) {
            return R.drawable.ic_m3_mouse;
        }
        if (key.contains("touch")) {
            return R.drawable.ic_m3_touchpad;
        }
        if (key.contains("keyboard")) {
            return R.drawable.ic_m3_keyboard;
        }
        if (key.contains("import")) {
            return R.drawable.ic_m3_download;
        }
        if (key.contains("export")) {
            return R.drawable.ic_m3_content_copy;
        }
        if (key.contains("language")) {
            return R.drawable.ic_m3_language;
        }
        if (key.contains("delete") || key.contains("disable")) {
            return key.contains("delete") ?
                    R.drawable.ic_m3_delete :
                    R.drawable.ic_m3_visibility_off;
        }
        if (key.contains("clipboard")) {
            return R.drawable.ic_m3_content_copy;
        }
        if (key.contains("pip") || key.contains("window")) {
            return R.drawable.ic_m3_apps;
        }
        if (key.contains("host") || key.contains("computer")) {
            return R.drawable.ic_m3_display;
        }
        if (key.contains("screen")) {
            return R.drawable.ic_m3_display;
        }
        return R.drawable.ic_m3_tune;
    }

    private static int exactItem(String key) {
        if (StreamResolutionSettingKeys.RESOLUTION.getName()
                .equals(key)) {
            return R.drawable.ic_m3_display;
        }
        if (StreamResolutionSettingKeys.ASPECT_RATIO.getName()
                .equals(key)) {
            return R.drawable.ic_m3_zoom_out_map;
        }
        if (StreamResolutionSettingKeys.FPS.getName().equals(key)) {
            return R.drawable.ic_m3_fps;
        }
        if (StreamVideoSettingKeys.BITRATE_KBPS.getName().equals(key) ||
                SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS
                        .equals(key)) {
            return R.drawable.ic_m3_speed;
        }
        if (key.endsWith("frame_pacing") ||
                key.endsWith("low_latency_decode") ||
                key.endsWith("optimize_game_settings") ||
                key.contains("performance_overlay")) {
            return R.drawable.ic_m3_speed;
        }
        if (key.endsWith("codec")) {
            return R.drawable.ic_m3_movie;
        }
        if (key.endsWith("unlock_frame_rates") ||
                key.endsWith("reduce_refresh_rate")) {
            return R.drawable.ic_m3_fps;
        }
        if (key.endsWith("full_range")) {
            return R.drawable.ic_m3_palette;
        }
        if (key.endsWith("stretch_video") ||
                key.endsWith("gravity")) {
            return R.drawable.ic_m3_tune;
        }
        if (key.endsWith("use_cutout_area")) {
            return R.drawable.ic_m3_display;
        }
        if (StreamVideoSettingKeys.SCREEN_ON_POLICY.getName()
                .equals(key)) {
            return R.drawable.ic_m3_brightness;
        }
        if (key.endsWith("device_screen_policy")) {
            return R.drawable.ic_m3_brightness;
        }
        if (key.endsWith("automatic_orientation") ||
                key.endsWith("portrait")) {
            return R.drawable.ic_m3_rotate;
        }
        if (key.endsWith("stick_deadzone_percent") ||
                key.endsWith("trigger_deadzone_disabled")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (key.contains("usb_driver")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (key.endsWith("scrolling_stick")) {
            return R.drawable.ic_m3_touch;
        }
        if (key.endsWith("flip_face_buttons")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (key.endsWith("touchpad_as_mouse")) {
            return R.drawable.ic_m3_touchpad;
        }
        if (key.contains("motion")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (InputSettingKeys.TOUCH_MODE.getName().equals(key)) {
            return R.drawable.ic_m3_mouse;
        }
        if (key.endsWith("local_system_cursor")) {
            return R.drawable.ic_m3_mouse;
        }
        if (key.endsWith("navigation_buttons")) {
            return R.drawable.ic_m3_touch;
        }
        if (key.endsWith("absolute_mouse")) {
            return R.drawable.ic_m3_mouse;
        }
        if (key.contains("force_press") ||
                key.contains("long_press") ||
                key.contains("opacity_percent")) {
            return R.drawable.ic_m3_touch;
        }
        if (TransferSettingKeys.CLIPBOARD_SYNC.getName().equals(key) ||
                key.contains("clipboard")) {
            return key.contains("directory") ?
                    R.drawable.ic_m3_folder :
                    R.drawable.ic_m3_content_copy;
        }
        if (ControllerSettingKeys.ONSCREEN_CONTROLLER.getName()
                .equals(key)) {
            return R.drawable.ic_m3_gamepad;
        }
        if (VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID.getName()
                .equals(key)) {
            return R.drawable.ic_m3_gamepad;
        }
        if (VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID.getName()
                .equals(key) ||
                VirtualControlSettingKeys.SHOW_VIRTUAL_KEYS_ON_START
                        .getName().equals(key)) {
            return R.drawable.ic_m3_keyboard;
        }
        if (VirtualControlSettingKeys.CONTROL_OPACITY_PERCENT
                        .getName().equals(key) ||
                VirtualControlSettingKeys.GAMEPAD_SCALE_PERCENT
                        .getName().equals(key)) {
            return R.drawable.ic_m3_gamepad;
        }
        if (StreamAudioSettingKeys.CHANNEL_CONFIGURATION.getName()
                        .equals(key) ||
                StreamAudioSettingKeys.PLAY_HOST_AUDIO.getName()
                        .equals(key) ||
                StreamAudioSettingKeys.AUDIO_EFFECTS.getName()
                        .equals(key) ||
                StreamAudioSettingKeys.MUTED.getName().equals(key)) {
            return R.drawable.ic_m3_volume_up;
        }
        if (key.endsWith("joycon_compatibility")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (key.endsWith("battery_reporting")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (StreamUiSettingKeys.FLOATING_CONTROL_ENABLED.getName()
                .equals(key)) {
            return R.drawable.ic_m3_apps;
        }
        if (key.endsWith("external_display")) {
            return R.drawable.ic_m3_display;
        }
        if (key.endsWith("game_mode_integration_disabled")) {
            return R.drawable.ic_m3_gamepad;
        }
        if (key.endsWith("key_logging") ||
                key.endsWith("combination_mode")) {
            return R.drawable.ic_m3_keyboard;
        }
        if (key.endsWith("picture_in_picture")) {
            return R.drawable.ic_m3_apps;
        }
        if (key.endsWith("connection_warnings_disabled")) {
            return R.drawable.ic_m3_notifications_off;
        }
        if (key.endsWith("latency_toast")) {
            return R.drawable.ic_m3_info;
        }
        if (key.endsWith("audio.output_target")) {
            return R.drawable.ic_m3_cast;
        }
        if (key.endsWith("keep_controller_rumble")) {
            return R.drawable.ic_m3_vibration;
        }
        if (key.startsWith("action_backup_hosts_")) {
            return R.drawable.ic_m3_backup;
        }
        if (key.startsWith("action_backup_certificate_") ||
                key.startsWith("action_backup_private_key_")) {
            return R.drawable.ic_m3_key;
        }
        if (key.startsWith("action_") && key.endsWith("_import")) {
            return R.drawable.ic_m3_download;
        }
        if (key.startsWith("action_") && key.endsWith("_export")) {
            return R.drawable.ic_m3_content_copy;
        }
        if (key.startsWith("action_about_")) {
            return R.drawable.ic_m3_info;
        }
        if (key.startsWith("action_accessibility_")) {
            return R.drawable.ic_m3_security;
        }
        if (key.endsWith("theme_mode")) {
            return R.drawable.ic_m3_palette;
        }
        return 0;
    }
}
