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

public final class StreamUiSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        StreamUiSettings settings =
                StreamUiSettingsLoader.load(new FakeRepository());

        assertTrue(settings.isFloatingControlEnabled());
        assertEquals(
                FloatingAction.GAME_MENU,
                settings.getFloatingAction());
        assertFalse(settings.shouldRememberFloatingPosition());
        assertFalse(settings.hasRememberedFloatingPosition());
        assertEquals(-1f, settings.getFloatingPositionX(), 0f);
        assertEquals(-1f, settings.getFloatingPositionY(), 0f);
        assertTrue(settings.isFloatingPositionNearestLeft());
        assertFalse(settings.isPerformanceOverlayEnabled());
        assertFalse(settings.isCompactPerformanceOverlay());
        assertTrue(
                settings.areCompactPerformanceDetailsEnabled());
        assertFalse(settings.isCompactPerformanceInteractive());
        assertFalse(settings.isRumbleOverlayEnabled());
        assertEquals(
                100,
                settings.getCompactPerformanceScalePercent());
        assertEquals(
                4,
                settings.getCompactPerformanceMarginTopDp());
    }

    @Test
    public void storedValuesBuildOneCoherentSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamUiSettingKeys.FLOATING_CONTROL_ENABLED,
                false);
        repository.put(
                StreamUiSettingKeys.FLOATING_ACTION,
                FloatingAction.FULL_KEYBOARD.getStorageValue());
        repository.put(
                StreamUiSettingKeys.REMEMBER_FLOATING_POSITION,
                true);
        repository.put(
                StreamUiSettingKeys.FLOATING_POSITION_X,
                321.5f);
        repository.put(
                StreamUiSettingKeys.FLOATING_POSITION_Y,
                123.25f);
        repository.put(
                StreamUiSettingKeys
                        .FLOATING_POSITION_NEAREST_LEFT,
                false);
        repository.put(
                StreamUiSettingKeys.PERFORMANCE_OVERLAY_ENABLED,
                true);
        repository.put(
                StreamUiSettingKeys.COMPACT_PERFORMANCE_OVERLAY,
                true);
        repository.put(
                StreamUiSettingKeys.COMPACT_PERFORMANCE_DETAILS,
                false);
        repository.put(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_INTERACTIVE,
                true);
        repository.put(
                StreamUiSettingKeys.RUMBLE_OVERLAY_ENABLED,
                true);
        repository.put(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_SCALE_PERCENT,
                175);
        repository.put(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_MARGIN_TOP_DP,
                42);

        StreamUiSettings settings =
                StreamUiSettingsLoader.load(repository);

        assertFalse(settings.isFloatingControlEnabled());
        assertEquals(
                FloatingAction.FULL_KEYBOARD,
                settings.getFloatingAction());
        assertTrue(settings.hasRememberedFloatingPosition());
        assertEquals(321.5f, settings.getFloatingPositionX(), 0f);
        assertEquals(123.25f, settings.getFloatingPositionY(), 0f);
        assertFalse(settings.isFloatingPositionNearestLeft());
        assertTrue(settings.isPerformanceOverlayEnabled());
        assertTrue(settings.isCompactPerformanceOverlay());
        assertFalse(
                settings.areCompactPerformanceDetailsEnabled());
        assertTrue(settings.isCompactPerformanceInteractive());
        assertTrue(settings.isRumbleOverlayEnabled());
        assertEquals(
                175,
                settings.getCompactPerformanceScalePercent());
        assertEquals(
                42,
                settings.getCompactPerformanceMarginTopDp());
    }

    @Test
    public void invalidValuesAreNormalizedAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamUiSettingKeys.FLOATING_ACTION.getName(),
                99);
        repository.values.put(
                StreamUiSettingKeys.FLOATING_POSITION_X.getName(),
                Float.NaN);
        repository.values.put(
                StreamUiSettingKeys.FLOATING_POSITION_Y.getName(),
                Float.POSITIVE_INFINITY);
        repository.values.put(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_SCALE_PERCENT
                        .getName(),
                Integer.MAX_VALUE);
        repository.values.put(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_MARGIN_TOP_DP
                        .getName(),
                -1);

        StreamUiSettings settings =
                StreamUiSettingsLoader.load(repository);

        assertEquals(
                FloatingAction.GAME_MENU,
                settings.getFloatingAction());
        assertEquals(-1f, settings.getFloatingPositionX(), 0f);
        assertEquals(-1f, settings.getFloatingPositionY(), 0f);
        assertEquals(
                230,
                settings.getCompactPerformanceScalePercent());
        assertEquals(
                0,
                settings.getCompactPerformanceMarginTopDp());
    }

    @Test
    public void statePublishesWholeReplacementSnapshot() {
        StreamUiSettingsState state =
                new StreamUiSettingsState(
                        StreamUiSettings.builder().build());
        StreamUiSettings replacement =
                StreamUiSettings.builder()
                        .setFloatingControlEnabled(false)
                        .setFloatingAction(
                                FloatingAction.SOFT_KEYBOARD)
                        .setPerformanceOverlayEnabled(true)
                        .setCompactPerformanceOverlay(true)
                        .build();

        state.replace(replacement);

        assertFalse(state.get().isFloatingControlEnabled());
        assertEquals(
                FloatingAction.SOFT_KEYBOARD,
                state.get().getFloatingAction());
        assertTrue(state.get().isPerformanceOverlayEnabled());
        assertTrue(state.get().isCompactPerformanceOverlay());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();

        private <T> void put(SettingKey<T> key, T value) {
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
