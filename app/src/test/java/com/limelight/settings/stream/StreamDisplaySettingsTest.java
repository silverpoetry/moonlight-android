package com.limelight.settings.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class StreamDisplaySettingsTest {
    @Test
    public void invalidPersistedGravityUsesProductDefault() {
        assertEquals(
                StreamDisplaySettings.Gravity.DEFAULT,
                StreamDisplaySettings.Gravity.fromStorageValue("broken"));
    }

    @Test
    public void rejectsNonPositiveDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StreamDisplaySettings(
                        0,
                        1080,
                        false,
                        false,
                        false,
                        false,
                        false,
                        StreamDisplaySettings.Gravity.DEFAULT));
    }
}
