package com.limelight.shortcuts.android;

import android.content.Context;
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
    private static final String PREFERENCES_NAME =
            "game_menu_shortcuts";
    private static final String LEGACY_CUSTOM_PREFERENCES_NAME =
            "quick_axi_keyAssemble";
    private static final String DOCUMENT_KEY =
            "game_menu_shortcuts_v2";
    private static final String LEGACY_IMPORTED_PREFERENCES_NAME =
            "specialPrefs";
    private static final String LEGACY_IMPORTED_KEY =
            "special_key";

    private final SharedPreferences preferences;
    private final SharedPreferences legacyCustomPreferences;
    private final SharedPreferences legacyImportedPreferences;
    private final GameMenuShortcutDocumentCodec documentCodec;
    private final LegacyGameMenuShortcutCodec legacyCodec;

    public SharedPreferencesGameMenuShortcutRepository(
            Context context) {
        this(
                openPreferences(context, PREFERENCES_NAME),
                openPreferences(
                        context,
                        LEGACY_CUSTOM_PREFERENCES_NAME),
                openPreferences(
                        context,
                        LEGACY_IMPORTED_PREFERENCES_NAME));
    }

    public SharedPreferencesGameMenuShortcutRepository(
            SharedPreferences preferences,
            SharedPreferences legacyCustomPreferences,
            SharedPreferences legacyImportedPreferences) {
        this(
                preferences,
                legacyCustomPreferences,
                legacyImportedPreferences,
                new GameMenuShortcutDocumentCodec(),
                new LegacyGameMenuShortcutCodec());
    }

    SharedPreferencesGameMenuShortcutRepository(
            SharedPreferences preferences,
            SharedPreferences legacyCustomPreferences,
            SharedPreferences legacyImportedPreferences,
            GameMenuShortcutDocumentCodec documentCodec,
            LegacyGameMenuShortcutCodec legacyCodec) {
        this.preferences = Objects.requireNonNull(
                preferences, "preferences");
        this.legacyCustomPreferences = Objects.requireNonNull(
                legacyCustomPreferences,
                "legacyCustomPreferences");
        this.legacyImportedPreferences = Objects.requireNonNull(
                legacyImportedPreferences,
                "legacyImportedPreferences");
        this.documentCodec = Objects.requireNonNull(
                documentCodec, "documentCodec");
        this.legacyCodec = Objects.requireNonNull(
                legacyCodec, "legacyCodec");
    }

    private static SharedPreferences openPreferences(
            Context context,
            String name) {
        Context providedContext = Objects.requireNonNull(
                context,
                "context");
        Context applicationContext =
                providedContext.getApplicationContext();
        Context storageContext = applicationContext != null
                ? applicationContext
                : providedContext;
        return storageContext.getSharedPreferences(
                name,
                Context.MODE_PRIVATE);
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
        return loadDocument(preferences, "canonical");
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

        GameMenuShortcutDecodeResult existingDocument =
                loadDocument(
                        legacyCustomPreferences,
                        "migrated canonical");
        if (existingDocument != null) {
            if (!write(existingDocument.getShortcuts())) {
                LimeLog.warning(
                        "Unable to move shortcut document to canonical storage");
                return false;
            }
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
                        legacyCustomPreferences.getAll().entrySet());
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
        boolean hasUnexpectedValues = false;
        for (String key : preferences.getAll().keySet()) {
            if (!DOCUMENT_KEY.equals(key)) {
                editor.remove(key);
                hasUnexpectedValues = true;
            }
        }
        if (hasUnexpectedValues && !editor.commit()) {
            LimeLog.warning(
                    "Unable to clean unexpected canonical shortcut values");
        }
        if (!legacyCustomPreferences.getAll().isEmpty() &&
                !legacyCustomPreferences.edit().clear().commit()) {
            LimeLog.warning(
                    "Unable to clean legacy custom shortcut storage");
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

    private GameMenuShortcutDecodeResult loadDocument(
            SharedPreferences source,
            String sourceName) {
        Object storedDocument = source.getAll().get(DOCUMENT_KEY);
        if (storedDocument == null) {
            return null;
        }
        if (!(storedDocument instanceof String)) {
            LimeLog.warning(
                    "Ignoring " + sourceName +
                            " shortcut document with invalid storage type");
            return null;
        }
        try {
            GameMenuShortcutDecodeResult decoded =
                    documentCodec.decode((String) storedDocument);
            logRejected(sourceName, decoded);
            return decoded;
        }
        catch (IllegalArgumentException error) {
            LimeLog.warning(
                    "Ignoring invalid " + sourceName +
                            " shortcut document");
            return null;
        }
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
