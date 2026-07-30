package com.limelight.binding.input;

import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.Objects;

/**
 * Owns keyboard translation, modifier state, local special chords, and
 * keyboard protocol output for one stream.
 *
 * <p>This controller is confined to the Android input thread.</p>
 */
public final class KeyboardInputController
        implements InputManager.InputDeviceListener {
    public interface Host {
        boolean isInputGrabbed();

        void onNonBackKeyDown();

        void requestToggleInputGrab();

        void requestQuit();

        void requestToggleCursorVisibility();
    }

    private final KeyboardTranslator translator;
    private final GamepadInputHandler gamepadInputHandler;
    private final KeyboardInputSink keyboardInputSink;
    private final PointerInputSink pointerInputSink;
    private final PreferenceConfiguration preferences;
    private final Host host;

    private int modifierFlags;
    private boolean waitingForAllModifiersUp;
    private int specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;

    public KeyboardInputController(
            KeyboardTranslator translator,
            GamepadInputHandler gamepadInputHandler,
            KeyboardInputSink keyboardInputSink,
            PointerInputSink pointerInputSink,
            PreferenceConfiguration preferences,
            Host host) {
        this.translator = Objects.requireNonNull(
                translator,
                "translator");
        this.gamepadInputHandler = Objects.requireNonNull(
                gamepadInputHandler,
                "gamepadInputHandler");
        this.keyboardInputSink = Objects.requireNonNull(
                keyboardInputSink,
                "keyboardInputSink");
        this.pointerInputSink = Objects.requireNonNull(
                pointerInputSink,
                "pointerInputSink");
        this.preferences = Objects.requireNonNull(
                preferences,
                "preferences");
        this.host = Objects.requireNonNull(host, "host");
    }

    public void resetModifierState() {
        modifierFlags = 0;
    }

    public boolean handleKeyDown(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (isVirtualNavigationKey(event)) {
            return false;
        }

        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            host.onNonBackKeyDown();
        }

        if (isSyntheticMouseBack(event)) {
            if (!preferences.mouseNavButtons) {
                pointerInputSink.sendMouseButtonDown(
                        MouseButtonPacket.BUTTON_RIGHT);
            }
            return true;
        }

        boolean handled = false;
        if (gamepadInputHandler.isGameControllerDevice(
                event.getDevice())) {
            handled = gamepadInputHandler.handleButtonDown(event);
        }
        if (handled) {
            return true;
        }

        if (handleSpecialKeys(event.getKeyCode(), true)) {
            return true;
        }
        if (!host.isInputGrabbed()) {
            return false;
        }

        short translated = translator.translate(
                event.getKeyCode(),
                event.getDeviceId());
        if (translated == 0) {
            int unicodeChar = event.getUnicodeChar();
            if (isSendableUnicodeCharacter(unicodeChar)) {
                keyboardInputSink.sendUtf8Text(
                        String.valueOf((char) unicodeChar));
                return true;
            }
            return false;
        }

        if (event.getRepeatCount() > 0) {
            return true;
        }

        keyboardInputSink.sendKey(
                translated,
                KeyboardPacket.KEY_DOWN,
                getModifierState(event),
                getKeyboardFlags(event));
        return true;
    }

    public boolean handleKeyUp(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (isVirtualNavigationKey(event)) {
            return false;
        }

        if (isSyntheticMouseBack(event)) {
            if (!preferences.mouseNavButtons) {
                pointerInputSink.sendMouseButtonUp(
                        MouseButtonPacket.BUTTON_RIGHT);
            }
            return true;
        }

        boolean handled = false;
        if (gamepadInputHandler.isGameControllerDevice(
                event.getDevice())) {
            handled = gamepadInputHandler.handleButtonUp(event);
        }
        if (handled) {
            return true;
        }

        if (handleSpecialKeys(event.getKeyCode(), false)) {
            return true;
        }
        if (!host.isInputGrabbed()) {
            return false;
        }

        short translated = translator.translate(
                event.getKeyCode(),
                event.getDeviceId());
        if (translated == 0) {
            return isSendableUnicodeCharacter(
                    event.getUnicodeChar());
        }

        keyboardInputSink.sendKey(
                translated,
                KeyboardPacket.KEY_UP,
                getModifierState(event),
                getKeyboardFlags(event));
        return true;
    }

    public boolean handleKeyMultiple(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN ||
                event.getCharacters() == null) {
            return false;
        }

        keyboardInputSink.sendUtf8Text(event.getCharacters());
        return true;
    }

    public void sendText(String text) {
        if (text != null && !text.isEmpty()) {
            keyboardInputSink.sendUtf8Text(text);
        }
    }

    public void sendRepeatedKey(short keyCode, int count) {
        if (count <= 0) {
            return;
        }

        byte modifiers = getModifierState();
        for (int i = 0; i < count; i++) {
            keyboardInputSink.sendKey(
                    keyCode,
                    KeyboardPacket.KEY_DOWN,
                    modifiers,
                    (byte) 0);
            keyboardInputSink.sendKey(
                    keyCode,
                    KeyboardPacket.KEY_UP,
                    modifiers,
                    (byte) 0);
        }
    }

    public void sendAndroidKey(
            boolean buttonDown,
            short androidKeyCode) {
        short translated = translator.translate(androidKeyCode, -1);
        if (translated == 0 ||
                handleSpecialKeys(androidKeyCode, buttonDown)) {
            return;
        }

        keyboardInputSink.sendKey(
                translated,
                buttonDown
                        ? KeyboardPacket.KEY_DOWN
                        : KeyboardPacket.KEY_UP,
                getModifierState(),
                (byte) 0);
    }

    public void sendChord(short[] keyCodes) {
        KeyboardChordSender.send(keyboardInputSink, keyCodes);
    }

    private boolean handleSpecialKeys(
            int androidKeyCode,
            boolean down) {
        int modifierMask = getModifierMask(androidKeyCode);
        int nonModifierKeyCode = modifierMask == 0
                ? androidKeyCode
                : KeyEvent.KEYCODE_UNKNOWN;
        if (down) {
            modifierFlags |= modifierMask;
        }
        else {
            modifierFlags &= ~modifierMask;
        }

        if (waitingForAllModifiersUp ||
                specialKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            if (specialKeyCode == androidKeyCode) {
                return true;
            }
            if (modifierFlags != 0) {
                return down;
            }

            performSpecialAction();
            specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
            waitingForAllModifiersUp = false;
        }
        else if (hasLocalSpecialChordModifiers() &&
                down &&
                nonModifierKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            switch (androidKeyCode) {
                case KeyEvent.KEYCODE_Z:
                case KeyEvent.KEYCODE_Q:
                case KeyEvent.KEYCODE_C:
                    specialKeyCode = androidKeyCode;
                    waitingForAllModifiersUp = true;
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }

    private void performSpecialAction() {
        switch (specialKeyCode) {
            case KeyEvent.KEYCODE_Z:
                host.requestToggleInputGrab();
                break;

            case KeyEvent.KEYCODE_Q:
                host.requestQuit();
                break;

            case KeyEvent.KEYCODE_C:
                host.requestToggleCursorVisibility();
                break;

            default:
                break;
        }
    }

    private boolean hasLocalSpecialChordModifiers() {
        int mask = KeyboardPacket.MODIFIER_CTRL |
                KeyboardPacket.MODIFIER_ALT |
                KeyboardPacket.MODIFIER_SHIFT;
        return (modifierFlags & mask) == mask;
    }

    private byte getModifierState(KeyEvent event) {
        byte modifiers = getModifierState();
        if (event.isShiftPressed()) {
            modifiers |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if (event.isCtrlPressed()) {
            modifiers |= KeyboardPacket.MODIFIER_CTRL;
        }
        if (event.isAltPressed()) {
            modifiers |= KeyboardPacket.MODIFIER_ALT;
        }
        if (event.isMetaPressed()) {
            modifiers |= KeyboardPacket.MODIFIER_META;
        }
        return modifiers;
    }

    private byte getModifierState() {
        return (byte) modifierFlags;
    }

    private byte getKeyboardFlags(KeyEvent event) {
        return translator.hasNormalizedMapping(
                event.getKeyCode(),
                event.getDeviceId())
                ? 0
                : MoonBridge.SS_KBE_FLAG_NON_NORMALIZED;
    }

    private static int getModifierMask(int androidKeyCode) {
        switch (androidKeyCode) {
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
                return KeyboardPacket.MODIFIER_CTRL;

            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return KeyboardPacket.MODIFIER_SHIFT;

            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                return KeyboardPacket.MODIFIER_ALT;

            case KeyEvent.KEYCODE_META_LEFT:
            case KeyEvent.KEYCODE_META_RIGHT:
                return KeyboardPacket.MODIFIER_META;

            default:
                return 0;
        }
    }

    private static boolean isVirtualNavigationKey(KeyEvent event) {
        return (event.getFlags() &
                KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0;
    }

    private static boolean isSyntheticMouseBack(KeyEvent event) {
        return PointerInputCompat.isMouseSource(event.getSource()) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK;
    }

    private static boolean isSendableUnicodeCharacter(
            int unicodeChar) {
        return (unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0 &&
                (unicodeChar &
                        KeyCharacterMap.COMBINING_ACCENT_MASK) != 0;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        translator.onInputDeviceAdded(deviceId);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        translator.onInputDeviceRemoved(deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        translator.onInputDeviceChanged(deviceId);
    }
}
