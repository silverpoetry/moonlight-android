package com.limelight.ui.gamemenu;

import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class GameMenuCardConfigurationTest {
    @Test
    public void missingLayoutUsesCatalogDefaults() {
        GameMenuCardCatalog.Card first =
                card("action:first", true);
        GameMenuCardCatalog.Card shortcut =
                card("shortcut:custom:one", false);
        GameMenuCardCatalog.Card second =
                card("action:second", true);

        GameMenuCardConfiguration.State state =
                GameMenuCardConfiguration.load(
                        GameMenuCardLayoutLoadResult.absent(),
                        Arrays.asList(first, shortcut, second));

        assertEquals(
                Arrays.asList(first, second),
                state.visible);
        assertEquals(
                Collections.singletonList(shortcut),
                state.hidden);
    }

    @Test
    public void storedReferencesFilterStaleIdsAndClassifyNewCards() {
        GameMenuCardCatalog.Card first =
                card("action:first", true);
        GameMenuCardCatalog.Card second =
                card("action:second", true);
        GameMenuCardCatalog.Card newShortcut =
                card("shortcut:custom:new", false);
        GameMenuCardLayout layout =
                new GameMenuCardLayout(
                        Arrays.asList(
                                second.id,
                                "action:removed",
                                first.id),
                        Collections.singleton(first.id));

        GameMenuCardConfiguration.State state =
                GameMenuCardConfiguration.load(
                        GameMenuCardLayoutLoadResult
                                .present(layout),
                        Arrays.asList(
                                first,
                                second,
                                newShortcut));

        assertEquals(
                Collections.singletonList(second),
                state.visible);
        assertEquals(
                Arrays.asList(first, newShortcut),
                state.hidden);
    }

    @Test
    public void visibleAndHiddenStateEncodesOnlyStableReferences() {
        GameMenuCardCatalog.Card first =
                card("action:first", true);
        GameMenuCardCatalog.Card shortcut =
                card("shortcut:custom:one", false);

        GameMenuCardLayout layout =
                GameMenuCardConfiguration.toLayout(
                        Collections.singletonList(first),
                        Collections.singletonList(shortcut));

        assertEquals(
                Arrays.asList(first.id, shortcut.id),
                layout.getOrderedCardIds());
        assertEquals(
                Collections.singleton(shortcut.id),
                layout.getHiddenCardIds());
    }

    private static GameMenuCardCatalog.Card card(
            String id,
            boolean defaultVisible) {
        return new GameMenuCardCatalog.Card(
                id,
                id,
                id,
                0,
                defaultVisible,
                null,
                null);
    }
}
