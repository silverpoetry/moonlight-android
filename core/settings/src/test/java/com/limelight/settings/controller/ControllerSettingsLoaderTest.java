package com.limelight.settings.controller;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.controller.ControllerSettings
        .AnalogStickForScrolling;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        ControllerSettings settings =
                ControllerSettingsLoader.load(
                        new FakeRepository());

        assertEquals(7, settings.getStickDeadzonePercent());
        assertTrue(settings.isMultiControllerEnabled());
        assertTrue(settings.isUsbDriverEnabled());
        assertFalse(settings.shouldClaimAllUsbDevices());
        assertTrue(settings.areMotionSensorsEnabled());
        assertEquals(100, settings.getMouseSensitivityPercent());
        assertTrue(settings.isMouseEmulationEnabled());
        assertTrue(settings.doesMouseEmulationOpenGameMenu());
        assertEquals(
                AnalogStickForScrolling.RIGHT,
                settings.getAnalogStickForScrolling());
        assertTrue(settings.isBatteryReportingEnabled());
        assertFalse(settings.isTriggerRumbleLinkEnabled());
        assertEquals(0, settings.getAdaptiveTriggerMode());
        assertEquals(230, settings.getAdaptiveTriggerStrength());
        assertEquals(10, settings.getAdaptiveTriggerFrequency());
        assertEquals(40, settings.getAdaptiveTriggerStartPosition());
        assertEquals(100, settings.getAdaptiveTriggerEndPosition());
    }

    @Test
    public void storedValuesBuildOneCoherentSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                ControllerSettingKeys.STICK_DEADZONE_PERCENT,
                25);
        repository.put(
                ControllerSettingKeys.MULTI_CONTROLLER,
                false);
        repository.put(
                ControllerSettingKeys.CLAIM_ALL_USB_DEVICES,
                true);
        repository.put(
                ControllerSettingKeys.MOUSE_SENSITIVITY_PERCENT,
                175);
        repository.put(
                ControllerSettingKeys.FORCE_GYRO,
                true);
        repository.put(
                ControllerSettingKeys
                        .FORCE_GYRO_REQUIRES_LEFT_TRIGGER,
                true);
        repository.put(
                ControllerSettingKeys
                        .FORCE_GYRO_SENSITIVITY_PERCENT,
                150);
        repository.put(
                ControllerSettingKeys.ANALOG_STICK_FOR_SCROLLING,
                "left");
        repository.put(
                ControllerSettingKeys.TRIGGER_RUMBLE_LINK,
                true);
        repository.put(
                ControllerSettingKeys.ADAPTIVE_TRIGGER_MODE,
                6);
        repository.put(
                ControllerSettingKeys.ADAPTIVE_TRIGGER_STRENGTH,
                200);
        repository.put(
                ControllerSettingKeys.ADAPTIVE_TRIGGER_FREQUENCY,
                12);
        repository.put(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_START_POSITION,
                50);
        repository.put(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_END_POSITION,
                150);
        ControllerSettings settings =
                ControllerSettingsLoader.load(repository);

        assertEquals(25, settings.getStickDeadzonePercent());
        assertFalse(settings.isMultiControllerEnabled());
        assertTrue(settings.shouldClaimAllUsbDevices());
        assertEquals(175, settings.getMouseSensitivityPercent());
        assertTrue(settings.isForceGyroEnabled());
        assertTrue(settings.isForceGyroLeftTriggerRequired());
        assertEquals(
                150,
                settings.getForceGyroSensitivityPercent());
        assertEquals(
                AnalogStickForScrolling.LEFT,
                settings.getAnalogStickForScrolling());
        assertTrue(settings.isTriggerRumbleLinkEnabled());
        assertEquals(6, settings.getAdaptiveTriggerMode());
        assertEquals(200, settings.getAdaptiveTriggerStrength());
        assertEquals(12, settings.getAdaptiveTriggerFrequency());
        assertEquals(50, settings.getAdaptiveTriggerStartPosition());
        assertEquals(150, settings.getAdaptiveTriggerEndPosition());
    }

    @Test
    public void invalidStoredValuesAreNormalizedByTypedSchema() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                ControllerSettingKeys
                        .MOUSE_SENSITIVITY_PERCENT
                        .getName(),
                5_000);
        repository.values.put(
                ControllerSettingKeys
                        .MOUSE_EMULATION_BUTTON
                        .getName(),
                99);
        repository.values.put(
                ControllerSettingKeys
                        .ANALOG_STICK_FOR_SCROLLING
                        .getName(),
                "broken");
        repository.values.put(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_MODE
                        .getName(),
                5);
        repository.values.put(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_STRENGTH
                        .getName(),
                5_000);
        repository.values.put(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_FREQUENCY
                        .getName(),
                -100);

        ControllerSettings settings =
                ControllerSettingsLoader.load(repository);

        assertEquals(300, settings.getMouseSensitivityPercent());
        assertEquals(0, settings.getMouseEmulationButton());
        assertEquals(
                AnalogStickForScrolling.RIGHT,
                settings.getAnalogStickForScrolling());
        assertEquals(0, settings.getAdaptiveTriggerMode());
        assertEquals(255, settings.getAdaptiveTriggerStrength());
        assertEquals(5, settings.getAdaptiveTriggerFrequency());
    }

    @Test
    public void statePublishesWholeReplacementSnapshot() {
        ControllerSettings original =
                ControllerSettings.builder().build();
        ControllerSettings replacement =
                ControllerSettings.builder()
                        .setClaimAllUsbDevices(true)
                        .setForceGyro(true, true, false, 175)
                        .setMouseSensitivityPercent(200)
                        .build();
        ControllerSettingsState state =
                new ControllerSettingsState(original);

        state.replace(replacement);

        assertTrue(state.get().isForceGyroEnabled());
        assertTrue(state.get().shouldClaimAllUsbDevices());
        assertTrue(
                state.get().isForceGyroLeftTriggerRequired());
        assertEquals(
                175,
                state.get().getForceGyroSensitivityPercent());
        assertEquals(200, state.get().getMouseSensitivityPercent());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        final Map<String, Object> values = new HashMap<>();

        <T> void put(SettingKey<T> key, T value) {
            values.put(key.getName(), value);
        }

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
