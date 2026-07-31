package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.ui.StreamUiSettings.FloatingAction;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamUiSettingsUpdateTest {
    @Test
    public void scalarUpdateChangesOnlyItsOwnedField() {
        StreamUiSettings original = representativeSettings();
        StreamUiSettingsUpdate update =
                StreamUiSettingsUpdate
                        .compactPerformanceScalePercent(5_000);

        StreamUiSettings updated = update.applyTo(original);

        assertEquals(
                230,
                updated.getCompactPerformanceScalePercent());
        assertEquals(
                original.getCompactPerformanceMarginTopDp(),
                updated.getCompactPerformanceMarginTopDp());
        assertEquals(
                original.getFloatingAction(),
                updated.getFloatingAction());
        assertEquals(
                original.isRumbleOverlayEnabled(),
                updated.isRumbleOverlayEnabled());
    }

    @Test
    public void disablingRememberPositionClearsCoordinatesAtomically() {
        FakeRepository repository = new FakeRepository();
        StreamUiSettingsUpdate update =
                StreamUiSettingsUpdate
                        .rememberFloatingPosition(false);

        StreamUiSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertFalse(updated.shouldRememberFloatingPosition());
        assertEquals(-1f, updated.getFloatingPositionX(), 0f);
        assertEquals(-1f, updated.getFloatingPositionY(), 0f);
        assertEquals(
                false,
                repository.values.get(
                        StreamUiSettingKeys
                                .REMEMBER_FLOATING_POSITION
                                .getName()));
        assertEquals(
                -1f,
                repository.values.get(
                        StreamUiSettingKeys
                                .FLOATING_POSITION_X
                                .getName()));
        assertEquals(
                -1f,
                repository.values.get(
                        StreamUiSettingKeys
                                .FLOATING_POSITION_Y
                                .getName()));
        assertEquals(3, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void enablingRememberPositionPreservesCoordinates() {
        FakeRepository repository = new FakeRepository();
        StreamUiSettings original = representativeSettings()
                .toBuilder()
                .setRememberFloatingPosition(false)
                .build();
        StreamUiSettingsUpdate update =
                StreamUiSettingsUpdate
                        .rememberFloatingPosition(true);

        StreamUiSettings updated = update.applyTo(original);
        update.persist(repository);

        assertTrue(updated.shouldRememberFloatingPosition());
        assertEquals(
                original.getFloatingPositionX(),
                updated.getFloatingPositionX(),
                0f);
        assertEquals(
                original.getFloatingPositionY(),
                updated.getFloatingPositionY(),
                0f);
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void positionUpdateNormalizesAndPersistsOneTransaction() {
        FakeRepository repository = new FakeRepository();
        StreamUiSettingsUpdate update =
                StreamUiSettingsUpdate.floatingPosition(
                        Float.NaN,
                        Float.MAX_VALUE,
                        false);

        StreamUiSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertEquals(-1f, updated.getFloatingPositionX(), 0f);
        assertEquals(
                1_000_000f,
                updated.getFloatingPositionY(),
                0f);
        assertFalse(updated.isFloatingPositionNearestLeft());
        assertEquals(3, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void floatingActionUsesCanonicalStorageValue() {
        FakeRepository repository = new FakeRepository();
        StreamUiSettingsUpdate update =
                StreamUiSettingsUpdate.floatingAction(
                        FloatingAction.FULL_KEYBOARD);

        StreamUiSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertEquals(
                FloatingAction.FULL_KEYBOARD,
                updated.getFloatingAction());
        assertEquals(
                FloatingAction.FULL_KEYBOARD.getStorageValue(),
                repository.values.get(
                        StreamUiSettingKeys.FLOATING_ACTION
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    private static StreamUiSettings representativeSettings() {
        return StreamUiSettings.builder()
                .setFloatingControlEnabled(false)
                .setFloatingAction(FloatingAction.SOFT_KEYBOARD)
                .setRememberFloatingPosition(true)
                .setFloatingPosition(321f, 123f, true)
                .setPerformanceOverlayEnabled(true)
                .setCompactPerformanceOverlay(true)
                .setCompactPerformanceDetails(false)
                .setCompactPerformanceInteractive(true)
                .setRumbleOverlayEnabled(true)
                .setCompactPerformanceScalePercent(175)
                .setCompactPerformanceMarginTopDp(42)
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
