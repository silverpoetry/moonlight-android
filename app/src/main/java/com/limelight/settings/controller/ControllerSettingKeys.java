package com.limelight.settings.controller;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for physical-controller runtime policy.
 */
public final class ControllerSettingKeys {
    public static final SettingKey<Integer> STICK_DEADZONE_PERCENT =
            SettingKey.integerKey("seekbar_deadzone", 7, 0, 50);
    public static final SettingKey<Boolean> MULTI_CONTROLLER =
            SettingKey.booleanKey("checkbox_multi_controller", true);
    public static final SettingKey<Boolean> USB_DRIVER =
            SettingKey.booleanKey("checkbox_usb_driver", true);
    public static final SettingKey<Boolean> CLAIM_ALL_USB_DEVICES =
            SettingKey.booleanKey(
                    "checkbox_usb_bind_all",
                    false);
    public static final SettingKey<Boolean> ONSCREEN_CONTROLLER =
            SettingKey.booleanKey(
                    "checkbox_show_onscreen_controls",
                    false);
    public static final SettingKey<Boolean> ONLY_L3_R3 =
            SettingKey.booleanKey(
                    "checkbox_only_show_L3R3",
                    false);
    public static final SettingKey<Boolean> DISABLE_TRIGGER_DEADZONE =
            SettingKey.booleanKey(
                    "checkbox_disable_trigger_deadzone",
                    false);
    public static final SettingKey<Boolean> DEVICE_RUMBLE =
            SettingKey.booleanKey(
                    "checkbox_enable_device_rumble",
                    false);
    public static final SettingKey<Boolean> MOTION_SENSORS =
            SettingKey.booleanKey(
                    "checkbox_gamepad_motion_sensors",
                    true);
    public static final SettingKey<Boolean>
            MOTION_SENSORS_FALLBACK_TO_DEVICE =
            SettingKey.booleanKey(
                    "checkbox_gamepad_motion_fallback",
                    false);
    public static final SettingKey<Boolean> JOY_CON_FIX =
            SettingKey.booleanKey(
                    "checkbox_enable_joyconfix",
                    false);
    public static final SettingKey<Boolean> TOUCHPAD_AS_MOUSE =
            SettingKey.booleanKey(
                    "checkbox_gamepad_touchpad_as_mouse",
                    false);
    public static final SettingKey<Integer> MOUSE_SENSITIVITY_PERCENT =
            SettingKey.integerKey(
                    "mouse_gamepad_sensitity",
                    100,
                    10,
                    300);
    public static final SettingKey<Boolean> FLIP_RUMBLE_MOTORS =
            SettingKey.booleanKey("checkbox_flip_rumble_ff", false);
    public static final SettingKey<Boolean> FORCE_STRONG_VIBRATIONS =
            SettingKey.booleanKey(
                    "enable_force_strong_vibrations",
                    false);
    public static final SettingKey<Boolean>
            FORCE_STRONG_VIBRATIONS_STOP_PULSE =
            SettingKey.booleanKey(
                    "enable_force_strong_vibrations_stop",
                    false);
    public static final SettingKey<Boolean> ONSCREEN_RUMBLE =
            SettingKey.booleanKey("checkbox_vibrate_osc", true);
    public static final SettingKey<Boolean> FALLBACK_DEVICE_RUMBLE =
            SettingKey.booleanKey("checkbox_vibrate_fallback", false);
    public static final SettingKey<Integer>
            FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT =
            SettingKey.integerKey(
                    "seekbar_vibrate_fallback_strength",
                    100,
                    0,
                    200);
    public static final SettingKey<Boolean> FORCE_GYRO =
            SettingKey.booleanKey("gameForceGyro", false);
    public static final SettingKey<Boolean>
            FORCE_GYRO_REQUIRES_LEFT_TRIGGER =
            SettingKey.booleanKey(
                    "gameForceGyroLeftTrigger",
                    false);
    public static final SettingKey<Boolean> FORCE_GYRO_SWAP_AXES =
            SettingKey.booleanKey("gameForceGyroXYSwitch", true);
    public static final SettingKey<Integer>
            FORCE_GYRO_SENSITIVITY_PERCENT =
            SettingKey.integerKey(
                    "gameForceGyroSensitivity",
                    120,
                    50,
                    200);
    public static final SettingKey<Boolean>
            VIRTUAL_CONTROLLER_MOTION =
            SettingKey.booleanKey(
                    "checkbox_enable_virtual_motion",
                    false);
    public static final SettingKey<Boolean> FLIP_FACE_BUTTONS =
            SettingKey.booleanKey(
                    "checkbox_flip_face_buttons",
                    false);
    public static final SettingKey<Boolean> MOUSE_EMULATION =
            SettingKey.booleanKey("checkbox_mouse_emulation", true);
    public static final SettingKey<Integer> MOUSE_EMULATION_BUTTON =
            SettingKey.integerSetKey(
                    "ax_quick_game_menu_key",
                    0,
                    0,
                    1,
                    2);
    public static final SettingKey<Boolean>
            MOUSE_EMULATION_OPENS_GAME_MENU =
            SettingKey.booleanKey(
                    "checkbox_enable_quit_dialog",
                    false);
    public static final SettingKey<Boolean> USB_GYROSCOPE_REPORTING =
            SettingKey.booleanKey("usbGyroscopeReport", true);
    public static final SettingKey<String> ANALOG_STICK_FOR_SCROLLING =
            SettingKey.stringSetKey(
                    "analog_scrolling",
                    "right",
                    "none",
                    "right",
                    "left");
    public static final SettingKey<Boolean> BATTERY_REPORTING =
            SettingKey.booleanKey(
                    "checkbox_gamepad_enable_battery_report",
                    true);
    public static final SettingKey<Boolean> TRIGGER_RUMBLE_LINK =
            SettingKey.booleanKey(
                    "gameTriggerRumbleLink",
                    false);
    public static final SettingKey<Integer> ADAPTIVE_TRIGGER_MODE =
            SettingKey.integerSetKey(
                    "ds5TriggerMode",
                    0,
                    0,
                    1,
                    2,
                    6);
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_STRENGTH =
            SettingKey.integerKey(
                    "ds5TriggerStrength",
                    230,
                    10,
                    255);
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_FREQUENCY =
            SettingKey.integerKey(
                    "ds5TriggerFrequency",
                    10,
                    5,
                    15);
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_START_POSITION =
            SettingKey.integerKey(
                    "ds5TriggerStart",
                    40,
                    10,
                    255);
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_END_POSITION =
            SettingKey.integerKey(
                    "ds5TriggerEnd",
                    100,
                    10,
                    255);

    private ControllerSettingKeys() {
    }
}
