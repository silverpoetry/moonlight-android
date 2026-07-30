package com.limelight.binding.input;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.pointer.ExternalPointerInputController;
import com.limelight.binding.input.touch.DirectContactInputController;
import com.limelight.binding.input.touch.TouchInputController;
import com.limelight.binding.input.touch.TouchInputMode;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class StreamInputControllerTest {
    private RecordingGamepadHandler gamepadHandler;
    private RecordingPointerInputSink inputSink;
    private RecordingHost host;
    private StreamInputController controller;
    private View streamView;
    private long downTimeMs;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();
        downTimeMs = SystemClock.uptimeMillis();
        streamView = new View(context);
        streamView.layout(0, 0, 1_000, 500);
        inputSink = new RecordingPointerInputSink();
        PreferenceConfiguration preferences =
                new PreferenceConfiguration();
        preferences.mouseTouchPadSensitityX = 100;
        preferences.mouseTouchPadSensitityY = 100;
        preferences.externalTouchPadSensitityX = 100;
        preferences.externalTouchPadSensitityY = 100;
        preferences.externalTouchPadScrollAmount = 5;
        preferences.quickSoftKeyboardFingers = 0;
        preferences.barometerForcePressThresholdHpa = 0.18f;
        preferences.barometerForcePressMinimumDurationMs = 100;

        DirectContactInputController directContactInputController =
                new DirectContactInputController(
                        streamView,
                        inputSink,
                        preferences);
        InputCaptureProvider inputCaptureProvider =
                new InputCaptureProvider() {
                };
        inputCaptureProvider.enableCapture();
        ExternalPointerInputController
                externalPointerInputController =
                new ExternalPointerInputController(
                        streamView,
                        inputSink,
                        inputCaptureProvider,
                        directContactInputController,
                        preferences);
        TouchInputController touchInputController =
                new TouchInputController(
                        context,
                        streamView,
                        inputSink,
                        directContactInputController,
                        preferences,
                        new TouchInputController.Host() {
                            @Override
                            public void showSoftKeyboard() {
                            }
                        });
        gamepadHandler = new RecordingGamepadHandler();
        host = new RecordingHost();
        controller = new StreamInputController(
                gamepadHandler,
                externalPointerInputController,
                touchInputController,
                host);
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.destroy();
        }
    }

    @Test
    public void joystickSourceHasExclusiveRoutingPrecedence() {
        gamepadHandler.handleResult = true;
        MotionEvent event = event(
                InputDevice.SOURCE_JOYSTICK,
                MotionEvent.ACTION_MOVE,
                MotionEvent.TOOL_TYPE_UNKNOWN,
                100,
                100);

        assertTrue(controller.handleMotionEvent(null, event));
        assertEquals(1, gamepadHandler.handleCount);
        assertEquals(0, inputSink.packetCount);
    }

    @Test
    public void unhandledJoystickDoesNotFallThroughToPointerRouting() {
        gamepadHandler.handleResult = false;
        MotionEvent event = event(
                InputDevice.SOURCE_JOYSTICK,
                MotionEvent.ACTION_MOVE,
                MotionEvent.TOOL_TYPE_UNKNOWN,
                100,
                100);

        assertFalse(controller.handleMotionEvent(streamView, event));
        assertEquals(1, gamepadHandler.handleCount);
        assertEquals(0, inputSink.packetCount);
    }

    @Test
    public void externalMouseRoutesBeforeTouchscreenInput() {
        assertTrue(controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_MOUSE,
                        MotionEvent.ACTION_HOVER_MOVE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        400,
                        200)));

        assertEquals(1, inputSink.absolutePositionCount);
        assertEquals(400, inputSink.lastX);
        assertEquals(200, inputSink.lastY);
    }

    @Test
    public void fingerTouchRoutesToSelectedTouchMode() {
        controller.setTouchMode(TouchInputMode.ABSOLUTE_MOUSE);

        assertTrue(controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_TOUCHSCREEN,
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        250,
                        125)));
        assertTrue(controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_TOUCHSCREEN,
                        MotionEvent.ACTION_UP,
                        MotionEvent.TOOL_TYPE_FINGER,
                        250,
                        125)));

        assertEquals(1, inputSink.absolutePositionCount);
        assertEquals(250, inputSink.lastX);
        assertEquals(125, inputSink.lastY);
    }

    @Test
    public void uiEditingPolicyConsumesFingerWithoutProtocolOutput() {
        host.suppressTouchscreenInput = true;
        controller.setTouchMode(TouchInputMode.ABSOLUTE_MOUSE);

        assertTrue(controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_TOUCHSCREEN,
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        250,
                        125)));

        assertEquals(0, inputSink.packetCount);
    }

    @Test
    public void unknownSourceRemainsUnhandled() {
        assertFalse(controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_UNKNOWN,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_UNKNOWN,
                        0,
                        0)));

        assertEquals(0, gamepadHandler.handleCount);
        assertEquals(0, inputSink.packetCount);
    }

    private MotionEvent event(
            int source,
            int action,
            int toolType,
            float x,
            float y) {
        MotionEvent.PointerProperties properties =
                new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = toolType;
        MotionEvent.PointerCoords coordinates =
                new MotionEvent.PointerCoords();
        coordinates.x = x;
        coordinates.y = y;
        coordinates.pressure = 1;
        coordinates.touchMajor = 20;
        coordinates.touchMinor = 16;
        return MotionEvent.obtain(
                downTimeMs,
                downTimeMs + 10,
                action,
                1,
                new MotionEvent.PointerProperties[] {properties},
                new MotionEvent.PointerCoords[] {coordinates},
                0,
                0,
                1,
                1,
                0,
                0,
                source,
                0);
    }

    private static final class RecordingGamepadHandler
            implements GamepadInputHandler {
        int handleCount;
        boolean handleResult;

        @Override
        public boolean isGameControllerDevice(InputDevice device) {
            return false;
        }

        @Override
        public boolean handleButtonDown(
                android.view.KeyEvent event) {
            return false;
        }

        @Override
        public boolean handleButtonUp(
                android.view.KeyEvent event) {
            return false;
        }

        @Override
        public boolean handleMotionEvent(MotionEvent event) {
            handleCount++;
            return handleResult;
        }

        @Override
        public boolean tryHandleTouchpadEvent(MotionEvent event) {
            return false;
        }
    }

    private static final class RecordingHost
            implements StreamInputController.Host {
        boolean suppressTouchscreenInput;

        @Override
        public boolean shouldSuppressTouchscreenInput() {
            return suppressTouchscreenInput;
        }
    }

    private static final class RecordingPointerInputSink
            implements PointerInputSink {
        int packetCount;
        int absolutePositionCount;
        short lastX;
        short lastY;

        @Override
        public void sendMousePosition(
                short x,
                short y,
                short referenceWidth,
                short referenceHeight) {
            packetCount++;
            absolutePositionCount++;
            lastX = x;
            lastY = y;
        }

        @Override
        public void sendMouseMove(short deltaX, short deltaY) {
            packetCount++;
        }

        @Override
        public void sendMouseMoveAsMousePosition(
                short deltaX,
                short deltaY,
                short referenceWidth,
                short referenceHeight) {
            packetCount++;
        }

        @Override
        public void sendMouseButtonDown(byte mouseButton) {
            packetCount++;
        }

        @Override
        public void sendMouseButtonUp(byte mouseButton) {
            packetCount++;
        }

        @Override
        public void sendMouseHighResScroll(short delta) {
            packetCount++;
        }

        @Override
        public void sendMouseHighResHScroll(short delta) {
            packetCount++;
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
            packetCount++;
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
            packetCount++;
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
            packetCount++;
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
            packetCount++;
            return 0;
        }
    }
}
