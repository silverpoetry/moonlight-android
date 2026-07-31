package com.limelight.settings.virtualcontrols;

import com.limelight.settings.SettingKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutProfiles;

/**
 * Canonical persisted schema for on-screen controls and keyboard overlays.
 */
public final class VirtualControlSettingKeys {
    public static final int DEFAULT_OPACITY_PERCENT = 90;
    public static final int DEFAULT_KEYBOARD_HEIGHT_DP = 200;
    public static final int DEFAULT_GAMEPAD_SCALE_PERCENT = 100;
    public static final int DEFAULT_FREE_STICK_OPACITY_PERCENT = 20;
    public static final int DEFAULT_NORMAL_COLOR = 0xFF888888;

    public static final SettingKey<Integer> CONTROL_OPACITY_PERCENT =
            SettingKey.integerKey(
                    "seekbar_osc_opacity",
                    DEFAULT_OPACITY_PERCENT,
                    0,
                    100);
    public static final SettingKey<Integer> KEYBOARD_OPACITY_PERCENT =
            SettingKey.integerKey(
                    "seekbar_keyboard_axi_opacity",
                    DEFAULT_OPACITY_PERCENT,
                    0,
                    100);
    public static final SettingKey<Integer> KEYBOARD_HEIGHT_DP =
            SettingKey.integerKey(
                    "seekbar_keyboard_axi_height",
                    DEFAULT_KEYBOARD_HEIGHT_DP,
                    100,
                    400);
    public static final SettingKey<Boolean> KEYBOARD_HAPTICS =
            SettingKey.booleanKey(
                    "checkbox_vibrate_keyboard",
                    false);
    public static final SettingKey<Boolean>
            SHOW_VIRTUAL_KEYS_ON_START =
            SettingKey.booleanKey(
                    "checkbox_enable_keyboard",
                    false);
    public static final SettingKey<Boolean> SQUARE_BUTTONS =
            SettingKey.booleanKey(
                    "checkbox_enable_keyboard_square",
                    false);
    public static final SettingKey<Boolean> SHOW_GUIDE_BUTTON =
            SettingKey.booleanKey(
                    "checkbox_show_guide_button",
                    true);
    public static final SettingKey<Integer> GAMEPAD_SKIN =
            SettingKey.integerSetKey(
                    "onscreen_game_pad_skin",
                    0,
                    0,
                    1,
                    2);
    public static final SettingKey<Boolean> FREE_STICKS =
            SettingKey.booleanKey(
                    "checkbox_enable_analog_stick_new",
                    false);
    public static final SettingKey<Integer> FREE_STICK_OPACITY_PERCENT =
            SettingKey.integerKey(
                    "seekbar_osc_free_analog_stick_opacity",
                    DEFAULT_FREE_STICK_OPACITY_PERCENT,
                    0,
                    100);
    public static final SettingKey<Boolean> FIXED_FREE_STICKS =
            SettingKey.booleanKey(
                    "checkbox_enable_analog_stick_new_fixed",
                    false);
    public static final SettingKey<Integer> NORMAL_COLOR =
            SettingKey.integerKey(
                    "virtual_key_view_normal_color",
                    DEFAULT_NORMAL_COLOR,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
    public static final SettingKey<Integer> GAMEPAD_SCALE_PERCENT =
            SettingKey.integerKey(
                    "virtualGamePadScaleFactor",
                    DEFAULT_GAMEPAD_SCALE_PERCENT,
                    20,
                    180);
    public static final SettingKey<Boolean> DISABLE_STICK_CLICK =
            SettingKey.booleanKey(
                    "checkbox_rocker_click_L3R3",
                    false);
    public static final SettingKey<Boolean>
            AUTOMATIC_SCREEN_ORIENTATION =
            SettingKey.booleanKey(
                    "checkbox_auto_screen_orientation",
                    false);
    public static final SettingKey<Boolean>
            KEYBOARD_COMBINATION_MODE =
            SettingKey.booleanKey(
                    "checkbox_enable_keyboard_axi_combination",
                    false);
    public static final SettingKey<String> KEYBOARD_LAYOUT_ID =
            SettingKey.stringSetKey(
                    "keyboard_axi_list",
                    VirtualControlLayoutProfiles.DEFAULT_KEYBOARD,
                    VirtualControlLayoutProfiles.keyboardIds());
    public static final SettingKey<String> GAMEPAD_LAYOUT_ID =
            SettingKey.stringSetKey(
                    "gamepad_axi_list",
                    VirtualControlLayoutProfiles.DEFAULT_GAMEPAD,
                    VirtualControlLayoutProfiles.gamepadIds());

    private VirtualControlSettingKeys() {
    }
}
