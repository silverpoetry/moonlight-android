package com.limelight.binding.input;

import android.os.Handler;
import android.os.Looper;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;

import java.util.Arrays;
import java.util.Objects;

/**
 * Sends an ordered Windows virtual-key chord and releases it in reverse order.
 */
public final class KeyboardChordSender {
    private static final long KEY_UP_DELAY_MS = 25;
    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private KeyboardChordSender() {
    }

    public static void send(NvConnection connection, short[] keyCodes) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(keyCodes, "keyCodes");
        if (keyCodes.length == 0) {
            return;
        }

        short[] chord = Arrays.copyOf(keyCodes, keyCodes.length);
        byte modifier = 0;
        for (short keyCode : chord) {
            connection.sendKeyboardInput(
                    keyCode, KeyboardPacket.KEY_DOWN, modifier, (byte) 0);
            modifier |= getModifier(keyCode);
        }

        final byte pressedModifiers = modifier;
        MAIN_HANDLER.postDelayed(
                () -> release(connection, chord, pressedModifiers),
                KEY_UP_DELAY_MS);
    }

    private static void release(NvConnection connection,
                                short[] chord,
                                byte pressedModifiers) {
        byte modifier = pressedModifiers;
        for (int index = chord.length - 1; index >= 0; index--) {
            short keyCode = chord[index];
            modifier &= ~getModifier(keyCode);
            connection.sendKeyboardInput(
                    keyCode, KeyboardPacket.KEY_UP, modifier, (byte) 0);
        }
    }

    private static byte getModifier(short keyCode) {
        switch (keyCode) {
            case KeyboardTranslator.VK_LSHIFT:
                return KeyboardPacket.MODIFIER_SHIFT;
            case KeyboardTranslator.VK_LCONTROL:
                return KeyboardPacket.MODIFIER_CTRL;
            case KeyboardTranslator.VK_LWIN:
                return KeyboardPacket.MODIFIER_META;
            case KeyboardTranslator.VK_LMENU:
                return KeyboardPacket.MODIFIER_ALT;
            default:
                return 0;
        }
    }
}
