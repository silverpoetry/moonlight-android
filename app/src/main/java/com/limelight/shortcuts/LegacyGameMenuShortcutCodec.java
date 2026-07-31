package com.limelight.shortcuts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only decoder for shortcut formats produced before the canonical
 * shortcut document. It is used exclusively by the one-time adapter
 * migration and never by runtime catalog loading.
 */
public final class LegacyGameMenuShortcutCodec {
    private final Gson gson = new Gson();

    public GameMenuShortcutDecodeResult decodeImported(
            String json) {
        if (json == null ||
                json.length() >
                        GameMenuShortcutDocumentCodec
                                .MAXIMUM_JSON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "Invalid imported shortcut document size");
        }
        List<GameMenuShortcut> shortcuts = new ArrayList<>();
        int rejected = 0;
        final JsonArray data;
        try {
            JsonObject root = JsonParser
                    .parseString(json)
                    .getAsJsonObject();
            data = root.getAsJsonArray("data");
        }
        catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    "Invalid imported shortcut JSON", error);
        }
        if (data == null) {
            throw new IllegalArgumentException(
                    "Imported shortcut array is missing");
        }
        for (JsonElement element : data) {
            if (shortcuts.size() >=
                    GameMenuShortcutDocumentCodec
                            .MAXIMUM_SHORTCUT_COUNT) {
                rejected++;
                continue;
            }
            try {
                JsonObject object = element.getAsJsonObject();
                String name = object.get("name").getAsString();
                JsonArray encodedKeys =
                        object.getAsJsonArray("data");
                short[] keys = new short[encodedKeys.size()];
                for (int index = 0;
                     index < encodedKeys.size(); index++) {
                    String encoded = encodedKeys
                            .get(index).getAsString();
                    if (encoded.length() < 3 ||
                            !(encoded.startsWith("0x") ||
                                    encoded.startsWith("0X"))) {
                        throw new IllegalArgumentException(
                                "Invalid imported key code");
                    }
                    int keyCode = Integer.parseInt(
                            encoded.substring(2), 16);
                    if (keyCode > 0xffff) {
                        throw new IllegalArgumentException(
                                "Imported key code is out of range");
                    }
                    keys[index] = (short) keyCode;
                }
                shortcuts.add(
                        GameMenuShortcut.moonlightChord(
                                GameMenuShortcutIds.imported(
                                        name, keys),
                                name,
                                "",
                                keys,
                                false));
            }
            catch (RuntimeException error) {
                rejected++;
            }
        }
        return new GameMenuShortcutDecodeResult(
                shortcuts, rejected);
    }

    public GameMenuShortcut decodeCustom(
            String storageKey, String json) {
        if (json == null ||
                json.length() >
                        GameMenuShortcutDocumentCodec
                                .MAXIMUM_JSON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "Invalid legacy custom shortcut size");
        }
        final LegacyEntry entry;
        try {
            entry = gson.fromJson(json, LegacyEntry.class);
        }
        catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    "Invalid legacy custom shortcut JSON", error);
        }
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Legacy custom shortcut is empty");
        }
        String rawId = entry.id == null ||
                entry.id.trim().isEmpty()
                ? storageKey : entry.id;
        String stableId = GameMenuShortcutIds.custom(rawId);
        if (entry.datas != null && entry.datas.length != 0) {
            return GameMenuShortcut.moonlightChord(
                    stableId,
                    entry.name,
                    entry.desc,
                    entry.datas,
                    true);
        }
        return GameMenuShortcut.androidChord(
                stableId,
                entry.name,
                entry.desc,
                parseAndroidKeyCodes(entry.codes),
                true);
    }

    static int[] parseAndroidKeyCodes(
            String encodedCodes) {
        if (encodedCodes == null ||
                encodedCodes.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Legacy Android key chord is empty");
        }
        String[] values = encodedCodes.split(",");
        int[] keys = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            try {
                keys[index] =
                        Integer.parseInt(values[index].trim());
            }
            catch (NumberFormatException error) {
                throw new IllegalArgumentException(
                        "Invalid legacy Android key code", error);
            }
        }
        return keys;
    }

    private static final class LegacyEntry {
        String id;
        String name;
        String desc;
        String codes;
        short[] datas;
    }
}
