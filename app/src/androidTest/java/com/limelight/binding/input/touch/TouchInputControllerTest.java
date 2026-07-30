package com.limelight.binding.input.touch;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class TouchInputControllerTest {
    private RecordingPointerInputSink inputSink;
    private RecordingHost host;
    private View streamView;
    private PreferenceConfiguration preferences;
    private TouchInputController controller;
    private long downTimeMs;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();
        downTimeMs = SystemClock.uptimeMillis();
        inputSink = new RecordingPointerInputSink();
        host = new RecordingHost();
        streamView = new View(context);
        streamView.layout(0, 0, 1_000, 500);
        preferences = new PreferenceConfiguration();
        preferences.mouseTouchPadSensitityX = 100;
        preferences.mouseTouchPadSensitityY = 100;
        preferences.quickSoftKeyboardFingers = 0;
        preferences.barometerForcePressThresholdHpa = 0.18f;
        preferences.barometerForcePressMinimumDurationMs = 100;
        DirectContactInputController directContactInputController =
                new DirectContactInputController(
                        streamView,
                        inputSink,
                        preferences);
        controller = new TouchInputController(
                context,
                streamView,
                inputSink,
                directContactInputController,
                preferences,
                host);
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.destroy();
        }
    }

    @Test
    public void disabledModeConsumesFingerInputWithoutProtocolOutput() {
        controller.setMode(TouchInputMode.DISABLED);

        assertTrue(controller.handleMotionEvent(
                streamView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        assertTrue(controller.handleMotionEvent(
                streamView,
                event(10, MotionEvent.ACTION_UP, 0, 100, 100)));

        assertEquals(0, inputSink.packetCount);
    }

    @Test
    public void absoluteModeUsesLegacyPointerPath() {
        controller.setMode(TouchInputMode.ABSOLUTE_MOUSE);

        assertTrue(controller.handleMotionEvent(
                streamView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        assertTrue(controller.handleMotionEvent(
                streamView,
                event(10, MotionEvent.ACTION_UP, 0, 100, 100)));

        assertEquals(1, inputSink.mousePositionPacketCount);
        assertEquals(0, inputSink.touchpadFramePacketCount);
    }

    @Test
    public void multiTouchModeDelegatesToDirectTouchPathFirst() {
        controller.setMode(TouchInputMode.MULTI_TOUCH);

        assertTrue(controller.handleMotionEvent(
                streamView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));

        assertEquals(1, inputSink.directTouchPacketCount);
    }

    @Test
    public void nativeTouchpadModePromotesSecondContactAtomically() {
        controller.setMode(TouchInputMode.NATIVE_TOUCHPAD);

        controller.handleMotionEvent(
                streamView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100));
        assertTrue(controller.handleMotionEvent(
                streamView,
                event(
                        10,
                        MotionEvent.ACTION_POINTER_DOWN,
                        1,
                        100,
                        100,
                        700,
                        300)));

        assertEquals(1, inputSink.touchpadFramePacketCount);
        assertEquals(2, inputSink.lastTouchpadContactCount);
    }

    @Test
    public void suspendedInputResumesWithoutChangingMode() {
        controller.setMode(TouchInputMode.ABSOLUTE_MOUSE);
        controller.setInputSuspended(true);
        controller.handleMotionEvent(
                streamView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100));
        assertEquals(0, inputSink.packetCount);

        controller.setInputSuspended(false);
        controller.handleMotionEvent(
                streamView,
                event(10, MotionEvent.ACTION_DOWN, 0, 120, 120));
        controller.handleMotionEvent(
                streamView,
                event(20, MotionEvent.ACTION_UP, 0, 120, 120));
        assertEquals(1, inputSink.mousePositionPacketCount);
    }

    private MotionEvent event(
            long elapsedMs,
            int actionMasked,
            int actionIndex,
            float... coordinates) {
        int pointerCount = coordinates.length / 2;
        MotionEvent.PointerProperties[] properties =
                new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] pointerCoords =
                new MotionEvent.PointerCoords[pointerCount];

        for (int i = 0; i < pointerCount; i++) {
            MotionEvent.PointerProperties pointerProperties =
                    new MotionEvent.PointerProperties();
            pointerProperties.id = i;
            pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[i] = pointerProperties;

            MotionEvent.PointerCoords coords =
                    new MotionEvent.PointerCoords();
            coords.x = coordinates[i * 2];
            coords.y = coordinates[i * 2 + 1];
            coords.pressure = 1;
            coords.size = 0.05f;
            coords.touchMajor = 20;
            coords.touchMinor = 16;
            pointerCoords[i] = coords;
        }

        int action = actionMasked;
        if (actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
                actionMasked == MotionEvent.ACTION_POINTER_UP) {
            action |= actionIndex <<
                    MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        }

        return MotionEvent.obtain(
                downTimeMs,
                downTimeMs + elapsedMs,
                action,
                pointerCount,
                properties,
                pointerCoords,
                0,
                0,
                1,
                1,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
    }

    private static final class RecordingHost
            implements TouchInputController.Host {
        @Override
        public void showSoftKeyboard() {
        }
    }

    private static final class RecordingPointerInputSink
            implements PointerInputSink {
        int packetCount;
        int mousePositionPacketCount;
        int directTouchPacketCount;
        int touchpadFramePacketCount;
        int lastTouchpadContactCount;

        @Override
        public void sendMousePosition(
                short x,
                short y,
                short referenceWidth,
                short referenceHeight) {
            packetCount++;
            mousePositionPacketCount++;
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
            directTouchPacketCount++;
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
            touchpadFramePacketCount++;
            lastTouchpadContactCount = contactCount;
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
