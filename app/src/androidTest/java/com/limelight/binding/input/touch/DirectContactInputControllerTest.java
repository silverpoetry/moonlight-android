package com.limelight.binding.input.touch;

import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class DirectContactInputControllerTest {
    private static final long DOWN_TIME_MS = 1_000;

    private View streamView;
    private RecordingPointerInputSink inputSink;
    private InputSettingsState settingsState;
    private DirectContactInputController controller;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();
        streamView = new View(context);
        streamView.layout(0, 0, 1_000, 500);
        inputSink = new RecordingPointerInputSink();
        settingsState = new InputSettingsState(
                InputSettings.builder().build());
        controller = new DirectContactInputController(
                streamView,
                inputSink,
                settingsState);
    }

    @Test
    public void touchCoordinatesAreNormalizedToStreamView() {
        assertTrue(controller.trySendTouchEvent(
                streamView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        250,
                        100,
                        0)));

        assertEquals(MoonBridge.LI_TOUCH_EVENT_DOWN,
                inputSink.lastEventType);
        assertEquals(0, inputSink.lastPointerId);
        assertEquals(0.25f, inputSink.lastX, 0.0001f);
        assertEquals(0.2f, inputSink.lastY, 0.0001f);
        assertEquals(1, inputSink.touchPacketCount);
    }

    @Test
    public void containingViewCoordinatesUseStreamViewOrigin() {
        Context context = streamView.getContext();
        FrameLayout parent = new FrameLayout(context);
        View containingView = new View(context);
        parent.addView(containingView);
        parent.addView(streamView);
        containingView.layout(0, 0, 1_200, 700);
        streamView.layout(100, 50, 1_100, 550);

        assertTrue(controller.trySendTouchEvent(
                containingView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        350,
                        150,
                        0)));

        assertEquals(0.25f, inputSink.lastX, 0.0001f);
        assertEquals(0.2f, inputSink.lastY, 0.0001f);
    }

    @Test
    public void containingViewCoordinatesUseCompleteStreamTransform() {
        Context context = streamView.getContext();
        FrameLayout parent = new FrameLayout(context);
        View containingView = new View(context);
        parent.addView(containingView);
        parent.addView(streamView);
        containingView.layout(0, 0, 1_200, 700);
        streamView.layout(100, 50, 1_100, 550);
        streamView.setPivotX(0f);
        streamView.setPivotY(0f);
        streamView.setScaleX(0.5f);
        streamView.setScaleY(0.5f);
        streamView.setTranslationX(30f);
        streamView.setTranslationY(20f);

        assertTrue(controller.trySendTouchEvent(
                containingView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        255,
                        120,
                        0)));

        assertEquals(0.25f, inputSink.lastX, 0.0001f);
        assertEquals(0.2f, inputSink.lastY, 0.0001f);
    }

    @Test
    public void contactAreaUsesDisplayPixelsInStreamReferenceSpace() {
        Context context = streamView.getContext();
        FrameLayout parent = new FrameLayout(context);
        View containingView = new View(context);
        parent.addView(containingView);
        parent.addView(streamView);
        containingView.layout(0, 0, 1_200, 700);
        containingView.setPivotX(0);
        containingView.setPivotY(0);
        containingView.setScaleX(2);
        containingView.setScaleY(2);
        streamView.layout(100, 50, 1_100, 550);
        streamView.setPivotX(0);
        streamView.setPivotY(0);
        streamView.setScaleX(0.5f);
        streamView.setScaleY(0.5f);

        assertTrue(controller.trySendTouchEvent(
                containingView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        175,
                        75,
                        0)));

        assertEquals(
                0.06324555f,
                inputSink.lastContactAreaMajor,
                0.000001f);
        assertEquals(
                0.05059644f,
                inputSink.lastContactAreaMinor,
                0.000001f);
    }

    @Test
    public void sensitivityStateUsesMappedStreamCoordinates() {
        Context context = streamView.getContext();
        FrameLayout parent = new FrameLayout(context);
        View containingView = new View(context);
        parent.addView(containingView);
        parent.addView(streamView);
        containingView.layout(0, 0, 1_200, 700);
        streamView.layout(-200, 0, 800, 500);
        settingsState.replace(
                settingsState.get()
                        .toBuilder()
                        .setDirectTouchSensitivityEnabled(true)
                        .setDirectTouchSensitivityGlobal(false)
                        .setDirectTouchSensitivity(200, 100)
                        .build());

        assertTrue(controller.trySendTouchEvent(
                containingView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        400,
                        100,
                        0)));
        assertTrue(controller.trySendTouchEvent(
                containingView,
                event(
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_FINGER,
                        410,
                        100,
                        0)));
        assertTrue(controller.trySendTouchEvent(
                containingView,
                event(
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_FINGER,
                        420,
                        100,
                        0)));

        assertEquals(0.63f, inputSink.lastX, 0.0001f);
        assertEquals(0.2f, inputSink.lastY, 0.0001f);
    }

    @Test
    public void cancelUsesSingleCancelAllPacket() {
        assertTrue(controller.trySendTouchEvent(
                streamView,
                event(
                        MotionEvent.ACTION_CANCEL,
                        MotionEvent.TOOL_TYPE_FINGER,
                        250,
                        100,
                        0)));

        assertEquals(1, inputSink.touchPacketCount);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                inputSink.lastEventType);
        assertEquals(MoonBridge.LI_ROT_UNKNOWN,
                inputSink.lastRotation);
    }

    @Test
    public void unsupportedDirectTouchFallsBackToLegacyPath() {
        inputSink.touchResult = MoonBridge.LI_ERR_UNSUPPORTED;

        assertFalse(controller.trySendTouchEvent(
                streamView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_FINGER,
                        250,
                        100,
                        0)));
    }

    @Test
    public void stylusButtonsAndToolTypeArePreserved() {
        assertTrue(controller.trySendPenEvent(
                streamView,
                event(
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.TOOL_TYPE_STYLUS,
                        500,
                        250,
                        MotionEvent.BUTTON_STYLUS_PRIMARY)));

        assertEquals(1, inputSink.penPacketCount);
        assertEquals(MoonBridge.LI_TOOL_TYPE_PEN,
                inputSink.lastToolType);
        assertEquals(MoonBridge.LI_PEN_BUTTON_PRIMARY,
                inputSink.lastPenButtons);
        assertEquals(0.5f, inputSink.lastX, 0.0001f);
        assertEquals(0.5f, inputSink.lastY, 0.0001f);
    }

    private static MotionEvent event(
            int action,
            int toolType,
            float x,
            float y,
            int buttonState) {
        MotionEvent.PointerProperties properties =
                new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = toolType;

        MotionEvent.PointerCoords coordinates =
                new MotionEvent.PointerCoords();
        coordinates.x = x;
        coordinates.y = y;
        coordinates.pressure = 0.75f;
        coordinates.touchMajor = 20;
        coordinates.touchMinor = 16;
        coordinates.toolMajor = 20;
        coordinates.toolMinor = 16;

        return MotionEvent.obtain(
                DOWN_TIME_MS,
                DOWN_TIME_MS + 10,
                action,
                1,
                new MotionEvent.PointerProperties[] {properties},
                new MotionEvent.PointerCoords[] {coordinates},
                0,
                buttonState,
                1,
                1,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
    }

    private static final class RecordingPointerInputSink
            implements PointerInputSink {
        int touchResult;
        int touchPacketCount;
        int penPacketCount;
        byte lastEventType;
        int lastPointerId;
        byte lastToolType;
        byte lastPenButtons;
        float lastX;
        float lastY;
        float lastContactAreaMajor;
        float lastContactAreaMinor;
        short lastRotation;

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
        }

        @Override
        public void sendMouseButtonUp(byte mouseButton) {
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
            touchPacketCount++;
            lastEventType = eventType;
            lastPointerId = pointerId;
            lastX = x;
            lastY = y;
            lastContactAreaMajor = contactAreaMajor;
            lastContactAreaMinor = contactAreaMinor;
            lastRotation = rotation;
            return touchResult;
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
            penPacketCount++;
            lastEventType = eventType;
            lastToolType = toolType;
            lastPenButtons = penButtons;
            lastX = x;
            lastY = y;
            lastContactAreaMajor = contactAreaMajor;
            lastContactAreaMinor = contactAreaMinor;
            lastRotation = rotation;
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
