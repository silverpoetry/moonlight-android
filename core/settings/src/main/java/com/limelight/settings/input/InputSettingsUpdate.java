package com.limelight.settings.input;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * One type-safe input-settings intent.
 *
 * <p>The composition root applies the immutable state transition and persists
 * its canonical keys. UI code cannot mutate session state or address
 * persistence by string name.</p>
 */
public final class InputSettingsUpdate {
    private interface Applier {
        InputSettings apply(InputSettings settings);
    }

    private interface Persister {
        void persist(SettingsRepository.Editor editor);
    }

    private final Applier applier;
    private final Persister persister;

    private InputSettingsUpdate(
            Applier applier,
            Persister persister) {
        this.applier = Objects.requireNonNull(applier, "applier");
        this.persister =
                Objects.requireNonNull(persister, "persister");
    }

    public static InputSettingsUpdate
            directTouchSensitivityEnabled(boolean enabled) {
        return single(
                InputSettingKeys.DIRECT_TOUCH_SENSITIVITY_ENABLED,
                enabled,
                (settings, value) -> settings.toBuilder()
                        .setDirectTouchSensitivityEnabled(value)
                        .build());
    }

    public static InputSettingsUpdate
            softKeyboardGestureFingers(int fingerCount) {
        return single(
                InputSettingKeys.SOFT_KEYBOARD_GESTURE_FINGERS,
                fingerCount,
                (settings, normalized) -> settings.toBuilder()
                        .setSoftKeyboardGestureFingers(normalized)
                        .build());
    }

    public static InputSettingsUpdate
            barometerForcePressEnabled(boolean enabled) {
        return single(
                InputSettingKeys.BAROMETER_FORCE_PRESS,
                enabled,
                (settings, value) -> settings.toBuilder()
                        .setBarometerForcePressEnabled(value)
                        .build());
    }

    public static InputSettingsUpdate
            barometerForcePressThresholdMilliHpa(int threshold) {
        return single(
                InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD,
                threshold,
                (settings, value) -> settings.toBuilder()
                        .setBarometerForcePressThresholdHpa(
                                value / 1_000f)
                        .build());
    }

    public static InputSettingsUpdate
            barometerForcePressMinimumDurationMs(int durationMs) {
        return single(
                InputSettingKeys
                        .BAROMETER_FORCE_PRESS_MINIMUM_DURATION,
                durationMs,
                (settings, value) -> settings.toBuilder()
                        .setBarometerForcePressMinimumDurationMs(value)
                        .build());
    }

    public static InputSettingsUpdate
            touchpadLongPressDurationMs(int durationMs) {
        return single(
                InputSettingKeys.TOUCHPAD_LONG_PRESS_DURATION,
                durationMs,
                (settings, value) -> settings.toBuilder()
                        .setTouchpadLongPressDurationMs(value)
                        .build());
    }

    public static InputSettingsUpdate
            directTouchRecenterEnabled(boolean enabled) {
        return single(
                InputSettingKeys.DIRECT_TOUCH_RECENTER,
                enabled,
                (settings, value) -> settings.toBuilder()
                        .setDirectTouchRecenterEnabled(value)
                        .build());
    }

    public static InputSettingsUpdate
            directTouchSensitivityGlobal(boolean enabled) {
        return single(
                InputSettingKeys.DIRECT_TOUCH_SENSITIVITY_GLOBAL,
                enabled,
                (settings, value) -> settings.toBuilder()
                        .setDirectTouchSensitivityGlobal(value)
                        .build());
    }

    public static InputSettingsUpdate
            directTouchSensitivityX(int value) {
        return single(
                InputSettingKeys.DIRECT_TOUCH_SENSITIVITY_X,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setDirectTouchSensitivity(
                                normalized,
                                settings
                                        .getDirectTouchSensitivityY())
                        .build());
    }

    public static InputSettingsUpdate
            directTouchSensitivityY(int value) {
        return single(
                InputSettingKeys.DIRECT_TOUCH_SENSITIVITY_Y,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setDirectTouchSensitivity(
                                settings
                                        .getDirectTouchSensitivityX(),
                                normalized)
                        .build());
    }

    public static InputSettingsUpdate
            touchpadPointerSensitivityX(int value) {
        return single(
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_X,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setTouchpadPointerSensitivity(
                                normalized,
                                settings
                                        .getTouchpadPointerSensitivityY())
                        .build());
    }

    public static InputSettingsUpdate
            touchpadPointerSensitivityY(int value) {
        return single(
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_Y,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setTouchpadPointerSensitivity(
                                settings
                                        .getTouchpadPointerSensitivityX(),
                                normalized)
                        .build());
    }

    public static InputSettingsUpdate
            virtualTouchpadSensitivityX(int value) {
        return single(
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_X,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setVirtualTouchpadSensitivity(
                                normalized,
                                settings
                                        .getVirtualTouchpadSensitivityY())
                        .build());
    }

    public static InputSettingsUpdate
            virtualTouchpadSensitivityY(int value) {
        return single(
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_Y,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setVirtualTouchpadSensitivity(
                                settings
                                        .getVirtualTouchpadSensitivityX(),
                                normalized)
                        .build());
    }

    public static InputSettingsUpdate
            externalTouchpadSensitivityX(int value) {
        return single(
                InputSettingKeys.EXTERNAL_TOUCHPAD_SENSITIVITY_X,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setExternalTouchpadSensitivity(
                                normalized,
                                settings
                                        .getExternalTouchpadSensitivityY())
                        .build());
    }

    public static InputSettingsUpdate
            externalTouchpadSensitivityY(int value) {
        return single(
                InputSettingKeys.EXTERNAL_TOUCHPAD_SENSITIVITY_Y,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setExternalTouchpadSensitivity(
                                settings
                                        .getExternalTouchpadSensitivityX(),
                                normalized)
                        .build());
    }

    public static InputSettingsUpdate
            externalTouchpadScrollAmount(int value) {
        return single(
                InputSettingKeys.EXTERNAL_TOUCHPAD_SCROLL_AMOUNT,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setExternalTouchpadScrollAmount(normalized)
                        .build());
    }

    public static InputSettingsUpdate
            mouseWheelScrollAmount(int value) {
        return single(
                InputSettingKeys.MOUSE_WHEEL_SCROLL_AMOUNT,
                value,
                (settings, normalized) -> settings.toBuilder()
                        .setMouseWheelScrollAmount(normalized)
                        .build());
    }

    public static InputSettingsUpdate resetSensitivity() {
        int sensitivity =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        int scroll = InputSettingKeys.DEFAULT_SCROLL_AMOUNT;
        return new InputSettingsUpdate(
                settings -> settings.toBuilder()
                        .setDirectTouchSensitivityEnabled(false)
                        .setDirectTouchSensitivity(
                                sensitivity,
                                sensitivity)
                        .setDirectTouchSensitivityGlobal(false)
                        .setDirectTouchRecenterEnabled(true)
                        .setTouchpadPointerSensitivity(
                                sensitivity,
                                sensitivity)
                        .setVirtualTouchpadSensitivity(
                                sensitivity,
                                sensitivity)
                        .setExternalTouchpadSensitivity(
                                sensitivity,
                                sensitivity)
                        .setExternalTouchpadScrollAmount(scroll)
                        .setMouseWheelScrollAmount(scroll)
                        .build(),
                editor -> editor
                        .put(
                                InputSettingKeys
                                        .DIRECT_TOUCH_SENSITIVITY_ENABLED,
                                false)
                        .put(
                                InputSettingKeys
                                        .DIRECT_TOUCH_SENSITIVITY_X,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .DIRECT_TOUCH_SENSITIVITY_Y,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .DIRECT_TOUCH_SENSITIVITY_GLOBAL,
                                false)
                        .put(
                                InputSettingKeys.DIRECT_TOUCH_RECENTER,
                                true)
                        .put(
                                InputSettingKeys
                                        .TOUCHPAD_POINTER_SENSITIVITY_X,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .TOUCHPAD_POINTER_SENSITIVITY_Y,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .VIRTUAL_TOUCHPAD_SENSITIVITY_X,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .VIRTUAL_TOUCHPAD_SENSITIVITY_Y,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .EXTERNAL_TOUCHPAD_SENSITIVITY_X,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .EXTERNAL_TOUCHPAD_SENSITIVITY_Y,
                                sensitivity)
                        .put(
                                InputSettingKeys
                                        .EXTERNAL_TOUCHPAD_SCROLL_AMOUNT,
                                scroll)
                        .put(
                                InputSettingKeys
                                        .MOUSE_WHEEL_SCROLL_AMOUNT,
                                scroll));
    }

    public InputSettings applyTo(InputSettings settings) {
        return applier.apply(
                Objects.requireNonNull(settings, "settings"));
    }

    public void persist(SettingsRepository repository) {
        SettingsRepository.Editor editor =
                Objects.requireNonNull(repository, "repository")
                        .edit();
        persister.persist(editor);
        editor.apply();
    }

    private interface ValueApplier<T> {
        InputSettings apply(InputSettings settings, T value);
    }

    private static <T> InputSettingsUpdate single(
            SettingKey<T> key,
            T value,
            ValueApplier<T> applier) {
        T normalized = key.normalizeValue(value);
        return new InputSettingsUpdate(
                settings -> applier.apply(
                        settings,
                        normalized),
                editor -> editor.put(key, normalized));
    }
}
