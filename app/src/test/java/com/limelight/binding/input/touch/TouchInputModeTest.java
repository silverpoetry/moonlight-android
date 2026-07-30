package com.limelight.binding.input.touch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class TouchInputModeTest {
    @Test
    public void persistedValuesRemainStable() {
        assertMode(0, TouchInputMode.MULTI_TOUCH);
        assertMode(1, TouchInputMode.ABSOLUTE_MOUSE);
        assertMode(2, TouchInputMode.NATIVE_TOUCHPAD);
        assertMode(3, TouchInputMode.DISABLED);
        assertMode(4, TouchInputMode.ABSOLUTE_MOUSE_SWAPPED);
        assertMode(5, TouchInputMode.TOUCHPAD_MOVE_ONLY);
        assertMode(6, TouchInputMode.TOUCHPAD_MOVE_AND_CLICK);
        assertNull(TouchInputMode.fromPreferenceValue(-1));
        assertNull(TouchInputMode.fromPreferenceValue(7));
    }

    private static void assertMode(
            int preferenceValue,
            TouchInputMode expectedMode) {
        assertEquals(
                expectedMode,
                TouchInputMode.fromPreferenceValue(preferenceValue));
        assertEquals(preferenceValue, expectedMode.getPreferenceValue());
    }
}
