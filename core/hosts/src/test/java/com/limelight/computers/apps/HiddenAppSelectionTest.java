package com.limelight.computers.apps;

import org.junit.Test;

import java.util.Arrays;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class HiddenAppSelectionTest {
    @Test
    public void normalizesDuplicatesAndOrdering() {
        HiddenAppSelection selection = HiddenAppSelection.of(
                Arrays.asList(9, 2, 9, 4));

        assertEquals(
                new TreeSet<>(Arrays.asList(2, 4, 9)),
                selection.getAppIds());
    }

    @Test
    public void reusesCanonicalEmptySelection() {
        assertSame(
                HiddenAppSelection.empty(),
                HiddenAppSelection.of(Arrays.<Integer>asList()));
    }

    @Test
    public void preservesFullSignedIntegerRange() {
        assertEquals(
                new TreeSet<>(Arrays.asList(
                        Integer.MIN_VALUE, -1, Integer.MAX_VALUE)),
                HiddenAppSelection.of(Arrays.asList(
                        Integer.MAX_VALUE, -1, Integer.MIN_VALUE))
                        .getAppIds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullAppId() {
        HiddenAppSelection.of(Arrays.asList(1, null));
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullCollection() {
        HiddenAppSelection.of(null);
    }
}
