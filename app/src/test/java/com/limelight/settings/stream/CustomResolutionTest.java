package com.limelight.settings.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class CustomResolutionTest {
    @Test
    public void parserAcceptsCanonicalAndMultiplicationSign() {
        assertEquals(
                new CustomResolution(1920, 1080),
                CustomResolution.parse(" 1920x1080 "));
        assertEquals(
                new CustomResolution(2560, 1600),
                CustomResolution.parse("2560×1600"));
    }

    @Test
    public void parserRejectsMalformedAndOutOfRangeValues() {
        assertNull(CustomResolution.parse(null));
        assertNull(CustomResolution.parse(""));
        assertNull(CustomResolution.parse("1920"));
        assertNull(CustomResolution.parse("1920x1080x60"));
        assertNull(CustomResolution.parse("0x1080"));
        assertNull(CustomResolution.parse("100000x1080"));
    }

    @Test
    public void orderingPreservesHistoricalCatalogOrder() {
        List<CustomResolution> values = new ArrayList<>();
        values.add(new CustomResolution(2560, 1440));
        values.add(new CustomResolution(1920, 1080));

        Collections.sort(values);

        assertEquals("1920x1080", values.get(0).toStorageValue());
        assertEquals("2560x1440", values.get(1).toStorageValue());
    }
}
