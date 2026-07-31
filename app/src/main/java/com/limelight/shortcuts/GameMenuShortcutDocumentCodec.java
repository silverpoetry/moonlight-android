package com.limelight.shortcuts;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded JSON codec for the canonical shortcut document.
 */
public final class GameMenuShortcutDocumentCodec {
    public static final int DOCUMENT_VERSION = 1;
    public static final int MAXIMUM_SHORTCUT_COUNT = 128;
    public static final int MAXIMUM_JSON_CHARACTERS = 131072;

    private final Gson gson;

    public GameMenuShortcutDocumentCodec() {
        gson = new Gson();
    }

    public String encode(List<GameMenuShortcut> shortcuts) {
        Objects.requireNonNull(shortcuts, "shortcuts");
        if (shortcuts.size() > MAXIMUM_SHORTCUT_COUNT) {
            throw new IllegalArgumentException(
                    "Shortcut document exceeds the entry limit");
        }

        Document document = new Document();
        document.version = DOCUMENT_VERSION;
        document.shortcuts = new ArrayList<>();
        for (GameMenuShortcut shortcut : shortcuts) {
            document.shortcuts.add(Entry.from(
                    Objects.requireNonNull(shortcut, "shortcut")));
        }
        String json = gson.toJson(document);
        if (json.length() > MAXIMUM_JSON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "Shortcut document exceeds the size limit");
        }
        return json;
    }

    public GameMenuShortcutDecodeResult decode(String json) {
        if (json == null ||
                json.length() > MAXIMUM_JSON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "Invalid shortcut document size");
        }

        final Document document;
        try {
            document = gson.fromJson(json, Document.class);
        }
        catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    "Invalid shortcut document JSON", error);
        }
        if (document == null ||
                document.version != DOCUMENT_VERSION ||
                document.shortcuts == null) {
            throw new IllegalArgumentException(
                    "Unsupported shortcut document");
        }

        Map<String, GameMenuShortcut> accepted =
                new LinkedHashMap<>();
        int rejected = 0;
        for (Entry entry : document.shortcuts) {
            if (accepted.size() >= MAXIMUM_SHORTCUT_COUNT) {
                rejected++;
                continue;
            }
            try {
                GameMenuShortcut shortcut = entry.toShortcut();
                if (accepted.containsKey(shortcut.getId())) {
                    rejected++;
                }
                else {
                    accepted.put(shortcut.getId(), shortcut);
                }
            }
            catch (RuntimeException error) {
                rejected++;
            }
        }
        return new GameMenuShortcutDecodeResult(
                new ArrayList<>(accepted.values()), rejected);
    }

    private static final class Document {
        int version;
        List<Entry> shortcuts;
    }

    private static final class Entry {
        String id;
        String name;
        String description;
        short[] moonlightKeyCodes;
        int[] androidKeyCodes;
        boolean editable;

        static Entry from(GameMenuShortcut shortcut) {
            if (shortcut.isEditable() !=
                    GameMenuShortcutIds.isCustom(
                            shortcut.getId()) ||
                    (!shortcut.isEditable() &&
                            !GameMenuShortcutIds.isImported(
                                    shortcut.getId()))) {
                throw new IllegalArgumentException(
                        "Shortcut cannot be stored in the canonical document");
            }
            Entry entry = new Entry();
            entry.id = shortcut.getId();
            entry.name = shortcut.getName();
            entry.description = shortcut.getDescription();
            entry.editable = shortcut.isEditable();
            if (shortcut.usesMoonlightKeyCodes()) {
                entry.moonlightKeyCodes =
                        shortcut.getMoonlightKeyCodes();
            }
            else {
                entry.androidKeyCodes =
                        shortcut.getAndroidKeyCodes();
            }
            return entry;
        }

        GameMenuShortcut toShortcut() {
            boolean hasMoonlightKeys =
                    moonlightKeyCodes != null &&
                            moonlightKeyCodes.length != 0;
            boolean hasAndroidKeys =
                    androidKeyCodes != null &&
                            androidKeyCodes.length != 0;
            if (hasMoonlightKeys == hasAndroidKeys) {
                throw new IllegalArgumentException(
                        "Persisted shortcut has an ambiguous chord");
            }
            if (editable !=
                    GameMenuShortcutIds.isCustom(id) ||
                    (!editable &&
                            !GameMenuShortcutIds.isImported(id))) {
                throw new IllegalArgumentException(
                        "Persisted shortcut identity is invalid");
            }
            if (hasMoonlightKeys) {
                return GameMenuShortcut.moonlightChord(
                        id,
                        name,
                        description,
                        moonlightKeyCodes,
                        editable);
            }
            return GameMenuShortcut.androidChord(
                    id,
                    name,
                    description,
                    androidKeyCodes,
                    editable);
        }
    }
}
