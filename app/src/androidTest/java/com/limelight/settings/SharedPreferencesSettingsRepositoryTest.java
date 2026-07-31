package com.limelight.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.settings.android.SharedPreferencesSettingsRepository;
import com.limelight.settings.ui.GameMenuCardLayoutCodec;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SharedPreferencesSettingsRepositoryTest {
    private static final String PREFERENCES_NAME =
            "typed-settings-repository-test";

    private SharedPreferences preferences;
    private SettingsRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        repository =
                new SharedPreferencesSettingsRepository(preferences);
    }

    @After
    public void tearDown() {
        preferences.edit().clear().commit();
    }

    @Test
    public void missingAndWrongTypeValuesUseSchemaDefault() {
        SettingKey<Integer> count =
                SettingKey.integerKey("count", 4, 0, 10);

        assertFalse(repository.contains(count));
        assertEquals(Integer.valueOf(4), repository.get(count));
        preferences.edit().putString("count", "9").commit();
        assertTrue(repository.contains(count));
        assertEquals(Integer.valueOf(4), repository.get(count));
    }

    @Test
    public void editorNormalizesAndCommitsAtomically() {
        SettingKey<Integer> count =
                SettingKey.integerKey("count", 4, 0, 10);
        SettingKey<Boolean> enabled =
                SettingKey.booleanKey("enabled", false);

        assertTrue(repository.edit()
                .put(count, 99)
                .put(enabled, true)
                .commit());

        assertEquals(Integer.valueOf(10), repository.get(count));
        assertTrue(repository.get(enabled));
    }

    @Test
    public void editorCannotBeReusedAfterTerminalOperation() {
        SettingsRepository.Editor editor = repository.edit();
        editor.apply();

        boolean rejected = false;
        try {
            editor.remove(SettingKey.booleanKey("enabled", false));
        }
        catch (IllegalStateException expected) {
            rejected = true;
        }
        assertTrue(rejected);
        assertFalse(preferences.contains("enabled"));
    }

    @Test
    public void stringCollectionRoundTripsWithoutMutableAliasing() {
        SettingKey<Set<String>> ids =
                SettingKey.boundedStringCollectionKey(
                        "ids",
                        3,
                        8);
        Set<String> source = new LinkedHashSet<>(
                Arrays.asList("first", "second"));

        repository.edit().put(ids, source).commit();
        source.add("late");
        Set<String> loaded = repository.get(ids);

        assertEquals(
                new LinkedHashSet<>(
                        Arrays.asList("first", "second")),
                loaded);
        boolean immutable = false;
        try {
            loaded.add("new");
        }
        catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable);
    }

    @Test
    public void versionedMigrationRepairsRealSharedPreferences() {
        preferences.edit()
                .putBoolean("checkbox_51_surround", true)
                .putBoolean(
                        "checkbox_disable_frame_drop",
                        true)
                .putBoolean(
                        "checkbox_clipboard_image_sync",
                        true)
                .commit();

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                preferences.getInt(
                        "settings_schema_version",
                        -1));
        assertEquals(
                "51",
                preferences.getString(
                        "list_audio_config",
                        null));
        assertEquals(
                "balanced",
                preferences.getString(
                        "frame_pacing",
                        null));
        assertTrue(preferences.getBoolean(
                "checkbox_clipboard_sync",
                false));
        assertFalse(preferences.contains(
                "checkbox_51_surround"));
        assertFalse(preferences.contains(
                "checkbox_disable_frame_drop"));
        assertFalse(preferences.contains(
                "checkbox_clipboard_image_sync"));
    }

    @Test
    public void versionTwoRepairsHistoricalGamepadLayoutDefault() {
        preferences.edit()
                .putInt("settings_schema_version", 1)
                .putString(
                        "gamepad_axi_list",
                        "OSC_GAMEPAD_1")
                .commit();

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                preferences.getInt(
                        "settings_schema_version",
                        -1));
        assertEquals(
                "gamePad",
                preferences.getString(
                        "gamepad_axi_list",
                        null));
    }

    @Test
    public void versionThreeMigratesLegacyBitrateWithoutOverwritingCurrentValue() {
        preferences.edit()
                .putInt("settings_schema_version", 2)
                .putInt("seekbar_bitrate", 75)
                .putInt("seekbar_bitrate_kbps", 60_000)
                .commit();

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                preferences.getInt(
                        "settings_schema_version",
                        -1));
        assertEquals(
                60_000,
                preferences.getInt(
                        "seekbar_bitrate_kbps",
                        -1));
        assertFalse(preferences.contains("seekbar_bitrate"));
    }

    @Test
    public void versionFourMigratesLegacyGameMenuLayoutAtomically() {
        preferences.edit()
                .putInt("settings_schema_version", 3)
                .putString(
                        "game_menu_action_order_v1",
                        "disconnect,performance")
                .putStringSet(
                        "game_menu_action_hidden_v1",
                        new LinkedHashSet<>(
                                Arrays.asList("performance")))
                .commit();

        SettingsMigrationRunner.migrate(repository);

        assertEquals(
                SettingsSchema.CURRENT_VERSION,
                preferences.getInt(
                        "settings_schema_version",
                        -1));
        assertEquals(
                Arrays.asList(
                        "action:disconnect",
                        "action:performance"),
                GameMenuCardLayoutCodec.decodeOrder(
                                preferences.getString(
                                        "game_menu_card_order_v2",
                                        "")));
        assertEquals(
                new LinkedHashSet<>(
                        Arrays.asList("action:performance")),
                preferences.getStringSet(
                        "game_menu_card_hidden_v2",
                        null));
        assertFalse(preferences.contains(
                "game_menu_action_order_v1"));
        assertFalse(preferences.contains(
                "game_menu_action_hidden_v1"));
    }
}
