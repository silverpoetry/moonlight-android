package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeyboardPresetFactoryTest {
    @Test
    public void encodesAxixShortcutProtocolKeys() {
        assertEquals(
                "29,52,37,52,7",
                KeyboardPresetFactory.functionKeyCodes(0));
        assertEquals(
                "29,52,37,52,13",
                KeyboardPresetFactory.functionKeyCodes(6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidShortcutSuffix() {
        KeyboardPresetFactory.functionKeyCodes(10);
    }
}
