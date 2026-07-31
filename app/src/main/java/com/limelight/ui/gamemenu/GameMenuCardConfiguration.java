package com.limelight.ui.gamemenu;

import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves persisted card references against the current action/shortcut
 * catalog. This policy performs no storage or Android I/O.
 */
final class GameMenuCardConfiguration {
    static final class State {
        final List<GameMenuCardCatalog.Card> visible;
        final List<GameMenuCardCatalog.Card> hidden;

        State(List<GameMenuCardCatalog.Card> visible,
              List<GameMenuCardCatalog.Card> hidden) {
            this.visible = immutableCopy(visible);
            this.hidden = immutableCopy(hidden);
        }

        private static List<GameMenuCardCatalog.Card> immutableCopy(
                List<GameMenuCardCatalog.Card> source) {
            return Collections.unmodifiableList(
                    new ArrayList<>(source));
        }
    }

    private GameMenuCardConfiguration() {
    }

    static State load(
            GameMenuCardLayoutLoadResult storedLayout,
            List<GameMenuCardCatalog.Card> catalog) {
        Map<String, GameMenuCardCatalog.Card> cardsById =
                indexCatalog(catalog);

        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        Set<String> hiddenIds = Collections.emptySet();
        if (storedLayout.hasLayout()) {
            GameMenuCardLayout layout =
                    storedLayout.getLayout();
            for (String id : layout.getOrderedCardIds()) {
                if (cardsById.containsKey(id)) {
                    orderedIds.add(id);
                }
            }
            hiddenIds = layout.getHiddenCardIds();
        }

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

    static GameMenuCardLayout toLayout(
            List<GameMenuCardCatalog.Card> visible,
            List<GameMenuCardCatalog.Card> hidden) {
        List<String> order = new ArrayList<>();
        appendIds(order, visible);
        appendIds(order, hidden);

        Set<String> hiddenIds = new HashSet<>();
        for (GameMenuCardCatalog.Card card : hidden) {
            hiddenIds.add(card.id);
        }
        return new GameMenuCardLayout(order, hiddenIds);
    }

    private static Map<String, GameMenuCardCatalog.Card> indexCatalog(
            List<GameMenuCardCatalog.Card> catalog) {
        Map<String, GameMenuCardCatalog.Card> cardsById = new HashMap<>();
        for (GameMenuCardCatalog.Card card : catalog) {
            cardsById.put(card.id, card);
        }
        return cardsById;
    }

    private static void appendIds(
            List<String> destination,
            List<GameMenuCardCatalog.Card> cards) {
        for (GameMenuCardCatalog.Card card : cards) {
            destination.add(card.id);
        }
    }
}
