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
        assertEquals(
                100,
                settings.getTouchpadPointerSensitivityX());
        assertEquals(
                5,
                settings.getExternalTouchpadScrollAmount());
        assertEquals(
                0.18f,
                settings.getBarometerForcePressThresholdHpa(),
                0f);
        assertTrue(settings.isDirectTouchRecenterEnabled());
    }

    @Test
    public void invalidValuesAreNormalizedAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put("mouse_model_list_axi", "broken");
        repository.values.put(
                "touch_number_quick_soft_keyboard",
                2);
        repository.values.put(
                "seekbar_mouse_touchpad_sensitivity_x_opacity",
                -20);
        repository.values.put(
                "touchpad_equipment_amount",
                500);

        InputSettings settings =
                InputSettingsLoader.load(repository);

        assertEquals(0, settings.getTouchModePreferenceValue());
        assertEquals(0, settings.getSoftKeyboardGestureFingers());
        assertEquals(
                InputSettingKeys.MIN_SENSITIVITY_PERCENT,
                settings.getTouchpadPointerSensitivityX());
        assertEquals(
                InputSettingKeys.MAX_SCROLL_AMOUNT,
                settings.getExternalTouchpadScrollAmount());
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
