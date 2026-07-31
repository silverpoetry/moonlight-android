package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class ControllerMouseEmulationTranslatorTest {
    @Test
    public void changedButtonsEmitPressAndReleaseOnce() {
        ControllerMouseEmulationTranslator translator =
                new ControllerMouseEmulationTranslator();
        RecordingOutput output = new RecordingOutput();

        translator.translate(
                ControllerPacket.A_FLAG |
                        ControllerPacket.X_FLAG,
                output);
        translator.translate(
                ControllerPacket.A_FLAG |
                        ControllerPacket.X_FLAG,
                output);
        translator.translate(0, output);

        assertEquals(
                Arrays.asList(
                        "mouse:" +
                                MouseButtonPacket.BUTTON_LEFT +
                                ":true",
                        "key:" +
                                KeyboardTranslator.VK_ESCAPE +
                                ":" +
                                KeyboardPacket.KEY_DOWN,
                        "mouse:" +
                                MouseButtonPacket.BUTTON_LEFT +
                                ":false",
                        "key:" +
                                KeyboardTranslator.VK_ESCAPE +
                                ":" +
                                KeyboardPacket.KEY_UP),
                output.events);
    }

    @Test
    public void directionalAndDesktopKeysPreserveMapping() {
        ControllerMouseEmulationTranslator translator =
                new ControllerMouseEmulationTranslator();
        RecordingOutput output = new RecordingOutput();

        translator.translate(
                ControllerPacket.UP_FLAG |
                        ControllerPacket.DOWN_FLAG |
                        ControllerPacket.LEFT_FLAG |
                        ControllerPacket.RIGHT_FLAG |
                        ControllerPacket.SPECIAL_BUTTON_FLAG |
                        ControllerPacket.LB_FLAG |
                        ControllerPacket.RB_FLAG |
                        ControllerPacket.PLAY_FLAG |
                        ControllerPacket.BACK_FLAG,
                output);

        assertEquals(
                Arrays.asList(
                        KeyboardTranslator.VK_UP,
                        KeyboardTranslator.VK_DOWN,
                        KeyboardTranslator.VK_RIGHT,
                        KeyboardTranslator.VK_LEFT,
                        KeyboardTranslator.VK_LWIN,
                        KeyboardTranslator.VK_LMENU,
                        KeyboardTranslator.VK_TAB,
                        KeyboardTranslator.VK_BACK_SPACE,
                        KeyboardTranslator.VK_SPACE),
                output.keyDowns);
    }

    @Test
    public void stickClicksEmitChordsOnlyOnPress() {
        ControllerMouseEmulationTranslator translator =
                new ControllerMouseEmulationTranslator();
        RecordingOutput output = new RecordingOutput();
        int clicks =
                ControllerPacket.LS_CLK_FLAG |
                        ControllerPacket.RS_CLK_FLAG;

        translator.translate(clicks, output);
        translator.translate(clicks, output);
        translator.translate(0, output);

        assertEquals(2, output.chords.size());
        assertEquals(
                Arrays.asList(
                        (short) KeyboardTranslator.VK_LWIN,
                        (short) KeyboardTranslator.VK_LCONTROL,
                        (short) KeyboardTranslator.VK_O),
                output.chords.get(0));
        assertEquals(
                Arrays.asList(
                        (short) KeyboardTranslator.VK_LWIN,
                        (short) KeyboardTranslator.VK_D),
                output.chords.get(1));
    }

    private static final class RecordingOutput
            implements ControllerMouseEmulationTranslator.Output {
        private final List<String> events = new ArrayList<>();
        private final List<Integer> keyDowns = new ArrayList<>();
        private final List<List<Short>> chords =
                new ArrayList<>();

        @Override
        public void sendMouseButton(byte button, boolean down) {
            events.add("mouse:" + button + ":" + down);
        }

        @Override
        public void sendKey(int keyCode, byte action) {
            events.add("key:" + keyCode + ":" + action);
            if (action == KeyboardPacket.KEY_DOWN) {
                keyDowns.add(keyCode);
            }
        }

        @Override
        public void sendChord(short[] keyCodes) {
            List<Short> copy = new ArrayList<>();
            for (short keyCode : keyCodes) {
                copy.add(keyCode);
            }
            chords.add(copy);
        }
    }
}
