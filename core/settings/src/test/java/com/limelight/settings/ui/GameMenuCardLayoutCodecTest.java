package com.limelight.settings.ui;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class GameMenuCardLayoutCodecTest {
    @Test
    public void validOrderRoundTripsWithStableDeduplication() {
        String document = GameMenuCardLayoutCodec.encodeOrder(
                Arrays.asList(
                        "action:disconnect",
                        "shortcut:custom:\"quoted\"",
                        "action:disconnect"));

        assertEquals(
                Arrays.asList(
                        "action:disconnect",
                        "shortcut:custom:\"quoted\""),
                GameMenuCardLayoutCodec.decodeOrder(document));
    }

    @Test
    public void malformedOrWrongShapeDocumentFallsBackSafely() {
        assertTrue(GameMenuCardLayoutCodec
                .decodeOrder("{bad")
                .isEmpty());
        assertTrue(GameMenuCardLayoutCodec
                .decodeOrder("{\"id\":\"action:x\"}")
                .isEmpty());
        assertTrue(GameMenuCardLayoutCodec
                .decodeOrder("[1]")
                .isEmpty());
    }

    @Test
    public void layoutCopiesAndValidatesItsCollections() {
        GameMenuCardLayout layout =
                new GameMenuCardLayout(
                        Arrays.asList(
                                "action:a",
                                "action:a",
                                "action:b"),
                        Collections.singleton("action:b"));

        assertEquals(
                Arrays.asList("action:a", "action:b"),
                layout.getOrderedCardIds());
        assertEquals(
                Collections.singleton("action:b"),
                layout.getHiddenCardIds());
    }
}
