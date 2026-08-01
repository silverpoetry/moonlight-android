package com.limelight.settings;

import com.limelight.settings.audio.StreamAudioSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamDecoderSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.ui.GameMenuCardSettingKeys;
import com.limelight.settings.ui.GameMenuCardLayoutCodec;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsMigrationRunnerTest {
    @Test
    public void allPendingMigrationsCommitAsOneTransaction() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                "checkbox_51_surround",
                true);
        repository.values.put(
                "checkbox_disable_frame_drop",
                true);
        repository.values.put(
                "checkbox_clipboard_sync",
                false);
        repository.values.put(
                "checkbox_clipboard_image_sync",
                true);

        SettingsMigrationRunner.migrate(repository);

        assertEquals("51", repository.values.get(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION
                        .getName()));
        assertEquals("balanced", repository.values.get(
                StreamDecoderSettingKeys.FRAME_PACING.getName()));
        assertEquals(true, repository.values.get(
                TransferSettingKeys.CLIPBOARD_SYNC.getName()));
        assertFalse(repository.values.containsKey(
                "checkbox_51_surround"));
        assertFalse(repository.values.containsKey(
                "checkbox_disable_frame_drop"));
        assertFalse(repository.values.containsKey(
                "checkbox_clipboard_image_sync"));
        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                repository.values.get("settings_schema_version"));
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void disabledLegacyAudioDoesNotOverrideCurrentChoice() {
        FakeRepository repository = new FakeRepository();
        repository.values.put("list_audio_config", "71");
        repository.values.put(
                "checkbox_51_surround",
                false);

        SettingsMigrationRunner.migrate(repository);

        assertEquals("71", repository.values.get(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION
                        .getName()));
        assertFalse(repository.values.containsKey(
                "list_audio_config"));
        assertFalse(repository.values.containsKey(
                "checkbox_51_surround"));
    }

    @Test
    public void semanticLegacyKeysCannotOverrideCanonicalChoices() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                SettingsSchema.VERSION.getName(),
                SettingsSchema.CURRENT_VERSION);
        repository.values.put(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION
                        .getName(),
                "71");
        repository.values.put(
                StreamDecoderSettingKeys.FRAME_PACING.getName(),
                StreamDecoderSettingKeys
                        .FRAME_PACING_MINIMUM_LATENCY);
        repository.values.put(
                TransferSettingKeys.CLIPBOARD_SYNC.getName(),
                false);
        repository.values.put("checkbox_51_surround", true);
        repository.values.put("checkbox_disable_frame_drop", true);
        repository.values.put("checkbox_clipboard_image_sync", true);

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                "71",
                repository.values.get(
                        StreamAudioSettingKeys
                                .CHANNEL_CONFIGURATION.getName()));
        assertEquals(
                StreamDecoderSettingKeys
                        .FRAME_PACING_MINIMUM_LATENCY,
                repository.values.get(
                        StreamDecoderSettingKeys.FRAME_PACING
                                .getName()));
        assertEquals(
                false,
                repository.values.get(
                        TransferSettingKeys.CLIPBOARD_SYNC
                                .getName()));
        assertFalse(repository.values.containsKey(
                "checkbox_51_surround"));
        assertFalse(repository.values.containsKey(
                "checkbox_disable_frame_drop"));
        assertFalse(repository.values.containsKey(
                "checkbox_clipboard_image_sync"));
    }

    @Test
    public void versionTwoRepairsInvalidLegacyGamepadLayout() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                "settings_schema_version",
                1);
        repository.values.put(
                "gamepad_axi_list",
                "OSC_GAMEPAD_1");

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID
                        .getDefaultValue(),
                repository.values.get(
                        VirtualControlSettingKeys
                                .GAMEPAD_LAYOUT_ID.getName()));
        assertFalse(repository.values.containsKey(
                "gamepad_axi_list"));
        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                repository.values.get("settings_schema_version"));
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void migrationIsIdempotent() {
        FakeRepository repository = new FakeRepository();

        SettingsMigrationRunner.migrate(repository);
        SettingsMigrationRunner.migrate(repository);

        assertEquals(1, repository.commitCount);
    }

    @Test
    public void lateLegacyValueIsHandledAfterSchemaVersion() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                "settings_schema_version",
                SettingsSchema.CURRENT_VERSION);
        repository.values.put(
                "checkbox_clipboard_image_sync",
                true);

        SettingsMigrationRunner.migrate(repository);

        assertTrue((Boolean) repository.values.get(
                TransferSettingKeys.CLIPBOARD_SYNC.getName()));
        assertFalse(repository.values.containsKey(
                "checkbox_clipboard_image_sync"));
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void futureSchemaVersionIsNeverDowngraded() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                "settings_schema_version",
                SettingsSchema.CURRENT_VERSION + 1);
        repository.values.put(
                "checkbox_clipboard_image_sync",
                true);

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                SettingsSchema.CURRENT_VERSION + 1,
                repository.values.get("settings_schema_version"));
        assertTrue((Boolean) repository.values.get(
                TransferSettingKeys.CLIPBOARD_SYNC.getName()));
    }

    @Test
    public void legacyBitrateMigratesWithoutOverwritingCurrentValue() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                "seekbar_bitrate",
                50);

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                50_000,
                repository.values.get(
                        StreamVideoSettingKeys.BITRATE_KBPS
                                .getName()));
        assertFalse(repository.values.containsKey(
                "seekbar_bitrate"));

        repository.values.put(
                "seekbar_bitrate",
                75);
        repository.values.put(
                "seekbar_bitrate_kbps",
                60_000);
        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                50_000,
                repository.values.get(
                        StreamVideoSettingKeys.BITRATE_KBPS
                                .getName()));
        assertFalse(repository.values.containsKey(
                "seekbar_bitrate_kbps"));
        assertFalse(repository.values.containsKey(
                StreamVideoSettingKeys
                        .LEGACY_BITRATE_MBPS
                        .getName()));
    }

    @Test
    public void versionFourMigratesLegacyGameMenuReferences() {
        FakeRepository repository = new FakeRepository();
        repository.values.put("settings_schema_version", 3);
        repository.values.put(
                "game_menu_action_order_v1",
                "disconnect,performance");
        repository.values.put(
                "game_menu_action_hidden_v1",
                new LinkedHashSet<>(
                        Arrays.asList("performance")));

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                Arrays.asList(
                        "action:disconnect",
                        "action:performance"),
                GameMenuCardLayoutCodec.decodeOrder(
                        (String) repository.values.get(
                                GameMenuCardSettingKeys
                                        .ORDER_DOCUMENT.getName())));
        assertEquals(
                new LinkedHashSet<>(
                        Arrays.asList("action:performance")),
                repository.values.get(
                        GameMenuCardSettingKeys
                                .HIDDEN_CARD_IDS.getName()));
        assertFalse(repository.values.containsKey(
                "game_menu_action_order_v1"));
        assertFalse(repository.values.containsKey(
                "game_menu_action_hidden_v1"));
        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                repository.values.get(
                        "settings_schema_version"));
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void legacyGameMenuValuesCannotOverwriteCurrentLayout() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                "settings_schema_version",
                SettingsSchema.CURRENT_VERSION);
        repository.values.put(
                "game_menu_card_order_v2",
                "[\"shortcut:custom:current\"]");
        repository.values.put(
                "game_menu_card_hidden_v2",
                new LinkedHashSet<String>());
        repository.values.put(
                "game_menu_action_order_v1",
                "disconnect");

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                "[\"shortcut:custom:current\"]",
                repository.values.get(
                        GameMenuCardSettingKeys
                                .ORDER_DOCUMENT.getName()));
        assertFalse(repository.values.containsKey(
                "game_menu_action_order_v1"));
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void everyDeclaredRenameMigratesInOneIdempotentTransaction() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                SettingsSchema.VERSION.getName(),
                SettingsSchema.CURRENT_VERSION - 1);

        for (SettingKey<?> key : SettingsKeyCatalog.all()) {
            if (!key.getLegacyNames().isEmpty()) {
                repository.values.put(
                        key.getLegacyNames().get(0),
                        key.getDefaultValue());
            }
        }

        SettingsMigrationRunner.migrate(repository);

        for (SettingKey<?> key : SettingsKeyCatalog.all()) {
            if (key.getLegacyNames().isEmpty()) {
                continue;
            }
            assertTrue(
                    "Missing canonical value: " + key.getName(),
                    repository.values.containsKey(key.getName()));
            for (String legacyName : key.getLegacyNames()) {
                assertFalse(
                        "Legacy value survived: " + legacyName,
                        repository.values.containsKey(legacyName));
            }
        }
        assertEquals(1, repository.commitCount);

        SettingsMigrationRunner.migrate(repository);
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void versionFivePreservesEnabledLegacyForcePress() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(SettingsSchema.VERSION.getName(), 4);
        repository.values.put(
                InputSettingKeys.BAROMETER_FORCE_PRESS.getName(),
                false);
        repository.values.put(
                "checkbox_barometer_force_press",
                true);

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                true,
                repository.values.get(
                        InputSettingKeys.BAROMETER_FORCE_PRESS
                                .getName()));
        assertFalse(repository.values.containsKey(
                "checkbox_barometer_force_press"));
    }

    private static final class FakeRepository
            implements SettingsRepository {
        final Map<String, Object> values = new HashMap<>();
        int commitCount;

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
            return new FakeEditor();
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> updates =
                    new HashMap<>();
            private final Map<String, Boolean> removals =
                    new HashMap<>();
            private boolean closed;

            @Override
            public <T> Editor put(SettingKey<T> key, T value) {
                ensureOpen();
                updates.put(
                        key.getName(),
                        key.normalizeValue(value));
                removals.remove(key.getName());
                return this;
            }

            @Override
            public Editor remove(SettingKey<?> key) {
                ensureOpen();
                removals.put(key.getName(), true);
                updates.remove(key.getName());
                return this;
            }

            @Override
            public void apply() {
                ensureOpen();
                closeAndApply();
            }

            @Override
            public boolean commit() {
                ensureOpen();
                commitCount++;
                closeAndApply();
                return true;
            }

            private void closeAndApply() {
                closed = true;
                for (String key : removals.keySet()) {
                    values.remove(key);
                }
                values.putAll(updates);
            }

            private void ensureOpen() {
                if (closed) {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
