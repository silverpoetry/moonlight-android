package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BoundedIntegerParserTest {
    @Test
    public void parsesValuesInsideInclusiveBounds() {
        assertEquals(
                Integer.valueOf(1),
                BoundedIntegerParser.parse(" 1 ", 1, 999));
        assertEquals(
                Integer.valueOf(999),
                BoundedIntegerParser.parse("999", 1, 999));
    }

    @Test
    public void rejectsEmptyMalformedAndOutOfRangeValues() {
        assertNull(BoundedIntegerParser.parse("", 1, 999));
        assertNull(BoundedIntegerParser.parse("12.5", 1, 999));
        assertNull(BoundedIntegerParser.parse("0", 1, 999));
        assertNull(BoundedIntegerParser.parse("1000", 1, 999));
        assertNull(BoundedIntegerParser.parse(
                "999999999999999999999", 1, 999));
    }

    @Test
    public void rejectsInvertedBounds() {
        assertNull(BoundedIntegerParser.parse("5", 10, 1));
    }
}
