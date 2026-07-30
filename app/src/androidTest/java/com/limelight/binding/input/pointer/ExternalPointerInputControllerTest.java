package com.limelight.binding.input.pointer;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.binding.input.PointerInputCompat;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.touch.DirectContactInputController;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class ExternalPointerInputControllerTest {
    private RecordingPointerInputSink inputSink;
    private FakeInputCaptureProvider captureProvider;
    private InputSettingsState settingsState;
    private View streamView;
    private ExternalPointerInputController controller;
    private long downTimeMs;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();
        downTimeMs = SystemClock.uptimeMillis();
        inputSink = new RecordingPointerInputSink();
        captureProvider = new FakeInputCaptureProvider();
        captureProvider.enableCapture();
        settingsState = new InputSettingsState(
                InputSettings.builder().build());
        streamView = new View(context);
        streamView.layout(0, 0, 1_000, 500);

        DirectContactInputController directContactInputController =
                new DirectContactInputController(
                        streamView,
                        inputSink,
                        settingsState);
        controller = new ExternalPointerInputController(
                streamView,
                inputSink,
                captureProvider,
                directContactInputController,
                settingsState);
    }

    @Test
    public void classificationSeparatesFingerTouchscreenFromMouse() {
        MotionEvent fingerEvent = event(
                InputDevice.SOURCE_TOUCHSCREEN,
                MotionEvent.ACTION_MOVE,
                MotionEvent.TOOL_TYPE_FINGER,
                0,
                100,
                100);
        MotionEvent mouseEvent = event(
                InputDevice.SOURCE_MOUSE,
                MotionEvent.ACTION_MOVE,
                MotionEvent.TOOL_TYPE_MOUSE,
                0,
                100,
                100);

        assertTrue(ExternalPointerInputController
                .isPointerClassEvent(fingerEvent));
        assertFalse(controller.canHandle(fingerEvent));
        assertTrue(controller.canHandle(mouseEvent));
    }

    @Test
    public void inactiveCaptureConsumesWithoutProtocolOutput() {
        captureProvider.disableCapture();

        assertTrue(controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_MOUSE,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        0,
                        400,
                        200)));

        assertEquals(0, inputSink.packetCount);
    }

    @Test
    public void relativeMotionAppliesConfiguredSensitivity() {
        settingsState.replace(
                settingsState.get()
                        .toBuilder()
                        .setExternalTouchpadSensitivity(200, 50)
                        .build());
        captureProvider.hasRelativeAxes = true;
        captureProvider.relativeX = 3;
        captureProvider.relativeY = -4;

        controller.handleMotionEvent(
                null,
                event(
                        PointerInputCompat.SOURCE_MOUSE_RELATIVE,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        0,
                        0,
                        0));

        assertEquals(1, inputSink.relativeMoveCount);
        assertEquals(6, inputSink.lastDeltaX);
        assertEquals(-2, inputSink.lastDeltaY);
    }

    @Test
    public void absoluteMouseModeUsesRelativePositionProtocol() {
        settingsState.replace(
                settingsState.get()
                        .toBuilder()
                        .setAbsoluteMouseMode(true)
                        .build());
        captureProvider.hasRelativeAxes = true;
        captureProvider.relativeX = 4;
        captureProvider.relativeY = 2;

        controller.handleMotionEvent(
                null,
                event(
                        PointerInputCompat.SOURCE_MOUSE_RELATIVE,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        0,
                        0,
                        0));

        assertEquals(1, inputSink.relativePositionCount);
        assertEquals(4, inputSink.lastDeltaX);
        assertEquals(2, inputSink.lastDeltaY);
        assertEquals(1_000, inputSink.lastReferenceWidth);
        assertEquals(500, inputSink.lastReferenceHeight);
    }

    @Test
    public void viewMouseCoordinatesUseStreamReferenceSpace() {
        FrameLayout parent = new FrameLayout(streamView.getContext());
        View containingView = new View(streamView.getContext());
        parent.addView(containingView);
        parent.addView(streamView);
        containingView.layout(0, 0, 1_200, 700);
        streamView.layout(100, 50, 1_100, 550);

        controller.handleMotionEvent(
                containingView,
                event(
                        InputDevice.SOURCE_MOUSE,
                        MotionEvent.ACTION_HOVER_MOVE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        0,
                        420,
                        230));

        assertEquals(1, inputSink.absolutePositionCount);
        assertEquals(320, inputSink.lastX);
        assertEquals(180, inputSink.lastY);
        assertEquals(1_000, inputSink.lastReferenceWidth);
        assertEquals(500, inputSink.lastReferenceHeight);
    }

    @Test
    public void viewMouseCoordinatesUseCompleteSiblingTransform() {
        FrameLayout parent = new FrameLayout(streamView.getContext());
        View containingView = new View(streamView.getContext());
        parent.addView(containingView);
        parent.addView(streamView);
        containingView.layout(40, 20, 1_240, 720);
        containingView.setPivotX(0);
        containingView.setPivotY(0);
        containingView.setScaleX(2);
        containingView.setScaleY(2);
        containingView.setTranslationX(10);
        containingView.setTranslationY(5);
        streamView.layout(100, 50, 1_100, 550);
        streamView.setPivotX(0);
        streamView.setPivotY(0);
        streamView.setScaleX(0.5f);
        streamView.setScaleY(0.5f);
        streamView.setTranslationX(30);
        streamView.setTranslationY(20);

        controller.handleMotionEvent(
                containingView,
                event(
                        InputDevice.SOURCE_MOUSE,
                        MotionEvent.ACTION_HOVER_MOVE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        0,
                        50,
                        25));

        assertEquals(1, inputSink.absolutePositionCount);
        assertEquals(40, inputSink.lastX);
        assertEquals(10, inputSink.lastY);
        assertEquals(1_000, inputSink.lastReferenceWidth);
        assertEquals(500, inputSink.lastReferenceHeight);
    }

    @Test
    public void mouseButtonTransitionsEmitOneDownAndOneUp() {
        controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_MOUSE,
                        MotionEvent.ACTION_BUTTON_PRESS,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        MotionEvent.BUTTON_PRIMARY,
                        200,
                        100));
        controller.handleMotionEvent(
                streamView,
                event(
                        InputDevice.SOURCE_MOUSE,
                        MotionEvent.ACTION_BUTTON_RELEASE,
                        MotionEvent.TOOL_TYPE_MOUSE,
                        0,
                        200,
                        100));

        assertEquals(1, inputSink.buttonDownCount);
        assertEquals(1, inputSink.buttonUpCount);
        assertEquals(
                MouseButtonPacket.BUTTON_LEFT,
                inputSink.lastButtonDown);
        assertEquals(
                MouseButtonPacket.BUTTON_LEFT,
                inputSink.lastButtonUp);
    }

    @Test
    public void twoFingerTouchpadPrimaryActionBecomesRightClick() {
        int pressedButtonState =
                ExternalPointerInputController.normalizeButtonState(
                        InputDevice.SOURCE_TOUCHPAD,
                        2,
                        MotionEvent.ACTION_BUTTON_PRESS,
                        MotionEvent.ACTION_BUTTON_PRESS,
                        MotionEvent.BUTTON_PRIMARY,
                        MotionEvent.BUTTON_PRIMARY,
                        0);
        int releasedButtonState =
                ExternalPointerInputController.normalizeButtonState(
                        InputDevice.SOURCE_TOUCHPAD,
                        2,
                        MotionEvent.ACTION_BUTTON_RELEASE,
                        MotionEvent.ACTION_BUTTON_RELEASE,
                        MotionEvent.BUTTON_PRIMARY,
                        0,
                        pressedButtonState);

        assertEquals(
                MotionEvent.BUTTON_SECONDARY,
                pressedButtonState);
        assertEquals(0, releasedButtonState);
    }

    @Test
    public void relativeTwoFingerTouchpadMoveProducesScrollOnly() {
        captureProvider.hasRelativeAxes = true;
        captureProvider.relativeX = 1;
        captureProvider.relativeY = -2;

        controller.handleMotionEvent(
                null,
                event(
                        InputDevice.SOURCE_TOUCHPAD,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.TOOL_TYPE_FINGER,
                        0,
                        100,
                        100,
                        300,
                        100));

        assertEquals(0, inputSink.relativeMoveCount);
        assertEquals(0, inputSink.relativePositionCount);
        assertEquals(1, inputSink.verticalScrollCount);
        assertEquals(1, inputSink.lastVerticalScroll);
        assertEquals(0, inputSink.horizontalScrollCount);
    }

    private MotionEvent event(
            int source,
            int action,
            int toolType,
            int buttonState,
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
            pointerProperties.toolType = toolType;
            properties[i] = pointerProperties;

            MotionEvent.PointerCoords coords =
                    new MotionEvent.PointerCoords();
            coords.x = coordinates[i * 2];
            coords.y = coordinates[i * 2 + 1];
            coords.pressure = 1;
            pointerCoords[i] = coords;
        }

        return MotionEvent.obtain(
                downTimeMs,
                downTimeMs + 10,
                action,
                pointerCount,
                properties,
                pointerCoords,
                0,
                buttonState,
                1,
                1,
                0,
                0,
                source,
                0);
    }

    private static final class FakeInputCaptureProvider
            extends InputCaptureProvider {
        boolean hasRelativeAxes;
        float relativeX;
        float relativeY;

        @Override
        public boolean eventHasRelativeMouseAxes(MotionEvent event) {
            return hasRelativeAxes;
        }

        @Override
        public float getRelativeAxisX(MotionEvent event) {
            return relativeX;
        }

        @Override
        public float getRelativeAxisY(MotionEvent event) {
            return relativeY;
        }
    }

    private static final class RecordingPointerInputSink
            implements PointerInputSink {
        int packetCount;
        int absolutePositionCount;
        int relativeMoveCount;
        int relativePositionCount;
        int verticalScrollCount;
        int horizontalScrollCount;
        int buttonDownCount;
        int buttonUpCount;
        short lastX;
        short lastY;
        short lastDeltaX;
        short lastDeltaY;
        short lastReferenceWidth;
        short lastReferenceHeight;
        short lastVerticalScroll;
        byte lastButtonDown;
        byte lastButtonUp;

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
            lastReferenceWidth = referenceWidth;
            lastReferenceHeight = referenceHeight;
        }

        @Override
        public void sendMouseMove(short deltaX, short deltaY) {
            packetCount++;
            relativeMoveCount++;
            lastDeltaX = deltaX;
            lastDeltaY = deltaY;
        }

        @Override
        public void sendMouseMoveAsMousePosition(
                short deltaX,
                short deltaY,
                short referenceWidth,
                short referenceHeight) {
            packetCount++;
            relativePositionCount++;
            lastDeltaX = deltaX;
            lastDeltaY = deltaY;
            lastReferenceWidth = referenceWidth;
            lastReferenceHeight = referenceHeight;
        }

        @Override
        public void sendMouseButtonDown(byte mouseButton) {
            packetCount++;
            buttonDownCount++;
            lastButtonDown = mouseButton;
        }

        @Override
        public void sendMouseButtonUp(byte mouseButton) {
            packetCount++;
            buttonUpCount++;
            lastButtonUp = mouseButton;
        }

        @Override
        public void sendMouseHighResScroll(short delta) {
            packetCount++;
            verticalScrollCount++;
            lastVerticalScroll = delta;
        }

        @Override
        public void sendMouseHighResHScroll(short delta) {
            packetCount++;
            horizontalScrollCount++;
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
