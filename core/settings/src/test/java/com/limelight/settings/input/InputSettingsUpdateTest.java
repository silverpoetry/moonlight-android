package com.limelight.settings.input;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InputSettingsUpdateTest {
    @Test
    public void scalarUpdateChangesOnlyItsOwnedField() {
        InputSettings original = representativeSettings();

        InputSettings updated =
                InputSettingsUpdate
                        .mouseWheelScrollAmount(17)
                        .applyTo(original);

        assertEquals(17, updated.getMouseWheelScrollAmount());
        assertEquals(
                original.getExternalTouchpadScrollAmount(),
                updated.getExternalTouchpadScrollAmount());
        assertEquals(
                original.getTouchModePreferenceValue(),
                updated.getTouchModePreferenceValue());
        assertEquals(
                original.getSoftKeyboardGestureFingers(),
                updated.getSoftKeyboardGestureFingers());
        assertEquals(
                original.isBarometerForcePressEnabled(),
                updated.isBarometerForcePressEnabled());
    }

    @Test
    public void updateNormalizesBeforeStateAndStorage() {
        FakeRepository repository = new FakeRepository();
        InputSettings original = representativeSettings();
        InputSettingsUpdate update =
                InputSettingsUpdate
                        .mouseWheelScrollAmount(Integer.MAX_VALUE);

        InputSettings updated = update.applyTo(original);
        update.persist(repository);

        assertEquals(
                InputSettingKeys.MAX_SCROLL_AMOUNT,
                updated.getMouseWheelScrollAmount());
        assertEquals(
                InputSettingKeys.MAX_SCROLL_AMOUNT,
                repository.values.get(
                        InputSettingKeys.MOUSE_WHEEL_SCROLL_AMOUNT
                                .getName()));
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void resetIsOneAtomicSensitivityIntent() {
        FakeRepository repository = new FakeRepository();
        InputSettings original = representativeSettings();
        InputSettingsUpdate reset =
                InputSettingsUpdate.resetSensitivity();

        InputSettings updated = reset.applyTo(original);
        reset.persist(repository);

        assertFalse(updated.isDirectTouchSensitivityEnabled());
        assertFalse(updated.isDirectTouchSensitivityGlobal());
        assertTrue(updated.isDirectTouchRecenterEnabled());
        assertEquals(
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT,
                updated.getDirectTouchSensitivityX());
        assertEquals(
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT,
                updated.getTouchpadPointerSensitivityY());
        assertEquals(
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT,
                updated.getVirtualTouchpadSensitivityX());
        assertEquals(
                InputSettingKeys.DEFAULT_SCROLL_AMOUNT,
                updated.getExternalTouchpadScrollAmount());
        assertEquals(
                InputSettingKeys.DEFAULT_SCROLL_AMOUNT,
                updated.getMouseWheelScrollAmount());

        assertEquals(
                original.getTouchModePreferenceValue(),
                updated.getTouchModePreferenceValue());
        assertEquals(
                original.getSoftKeyboardGestureFingers(),
                updated.getSoftKeyboardGestureFingers());
        assertTrue(updated.isAbsoluteMouseMode());
        assertTrue(updated.isBarometerForcePressEnabled());

        assertEquals(13, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void softKeyboardGestureUsesAllowedFingerCounts() {
        FakeRepository repository = new FakeRepository();
        InputSettings original = representativeSettings();
        InputSettingsUpdate update =
                InputSettingsUpdate
                        .softKeyboardGestureFingers(99);

        InputSettings updated = update.applyTo(original);
        update.persist(repository);

        assertEquals(0, updated.getSoftKeyboardGestureFingers());
        assertEquals(
                original.getMouseWheelScrollAmount(),
                updated.getMouseWheelScrollAmount());
        assertEquals(
                0,
                repository.values.get(
                        InputSettingKeys
                                .SOFT_KEYBOARD_GESTURE_FINGERS
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void touchpadLongPressUpdateIsTypedAndClamped() {
        FakeRepository repository = new FakeRepository();
        InputSettings original = representativeSettings();
        InputSettingsUpdate update =
                InputSettingsUpdate.touchpadLongPressDurationMs(
                        Integer.MIN_VALUE);

        InputSettings updated = update.applyTo(original);
        update.persist(repository);

        assertEquals(
                InputSettingKeys.MIN_TOUCHPAD_LONG_PRESS_DURATION_MS,
                updated.getTouchpadLongPressDurationMs());
        assertEquals(
                InputSettingKeys.MIN_TOUCHPAD_LONG_PRESS_DURATION_MS,
                repository.values.get(
                        InputSettingKeys.TOUCHPAD_LONG_PRESS_DURATION
                                .getName()));
        assertEquals(1, repository.applyCount);
    }

    private static InputSettings representativeSettings() {
        return InputSettings.builder()
                .setTouchModePreferenceValue(5)
                .setAbsoluteMouseMode(true)
                .setBarometerForcePressEnabled(true)
                .setBarometerForcePressThresholdHpa(0.25f)
                .setBarometerForcePressMinimumDurationMs(100)
                .setTouchpadLongPressDurationMs(700)
                .setSoftKeyboardGestureFingers(4)
                .setTouchpadPointerSensitivity(130, 140)
                .setVirtualTouchpadSensitivity(150, 160)
                .setExternalTouchpadSensitivity(170, 180)
                .setExternalTouchpadScrollAmount(19)
                .setMouseWheelScrollAmount(20)
                .setDirectTouchSensitivityEnabled(true)
                .setDirectTouchSensitivity(210, 220)
                .setDirectTouchSensitivityGlobal(true)
                .setDirectTouchRecenterEnabled(false)
                .build();
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();
        private int applyCount;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(
                    values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public <T> Editor put(
                        SettingKey<T> key,
                        T value) {
                    values.put(
                            key.getName(),
                            key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    values.remove(key.getName());
                    return this;
                }

                @Override
                public void apply() {
                    applyCount++;
                }

                @Override
                public boolean commit() {
                    applyCount++;
                    return true;
                }
            };
        }
    }
}
