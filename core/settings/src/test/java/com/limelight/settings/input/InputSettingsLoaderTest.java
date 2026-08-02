package com.limelight.settings.input;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        InputSettings settings = InputSettingsLoader.load(
                new FakeRepository());

        assertEquals(0, settings.getTouchModePreferenceValue());
        assertFalse(settings.isAbsoluteMouseMode());
        assertFalse(settings.isLocalSystemCursorEnabled());
        assertTrue(
                settings.isAdaptiveInputThrottlingDisabled());
        assertEquals(
                100,
                settings.getTouchpadPointerSensitivityX());
        assertEquals(
                100,
                settings.getVirtualTouchpadSensitivityX());
        assertEquals(
                5,
                settings.getExternalTouchpadScrollAmount());
        assertEquals(
                5,
                settings.getMouseWheelScrollAmount());
        assertEquals(
                0.18f,
                settings.getBarometerForcePressThresholdHpa(),
                0f);
        assertEquals(
                InputSettingKeys
                        .DEFAULT_TOUCHPAD_LONG_PRESS_DURATION_MS,
                settings.getTouchpadLongPressDurationMs());
        assertTrue(settings.isDirectTouchRecenterEnabled());
    }

    @Test
    public void invalidValuesAreNormalizedAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                InputSettingKeys.TOUCH_MODE.getName(),
                "broken");
        repository.values.put(
                InputSettingKeys.SOFT_KEYBOARD_GESTURE_FINGERS
                        .getName(),
                2);
        repository.values.put(
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_X
                        .getName(),
                -20);
        repository.values.put(
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_X
                        .getName(),
                5_000);
        repository.values.put(
                InputSettingKeys.EXTERNAL_TOUCHPAD_SCROLL_AMOUNT
                        .getName(),
                500);
        repository.values.put(
                InputSettingKeys.MOUSE_WHEEL_SCROLL_AMOUNT
                        .getName(),
                -500);
        repository.values.put(
                InputSettingKeys.TOUCHPAD_LONG_PRESS_DURATION
                        .getName(),
                Integer.MAX_VALUE);

        InputSettings settings =
                InputSettingsLoader.load(repository);

        assertEquals(0, settings.getTouchModePreferenceValue());
        assertEquals(0, settings.getSoftKeyboardGestureFingers());
        assertEquals(
                InputSettingKeys.MIN_SENSITIVITY_PERCENT,
                settings.getTouchpadPointerSensitivityX());
        assertEquals(
                InputSettingKeys.MAX_SENSITIVITY_PERCENT,
                settings.getVirtualTouchpadSensitivityX());
        assertEquals(
                InputSettingKeys.MAX_SCROLL_AMOUNT,
                settings.getExternalTouchpadScrollAmount());
        assertEquals(
                InputSettingKeys.MIN_SCROLL_AMOUNT,
                settings.getMouseWheelScrollAmount());
        assertEquals(
                InputSettingKeys
                        .MAX_TOUCHPAD_LONG_PRESS_DURATION_MS,
                settings.getTouchpadLongPressDurationMs());
    }

    @Test
    public void cursorAndTransportPolicyComeFromTypedKeys() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                InputSettingKeys.LOCAL_SYSTEM_CURSOR.getName(),
                true);
        repository.values.put(
                InputSettingKeys
                        .DISABLE_ADAPTIVE_INPUT_THROTTLING
                        .getName(),
                false);

        InputSettings settings =
                InputSettingsLoader.load(repository);

        assertTrue(settings.isLocalSystemCursorEnabled());
        assertFalse(
                settings.isAdaptiveInputThrottlingDisabled());
    }

    @Test
    public void snapshotReplacementIsAtomicForReaders() {
        InputSettings original = InputSettings.builder().build();
        InputSettings replacement = original.toBuilder()
                .setAbsoluteMouseMode(true)
                .setDirectTouchSensitivityEnabled(true)
                .build();
        InputSettingsState state =
                new InputSettingsState(original);

        state.replace(replacement);

        assertTrue(state.get().isAbsoluteMouseMode());
        assertTrue(state.get().isDirectTouchSensitivityEnabled());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        final Map<String, Object> values = new HashMap<>();

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
            throw new UnsupportedOperationException();
        }
    }
}
