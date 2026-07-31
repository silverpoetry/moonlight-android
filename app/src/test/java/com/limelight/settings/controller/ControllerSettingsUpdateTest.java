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
    }

    @Test
    public void typedUpdateNormalizesStateAndCanonicalStorage() {
        FakeRepository repository = new FakeRepository();
        ControllerSettingsUpdate<Integer> update =
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
