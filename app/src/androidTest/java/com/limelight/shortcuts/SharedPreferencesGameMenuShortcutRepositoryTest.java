package com.limelight.shortcuts;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.shortcuts.android.SharedPreferencesGameMenuShortcutRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SharedPreferencesGameMenuShortcutRepositoryTest {
    private static final String CANONICAL_PREFERENCES_NAME =
            "game-menu-shortcut-repository-test";
    private static final String LEGACY_CUSTOM_PREFERENCES_NAME =
            "game-menu-shortcut-legacy-custom-test";
    private static final String LEGACY_IMPORTED_PREFERENCES_NAME =
            "game-menu-shortcut-legacy-imported-test";
    private static final String DOCUMENT_KEY =
            "game_menu_shortcuts_v2";
    private static final String LEGACY_IMPORTED_KEY =
            "special_key";

    private Context context;
    private SharedPreferences canonicalPreferences;
    private SharedPreferences legacyCustomPreferences;
    private SharedPreferences importedPreferences;
    private SharedPreferencesGameMenuShortcutRepository repository;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        canonicalPreferences = context.getSharedPreferences(
                CANONICAL_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        legacyCustomPreferences = context.getSharedPreferences(
                LEGACY_CUSTOM_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        importedPreferences = context.getSharedPreferences(
                LEGACY_IMPORTED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        clear();
        repository =
                new SharedPreferencesGameMenuShortcutRepository(
                        canonicalPreferences,
                        legacyCustomPreferences,
                        importedPreferences);
    }

    @After
    public void tearDown() {
        clear();
    }

    @Test
    public void migratesBothLegacySourcesOnceInPriorOrder() {
        importedPreferences.edit()
                .putString(
                        LEGACY_IMPORTED_KEY,
                        "{\"data\":[{\"name\":\"Imported\"," +
                                "\"data\":[\"0x1B\"]}]}")
                .commit();
        legacyCustomPreferences.edit()
                .putString(
                        "quick_assemble_key_b",
                        legacyCustomJson(
                                "quick_assemble_key_b",
                                "B",
                                "2"))
                .putString(
                        "quick_assemble_key_a",
                        legacyCustomJson(
                                "quick_assemble_key_a",
                                "A",
                                "1"))
                .commit();

        List<GameMenuShortcut> migrated = repository.load();

        assertEquals(3, migrated.size());
        assertEquals("Imported", migrated.get(0).getName());
        assertEquals("A", migrated.get(1).getName());
        assertEquals("B", migrated.get(2).getName());
        assertTrue(canonicalPreferences.contains(
                DOCUMENT_KEY));
        assertEquals(1, canonicalPreferences.getAll().size());
        assertTrue(legacyCustomPreferences.getAll().isEmpty());
        assertFalse(importedPreferences.contains(
                LEGACY_IMPORTED_KEY));

        assertEquals(
                migrated.get(0).getId(),
                repository.load().get(0).getId());
    }

    @Test
    public void movesExistingCanonicalDocumentFromLegacyStoreOnce() {
        GameMenuShortcut shortcut =
                GameMenuShortcut.androidChord(
                        "shortcut:custom:migrated",
                        "Migrated",
                        "Legacy store",
                        new int[] {7, 8},
                        true);
        assertTrue(legacyCustomPreferences.edit()
                .putString(
                        DOCUMENT_KEY,
                        new GameMenuShortcutDocumentCodec()
                                .encode(java.util.Collections.singletonList(
                                        shortcut)))
                .commit());

        List<GameMenuShortcut> migrated = repository.load();

        assertEquals(1, migrated.size());
        assertEquals(shortcut.getId(), migrated.get(0).getId());
        assertTrue(canonicalPreferences.contains(
                DOCUMENT_KEY));
        assertTrue(legacyCustomPreferences.getAll().isEmpty());
    }

    @Test
    public void savesAndDeletesCanonicalShortcut() {
        GameMenuShortcut shortcut =
                GameMenuShortcut.androidChord(
                        "shortcut:custom:test",
                        "Test",
                        "One",
                        new int[] {1, 2},
                        true);

        assertTrue(repository.save(shortcut));
        List<GameMenuShortcut> loaded = repository.load();
        assertEquals(1, loaded.size());
        assertArrayEquals(
                new int[] {1, 2},
                loaded.get(0).getAndroidKeyCodes());

        assertTrue(repository.delete(shortcut.getId()));
        assertTrue(repository.load().isEmpty());
    }

    @Test
    public void canonicalDocumentNeverFallsBackToLegacyValues() {
        String invalidDocument = "not-json";
        canonicalPreferences.edit()
                .putString(
                        DOCUMENT_KEY,
                        invalidDocument)
                .putString(
                        "quick_assemble_key_old",
                        legacyCustomJson(
                                "quick_assemble_key_old",
                                "Old",
                                "1"))
                .commit();

        assertTrue(repository.load().isEmpty());
        assertFalse(repository.save(
                GameMenuShortcut.androidChord(
                        "shortcut:custom:new",
                        "New",
                        "",
                        new int[] {1},
                        true)));
        assertEquals(
                invalidDocument,
                canonicalPreferences.getString(
                        DOCUMENT_KEY,
                        null));
        assertFalse(canonicalPreferences.contains(
                "quick_assemble_key_old"));
    }

    @Test
    public void wrongPreferenceTypesAreIsolated() {
        importedPreferences.edit()
                .putInt(
                        LEGACY_IMPORTED_KEY,
                        7)
                .commit();
        legacyCustomPreferences.edit()
                .putString(
                        "quick_assemble_key_valid",
                        legacyCustomJson(
                                "quick_assemble_key_valid",
                                "Valid",
                                "1"))
                .putBoolean("wrong_type", true)
                .commit();

        List<GameMenuShortcut> loaded = repository.load();

        assertEquals(1, loaded.size());
        assertEquals("Valid", loaded.get(0).getName());
        assertEquals(1, canonicalPreferences.getAll().size());
        assertFalse(importedPreferences.contains(
                LEGACY_IMPORTED_KEY));
    }

    @Test
    public void combinedLegacySourcesRespectCanonicalLimit() {
        importedPreferences.edit()
                .putString(
                        LEGACY_IMPORTED_KEY,
                        "{\"data\":[{\"name\":\"Imported\"," +
                                "\"data\":[\"0x1B\"]}]}")
                .commit();
        SharedPreferences.Editor editor =
                legacyCustomPreferences.edit();
        for (int index = 0; index < 130; index++) {
            String id = String.format(
                    Locale.ROOT,
                    "quick_assemble_key_%03d",
                    index);
            editor.putString(
                    id,
                    legacyCustomJson(
                            id,
                            "Shortcut " + index,
                            "1"));
        }
        editor.commit();

        List<GameMenuShortcut> migrated = repository.load();

        assertEquals(
                GameMenuShortcutDocumentCodec
                        .MAXIMUM_SHORTCUT_COUNT,
                migrated.size());
        assertEquals("Imported", migrated.get(0).getName());
        assertEquals(1, canonicalPreferences.getAll().size());
    }

    @Test
    public void contextConstructorOwnsCanonicalAndMigrationStores() {
        SharedPreferences productionCanonical =
                context.getSharedPreferences(
                        "game_menu_shortcuts",
                        Context.MODE_PRIVATE);
        SharedPreferences productionLegacyCustom =
                context.getSharedPreferences(
                        "quick_axi_keyAssemble",
                        Context.MODE_PRIVATE);
        SharedPreferences productionLegacyImported =
                context.getSharedPreferences(
                        "specialPrefs",
                        Context.MODE_PRIVATE);
        clear(
                productionCanonical,
                productionLegacyCustom,
                productionLegacyImported);
        try {
            productionLegacyImported.edit()
                    .putString(
                            LEGACY_IMPORTED_KEY,
                            "{\"data\":[{\"name\":\"Imported\"," +
                                    "\"data\":[\"0x1B\"]}]}")
                    .commit();
            productionLegacyCustom.edit()
                    .putString(
                            "quick_assemble_key_context",
                            legacyCustomJson(
                                    "quick_assemble_key_context",
                                    "Custom",
                                    "1"))
                    .commit();

            SharedPreferencesGameMenuShortcutRepository
                    contextRepository =
                    new SharedPreferencesGameMenuShortcutRepository(
                            context);
            List<GameMenuShortcut> loaded =
                    contextRepository.load();

            assertEquals(2, loaded.size());
            assertEquals("Imported", loaded.get(0).getName());
            assertEquals("Custom", loaded.get(1).getName());
            assertTrue(productionCanonical.contains(DOCUMENT_KEY));
            assertTrue(productionLegacyCustom.getAll().isEmpty());
            assertTrue(productionLegacyImported.getAll().isEmpty());
        }
        finally {
            clear(
                    productionCanonical,
                    productionLegacyCustom,
                    productionLegacyImported);
        }
    }

    private void clear() {
        clear(
                canonicalPreferences,
                legacyCustomPreferences,
                importedPreferences);
    }

    private static void clear(
            SharedPreferences... stores) {
        for (SharedPreferences store : stores) {
            if (store != null) {
                store.edit().clear().commit();
            }
        }
    }

    private static String legacyCustomJson(
            String id, String name, String codes) {
        return "{\"id\":\"" + id +
                "\",\"name\":\"" + name +
                "\",\"codes\":\"" + codes + "\"}";
    }
}
