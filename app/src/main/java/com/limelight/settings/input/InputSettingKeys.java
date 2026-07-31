package com.limelight.settings.input;

import com.limelight.settings.SettingKey;

/**
 * Persisted keys for pointer and touchscreen input policy.
 */
public final class InputSettingKeys {
    public static final int DEFAULT_SENSITIVITY_PERCENT = 100;
    public static final int MIN_SENSITIVITY_PERCENT = 10;
    public static final int MAX_SENSITIVITY_PERCENT = 300;
    public static final int MAX_DIRECT_TOUCH_SENSITIVITY_PERCENT =
            800;
    public static final int DEFAULT_SCROLL_AMOUNT = 5;
    public static final int MIN_SCROLL_AMOUNT = 1;
    public static final int MAX_SCROLL_AMOUNT = 30;
    public static final int DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            180;
    public static final int MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            50;
    public static final int MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            1_000;
    public static final int DEFAULT_FORCE_PRESS_MINIMUM_DURATION_MS =
            100;
    public static final int MAX_FORCE_PRESS_MINIMUM_DURATION_MS =
            500;

    public static final SettingKey<String> TOUCH_MODE =
            SettingKey.stringSetKey(
                    "mouse_model_list_axi",
                    "0",
                    "0",
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6");
    public static final SettingKey<Boolean> MOUSE_NAVIGATION_BUTTONS =
            SettingKey.booleanKey(
                    "checkbox_mouse_nav_buttons",
                    false);
    public static final SettingKey<Boolean> ABSOLUTE_MOUSE_MODE =
            SettingKey.booleanKey(
                    "checkbox_absolute_mouse_mode",
                    false);
    public static final SettingKey<Boolean> LOCAL_SYSTEM_CURSOR =
            SettingKey.booleanKey(
                    "checkbox_mouse_local_cursor",
                    false);
    public static final SettingKey<Boolean>
            DISABLE_ADAPTIVE_INPUT_THROTTLING =
            SettingKey.booleanKey(
                    "checkbox_disable_adaptive_input_throttling",
                    true);
    public static final SettingKey<Boolean> BAROMETER_FORCE_PRESS =
            SettingKey.booleanKey(
                    "checkbox_barometer_force_press",
                    false);
    public static final SettingKey<Integer>
            BAROMETER_FORCE_PRESS_THRESHOLD =
            SettingKey.integerKey(
                    "seekbar_barometer_force_press_threshold",
                    DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA,
                    MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA,
                    MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA);
    public static final SettingKey<Integer>
            BAROMETER_FORCE_PRESS_MINIMUM_DURATION =
            SettingKey.integerKey(
                    "seekbar_barometer_force_press_min_duration",
                    DEFAULT_FORCE_PRESS_MINIMUM_DURATION_MS,
                    0,
                    MAX_FORCE_PRESS_MINIMUM_DURATION_MS);
    public static final SettingKey<Integer>
            SOFT_KEYBOARD_GESTURE_FINGERS =
            SettingKey.integerSetKey(
                    "touch_number_quick_soft_keyboard",
                    0,
                    0,
                    3,
                    4,
                    5);
    public static final SettingKey<Integer>
            TOUCHPAD_POINTER_SENSITIVITY_X =
            sensitivityKey(
                    "seekbar_mouse_touchpad_sensitivity_x_opacity");
    public static final SettingKey<Integer>
            TOUCHPAD_POINTER_SENSITIVITY_Y =
            sensitivityKey(
                    "seekbar_mouse_touchpad_sensitivity_y_opacity");
    public static final SettingKey<Integer>
            VIRTUAL_TOUCHPAD_SENSITIVITY_X =
            sensitivityKey(
                    "seekbar_touchpad_sensitivity_opacity");
    public static final SettingKey<Integer>
            VIRTUAL_TOUCHPAD_SENSITIVITY_Y =
            sensitivityKey(
                    "seekbar_touchpad_sensitivity_y_opacity");
    public static final SettingKey<Integer>
            EXTERNAL_TOUCHPAD_SENSITIVITY_X =
            sensitivityKey("touchpad_equipment_view_x");
    public static final SettingKey<Integer>
            EXTERNAL_TOUCHPAD_SENSITIVITY_Y =
            sensitivityKey("touchpad_equipment_view_y");
    public static final SettingKey<Integer>
            EXTERNAL_TOUCHPAD_SCROLL_AMOUNT =
            SettingKey.integerKey(
                    "touchpad_equipment_amount",
                    DEFAULT_SCROLL_AMOUNT,
                    MIN_SCROLL_AMOUNT,
                    MAX_SCROLL_AMOUNT);
    public static final SettingKey<Integer>
            MOUSE_WHEEL_SCROLL_AMOUNT =
            SettingKey.integerKey(
                    "mouse_sc_amount",
                    DEFAULT_SCROLL_AMOUNT,
                    MIN_SCROLL_AMOUNT,
                    MAX_SCROLL_AMOUNT);
    public static final SettingKey<Boolean>
            DIRECT_TOUCH_SENSITIVITY_ENABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_touch_sensitivity",
                    false);
    public static final SettingKey<Integer>
            DIRECT_TOUCH_SENSITIVITY_X =
            SettingKey.integerKey(
                    "seekbar_touch_sensitivity_opacity_x",
                    DEFAULT_SENSITIVITY_PERCENT,
                    MIN_SENSITIVITY_PERCENT,
                    MAX_DIRECT_TOUCH_SENSITIVITY_PERCENT);
    public static final SettingKey<Integer>
            DIRECT_TOUCH_SENSITIVITY_Y =
            SettingKey.integerKey(
                    "seekbar_touch_sensitivity_opacity_y",
                    DEFAULT_SENSITIVITY_PERCENT,
                    MIN_SENSITIVITY_PERCENT,
                    MAX_DIRECT_TOUCH_SENSITIVITY_PERCENT);
    public static final SettingKey<Boolean>
            DIRECT_TOUCH_SENSITIVITY_GLOBAL =
            SettingKey.booleanKey(
                    "checkbox_enable_global_touch_sensitivity",
                    false);
    public static final SettingKey<Boolean>
            DIRECT_TOUCH_RECENTER =
            SettingKey.booleanKey(
                    "checkbox_enable_touch_sensitivity_rotation_auto",
                    true);

    private InputSettingKeys() {
    }

    private static SettingKey<Integer> sensitivityKey(
            String name) {
        return SettingKey.integerKey(
                name,
                DEFAULT_SENSITIVITY_PERCENT,
                MIN_SENSITIVITY_PERCENT,
                MAX_SENSITIVITY_PERCENT);
    }
}
