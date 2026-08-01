package com.limelight.settings.ui;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Bounded codec for the historical JSON card-order representation.
 */
public final class GameMenuCardLayoutCodec {
    public static final int MAXIMUM_DOCUMENT_LENGTH = 65_536;

    private static final Gson GSON = new Gson();

    private GameMenuCardLayoutCodec() {
    }

    public static List<String> decodeOrder(String document) {
        if (document == null ||
                document.isEmpty() ||
                document.length() > MAXIMUM_DOCUMENT_LENGTH) {
            return Collections.emptyList();
        }

        try {
            JsonElement root = JsonParser.parseString(document);
            if (!root.isJsonArray()) {
                return Collections.emptyList();
            }
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonPrimitive() ||
                        !element.getAsJsonPrimitive().isString()) {
                    return Collections.emptyList();
                }
                String id = element.getAsString();
                if (id.isEmpty() ||
                        id.length() >
                                GameMenuCardLayout
                                        .MAXIMUM_CARD_ID_LENGTH) {
                    return Collections.emptyList();
                }
                result.add(id);
                if (result.size() >
                        GameMenuCardLayout.MAXIMUM_CARD_COUNT) {
                    return Collections.emptyList();
                }
            }
            return Collections.unmodifiableList(
                    new ArrayList<>(result));
        }
        catch (JsonParseException | IllegalStateException error) {
            return Collections.emptyList();
        }
    }

    public static String encodeOrder(List<String> orderedCardIds) {
        GameMenuCardLayout validated =
                new GameMenuCardLayout(
                        orderedCardIds,
                        Collections.emptySet());
        String document = GSON.toJson(
                validated.getOrderedCardIds());
        if (document.length() > MAXIMUM_DOCUMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Card-order document exceeds the size limit");
        }
        return document;
    }
}
