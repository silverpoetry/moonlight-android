package com.limelight.settings.controller;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerSettingsUpdateTest {
    @Test
    public void copyBuilderPreservesWholeSnapshot() {
        ControllerSettings original =
                ControllerSettings.builder()
                        .setStickDeadzonePercent(21)
                        .setMultiControllerEnabled(false)
                        .setUsbDriverEnabled(false)
                        .setClaimAllUsbDevices(true)
                        .setMotionSensorsEnabled(false)
                        .setTouchpadAsMouse(true)
                        .setMouseSensitivityPercent(150)
                        .setForceGyro(true, true, false, 175)
                        .setBatteryReportingEnabled(false)
                        .setTriggerRumbleLinkEnabled(true)
                        .setAdaptiveTriggerMode(6)
                        .setAdaptiveTriggerStrength(200)
                        .setAdaptiveTriggerFrequency(12)
                        .setAdaptiveTriggerStartPosition(50)
                        .setAdaptiveTriggerEndPosition(150)
                        .build();

        ControllerSettings copy = original.toBuilder().build();

        assertEquals(21, copy.getStickDeadzonePercent());
        assertFalse(copy.isMultiControllerEnabled());
        assertFalse(copy.isUsbDriverEnabled());
        assertTrue(copy.shouldClaimAllUsbDevices());
        assertFalse(copy.areMotionSensorsEnabled());
        assertTrue(copy.isTouchpadAsMouse());
        assertEquals(150, copy.getMouseSensitivityPercent());
        assertTrue(copy.isForceGyroEnabled());
        assertTrue(copy.isForceGyroLeftTriggerRequired());
        assertFalse(copy.areForceGyroAxesSwapped());
        assertEquals(175, copy.getForceGyroSensitivityPercent());
        assertFalse(copy.isBatteryReportingEnabled());
        assertTrue(copy.isTriggerRumbleLinkEnabled());
        assertEquals(6, copy.getAdaptiveTriggerMode());
        assertEquals(200, copy.getAdaptiveTriggerStrength());
        assertEquals(12, copy.getAdaptiveTriggerFrequency());
        assertEquals(50, copy.getAdaptiveTriggerStartPosition());
        assertEquals(150, copy.getAdaptiveTriggerEndPosition());
    }

    @Test
    public void typedUpdateNormalizesStateAndCanonicalStorage() {
        FakeRepository repository = new FakeRepository();
        ControllerSettingsUpdate update =
                ControllerSettingsUpdate
                        .mouseSensitivityPercent(5_000);

        ControllerSettings updated = update.applyTo(
                ControllerSettings.builder()
                        .setStickDeadzonePercent(21)
                        .build());
        update.persist(repository);

        assertEquals(300, updated.getMouseSensitivityPercent());
        assertEquals(21, updated.getStickDeadzonePercent());
        assertEquals(
                300,
                repository.values.get(
                        ControllerSettingKeys
                                .MOUSE_SENSITIVITY_PERCENT
                                .getName()));
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void claimAllUsbIsOneCompoundPolicyTransaction() {
        FakeRepository repository = new FakeRepository();
        ControllerSettings original =
                ControllerSettings.builder()
                        .setUsbDriverEnabled(false)
                        .setClaimAllUsbDevices(false)
                        .build();
        ControllerSettingsUpdate enable =
                ControllerSettingsUpdate
                        .claimAllUsbDevices(true);

        ControllerSettings enabled = enable.applyTo(original);
        enable.persist(repository);

        assertTrue(enabled.isUsbDriverEnabled());
        assertTrue(enabled.shouldClaimAllUsbDevices());
        assertEquals(
                true,
                repository.values.get(
                        ControllerSettingKeys.USB_DRIVER.getName()));
        assertEquals(
                true,
                repository.values.get(
                        ControllerSettingKeys
                                .CLAIM_ALL_USB_DEVICES
                                .getName()));
        assertEquals(1, repository.applyCount);

        repository.values.clear();
        ControllerSettingsUpdate disable =
                ControllerSettingsUpdate
                        .claimAllUsbDevices(false);
        ControllerSettings disabled = disable.applyTo(enabled);
        disable.persist(repository);

        assertTrue(disabled.isUsbDriverEnabled());
        assertFalse(disabled.shouldClaimAllUsbDevices());
        assertFalse(repository.values.containsKey(
                ControllerSettingKeys.USB_DRIVER.getName()));
        assertEquals(
                false,
                repository.values.get(
                        ControllerSettingKeys
                                .CLAIM_ALL_USB_DEVICES
                                .getName()));
        assertEquals(2, repository.applyCount);
    }

    @Test
    public void adaptiveTriggerUpdatesNormalizeIndependently() {
        FakeRepository repository = new FakeRepository();
        ControllerSettings original =
                ControllerSettings.builder()
                        .setAdaptiveTriggerMode(6)
                        .setAdaptiveTriggerStrength(200)
                        .setAdaptiveTriggerFrequency(12)
                        .setAdaptiveTriggerStartPosition(50)
                        .setAdaptiveTriggerEndPosition(150)
                        .build();
        ControllerSettingsUpdate update =
                ControllerSettingsUpdate
                        .adaptiveTriggerStrength(5_000);

        ControllerSettings updated = update.applyTo(original);
        update.persist(repository);

        assertEquals(6, updated.getAdaptiveTriggerMode());
        assertEquals(255, updated.getAdaptiveTriggerStrength());
        assertEquals(12, updated.getAdaptiveTriggerFrequency());
        assertEquals(50, updated.getAdaptiveTriggerStartPosition());
        assertEquals(150, updated.getAdaptiveTriggerEndPosition());
        assertEquals(
                255,
                repository.values.get(
                        ControllerSettingKeys
                                .ADAPTIVE_TRIGGER_STRENGTH
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void mouseEmulationIsOneCompoundPolicyTransaction() {
        FakeRepository repository = new FakeRepository();
        ControllerSettings original =
                ControllerSettings.builder()
                        .setMouseEmulationEnabled(false)
                        .setMouseEmulationButton(0)
                        .setForceGyro(true, true, false, 175)
                        .build();
        ControllerSettingsUpdate update =
                ControllerSettingsUpdate.mouseEmulation(
                        true,
                        99);

        ControllerSettings updated = update.applyTo(original);
        update.persist(repository);

        assertTrue(updated.isMouseEmulationEnabled());
        assertEquals(0, updated.getMouseEmulationButton());
        assertTrue(updated.isForceGyroEnabled());
        assertTrue(updated.isForceGyroLeftTriggerRequired());
        assertEquals(
                true,
                repository.values.get(
                        ControllerSettingKeys
                                .MOUSE_EMULATION
                                .getName()));
        assertEquals(
                0,
                repository.values.get(
                        ControllerSettingKeys
                                .MOUSE_EMULATION_BUTTON
                                .getName()));
        assertEquals(2, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void forceGyroScalarUpdatePreservesOtherPolicy() {
        FakeRepository repository = new FakeRepository();
        ControllerSettings original =
                ControllerSettings.builder()
                        .setForceGyro(true, true, false, 175)
                        .setAdaptiveTriggerMode(6)
                        .build();
        ControllerSettingsUpdate update =
                ControllerSettingsUpdate
                        .forceGyroSensitivityPercent(5_000);

        ControllerSettings updated = update.applyTo(original);
        update.persist(repository);

        assertEquals(
                200,
                updated.getForceGyroSensitivityPercent());
        assertTrue(updated.isForceGyroEnabled());
        assertTrue(updated.isForceGyroLeftTriggerRequired());
        assertFalse(updated.areForceGyroAxesSwapped());
        assertEquals(6, updated.getAdaptiveTriggerMode());
        assertEquals(
                200,
                repository.values.get(
                        ControllerSettingKeys
                                .FORCE_GYRO_SENSITIVITY_PERCENT
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
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
