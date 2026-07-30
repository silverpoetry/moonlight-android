package com.limelight.binding.input;

/**
 * Protocol-facing output port for keyboard and UTF-8 text input.
 */
public interface KeyboardInputSink {
    void sendKey(
            short keyCode,
            byte keyAction,
            byte modifiers,
            byte flags);

    void sendUtf8Text(String text);
}
