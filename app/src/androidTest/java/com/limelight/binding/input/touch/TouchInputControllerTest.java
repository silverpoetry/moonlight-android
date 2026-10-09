package com.limelight.binding.input.touch;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class TouchInputControllerTest {
    private RecordingPointerInputSink inputSink;
    private RecordingHost host;
    private View streamView;
    private InputSettingsState settingsState;
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
        settingsState = new InputSettingsState(
                InputSettings.builder().build());
        DirectContactInputController directContactInputController =
                new DirectContactInputController(
                        streamView,
                        inputSink);
        controller = new TouchInputController(
                context,
                streamView,
                inputSink,
                directContactInputController,
                settingsState,
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
    public void absoluteModeUsesCompleteSiblingTransform() {
        FrameLayout parent = new FrameLayout(streamView.getContext());
        View backgroundView = new View(streamView.getContext());
        parent.addView(backgroundView);
        parent.addView(streamView);
        backgroundView.layout(40, 20, 1_240, 720);
        backgroundView.setPivotX(0);
        backgroundView.setPivotY(0);
        backgroundView.setScaleX(2);
        backgroundView.setScaleY(2);
        backgroundView.setTranslationX(10);
        backgroundView.setTranslationY(5);
        streamView.layout(100, 50, 1_100, 550);
        streamView.setPivotX(0);
        streamView.setPivotY(0);
        streamView.setScaleX(0.5f);
        streamView.setScaleY(0.5f);
        streamView.setTranslationX(30);
        streamView.setTranslationY(20);
        controller.setMode(TouchInputMode.ABSOLUTE_MOUSE);
        controller.setViewportZoomEnabled(true);

        assertTrue(controller.handleMotionEvent(
                backgroundView,
                event(0, MotionEvent.ACTION_DOWN, 0, 50, 25)));
        assertTrue(controller.handleMotionEvent(
                backgroundView,
                event(10, MotionEvent.ACTION_UP, 0, 50, 25)));

        assertEquals(1, inputSink.mousePositionPacketCount);
        assertEquals(40, inputSink.lastMouseX);
        assertEquals(10, inputSink.lastMouseY);
        assertEquals(1_000, inputSink.lastMouseReferenceWidth);
        assertEquals(500, inputSink.lastMouseReferenceHeight);
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
    public void enablingZoomKeepsOrdinaryClicksOnTheSelectedInputMode() {
        FrameLayout parent = new FrameLayout(streamView.getContext());
        parent.addView(streamView);
        controller.setMode(TouchInputMode.ABSOLUTE_MOUSE);
        controller.setViewportZoomEnabled(true);
        controller.handleMotionEvent(
                streamView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100));
        controller.handleMotionEvent(
                streamView,
                event(10, MotionEvent.ACTION_UP, 0, 100, 100));
        assertEquals(1, inputSink.mousePositionPacketCount);

        controller.setViewportZoomEnabled(false);
        controller.handleMotionEvent(
                streamView,
                event(300, MotionEvent.ACTION_DOWN, 0, 120, 120));
        controller.handleMotionEvent(
                streamView,
                event(310, MotionEvent.ACTION_UP, 0, 120, 120));
        assertEquals(2, inputSink.mousePositionPacketCount);
    }

    @Test
    public void localPinchDoesNotClickAndNextClickUsesTheZoomedVideoCoordinates() {
        View background = attachInputSurface();
        controller.setMode(TouchInputMode.ABSOLUTE_MOUSE);
        controller.setViewportZoomEnabled(true);
        controller.handleMotionEvent(background,
                event(0, MotionEvent.ACTION_DOWN, 0, 200, 250));
        controller.handleMotionEvent(background,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1, 200, 250, 400, 250));
        controller.handleMotionEvent(background,
                event(30, MotionEvent.ACTION_MOVE, 0, 150, 250, 450, 250));
        assertEquals(1.5f, streamView.getScaleX(), 0.001f);
        assertEquals(0, inputSink.touchpadFramePacketCount);
        controller.handleMotionEvent(background,
                event(40, MotionEvent.ACTION_POINTER_UP, 1, 150, 250, 450, 250));
        controller.handleMotionEvent(background,
                event(50, MotionEvent.ACTION_UP, 0, 150, 250));
        assertEquals(0, inputSink.mousePositionPacketCount);

        controller.handleMotionEvent(background,
                event(400, MotionEvent.ACTION_DOWN, 0, 600, 250));
        controller.handleMotionEvent(background,
                event(410, MotionEvent.ACTION_UP, 0, 600, 250));
        assertEquals(1, inputSink.mousePositionPacketCount);
        assertEquals(500, inputSink.lastMouseX);
        assertEquals(250, inputSink.lastMouseY);
    }

    @Test
    public void parallelTwoFingerMovementStillReachesTheNativeTouchpad() {
        View background = attachInputSurface();
        controller.setMode(TouchInputMode.NATIVE_TOUCHPAD);
        controller.setViewportZoomEnabled(true);
        controller.handleMotionEvent(background,
                event(0, MotionEvent.ACTION_DOWN, 0, 200, 250));
        controller.handleMotionEvent(background,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1, 200, 250, 400, 250));
        controller.handleMotionEvent(background,
                event(30, MotionEvent.ACTION_MOVE, 0, 200, 350, 400, 350));
        assertTrue(inputSink.touchpadFramePacketCount > 0);
        assertEquals(2, inputSink.lastTouchpadContactCount);
        assertEquals(1f, streamView.getScaleX(), 0f);
    }

    @Test
    public void thirdFingerReleasesPendingInputToTheDirectTouchMode() {
        View background = attachInputSurface();
        controller.setMode(TouchInputMode.MULTI_TOUCH);
        controller.setViewportZoomEnabled(true);
        controller.handleMotionEvent(background,
                event(0, MotionEvent.ACTION_DOWN, 0, 200, 250));
        controller.handleMotionEvent(background,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1, 200, 250, 400, 250));
        controller.handleMotionEvent(background,
                event(20, MotionEvent.ACTION_POINTER_DOWN, 2, 200, 250, 400, 250, 600, 250));
        assertTrue(inputSink.directTouchPacketCount >= 3);
        assertEquals(1f, streamView.getScaleX(), 0f);
    }

    @Test
    public void pinchStartingDuringKeyboardTapReplayIsRecognizedOnce() throws Exception {
        CountDownLatch nextPinchMoved = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View background = attachInputSurface();
            settingsState.replace(settingsState.get().toBuilder()
                    .setSoftKeyboardGestureFingers(3).build());
            controller.setMode(TouchInputMode.ABSOLUTE_MOUSE);
            controller.setViewportZoomEnabled(true);
            Handler mainHandler = new Handler(Looper.getMainLooper());

            // The first native frame marks the keyboard coordinator's timed
            // replay. Start a new pinch before that replay's final UP arrives.
            inputSink.onFirstMultiContactFrame = () -> mainHandler.post(() -> {
                downTimeMs = SystemClock.uptimeMillis();
                controller.handleMotionEvent(background,
                        event(0, MotionEvent.ACTION_DOWN, 0, 200, 250));
                controller.handleMotionEvent(background,
                        event(1, MotionEvent.ACTION_POINTER_DOWN, 1, 200, 250, 400, 250));
                mainHandler.postDelayed(() -> {
                    controller.handleMotionEvent(background,
                            event(SystemClock.uptimeMillis() - downTimeMs,
                                    MotionEvent.ACTION_MOVE, 0, 150, 250, 450, 250));
                    nextPinchMoved.countDown();
                }, BufferedTouchEventDispatcher.MIN_NATIVE_TAP_HOLD_MS + 20);
            });

            downTimeMs = SystemClock.uptimeMillis();
            controller.handleMotionEvent(background,
                    event(0, MotionEvent.ACTION_DOWN, 0, 200, 250));
            controller.handleMotionEvent(background,
                    event(10, MotionEvent.ACTION_POINTER_DOWN, 1, 200, 250, 400, 250));
            controller.handleMotionEvent(background,
                    event(20, MotionEvent.ACTION_POINTER_UP, 1, 200, 250, 400, 250));
            controller.handleMotionEvent(background,
                    event(21, MotionEvent.ACTION_UP, 0, 200, 250));
        });

        assertTrue("Queued pinch was not dispatched", nextPinchMoved.await(2, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                assertEquals(1.5f, streamView.getScaleX(), 0.001f));
    }

    private View attachInputSurface() {
        FrameLayout parent = new FrameLayout(streamView.getContext());
        View background = new View(streamView.getContext());
        parent.addView(background);
        parent.addView(streamView);
        parent.layout(0, 0, 1000, 500);
        background.layout(0, 0, 1000, 500);
        streamView.layout(0, 0, 1000, 500);
        return background;
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
        int lastMouseX;
        int lastMouseY;
        int lastMouseReferenceWidth;
        int lastMouseReferenceHeight;
        Runnable onFirstMultiContactFrame;

        @Override
        public void sendMousePosition(
                short x,
                short y,
                short referenceWidth,
                short referenceHeight) {
            packetCount++;
            mousePositionPacketCount++;
            lastMouseX = x;
            lastMouseY = y;
            lastMouseReferenceWidth = referenceWidth;
            lastMouseReferenceHeight = referenceHeight;
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
            if (contactCount == 2 && onFirstMultiContactFrame != null) {
                Runnable callback = onFirstMultiContactFrame;
                onFirstMultiContactFrame = null;
                callback.run();
            }
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
