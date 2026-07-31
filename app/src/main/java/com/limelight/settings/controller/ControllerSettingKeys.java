package com.limelight.settings.controller;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for physical-controller runtime policy.
 */
public final class ControllerSettingKeys {
    public static final SettingKey<Integer> STICK_DEADZONE_PERCENT =
            SettingKey.integerKey(
                    "input.controller.stick_deadzone_percent",
                    7,
                    0,
                    50)
                    .renamedFrom("seekbar_deadzone");
    public static final SettingKey<Boolean> MULTI_CONTROLLER =
            SettingKey.booleanKey(
                    "input.controller.multiple_controllers",
                    true)
                    .renamedFrom("checkbox_multi_controller");
    public static final SettingKey<Boolean> USB_DRIVER =
            SettingKey.booleanKey(
                    "input.controller.usb_driver.enabled",
                    true)
                    .renamedFrom("checkbox_usb_driver");
    public static final SettingKey<Boolean> CLAIM_ALL_USB_DEVICES =
            SettingKey.booleanKey(
                    "input.controller.usb_driver.claim_all_devices",
                    false)
                    .renamedFrom("checkbox_usb_bind_all");
    public static final SettingKey<Boolean> ONSCREEN_CONTROLLER =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.enabled",
                    false)
                    .renamedFrom("checkbox_show_onscreen_controls");
    public static final SettingKey<Boolean> ONLY_L3_R3 =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.only_stick_clicks",
                    false)
                    .renamedFrom("checkbox_only_show_L3R3");
    public static final SettingKey<Boolean> DISABLE_TRIGGER_DEADZONE =
            SettingKey.booleanKey(
                    "input.controller.trigger_deadzone_disabled",
                    false)
                    .renamedFrom("checkbox_disable_trigger_deadzone");
    public static final SettingKey<Boolean> DEVICE_RUMBLE =
            SettingKey.booleanKey(
                    "input.controller.rumble.use_device",
                    false)
                    .renamedFrom("checkbox_enable_device_rumble");
    public static final SettingKey<Boolean> MOTION_SENSORS =
            SettingKey.booleanKey(
                    "input.controller.motion.enabled",
                    true)
                    .renamedFrom("checkbox_gamepad_motion_sensors");
    public static final SettingKey<Boolean>
            MOTION_SENSORS_FALLBACK_TO_DEVICE =
            SettingKey.booleanKey(
                    "input.controller.motion.fallback_to_device",
                    false)
                    .renamedFrom("checkbox_gamepad_motion_fallback");
    public static final SettingKey<Boolean> JOY_CON_FIX =
            SettingKey.booleanKey(
                    "input.controller.joycon_compatibility",
                    false)
                    .renamedFrom("checkbox_enable_joyconfix");
    public static final SettingKey<Boolean> TOUCHPAD_AS_MOUSE =
            SettingKey.booleanKey(
                    "input.controller.touchpad_as_mouse",
                    false)
                    .renamedFrom("checkbox_gamepad_touchpad_as_mouse");
    public static final SettingKey<Integer> MOUSE_SENSITIVITY_PERCENT =
            SettingKey.integerKey(
                    "input.controller.mouse_emulation.sensitivity_percent",
                    100,
                    10,
                    300)
                    .renamedFrom("mouse_gamepad_sensitity");
    public static final SettingKey<Boolean> FLIP_RUMBLE_MOTORS =
            SettingKey.booleanKey(
                    "input.controller.rumble.flip_motors",
                    false)
                    .renamedFrom("checkbox_flip_rumble_ff");
    public static final SettingKey<Boolean> FORCE_STRONG_VIBRATIONS =
            SettingKey.booleanKey(
                    "input.controller.rumble.force_strong",
                    false)
                    .renamedFrom("enable_force_strong_vibrations");
    public static final SettingKey<Boolean>
            FORCE_STRONG_VIBRATIONS_STOP_PULSE =
            SettingKey.booleanKey(
                    "input.controller.rumble.stop_pulse",
                    false)
                    .renamedFrom(
                            "enable_force_strong_vibrations_stop");
    public static final SettingKey<Boolean> ONSCREEN_RUMBLE =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.haptics",
                    true)
                    .renamedFrom("checkbox_vibrate_osc");
    public static final SettingKey<Boolean> FALLBACK_DEVICE_RUMBLE =
            SettingKey.booleanKey(
                    "input.controller.rumble.fallback_to_device",
                    false)
                    .renamedFrom("checkbox_vibrate_fallback");
    public static final SettingKey<Integer>
            FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT =
            SettingKey.integerKey(
                    "input.controller.rumble.fallback_strength_percent",
                    100,
                    0,
                    200)
                    .renamedFrom("seekbar_vibrate_fallback_strength");
    public static final SettingKey<Boolean> FORCE_GYRO =
            SettingKey.booleanKey(
                    "input.controller.motion.force_gyro",
                    false)
                    .renamedFrom("gameForceGyro");
    public static final SettingKey<Boolean>
            FORCE_GYRO_REQUIRES_LEFT_TRIGGER =
            SettingKey.booleanKey(
                    "input.controller.motion.force_gyro_requires_left_trigger",
                    false)
                    .renamedFrom("gameForceGyroLeftTrigger");
    public static final SettingKey<Boolean> FORCE_GYRO_SWAP_AXES =
            SettingKey.booleanKey(
                    "input.controller.motion.force_gyro_swap_axes",
                    true)
                    .renamedFrom("gameForceGyroXYSwitch");
    public static final SettingKey<Integer>
            FORCE_GYRO_SENSITIVITY_PERCENT =
            SettingKey.integerKey(
                    "input.controller.motion.force_gyro_sensitivity_percent",
                    120,
                    50,
                    200)
                    .renamedFrom("gameForceGyroSensitivity");
    public static final SettingKey<Boolean>
            VIRTUAL_CONTROLLER_MOTION =
            SettingKey.booleanKey(
                    "input.virtual_gamepad.device_motion",
                    false)
                    .renamedFrom("checkbox_enable_virtual_motion");
    public static final SettingKey<Boolean> FLIP_FACE_BUTTONS =
            SettingKey.booleanKey(
                    "input.controller.flip_face_buttons",
                    false)
                    .renamedFrom("checkbox_flip_face_buttons");
    public static final SettingKey<Boolean> MOUSE_EMULATION =
            SettingKey.booleanKey(
                    "input.controller.mouse_emulation.enabled",
                    true)
                    .renamedFrom("checkbox_mouse_emulation");
    public static final SettingKey<Integer> MOUSE_EMULATION_BUTTON =
            SettingKey.integerSetKey(
                    "input.controller.mouse_emulation.button",
                    0,
                    0,
                    1,
                    2)
                    .renamedFrom("ax_quick_game_menu_key");
    public static final SettingKey<Boolean>
            MOUSE_EMULATION_OPENS_GAME_MENU =
            SettingKey.booleanKey(
                    "input.controller.mouse_emulation.opens_game_menu",
                    false)
                    .renamedFrom("checkbox_enable_quit_dialog");
    public static final SettingKey<Boolean> USB_GYROSCOPE_REPORTING =
            SettingKey.booleanKey(
                    "input.controller.usb_driver.gyroscope_reporting",
                    true)
                    .renamedFrom("usbGyroscopeReport");
    public static final SettingKey<String> ANALOG_STICK_FOR_SCROLLING =
            SettingKey.stringSetKey(
                    "input.controller.mouse_emulation.scrolling_stick",
                    "right",
                    "none",
                    "right",
                    "left")
                    .renamedFrom("analog_scrolling");
    public static final SettingKey<Boolean> BATTERY_REPORTING =
            SettingKey.booleanKey(
                    "input.controller.battery_reporting",
                    true)
                    .renamedFrom(
                            "checkbox_gamepad_enable_battery_report");
    public static final SettingKey<Boolean> TRIGGER_RUMBLE_LINK =
            SettingKey.booleanKey(
                    "input.controller.rumble.link_triggers",
                    false)
                    .renamedFrom("gameTriggerRumbleLink");
    public static final SettingKey<Integer> ADAPTIVE_TRIGGER_MODE =
            SettingKey.integerSetKey(
                    "input.controller.adaptive_triggers.mode",
                    0,
                    0,
                    1,
                    2,
                    6)
                    .renamedFrom("ds5TriggerMode");
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_STRENGTH =
            SettingKey.integerKey(
                    "input.controller.adaptive_triggers.strength",
                    230,
                    10,
                    255)
                    .renamedFrom("ds5TriggerStrength");
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_FREQUENCY =
            SettingKey.integerKey(
                    "input.controller.adaptive_triggers.frequency",
                    10,
                    5,
                    15)
                    .renamedFrom("ds5TriggerFrequency");
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_START_POSITION =
            SettingKey.integerKey(
                    "input.controller.adaptive_triggers.start_position",
                    40,
                    10,
                    255)
                    .renamedFrom("ds5TriggerStart");
    public static final SettingKey<Integer>
            ADAPTIVE_TRIGGER_END_POSITION =
            SettingKey.integerKey(
                    "input.controller.adaptive_triggers.end_position",
                    100,
                    10,
                    255)
                    .renamedFrom("ds5TriggerEnd");

    private ControllerSettingKeys() {
    }
}
