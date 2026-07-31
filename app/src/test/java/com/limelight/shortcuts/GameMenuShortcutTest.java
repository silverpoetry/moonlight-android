package com.limelight.shortcuts;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class GameMenuShortcutTest {
    @Test
    public void copiesInputAndOutputKeyArrays() {
        short[] source = {1, 2};
        GameMenuShortcut shortcut =
                GameMenuShortcut.moonlightChord(
                        "shortcut:custom:copy",
                        "Copy",
                        "",
                        source,
                        true);

        source[0] = 99;
        short[] firstRead =
                shortcut.getMoonlightKeyCodes();
        firstRead[1] = 88;

        assertArrayEquals(
                new short[] {1, 2},
                shortcut.getMoonlightKeyCodes());
        assertEquals(
                0,
                shortcut.getAndroidKeyCodes().length);
    }

    @Test
    public void rejectsMissingOrAmbiguousChord() {
        assertInvalid(() ->
                GameMenuShortcut.androidChord(
                        "shortcut:custom:empty",
                        "Empty",
                        "",
                        new int[0],
                        true));
    }

    @Test
    public void customIdDoesNotDoublePrefixCanonicalId() {
        assertEquals(
                "shortcut:custom:one",
                GameMenuShortcutIds.custom(
                        "shortcut:custom:one"));
    }

    @Test
    public void customIdRejectsEmptyCanonicalSuffix() {
        assertInvalid(() ->
                GameMenuShortcutIds.custom(
                        "shortcut:custom:"));
    }

    private static void assertInvalid(Runnable operation) {
        try {
            operation.run();
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
