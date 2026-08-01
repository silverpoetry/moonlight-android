package com.limelight.virtualcontrols.layout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class VirtualControlLayoutKeyTest {
    @Test
    public void acceptsEveryPersistedProfileFamily() {
        VirtualControlLayoutKey keyboard =
                VirtualControlLayoutKey.keyboard(
                        "OSC_Keyboard_5",
                        VirtualControlLayoutOrientation.PORTRAIT);
        VirtualControlLayoutKey gamepad =
                VirtualControlLayoutKey.gamepad(
                        "gamePad_5",
                        VirtualControlLayoutOrientation.LANDSCAPE);

        assertEquals(
                VirtualControlLayoutKind.KEYBOARD,
                keyboard.getKind());
        assertEquals("OSC_Keyboard_5", keyboard.getProfileId());
        assertEquals(
                VirtualControlLayoutKind.GAMEPAD,
                gamepad.getKind());
        assertEquals("gamePad_5", gamepad.getProfileId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversal() {
        VirtualControlLayoutKey.keyboard(
                "../client.key",
                VirtualControlLayoutOrientation.LANDSCAPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsProfileFromWrongFamily() {
        VirtualControlLayoutKey.gamepad(
                "OSC_Keyboard",
                VirtualControlLayoutOrientation.LANDSCAPE);
    }

    @Test
    public void equalityIncludesOrientation() {
        VirtualControlLayoutKey landscape =
                VirtualControlLayoutKey.keyboard(
                        "OSC_Keyboard",
                        VirtualControlLayoutOrientation.LANDSCAPE);
        VirtualControlLayoutKey portrait =
                VirtualControlLayoutKey.keyboard(
                        "OSC_Keyboard",
                        VirtualControlLayoutOrientation.PORTRAIT);

        assertNotEquals(landscape, portrait);
    }
}
