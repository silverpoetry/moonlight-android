package com.limelight.ui.gamemenu;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persists first-screen card references without copying action or shortcut
 * payloads. Missing references are ignored and newly discovered shortcuts are
 * placed in the hidden section.
 */
final class GameMenuCardConfiguration {
    private static final String ORDER_PREF =
            "game_menu_card_order_v2";
    private static final String HIDDEN_PREF =
            "game_menu_card_hidden_v2";

    private static final String LEGACY_ORDER_PREF =
            "game_menu_action_order_v1";
    private static final String LEGACY_HIDDEN_PREF =
            "game_menu_action_hidden_v1";

    static final class State {
        final List<GameMenuCardCatalog.Card> visible;
        final List<GameMenuCardCatalog.Card> hidden;

        State(List<GameMenuCardCatalog.Card> visible,
              List<GameMenuCardCatalog.Card> hidden) {
            this.visible = visible;
            this.hidden = hidden;
        }
    }

    private GameMenuCardConfiguration() {
    }

    static State load(
            Context context, List<GameMenuCardCatalog.Card> catalog) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, GameMenuCardCatalog.Card> cardsById =
                indexCatalog(catalog);

        boolean hasCurrentConfiguration =
                preferences.contains(ORDER_PREF);
        LinkedHashSet<String> orderedIds =
                hasCurrentConfiguration ?
                        readCurrentOrder(preferences, cardsById) :
                        readLegacyOrder(preferences, cardsById);
        Set<String> hiddenIds =
                hasCurrentConfiguration ?
                        new HashSet<>(preferences.getStringSet(
                                HIDDEN_PREF,
                                Collections.<String>emptySet())) :
                        readLegacyHidden(preferences);

        Set<String> previouslyKnownIds =
                new HashSet<>(orderedIds);
        for (GameMenuCardCatalog.Card card : catalog) {
            orderedIds.add(card.id);
        }

        List<GameMenuCardCatalog.Card> visible = new ArrayList<>();
        List<GameMenuCardCatalog.Card> hidden = new ArrayList<>();
        for (String id : orderedIds) {
            GameMenuCardCatalog.Card card = cardsById.get(id);
            if (card == null) {
                continue;
            }
            boolean newlyDiscovered = !previouslyKnownIds.contains(id);
            if (hiddenIds.contains(id) ||
                    (newlyDiscovered && !card.defaultVisible)) {
                hidden.add(card);
            } else {
                visible.add(card);
            }
        }
        return new State(visible, hidden);
    }

    static State defaults(List<GameMenuCardCatalog.Card> catalog) {
        List<GameMenuCardCatalog.Card> visible = new ArrayList<>();
        List<GameMenuCardCatalog.Card> hidden = new ArrayList<>();
        for (GameMenuCardCatalog.Card card : catalog) {
            (card.defaultVisible ? visible : hidden).add(card);
        }
        return new State(visible, hidden);
    }

    static void save(
            Context context,
            List<GameMenuCardCatalog.Card> visible,
            List<GameMenuCardCatalog.Card> hidden) {
        JSONArray order = new JSONArray();
        appendIds(order, visible);
        appendIds(order, hidden);

        Set<String> hiddenIds = new HashSet<>();
        for (GameMenuCardCatalog.Card card : hidden) {
            hiddenIds.add(card.id);
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(ORDER_PREF, order.toString())
                .putStringSet(HIDDEN_PREF, hiddenIds)
                .apply();
    }

    private static Map<String, GameMenuCardCatalog.Card> indexCatalog(
            List<GameMenuCardCatalog.Card> catalog) {
        Map<String, GameMenuCardCatalog.Card> cardsById = new HashMap<>();
        for (GameMenuCardCatalog.Card card : catalog) {
            cardsById.put(card.id, card);
        }
        return cardsById;
    }

    private static LinkedHashSet<String> readCurrentOrder(
            SharedPreferences preferences,
            Map<String, GameMenuCardCatalog.Card> cardsById) {
        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        String storedOrder = preferences.getString(ORDER_PREF, "");
        if (storedOrder == null || storedOrder.isEmpty()) {
            return orderedIds;
        }
        try {
            JSONArray order = new JSONArray(storedOrder);
            for (int index = 0; index < order.length(); index++) {
                String id = order.getString(index);
                if (cardsById.containsKey(id)) {
                    orderedIds.add(id);
                }
            }
        } catch (JSONException ignored) {
            // A corrupt preference falls back to catalog order below.
        }
        return orderedIds;
    }

    private static LinkedHashSet<String> readLegacyOrder(
            SharedPreferences preferences,
            Map<String, GameMenuCardCatalog.Card> cardsById) {
        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        String storedOrder =
                preferences.getString(LEGACY_ORDER_PREF, "");
        if (storedOrder == null || storedOrder.isEmpty()) {
            return orderedIds;
        }
        for (String legacyId : storedOrder.split(",")) {
            String id = GameMenuCardCatalog.actionCardId(legacyId);
            if (cardsById.containsKey(id)) {
                orderedIds.add(id);
            }
        }
        return orderedIds;
    }

    private static Set<String> readLegacyHidden(
            SharedPreferences preferences) {
        Set<String> hidden = new HashSet<>();
        for (String legacyId : preferences.getStringSet(
                LEGACY_HIDDEN_PREF,
                Collections.<String>emptySet())) {
            hidden.add(GameMenuCardCatalog.actionCardId(legacyId));
        }
        return hidden;
    }

    private static void appendIds(
            JSONArray destination,
            List<GameMenuCardCatalog.Card> cards) {
        for (GameMenuCardCatalog.Card card : cards) {
            destination.put(card.id);
        }
    }
}
