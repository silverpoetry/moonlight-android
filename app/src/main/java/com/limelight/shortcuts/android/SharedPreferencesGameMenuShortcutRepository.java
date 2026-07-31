package com.limelight.shortcuts.android;

import android.content.SharedPreferences;

import com.limelight.LimeLog;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.shortcuts.GameMenuShortcutDecodeResult;
import com.limelight.shortcuts.GameMenuShortcutDocumentCodec;
import com.limelight.shortcuts.GameMenuShortcutIds;
import com.limelight.shortcuts.GameMenuShortcutRepository;
import com.limelight.shortcuts.LegacyGameMenuShortcutCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Android storage adapter for stream-menu shortcuts.
 *
 * <p>The first read migrates both historical named preference formats into
 * one bounded canonical document. Once that write commits, runtime reads use
 * only the canonical document and legacy values are cleanup-only.</p>
 */
public final class SharedPreferencesGameMenuShortcutRepository
        implements GameMenuShortcutRepository {
    public static final String PREFERENCES_NAME =
            "quick_axi_keyAssemble";
    public static final String DOCUMENT_KEY =
            "game_menu_shortcuts_v2";
    public static final String LEGACY_IMPORTED_PREFERENCES_NAME =
            "specialPrefs";
    public static final String LEGACY_IMPORTED_KEY =
            "special_key";

    private final SharedPreferences preferences;
    private final SharedPreferences legacyImportedPreferences;
    private final GameMenuShortcutDocumentCodec documentCodec;
    private final LegacyGameMenuShortcutCodec legacyCodec;

    public SharedPreferencesGameMenuShortcutRepository(
            SharedPreferences preferences,
            SharedPreferences legacyImportedPreferences) {
        this(
                preferences,
                legacyImportedPreferences,
                new GameMenuShortcutDocumentCodec(),
                new LegacyGameMenuShortcutCodec());
    }

    SharedPreferencesGameMenuShortcutRepository(
            SharedPreferences preferences,
            SharedPreferences legacyImportedPreferences,
            GameMenuShortcutDocumentCodec documentCodec,
            LegacyGameMenuShortcutCodec legacyCodec) {
        this.preferences = Objects.requireNonNull(
                preferences, "preferences");
        this.legacyImportedPreferences = Objects.requireNonNull(
                legacyImportedPreferences,
                "legacyImportedPreferences");
        this.documentCodec = Objects.requireNonNull(
                documentCodec, "documentCodec");
        this.legacyCodec = Objects.requireNonNull(
                legacyCodec, "legacyCodec");
    }

    @Override
    public synchronized List<GameMenuShortcut> load() {
        if (!migrateIfRequired()) {
            return Collections.emptyList();
        }
        GameMenuShortcutDecodeResult decoded =
                loadCanonicalDocument();
        return decoded == null
                ? Collections.emptyList()
                : decoded.getShortcuts();
    }

    private GameMenuShortcutDecodeResult
            loadCanonicalDocument() {
        Object storedDocument =
                preferences.getAll().get(DOCUMENT_KEY);
        if (storedDocument == null) {
            return null;
        }
        if (!(storedDocument instanceof String)) {
            LimeLog.warning(
                    "Ignoring shortcut document with invalid storage type");
            return null;
        }
        try {
            GameMenuShortcutDecodeResult decoded =
                    documentCodec.decode(
                            (String) storedDocument);
            logRejected("canonical", decoded);
            return decoded;
        }
        catch (IllegalArgumentException error) {
            LimeLog.warning(
                    "Ignoring invalid shortcut document");
            return null;
        }
    }

    @Override
    public synchronized boolean save(
            GameMenuShortcut shortcut) {
        Objects.requireNonNull(shortcut, "shortcut");
        if (!shortcut.isEditable()) {
            throw new IllegalArgumentException(
                    "Only editable shortcuts can be saved");
        }
        if (!GameMenuShortcutIds.isCustom(
                shortcut.getId())) {
            throw new IllegalArgumentException(
                    "Only custom shortcut IDs can be saved");
        }
        if (!migrateIfRequired()) {
            return false;
        }
        GameMenuShortcutDecodeResult decoded =
                loadCanonicalDocument();
        if (decoded == null) {
            return false;
        }
        LinkedHashMap<String, GameMenuShortcut> shortcuts =
                index(decoded.getShortcuts());
        shortcuts.put(shortcut.getId(), shortcut);
        return write(shortcuts.values());
    }

    @Override
    public synchronized boolean delete(String shortcutId) {
        if (!migrateIfRequired()) {
            return false;
        }
        GameMenuShortcutDecodeResult decoded =
                loadCanonicalDocument();
        if (decoded == null) {
            return false;
        }
        LinkedHashMap<String, GameMenuShortcut> shortcuts =
                index(decoded.getShortcuts());
        GameMenuShortcut existing =
                shortcuts.get(shortcutId);
        if (existing == null) {
            return true;
        }
        if (!existing.isEditable()) {
            return false;
        }
        shortcuts.remove(shortcutId);
        return write(shortcuts.values());
    }

    private boolean migrateIfRequired() {
        if (preferences.contains(DOCUMENT_KEY)) {
            cleanupLegacyValues();
            return true;
        }

        LinkedHashMap<String, GameMenuShortcut> migrated =
                new LinkedHashMap<>();
        migrateImported(migrated);
        migrateCustom(migrated);
        final boolean committed;
        try {
            committed = write(migrated.values());
        }
        catch (IllegalArgumentException error) {
            LimeLog.warning(
                    "Unable to encode shortcut migration");
            return false;
        }
        if (!committed) {
            LimeLog.warning(
                    "Unable to commit shortcut migration");
            return false;
        }
        cleanupLegacyValues();
        return true;
    }

    private void migrateImported(
            Map<String, GameMenuShortcut> destination) {
        Object storedImportedDocument =
                legacyImportedPreferences.getAll().get(
                        LEGACY_IMPORTED_KEY);
        if (!(storedImportedDocument instanceof String)) {
            if (storedImportedDocument != null) {
                LimeLog.warning(
                        "Ignoring imported shortcut document with invalid storage type");
            }
            return;
        }
        String importedJson =
                (String) storedImportedDocument;
        if (importedJson.trim().isEmpty()) {
            return;
        }
        try {
            GameMenuShortcutDecodeResult decoded =
                    legacyCodec.decodeImported(importedJson);
            for (GameMenuShortcut shortcut :
                    decoded.getShortcuts()) {
                if (!destination.containsKey(
                        shortcut.getId())) {
                    destination.put(
                            shortcut.getId(), shortcut);
                }
            }
            logRejected("imported legacy", decoded);
        }
        catch (IllegalArgumentException error) {
            LimeLog.warning(
                    "Ignoring invalid imported shortcut document");
        }
    }

    private void migrateCustom(
            Map<String, GameMenuShortcut> destination) {
        List<Map.Entry<String, ?>> entries =
                new ArrayList<>(
                        preferences.getAll().entrySet());
        Collections.sort(
                entries,
                (left, right) -> left.getKey()
                        .compareTo(right.getKey()));
        int rejected = 0;
        for (Map.Entry<String, ?> entry : entries) {
            if (DOCUMENT_KEY.equals(entry.getKey())) {
                continue;
            }
            if (!(entry.getValue() instanceof String) ||
                    destination.size() >=
                            GameMenuShortcutDocumentCodec
                                    .MAXIMUM_SHORTCUT_COUNT) {
                rejected++;
                continue;
            }
            try {
                GameMenuShortcut shortcut =
                        legacyCodec.decodeCustom(
                                entry.getKey(),
                                (String) entry.getValue());
                destination.put(
                        shortcut.getId(), shortcut);
            }
            catch (RuntimeException error) {
                rejected++;
            }
        }
        if (rejected != 0) {
            LimeLog.warning(
                    "Ignored " + rejected +
                            " invalid legacy custom shortcuts");
        }
    }

    private void cleanupLegacyValues() {
        SharedPreferences.Editor editor = preferences.edit();
        boolean hasCustomLegacyValues = false;
        for (String key : preferences.getAll().keySet()) {
            if (!DOCUMENT_KEY.equals(key)) {
                editor.remove(key);
                hasCustomLegacyValues = true;
            }
        }
        if (hasCustomLegacyValues && !editor.commit()) {
            LimeLog.warning(
                    "Unable to clean legacy custom shortcuts");
        }
        if (legacyImportedPreferences
                .contains(LEGACY_IMPORTED_KEY) &&
                !legacyImportedPreferences.edit()
                        .remove(LEGACY_IMPORTED_KEY)
                        .commit()) {
            LimeLog.warning(
                    "Unable to clean imported shortcut source");
        }
    }

    private boolean write(
            Iterable<GameMenuShortcut> shortcuts) {
        List<GameMenuShortcut> document = new ArrayList<>();
        for (GameMenuShortcut shortcut : shortcuts) {
            document.add(shortcut);
        }
        return preferences.edit()
                .putString(
                        DOCUMENT_KEY,
                        documentCodec.encode(document))
                .commit();
    }

    private static LinkedHashMap<String, GameMenuShortcut> index(
            List<GameMenuShortcut> shortcuts) {
        LinkedHashMap<String, GameMenuShortcut> indexed =
                new LinkedHashMap<>();
        for (GameMenuShortcut shortcut : shortcuts) {
            indexed.put(shortcut.getId(), shortcut);
        }
        return indexed;
    }

    private static void logRejected(
            String source,
            GameMenuShortcutDecodeResult result) {
        if (result.getRejectedEntryCount() != 0) {
            LimeLog.warning(
                    "Ignored " +
                            result.getRejectedEntryCount() +
                            " invalid " + source +
                            " shortcut entries");
        }
    }
}
