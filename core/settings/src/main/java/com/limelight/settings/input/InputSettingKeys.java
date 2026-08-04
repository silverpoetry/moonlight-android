package com.limelight.settings.input;

import com.limelight.settings.SettingKey;

/**
 * Persisted keys for pointer and touchscreen input policy.
 */
public final class InputSettingKeys {
    public static final int DEFAULT_SENSITIVITY_PERCENT = 100;
    public static final int MIN_SENSITIVITY_PERCENT = 10;
    public static final int MAX_SENSITIVITY_PERCENT = 300;
    public static final int DEFAULT_SCROLL_AMOUNT = 5;
    public static final int MIN_SCROLL_AMOUNT = 1;
    public static final int MAX_SCROLL_AMOUNT = 30;
    public static final int DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            180;
    public static final int MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            50;
    public static final int MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            5_000;
    public static final int DEFAULT_FORCE_PRESS_MINIMUM_DURATION_MS =
            100;
    public static final int MAX_FORCE_PRESS_MINIMUM_DURATION_MS =
            500;
    public static final int DEFAULT_TOUCHPAD_LONG_PRESS_DURATION_MS =
            500;
    public static final int MIN_TOUCHPAD_LONG_PRESS_DURATION_MS =
            300;
    public static final int MAX_TOUCHPAD_LONG_PRESS_DURATION_MS =
            1_500;

    public static final SettingKey<String> TOUCH_MODE =
            SettingKey.stringSetKey(
                    "input.pointer.mode",
                    "0",
                    "0",
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6")
                    .renamedFrom("mouse_model_list_axi");
    public static final SettingKey<Boolean> MOUSE_NAVIGATION_BUTTONS =
            SettingKey.booleanKey(
                    "input.pointer.navigation_buttons",
                    false)
                    .renamedFrom("checkbox_mouse_nav_buttons");
    public static final SettingKey<Boolean> ABSOLUTE_MOUSE_MODE =
            SettingKey.booleanKey(
                    "input.pointer.absolute_mouse",
                    false)
                    .renamedFrom("checkbox_absolute_mouse_mode");
    public static final SettingKey<Boolean> LOCAL_SYSTEM_CURSOR =
            SettingKey.booleanKey(
                    "input.pointer.local_system_cursor",
                    false)
                    .renamedFrom("checkbox_mouse_local_cursor");
    public static final SettingKey<Boolean>
            DISABLE_ADAPTIVE_INPUT_THROTTLING =
            SettingKey.booleanKey(
                    "input.transport.disable_adaptive_throttling",
                    true)
                    .renamedFrom(
                            "checkbox_disable_adaptive_input_throttling");
    public static final SettingKey<Boolean>
            ACCESSIBILITY_KEY_LOGGING =
            SettingKey.booleanKey(
                    "input.accessibility.key_logging",
                    false)
                    .renamedFrom(
                            "checkbox_enable_accessibility_show_log");
    public static final SettingKey<Boolean> BAROMETER_FORCE_PRESS =
            SettingKey.booleanKey(
                    "input.touch.force_press.enabled",
                    false)
                    .renamedFrom("checkbox_barometer_force_press");
    public static final SettingKey<Integer>
            BAROMETER_FORCE_PRESS_THRESHOLD =
            SettingKey.integerKey(
                    "input.touch.force_press.threshold_milli_hpa",
                    DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA,
                    MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA,
                    MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA)
                    .renamedFrom(
                            "seekbar_barometer_force_press_threshold");
    public static final SettingKey<Integer>
            BAROMETER_FORCE_PRESS_MINIMUM_DURATION =
            SettingKey.integerKey(
                    "input.touch.force_press.minimum_duration_ms",
                    DEFAULT_FORCE_PRESS_MINIMUM_DURATION_MS,
                    0,
                    MAX_FORCE_PRESS_MINIMUM_DURATION_MS)
                    .renamedFrom(
                            "seekbar_barometer_force_press_min_duration");
    public static final SettingKey<Integer>
            TOUCHPAD_LONG_PRESS_DURATION =
            SettingKey.integerKey(
                    "input.touchpad.long_press_duration_ms",
                    DEFAULT_TOUCHPAD_LONG_PRESS_DURATION_MS,
                    MIN_TOUCHPAD_LONG_PRESS_DURATION_MS,
                    MAX_TOUCHPAD_LONG_PRESS_DURATION_MS);
    public static final SettingKey<Integer>
            SOFT_KEYBOARD_GESTURE_FINGERS =
            SettingKey.integerSetKey(
                    "input.touch.keyboard_gesture_finger_count",
                    0,
                    0,
                    3,
                    4,
                    5)
                    .renamedFrom(
                            "touch_number_quick_soft_keyboard");
    public static final SettingKey<Integer>
            TOUCHPAD_POINTER_SENSITIVITY_X =
            sensitivityKey(
                    "input.touchpad.pointer_sensitivity_x",
                    "seekbar_mouse_touchpad_sensitivity_x_opacity");
    public static final SettingKey<Integer>
            TOUCHPAD_POINTER_SENSITIVITY_Y =
            sensitivityKey(
                    "input.touchpad.pointer_sensitivity_y",
                    "seekbar_mouse_touchpad_sensitivity_y_opacity");
    public static final SettingKey<Integer>
            VIRTUAL_TOUCHPAD_SENSITIVITY_X =
            sensitivityKey(
                    "input.virtual_touchpad.sensitivity_x",
                    "seekbar_touchpad_sensitivity_opacity");
    public static final SettingKey<Integer>
            VIRTUAL_TOUCHPAD_SENSITIVITY_Y =
            sensitivityKey(
                    "input.virtual_touchpad.sensitivity_y",
                    "seekbar_touchpad_sensitivity_y_opacity");
    public static final SettingKey<Integer>
            EXTERNAL_TOUCHPAD_SENSITIVITY_X =
            sensitivityKey(
                    "input.external_touchpad.sensitivity_x",
                    "touchpad_equipment_view_x");
    public static final SettingKey<Integer>
            EXTERNAL_TOUCHPAD_SENSITIVITY_Y =
            sensitivityKey(
                    "input.external_touchpad.sensitivity_y",
                    "touchpad_equipment_view_y");
    public static final SettingKey<Integer>
            EXTERNAL_TOUCHPAD_SCROLL_AMOUNT =
            SettingKey.integerKey(
                    "input.external_touchpad.scroll_amount",
                    DEFAULT_SCROLL_AMOUNT,
                    MIN_SCROLL_AMOUNT,
                    MAX_SCROLL_AMOUNT)
                    .renamedFrom("touchpad_equipment_amount");
    public static final SettingKey<Integer>
            MOUSE_WHEEL_SCROLL_AMOUNT =
            SettingKey.integerKey(
                    "input.mouse.wheel_scroll_amount",
                    DEFAULT_SCROLL_AMOUNT,
                    MIN_SCROLL_AMOUNT,
                    MAX_SCROLL_AMOUNT)
                    .renamedFrom("mouse_sc_amount");
    private InputSettingKeys() {
    }

    private static SettingKey<Integer> sensitivityKey(
            String name,
            String legacyName) {
        return SettingKey.integerKey(
                name,
                DEFAULT_SENSITIVITY_PERCENT,
                MIN_SENSITIVITY_PERCENT,
                MAX_SENSITIVITY_PERCENT)
                .renamedFrom(legacyName);
    }
}
