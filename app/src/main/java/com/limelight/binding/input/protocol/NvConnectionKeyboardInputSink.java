package com.limelight.binding.input.protocol;

import com.limelight.binding.input.KeyboardInputSink;
import com.limelight.nvstream.NvConnection;

import java.util.Objects;

/**
 * Keyboard protocol adapter backed by the active Moonlight connection.
 */
public final class NvConnectionKeyboardInputSink
        implements KeyboardInputSink {
    private final NvConnection connection;

    public NvConnectionKeyboardInputSink(NvConnection connection) {
        this.connection = Objects.requireNonNull(
                connection,
                "connection");
    }

    @Override
    public void sendKey(
            short keyCode,
            byte keyAction,
            byte modifiers,
            byte flags) {
        connection.sendKeyboardInput(
                keyCode,
                keyAction,
                modifiers,
                flags);
    }

    @Override
    public void sendUtf8Text(String text) {
        connection.sendUtf8Text(text);
    }
}
