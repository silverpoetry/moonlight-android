package com.limelight.settings.virtualcontrols;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class VirtualControlSettingsLoaderTest {
    @Test
    public void missingValuesUseLegacyRuntimeDefaults() {
        VirtualControlSettings settings =
                VirtualControlSettingsLoader.load(
                        new FakeRepository());

        assertEquals(90, settings.getControlOpacityPercent());
        assertEquals(90, settings.getKeyboardOpacityPercent());
        assertEquals(200, settings.getKeyboardHeightDp());
        assertEquals(100, settings.getGamepadScalePercent());
        assertEquals(20, settings.getFreeStickOpacityPercent());
        assertEquals(0xFF888888, settings.getNormalColor());
        assertEquals("OSC_Keyboard", settings.getKeyboardLayoutId());
        assertEquals("gamePad", settings.getGamepadLayoutId());
        assertTrue(settings.isGuideButtonVisible());
        assertFalse(settings.isKeyboardHapticsEnabled());
        assertFalse(settings.shouldShowVirtualKeysOnStart());
        assertFalse(settings.isStickClickDisabled());
        assertFalse(
                settings.isAutomaticScreenOrientationEnabled());
    }

    @Test
    public void invalidStoredValuesAreNormalizedTogether() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                VirtualControlSettingKeys
                        .CONTROL_OPACITY_PERCENT.getName(),
                500);
        repository.values.put(
                VirtualControlSettingKeys.KEYBOARD_HEIGHT_DP.getName(),
                -10);
        repository.values.put(
                VirtualControlSettingKeys.GAMEPAD_SKIN.getName(),
                99);
        repository.values.put(
                VirtualControlSettingKeys
                        .GAMEPAD_SCALE_PERCENT.getName(),
                1);
        repository.values.put(
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID.getName(),
                "../outside");
        repository.values.put(
                VirtualControlSettingKeys
                        .SHOW_VIRTUAL_KEYS_ON_START.getName(),
                true);
        repository.values.put(
                VirtualControlSettingKeys
                        .AUTOMATIC_SCREEN_ORIENTATION.getName(),
                true);

        VirtualControlSettings settings =
                VirtualControlSettingsLoader.load(repository);

        assertEquals(100, settings.getControlOpacityPercent());
        assertEquals(100, settings.getKeyboardHeightDp());
        assertEquals(0, settings.getGamepadSkin());
        assertEquals(20, settings.getGamepadScalePercent());
        assertEquals("gamePad", settings.getGamepadLayoutId());
        assertTrue(settings.shouldShowVirtualKeysOnStart());
        assertTrue(
                settings.isAutomaticScreenOrientationEnabled());
    }

    @Test
    public void saveWritesOneAtomicDomainSnapshot() {
        FakeRepository repository = new FakeRepository();
        VirtualControlSettings settings =
                VirtualControlSettings.builder()
                        .setControlOpacityPercent(75)
                        .setKeyboardHapticsEnabled(true)
                        .setShowVirtualKeysOnStart(true)
                        .setKeyboardLayoutId("OSC_Keyboard_2")
                        .setGamepadLayoutId("gamePad_2")
                        .build();

        VirtualControlSettingsLoader.save(repository, settings);

        assertEquals(1, repository.appliedEdits);
        assertEquals(
                75,
                repository.get(
                        VirtualControlSettingKeys
                                .CONTROL_OPACITY_PERCENT)
                        .intValue());
        assertTrue(repository.get(
                VirtualControlSettingKeys.KEYBOARD_HAPTICS));
        assertTrue(repository.get(
                VirtualControlSettingKeys
                        .SHOW_VIRTUAL_KEYS_ON_START));
        assertEquals(
                "OSC_Keyboard_2",
                repository.get(
                        VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID));
        assertEquals(
                "gamePad_2",
                repository.get(
                        VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID));
    }

    @Test
    public void statePublishesWholeReplacementSnapshot() {
        VirtualControlSettings original =
                VirtualControlSettings.builder().build();
        VirtualControlSettings replacement =
                original.toBuilder()
                        .setControlOpacityPercent(66)
                        .setKeyboardHapticsEnabled(true)
                        .build();
        VirtualControlSettingsState state =
                new VirtualControlSettingsState(original);

        state.replace(replacement);

        assertSame(replacement, state.get());
        assertEquals(66, state.get().getControlOpacityPercent());
        assertTrue(state.get().isKeyboardHapticsEnabled());
    }

    @Test
    public void typedUpdateChangesAndPersistsOnlyItsOwnField() {
        VirtualControlSettings original =
                VirtualControlSettings.builder()
                        .setKeyboardOpacityPercent(60)
                        .build();
        VirtualControlSettingsUpdate<Integer> update =
                VirtualControlSettingsUpdate
                        .controlOpacityPercent(75);
        FakeRepository repository = new FakeRepository();

        VirtualControlSettings updated =
                update.applyTo(original);
        update.persist(repository);

        assertEquals(75, updated.getControlOpacityPercent());
        assertEquals(60, updated.getKeyboardOpacityPercent());
        assertEquals(1, repository.appliedEdits);
        assertEquals(1, repository.values.size());
        assertEquals(
                75,
                repository.get(
                        VirtualControlSettingKeys
                                .CONTROL_OPACITY_PERCENT)
                        .intValue());
    }

    @Test
    public void startupVisibilityUpdateDoesNotMutateRuntimePresentation() {
        VirtualControlSettings original =
                VirtualControlSettings.builder()
                        .setShowVirtualKeysOnStart(false)
                        .setKeyboardOpacityPercent(60)
                        .build();
        VirtualControlSettingsUpdate<Boolean> update =
                VirtualControlSettingsUpdate
                        .showVirtualKeysOnStart(true);
        FakeRepository repository = new FakeRepository();

        VirtualControlSettings updated =
                update.applyTo(original);
        update.persist(repository);

        assertTrue(updated.shouldShowVirtualKeysOnStart());
        assertEquals(60, updated.getKeyboardOpacityPercent());
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.appliedEdits);
        assertTrue(repository.get(
                VirtualControlSettingKeys
                        .SHOW_VIRTUAL_KEYS_ON_START));
    }

    private static final class FakeRepository
            implements SettingsRepository {
        final Map<String, Object> values = new HashMap<>();
        int appliedEdits;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> staged = new HashMap<>();

            @Override
            public <T> Editor put(SettingKey<T> key, T value) {
                staged.put(
                        key.getName(),
                        key.normalizeValue(value));
                return this;
            }

            @Override
            public Editor remove(SettingKey<?> key) {
                staged.put(key.getName(), null);
                return this;
            }

            @Override
            public void apply() {
                commitStaged();
            }

            @Override
            public boolean commit() {
                commitStaged();
                return true;
            }

            private void commitStaged() {
                for (Map.Entry<String, Object> entry :
                        staged.entrySet()) {
                    if (entry.getValue() == null) {
                        values.remove(entry.getKey());
                    }
                    else {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
                appliedEdits++;
            }
        }
    }
}
