package com.limelight.binding.input;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class KeyboardInputControllerTest {
    private RecordingGamepadHandler gamepadHandler;
    private RecordingKeyboardInputSink keyboardSink;
    private RecordingPointerInputSink pointerSink;
    private RecordingHost host;
    private KeyboardInputController controller;
    private long downTimeMs;

    @Before
    public void setUp() {
        downTimeMs = SystemClock.uptimeMillis();
        gamepadHandler = new RecordingGamepadHandler();
        keyboardSink = new RecordingKeyboardInputSink();
        pointerSink = new RecordingPointerInputSink();
        host = new RecordingHost();
        host.inputGrabbed = true;
        controller = new KeyboardInputController(
                new KeyboardTranslator(),
                gamepadHandler,
                keyboardSink,
                pointerSink,
                new InputSettingsState(
                        InputSettings.builder().build()),
                host);
    }

    @Test
    public void regularKeyDownAndUpPreserveProtocolTrace() {
        assertTrue(controller.handleKeyDown(
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)));
        assertTrue(controller.handleKeyUp(
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A)));

        assertEquals(2, keyboardSink.keyPackets.size());
        KeyPacket down = keyboardSink.keyPackets.get(0);
        KeyPacket up = keyboardSink.keyPackets.get(1);
        assertEquals(
                new KeyboardTranslator().translate(
                        KeyEvent.KEYCODE_A,
                        -1),
                down.keyCode);
        assertEquals(KeyboardPacket.KEY_DOWN, down.action);
        assertEquals(KeyboardPacket.KEY_UP, up.action);
        assertEquals(0, down.modifiers);
        assertEquals(0, up.modifiers);
    }

    @Test
    public void globalModifierStateIsAppliedToFollowingKey() {
        controller.handleKeyDown(
                keyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_SHIFT_LEFT));
        controller.handleKeyDown(
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A));
        controller.handleKeyUp(
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A));
        controller.handleKeyUp(
                keyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_SHIFT_LEFT));

        assertEquals(4, keyboardSink.keyPackets.size());
        assertEquals(
                KeyboardPacket.MODIFIER_SHIFT,
                keyboardSink.keyPackets.get(1).modifiers);
        assertEquals(
                KeyboardPacket.MODIFIER_SHIFT,
                keyboardSink.keyPackets.get(2).modifiers);
        assertEquals(
                0,
                keyboardSink.keyPackets.get(3).modifiers);
    }

    @Test
    public void repeatedMappedKeyDownIsConsumedWithoutDuplicatePacket() {
        KeyEvent repeated = new KeyEvent(
                downTimeMs,
                downTimeMs + 10,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A,
                1);

        assertTrue(controller.handleKeyDown(repeated));
        assertEquals(0, keyboardSink.keyPackets.size());
    }

    @Test
    public void syntheticMouseBackMapsToRightMouseButton() {
        assertTrue(controller.handleKeyDown(
                keyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_BACK,
                        PointerInputCompat.SOURCE_MOUSE_RELATIVE)));
        assertTrue(controller.handleKeyUp(
                keyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_BACK,
                        PointerInputCompat.SOURCE_MOUSE_RELATIVE)));

        assertEquals(1, pointerSink.buttonDownCount);
        assertEquals(1, pointerSink.buttonUpCount);
        assertEquals(
                MouseButtonPacket.BUTTON_RIGHT,
                pointerSink.lastButtonDown);
        assertEquals(
                MouseButtonPacket.BUTTON_RIGHT,
                pointerSink.lastButtonUp);
    }

    @Test
    public void gamepadButtonHandlerHasPrecedenceOverKeyboard() {
        gamepadHandler.gameControllerDevice = true;
        gamepadHandler.buttonDownResult = true;

        assertTrue(controller.handleKeyDown(
                keyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_UP)));

        assertEquals(1, gamepadHandler.buttonDownCount);
        assertEquals(0, keyboardSink.keyPackets.size());
    }

    @Test
    public void localSpecialChordFiresAfterEveryModifierIsReleased() {
        controller.handleKeyDown(keyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_CTRL_LEFT));
        controller.handleKeyDown(keyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ALT_LEFT));
        controller.handleKeyDown(keyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_SHIFT_LEFT));
        controller.handleKeyDown(keyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_C));
        controller.handleKeyUp(keyEvent(
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_C));
        controller.handleKeyUp(keyEvent(
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_SHIFT_LEFT));
        controller.handleKeyUp(keyEvent(
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_ALT_LEFT));

        assertEquals(0, host.toggleCursorCount);
        controller.handleKeyUp(keyEvent(
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_CTRL_LEFT));

        assertEquals(1, host.toggleCursorCount);
        assertEquals(6, keyboardSink.keyPackets.size());
        short translatedC = new KeyboardTranslator().translate(
                KeyEvent.KEYCODE_C,
                -1);
        for (KeyPacket packet : keyboardSink.keyPackets) {
            assertFalse(packet.keyCode == translatedC);
        }
    }

    @Test
    public void ungrabbedKeyboardInputPassesThroughWithoutProtocolOutput() {
        host.inputGrabbed = false;

        assertFalse(controller.handleKeyDown(
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)));
        assertFalse(controller.handleKeyUp(
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A)));

        assertEquals(0, keyboardSink.keyPackets.size());
    }

    @Test
    public void actionMultipleUnknownKeySendsCharactersAsUtf8() {
        KeyEvent event = new KeyEvent(
                downTimeMs,
                "text",
                0,
                0);

        assertTrue(controller.handleKeyMultiple(event));
        assertEquals(1, keyboardSink.textPackets.size());
        assertEquals("text", keyboardSink.textPackets.get(0));
    }

    private KeyEvent keyEvent(int action, int keyCode) {
        return keyEvent(action, keyCode, InputDevice.SOURCE_KEYBOARD);
    }

    private KeyEvent keyEvent(
            int action,
            int keyCode,
            int source) {
        return new KeyEvent(
                downTimeMs,
                downTimeMs + 10,
                action,
                keyCode,
                0,
                0,
                -1,
                0,
                0,
                source);
    }

    private static final class RecordingHost
            implements KeyboardInputController.Host {
        boolean inputGrabbed;
        int toggleCursorCount;

        @Override
        public boolean isInputGrabbed() {
            return inputGrabbed;
        }

        @Override
        public void onNonBackKeyDown() {
        }

        @Override
        public void requestToggleInputGrab() {
        }

        @Override
        public void requestQuit() {
        }

        @Override
        public void requestToggleCursorVisibility() {
            toggleCursorCount++;
        }
    }

    private static final class RecordingGamepadHandler
            implements GamepadInputHandler {
        boolean gameControllerDevice;
        boolean buttonDownResult;
        int buttonDownCount;

        @Override
        public boolean isGameControllerDevice(InputDevice device) {
            return gameControllerDevice;
        }

        @Override
        public boolean handleButtonDown(KeyEvent event) {
            buttonDownCount++;
            return buttonDownResult;
        }

        @Override
        public boolean handleButtonUp(KeyEvent event) {
            return false;
        }

        @Override
        public boolean handleMotionEvent(MotionEvent event) {
            return false;
        }

        @Override
        public boolean tryHandleTouchpadEvent(MotionEvent event) {
            return false;
        }
    }

    private static final class RecordingKeyboardInputSink
            implements KeyboardInputSink {
        final List<KeyPacket> keyPackets = new ArrayList<>();
        final List<String> textPackets = new ArrayList<>();

        @Override
        public void sendKey(
                short keyCode,
                byte keyAction,
                byte modifiers,
                byte flags) {
            keyPackets.add(
                    new KeyPacket(
                            keyCode,
                            keyAction,
                            modifiers,
                            flags));
        }

        @Override
        public void sendUtf8Text(String text) {
            textPackets.add(text);
        }
    }

    private static final class KeyPacket {
        final short keyCode;
        final byte action;
        final byte modifiers;
        final byte flags;

        private KeyPacket(
                short keyCode,
                byte action,
                byte modifiers,
                byte flags) {
            this.keyCode = keyCode;
            this.action = action;
            this.modifiers = modifiers;
            this.flags = flags;
        }
    }

    private static final class RecordingPointerInputSink
            implements PointerInputSink {
        int buttonDownCount;
        int buttonUpCount;
        byte lastButtonDown;
        byte lastButtonUp;

        @Override
        public void sendMousePosition(
                short x,
                short y,
                short referenceWidth,
                short referenceHeight) {
        }

        @Override
        public void sendMouseMove(short deltaX, short deltaY) {
        }

        @Override
        public void sendMouseMoveAsMousePosition(
                short deltaX,
                short deltaY,
                short referenceWidth,
                short referenceHeight) {
        }

        @Override
        public void sendMouseButtonDown(byte mouseButton) {
            buttonDownCount++;
            lastButtonDown = mouseButton;
        }

        @Override
        public void sendMouseButtonUp(byte mouseButton) {
            buttonUpCount++;
            lastButtonUp = mouseButton;
        }

        @Override
        public void sendMouseHighResScroll(short delta) {
        }

        @Override
        public void sendMouseHighResHScroll(short delta) {
        }

        @Override
        public int sendTouchEvent(
                byte eventType,
                int pointerId,
                float x,
                float y,
                float pressureOrDistance,
                float contactAreaMajor,
                float contactAreaMinor,
                short rotation) {
            return 0;
        }

        @Override
        public int sendPenEvent(
                byte eventType,
                byte toolType,
                byte penButtons,
                float x,
                float y,
                float pressureOrDistance,
                float contactAreaMajor,
                float contactAreaMinor,
                short rotation,
                byte tilt) {
            return 0;
        }

        @Override
        public int sendTouchpadFrameEvent(
                byte contactCount,
                byte[] eventTypes,
                int[] pointerIds,
                float[] x,
                float[] y,
                float[] pressure,
                short rotation,
                short deviceWidthMm,
                short deviceHeightMm,
                byte buttonState) {
            return 0;
        }

        @Override
        public int sendTouchpadEvent(
                byte eventType,
                int pointerId,
                float x,
                float y,
                float pressure,
                float contactAreaMajor,
                float contactAreaMinor,
                short rotation,
                short deviceWidthMm,
                short deviceHeightMm,
                byte buttonState) {
            return 0;
        }
    }
}
