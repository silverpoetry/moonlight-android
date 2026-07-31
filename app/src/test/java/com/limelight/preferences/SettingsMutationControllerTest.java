package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class SettingsMutationControllerTest {
    @Test
    public void resolutionAndNativeFrameRateHaveExplicitSemantics() {
        FakeRepository repository = new FakeRepository();
        SettingsMutationController controller =
                new SettingsMutationController(
                        new SettingsStore(repository));
        SettingsItem resolution = item(
                StreamResolutionSettingKeys.RESOLUTION);
        SettingsItem frameRate = item(
                StreamResolutionSettingKeys.FPS);

        controller.changeList(
                resolution,
                StreamResolutionCodec.RESOLUTION_1080P,
                "75");
        assertEquals(
                StreamResolutionCodec.SELECTION_PRESET,
                repository.get(
                        StreamResolutionSettingKeys.SELECTION));
        assertEquals(
                StreamResolutionCodec.RESOLUTION_1080P,
                repository.get(
                        StreamResolutionSettingKeys.RESOLUTION));

        controller.changeList(
                resolution,
                "2000x1000",
                "75");
        assertEquals(
                StreamResolutionCodec.SELECTION_CUSTOM_OR_NATIVE,
                repository.get(
                        StreamResolutionSettingKeys.SELECTION));
        assertEquals(
                "2000x1000",
                repository.get(
                        StreamResolutionSettingKeys.RESOLUTION));

        assertTrue(controller.changeList(
                        frameRate,
                        "75",
                        "75")
                .shouldShowNativeFrameRateWarning());
        assertFalse(controller.changeList(
                        frameRate,
                        "60",
                        "75")
                .shouldShowNativeFrameRateWarning());
    }

    @Test
    public void customBitrateAcceptsExactKbpsAndRejectsInvalidInput() {
        FakeRepository repository = new FakeRepository();
        SettingsMutationController controller =
                new SettingsMutationController(
                        new SettingsStore(repository));
        SettingsItem editor = new SettingsItem();
        editor.key = SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS;
        editor.type = SettingsItem.Type.TEXT;

        assertTrue(controller.changeText(editor, "12.5")
                .isAccepted());
        assertEquals(Integer.valueOf(12500), repository.get(
                StreamVideoSettingKeys.BITRATE_KBPS));

        for (String invalid : new String[] {
                "",
                "-1",
                "9999.1",
                "0.0001",
                "not-a-number",
        }) {
            SettingsMutationController.ChangeResult result =
                    controller.changeText(editor, invalid);
            assertFalse(invalid, result.isAccepted());
            assertEquals(
                    invalid,
                    SettingsMutationController.ValidationError
                            .INVALID_BITRATE,
                    result.getValidationError());
        }
        assertEquals(Integer.valueOf(12500), repository.get(
                StreamVideoSettingKeys.BITRATE_KBPS));
    }

    @Test
    public void regularTextUsesItsTypedKey() {
        FakeRepository repository = new FakeRepository();
        SettingsMutationController controller =
                new SettingsMutationController(
                        new SettingsStore(repository));
        SettingKey<String> key = SettingKey.stringKey("text", "");
        SettingsItem item = item(key);

        assertTrue(controller.changeText(item, "value")
                .isAccepted());
        assertEquals("value", repository.get(key));
    }

    @Test
    public void booleanAndIntegerChangesCommitThroughOneBoundary() {
        FakeRepository repository = new FakeRepository();
        SettingsMutationController controller =
                new SettingsMutationController(
                        new SettingsStore(repository));
        SettingKey<Boolean> booleanKey =
                SettingKey.booleanKey("boolean", false);
        SettingKey<Integer> integerKey =
                SettingKey.integerKey("integer", 0, 0, 100);

        assertTrue(controller.changeBoolean(
                        item(booleanKey),
                        true,
                        false)
                .isAccepted());
        assertTrue(controller.changeInteger(
                        item(integerKey),
                        42)
                .isAccepted());

        assertTrue(repository.get(booleanKey));
        assertEquals(Integer.valueOf(42), repository.get(integerKey));
    }

    @Test
    public void changeEffectsMatchVisibilityAndFrameRatePolicy() {
        SettingsMutationController controller =
                new SettingsMutationController(
                        new SettingsStore(new FakeRepository()));

        assertEffect(
                controller.changeBoolean(
                        item(InputSettingKeys.BAROMETER_FORCE_PRESS),
                        true,
                        true),
                SettingsMutationController.ChangeEffect.Type.RELOAD,
                180);
        assertEffect(
                controller.changeBoolean(
                        item(StreamVideoSettingKeys.UNLOCK_FPS),
                        true,
                        true),
                SettingsMutationController.ChangeEffect.Type.RELOAD,
                500);
        assertEffect(
                controller.changeBoolean(
                        item(SettingKey.booleanKey("other", false)),
                        true,
                        true),
                SettingsMutationController.ChangeEffect.Type.REFRESH,
                180);
        assertEffect(
                controller.changeBoolean(
                        item(SettingKey.booleanKey("other", false)),
                        false,
                        false),
                SettingsMutationController.ChangeEffect.Type.REFRESH,
                0);
    }

    private static void assertEffect(
            SettingsMutationController.ChangeResult result,
            SettingsMutationController.ChangeEffect.Type type,
            long delayMs) {
        assertTrue(result.isAccepted());
        SettingsMutationController.ChangeEffect effect =
                result.getEffect();
        assertEquals(type, effect.getType());
        assertEquals(delayMs, effect.getDelayMs());
    }

    private static SettingsItem item(SettingKey<?> key) {
        SettingsItem item = new SettingsItem();
        item.key = key.getName();
        item.settingKey = key;
        item.type = key.getStorageType() == SettingKey.StorageType.BOOLEAN
                ? SettingsItem.Type.SWITCH
                : SettingsItem.Type.LIST;
        return item;
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return contains(key)
                    ? key.normalizeStoredValue(values.get(key.getName()))
                    : key.getDefaultValue();
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> changes = new HashMap<>();

            @Override
            public <T> Editor put(SettingKey<T> key, T value) {
                changes.put(key.getName(), key.normalizeValue(value));
                return this;
            }

            @Override
            public Editor remove(SettingKey<?> key) {
                changes.put(key.getName(), null);
                return this;
            }

            @Override
            public void apply() {
                commit();
            }

            @Override
            public boolean commit() {
                for (Map.Entry<String, Object> change :
                        changes.entrySet()) {
                    if (change.getValue() == null) {
                        values.remove(change.getKey());
                    }
                    else {
                        values.put(change.getKey(), change.getValue());
                    }
                }
                return true;
            }
        }
    }
}
