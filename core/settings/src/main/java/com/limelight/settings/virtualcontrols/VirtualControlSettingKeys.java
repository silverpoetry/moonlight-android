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
                    "input.virtual_gamepad.opacity_percent",
                    DEFAULT_OPACITY_PERCENT,
                    0,
                    100)
                    .renamedFrom("seekbar_osc_opacity");
    public static final SettingKey<Integer> KEYBOARD_OPACITY_PERCENT =
            SettingKey.integerKey(
                    "input.virtual_keyboard.opacity_percent",
                    DEFAULT_OPACITY_PERCENT,
                    0,
                    100)
                    .renamedFrom("seekbar_keyboard_axi_opacity");
    public static final SettingKey<Integer> KEYBOARD_HEIGHT_DP =
            SettingKey.integerKey(
                    "input.virtual_keyboard.height_dp",
                    DEFAULT_KEYBOARD_HEIGHT_DP,
                    100,
                    400)
                    .renamedFrom("seekbar_keyboard_axi_height");
    public static final SettingKey<Boolean> KEYBOARD_HAPTICS =
            SettingKey.booleanKey(
                    "input.virtual_keyboard.haptics",
                    false)
                    .renamedFrom("checkbox_vibrate_keyboard");
    public static final SettingKey<Boolean>
            SHOW_VIRTUAL_KEYS_ON_START =
            SettingKey.booleanKey(
                    "input.virtual_keyboard.show_on_start",
                    false)
                    .renamedFrom("checkbox_enable_keyboard");
    public static final SettingKey<Boolean> SQUARE_BUTTONS =
            SettingKey.booleanKey(
                    "input.virtual_keyboard.square_buttons",
                    false)
                    .renamedFrom("checkbox_enable_keyboard_square");
    public static final SettingKey<Boolean> SHOW_GUIDE_BUTTON =
            SettingKey.booleanKey(
                    "input.virtual_controls.show_guide_button",
                    true)
                    .renamedFrom("checkbox_show_guide_button");
    public static final SettingKey<Integer> GAMEPAD_SKIN =
            SettingKey.integerSetKey(
                    "input.virtual_gamepad.skin",
                    0,
                    0,
                    1,
                    2)
                    .renamedFrom("onscreen_game_pad_skin");
    public static final SettingKey<Boolean> FREE_STICKS =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.free_sticks",
                    false)
                    .renamedFrom("checkbox_enable_analog_stick_new");
    public static final SettingKey<Integer> FREE_STICK_OPACITY_PERCENT =
            SettingKey.integerKey(
                    "input.virtual_gamepad.free_stick_opacity_percent",
                    DEFAULT_FREE_STICK_OPACITY_PERCENT,
                    0,
                    100)
                    .renamedFrom(
                            "seekbar_osc_free_analog_stick_opacity");
    public static final SettingKey<Boolean> FIXED_FREE_STICKS =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.fixed_free_sticks",
                    false)
                    .renamedFrom(
                            "checkbox_enable_analog_stick_new_fixed");
    public static final SettingKey<Integer> NORMAL_COLOR =
            SettingKey.integerKey(
                    "input.virtual_controls.normal_color",
                    DEFAULT_NORMAL_COLOR,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE)
                    .renamedFrom("virtual_key_view_normal_color");
    public static final SettingKey<Integer> GAMEPAD_SCALE_PERCENT =
            SettingKey.integerKey(
                    "input.virtual_gamepad.scale_percent",
                    DEFAULT_GAMEPAD_SCALE_PERCENT,
                    20,
                    180)
                    .renamedFrom("virtualGamePadScaleFactor");
    public static final SettingKey<Boolean> DISABLE_STICK_CLICK =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.disable_stick_click",
                    false)
                    .renamedFrom("checkbox_rocker_click_L3R3");
    public static final SettingKey<Boolean>
            AUTOMATIC_SCREEN_ORIENTATION =
            SettingKey.booleanKey(
                    "stream.display.automatic_orientation",
                    false)
                    .renamedFrom("checkbox_auto_screen_orientation");
    public static final SettingKey<Boolean>
            KEYBOARD_COMBINATION_MODE =
            SettingKey.booleanKey(
                    "input.virtual_keyboard.combination_mode",
                    false)
                    .renamedFrom(
                            "checkbox_enable_keyboard_axi_combination");
    public static final SettingKey<String> KEYBOARD_LAYOUT_ID =
            SettingKey.stringSetKey(
                    "input.virtual_keyboard.layout_id",
                    VirtualControlLayoutProfiles.DEFAULT_KEYBOARD,
                    VirtualControlLayoutProfiles.keyboardIds())
                    .renamedFrom("keyboard_axi_list");
    public static final SettingKey<String> GAMEPAD_LAYOUT_ID =
            SettingKey.stringSetKey(
                    "input.virtual_gamepad.layout_id",
                    VirtualControlLayoutProfiles.DEFAULT_GAMEPAD,
                    VirtualControlLayoutProfiles.gamepadIds())
                    .renamedFrom("gamepad_axi_list");

    private VirtualControlSettingKeys() {
    }
}
