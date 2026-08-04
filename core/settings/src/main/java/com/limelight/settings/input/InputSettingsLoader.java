package com.limelight.settings.input;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Builds a validated input-settings snapshot from persistent storage.
 */
public final class InputSettingsLoader {
    private InputSettingsLoader() {
    }

    public static InputSettings load(SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");

        return InputSettings.builder()
                .setTouchModePreferenceValue(parseTouchMode(
                        repository.get(InputSettingKeys.TOUCH_MODE)))
                .setMouseNavigationButtonsEnabled(repository.get(
                        InputSettingKeys
                                .MOUSE_NAVIGATION_BUTTONS))
                .setAbsoluteMouseMode(repository.get(
                        InputSettingKeys.ABSOLUTE_MOUSE_MODE))
                .setLocalSystemCursorEnabled(repository.get(
                        InputSettingKeys.LOCAL_SYSTEM_CURSOR))
                .setAdaptiveInputThrottlingDisabled(
                        repository.get(
                                InputSettingKeys
                                        .DISABLE_ADAPTIVE_INPUT_THROTTLING))
                .setBarometerForcePressEnabled(repository.get(
                        InputSettingKeys.BAROMETER_FORCE_PRESS))
                .setBarometerForcePressThresholdHpa(
                        repository.get(
                                InputSettingKeys
                                        .BAROMETER_FORCE_PRESS_THRESHOLD) /
                                1_000f)
                .setBarometerForcePressMinimumDurationMs(
                        repository.get(
                                InputSettingKeys
                                        .BAROMETER_FORCE_PRESS_MINIMUM_DURATION))
                .setTouchpadLongPressDurationMs(repository.get(
                        InputSettingKeys.TOUCHPAD_LONG_PRESS_DURATION))
                .setSoftKeyboardGestureFingers(normalizeFingerCount(
                        repository.get(
                                InputSettingKeys
                                        .SOFT_KEYBOARD_GESTURE_FINGERS)))
                .setTouchpadPointerSensitivity(
                        repository.get(
                                InputSettingKeys
                                        .TOUCHPAD_POINTER_SENSITIVITY_X),
                        repository.get(
                                InputSettingKeys
                                        .TOUCHPAD_POINTER_SENSITIVITY_Y))
                .setVirtualTouchpadSensitivity(
                        repository.get(
                                InputSettingKeys
                                        .VIRTUAL_TOUCHPAD_SENSITIVITY_X),
                        repository.get(
                                InputSettingKeys
                                        .VIRTUAL_TOUCHPAD_SENSITIVITY_Y))
                .setExternalTouchpadSensitivity(
                        repository.get(
                                InputSettingKeys
                                        .EXTERNAL_TOUCHPAD_SENSITIVITY_X),
                        repository.get(
                                InputSettingKeys
                                        .EXTERNAL_TOUCHPAD_SENSITIVITY_Y))
                .setExternalTouchpadScrollAmount(repository.get(
                        InputSettingKeys
                                .EXTERNAL_TOUCHPAD_SCROLL_AMOUNT))
                .setMouseWheelScrollAmount(repository.get(
                        InputSettingKeys
                                .MOUSE_WHEEL_SCROLL_AMOUNT))
                .build();
    }

    private static int parseTouchMode(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 && parsed <= 6 ? parsed : 0;
        }
        catch (NumberFormatException invalidValue) {
            return 0;
        }
    }

    private static int normalizeFingerCount(int value) {
        return value >= 3 && value <= 5 ? value : 0;
    }
}
