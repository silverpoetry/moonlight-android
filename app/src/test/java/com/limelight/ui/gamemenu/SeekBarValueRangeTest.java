package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SeekBarValueRangeTest {
    @Test
    public void mapsLogicalRangeToZeroBasedProgress() {
        SeekBarValueRange range = new SeekBarValueRange(10, 300);

        assertEquals(290, range.getProgressMaximum());
        assertEquals(0, range.valueToProgress(10));
        assertEquals(90, range.valueToProgress(100));
        assertEquals(300, range.progressToValue(290));
    }

    @Test
    public void clampsValuesAndProgressAtBothEnds() {
        SeekBarValueRange range = new SeekBarValueRange(1, 30);

        assertEquals(0, range.valueToProgress(-100));
        assertEquals(29, range.valueToProgress(100));
        assertEquals(1, range.progressToValue(-100));
        assertEquals(30, range.progressToValue(100));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvertedRange() {
        new SeekBarValueRange(10, 9);
    }
}
