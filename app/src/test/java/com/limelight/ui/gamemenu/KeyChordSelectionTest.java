package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyChordSelectionTest {
    @Test
    public void preservesSelectionOrderAndEncoding() {
        KeyChordSelection selection =
                new KeyChordSelection(3);

        assertTrue(selection.add("29", "A"));
        assertTrue(selection.add("59", "Shift"));

        assertEquals("29,59", selection.getEncodedKeyCodes());
        assertEquals("A+Shift", selection.getDisplayName());
    }

    @Test
    public void rejectsSelectionsBeyondMaximumSize() {
        KeyChordSelection selection =
                new KeyChordSelection(2);

        assertTrue(selection.add("29", "A"));
        assertTrue(selection.add("30", "B"));
        assertFalse(selection.add("31", "C"));

        assertEquals("29,30", selection.getEncodedKeyCodes());
        assertEquals("A+B", selection.getDisplayName());
    }

    @Test
    public void clearRemovesCodesAndNamesTogether() {
        KeyChordSelection selection =
                new KeyChordSelection(2);
        selection.add("29", "A");

        selection.clear();

        assertTrue(selection.isEmpty());
        assertEquals("", selection.getEncodedKeyCodes());
        assertEquals("", selection.getDisplayName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveMaximumSize() {
        new KeyChordSelection(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullKeyData() {
        new KeyChordSelection(1).add(null, "A");
    }
}
