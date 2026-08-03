package com.limelight.binding.input.pointer;

import android.graphics.Matrix;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.binding.input.PointerInputCompat;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.touch.DirectContactInputController;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.utils.ViewCoordinateMapper;

import java.util.Locale;
import java.util.Objects;

/**
 * Owns translation of external mouse, physical touchpad, and stylus events
 * into Moonlight pointer protocol output.
 *
 * <p>The controller is confined to the Android input thread. It intentionally
 * owns all cross-event pointer state so the Activity remains a composition and
 * source-routing boundary.</p>
 */
public final class ExternalPointerInputController {
    private static final int SAMSUNG_DEX_MOUSE_SOURCE = 12_290;
    private static final float TOUCHPAD_SCROLL_FACTOR = 0.15f;
    private static final int STYLUS_DOWN_DEAD_ZONE_DELAY_MS = 100;
    private static final int STYLUS_DOWN_DEAD_ZONE_RADIUS_PX = 20;
    private static final int STYLUS_UP_DEAD_ZONE_DELAY_MS = 150;
    private static final int STYLUS_UP_DEAD_ZONE_RADIUS_PX = 50;

    private final View streamView;
    private final PointerInputSink inputSink;
    private final InputCaptureProvider inputCaptureProvider;
    private final DirectContactInputController directContactInputController;
    private final InputSettingsState settingsState;
    private final float[] mappedPosition = new float[2];
    private final Matrix streamViewInverse = new Matrix();

    private int lastButtonState;
    private float relativeScrollRemainderX;
    private float relativeScrollRemainderY;
    private float absoluteScrollX = -1;
    private float absoluteScrollY = -1;
    private long lastStylusUpTime;
    private long lastStylusDownTime;
    private float lastStylusUpX;
    private float lastStylusUpY;
    private float lastStylusDownX;
    private float lastStylusDownY;

    public ExternalPointerInputController(
            View streamView,
            PointerInputSink inputSink,
            InputCaptureProvider inputCaptureProvider,
            DirectContactInputController directContactInputController,
            InputSettingsState settingsState) {
        this.streamView = Objects.requireNonNull(
                streamView,
                "streamView");
        this.inputSink = Objects.requireNonNull(inputSink, "inputSink");
        this.inputCaptureProvider = Objects.requireNonNull(
                inputCaptureProvider,
                "inputCaptureProvider");
        this.directContactInputController = Objects.requireNonNull(
                directContactInputController,
                "directContactInputController");
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
    }

    public static boolean isPointerClassEvent(MotionEvent event) {
        Objects.requireNonNull(event, "event");
        int source = event.getSource();
        return (source & InputDevice.SOURCE_CLASS_POINTER) != 0 ||
                (source & InputDevice.SOURCE_CLASS_POSITION) != 0 ||
                source == PointerInputCompat.SOURCE_MOUSE_RELATIVE;
    }

    public boolean canHandle(MotionEvent event) {
        Objects.requireNonNull(event, "event");
        if (!isPointerClassEvent(event)) {
            return false;
        }

        int source = event.getSource();
        return PointerInputCompat.isMouseSource(source) ||
                (source & InputDevice.SOURCE_CLASS_POSITION) != 0 ||
                isMouseOrStylusTool(event) ||
                source == SAMSUNG_DEX_MOUSE_SOURCE;
    }

    /**
     * Handles an event accepted by {@link #canHandle(MotionEvent)}.
     *
     * @param eventView source view, or {@code null} for Activity-level generic
     *                  motion callbacks
     * @return {@code true} when this controller owns the event
     */
    public boolean handleMotionEvent(
            View eventView,
            MotionEvent event) {
        Objects.requireNonNull(event, "event");
        if (!canHandle(event)) {
            return false;
        }

        int source = event.getSource();
        int buttonState = normalizeButtonState(event, source);
        int changedButtons = buttonState ^ lastButtonState;

        if (!inputCaptureProvider.isCapturingActive()) {
            // Consuming prevents Android from synthesizing d-pad events from
            // an external pointer while the stream is not capturing input.
            return true;
        }

        if (inputCaptureProvider.eventHasRelativeMouseAxes(event)) {
            sendRelativeMotion(event, source);
        }
        else if ((source & InputDevice.SOURCE_CLASS_POSITION) != 0) {
            sendDeviceAbsolutePosition(event, source);
        }
        else if (eventView != null &&
                directContactInputController.trySendPenEvent(
                        eventView,
                        event)) {
            return true;
        }
        else if (eventView != null) {
            sendViewRelativeMotion(eventView, event, buttonState);
        }

        sendAxisScroll(event);
        resetRelativeScrollRemaindersIfGestureEnded(event, source);
        sendChangedButtons(changedButtons, buttonState);
        sendStylusContactButton(event);
        lastButtonState = buttonState;
        return true;
    }

    private static boolean isMouseOrStylusTool(MotionEvent event) {
        if (event.getPointerCount() < 1) {
            return false;
        }

        int toolType = event.getToolType(0);
        return toolType == MotionEvent.TOOL_TYPE_MOUSE ||
                toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                toolType == MotionEvent.TOOL_TYPE_ERASER;
    }

    private int normalizeButtonState(
            MotionEvent event,
            int source) {
        return normalizeButtonState(
                source,
                event.getPointerCount(),
                event.getActionMasked(),
                event.getAction(),
                PointerInputCompat.getActionButton(event),
                event.getButtonState(),
                lastButtonState);
    }

    static int normalizeButtonState(
            int source,
            int pointerCount,
            int actionMasked,
            int action,
            int actionButton,
            int buttonState,
            int previousButtonState) {

        // Samsung DeX reports regular clicks as ACTION_DOWN/UP without a
        // primary-button bit, while right-click uses BUTTON_SECONDARY.
        if (source == SAMSUNG_DEX_MOUSE_SOURCE) {
            if (actionMasked == MotionEvent.ACTION_DOWN) {
                buttonState |= MotionEvent.BUTTON_PRIMARY;
            }
            else if (action == MotionEvent.ACTION_UP) {
                buttonState &= ~MotionEvent.BUTTON_PRIMARY;
            }
            else {
                buttonState |=
                        previousButtonState &
                                MotionEvent.BUTTON_PRIMARY;
            }
        }

        // Some physical touchpads encode a two-finger tap as a primary action
        // button. Promote only that action to a secondary click.
        if (source == InputDevice.SOURCE_TOUCHPAD &&
                pointerCount == 2 &&
                actionButton == MotionEvent.BUTTON_PRIMARY) {
            if (actionMasked ==
                    MotionEvent.ACTION_BUTTON_PRESS) {
                buttonState |= MotionEvent.BUTTON_SECONDARY;
            }
            else if (actionMasked ==
                    MotionEvent.ACTION_BUTTON_RELEASE) {
                buttonState &= ~MotionEvent.BUTTON_SECONDARY;
            }

            buttonState &= ~MotionEvent.BUTTON_PRIMARY;
            buttonState |=
                    previousButtonState &
                            MotionEvent.BUTTON_PRIMARY;
        }

        return buttonState;
    }

    private void sendRelativeMotion(
            MotionEvent event,
            int source) {
        InputSettings settings = settingsState.get();
        float rawDeltaX =
                inputCaptureProvider.getRelativeAxisX(event);
        float rawDeltaY =
                inputCaptureProvider.getRelativeAxisY(event);
        short deltaX = (short) (
                rawDeltaX *
                        settings.getExternalTouchpadSensitivityX() *
                        0.01f);
        short deltaY = (short) (
                rawDeltaY *
                        settings.getExternalTouchpadSensitivityY() *
                        0.01f);
        if (deltaX == 0 && deltaY == 0) {
            return;
        }

        if (isTwoFingerTouchpadMove(event, source)) {
            accumulateRelativeTouchpadScroll(
                    rawDeltaX,
                    rawDeltaY);
            return;
        }

        relativeScrollRemainderX = 0;
        relativeScrollRemainderY = 0;
        if (settings.isAbsoluteMouseMode()) {
            inputSink.sendMouseMoveAsMousePosition(
                    deltaX,
                    deltaY,
                    (short) streamView.getWidth(),
                    (short) streamView.getHeight());
        }
        else {
            inputSink.sendMouseMove(deltaX, deltaY);
        }
    }

    private static boolean isTwoFingerTouchpadMove(
            MotionEvent event,
            int source) {
        return source == InputDevice.SOURCE_TOUCHPAD &&
                event.getPointerCount() == 2 &&
                event.getActionMasked() == MotionEvent.ACTION_MOVE;
    }

    private void accumulateRelativeTouchpadScroll(
            float rawDeltaX,
            float rawDeltaY) {
        float scrollFactor =
                settingsState.get().getExternalTouchpadScrollAmount() *
                        TOUCHPAD_SCROLL_FACTOR;
        relativeScrollRemainderX += -rawDeltaX * scrollFactor;
        relativeScrollRemainderY += -rawDeltaY * scrollFactor;

        short horizontalScroll = 0;
        short verticalScroll = 0;
        if (Math.abs(relativeScrollRemainderX) >= 1) {
            horizontalScroll = (short) relativeScrollRemainderX;
            relativeScrollRemainderX -= horizontalScroll;
        }
        if (Math.abs(relativeScrollRemainderY) >= 1) {
            verticalScroll = (short) relativeScrollRemainderY;
            relativeScrollRemainderY -= verticalScroll;
        }

        if (verticalScroll != 0) {
            inputSink.sendMouseHighResScroll(verticalScroll);
        }
        if (horizontalScroll != 0) {
            inputSink.sendMouseHighResHScroll(horizontalScroll);
        }
    }

    private void sendDeviceAbsolutePosition(
            MotionEvent event,
            int source) {
        InputDevice device = event.getDevice();
        if (device == null) {
            return;
        }

        InputDevice.MotionRange xRange =
                device.getMotionRange(MotionEvent.AXIS_X, source);
        InputDevice.MotionRange yRange =
                device.getMotionRange(MotionEvent.AXIS_Y, source);
        if (xRange == null || yRange == null ||
                xRange.getMin() != 0 || yRange.getMin() != 0) {
            return;
        }

        int xMax = (int) xRange.getMax();
        int yMax = (int) yRange.getMax();
        if (xMax <= Short.MAX_VALUE && yMax <= Short.MAX_VALUE) {
            inputSink.sendMousePosition(
                    (short) event.getX(),
                    (short) event.getY(),
                    (short) xMax,
                    (short) yMax);
        }
    }

    private void sendViewRelativeMotion(
            View eventView,
            MotionEvent event,
            int buttonState) {
        if (shouldUseAbsoluteTouchpadScroll(event, buttonState)) {
            float currentX = event.getX();
            float currentY = event.getY();
            if (absoluteScrollX != -1 && absoluteScrollY != -1) {
                float deltaX = currentX - absoluteScrollX;
                float deltaY = currentY - absoluteScrollY;
                short verticalScroll = (short) (deltaY * 4);
                short horizontalScroll = (short) (-deltaX * 4);
                if (Math.abs(verticalScroll) >
                        Math.abs(horizontalScroll)) {
                    inputSink.sendMouseHighResScroll(verticalScroll);
                }
                else if (Math.abs(horizontalScroll) >
                        Math.abs(verticalScroll)) {
                    inputSink.sendMouseHighResHScroll(horizontalScroll);
                }
            }

            absoluteScrollX = currentX;
            absoluteScrollY = currentY;
            return;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            absoluteScrollX = -1;
            absoluteScrollY = -1;
        }
        sendAbsoluteMousePosition(eventView, event);
    }

    private void sendAbsoluteMousePosition(
            View eventView,
            MotionEvent event) {
        float eventX = event.getX(0);
        float eventY = event.getY(0);
        if (eventView != streamView) {
            mappedPosition[0] = eventX;
            mappedPosition[1] = eventY;
            if (!ViewCoordinateMapper.mapPointBetweenSiblings(
                    eventView,
                    streamView,
                    mappedPosition,
                    streamViewInverse)) {
                return;
            }
            eventX = mappedPosition[0];
            eventY = mappedPosition[1];
        }

        if (isSingleStylusContact(event) &&
                isInsideStylusDeadZone(event, eventX, eventY)) {
            return;
        }

        eventX = clamp(eventX, 0, streamView.getWidth());
        eventY = clamp(eventY, 0, streamView.getHeight());
        inputSink.sendMousePosition(
                (short) eventX,
                (short) eventY,
                (short) streamView.getWidth(),
                (short) streamView.getHeight());
    }

    private static boolean isSingleStylusContact(
            MotionEvent event) {
        if (event.getPointerCount() != 1 ||
                event.getActionIndex() != 0) {
            return false;
        }

        int toolType = event.getToolType(0);
        return toolType == MotionEvent.TOOL_TYPE_ERASER ||
                toolType == MotionEvent.TOOL_TYPE_STYLUS;
    }

    private boolean isInsideStylusDeadZone(
            MotionEvent event,
            float eventX,
            float eventY) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_HOVER_MOVE:
                return event.getEventTime() - lastStylusUpTime <=
                        STYLUS_UP_DEAD_ZONE_DELAY_MS &&
                        distance(
                                eventX,
                                eventY,
                                lastStylusUpX,
                                lastStylusUpY) <=
                                STYLUS_UP_DEAD_ZONE_RADIUS_PX;

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                return event.getEventTime() - lastStylusDownTime <=
                        STYLUS_DOWN_DEAD_ZONE_DELAY_MS &&
                        distance(
                                eventX,
                                eventY,
                                lastStylusDownX,
                                lastStylusDownY) <=
                                STYLUS_DOWN_DEAD_ZONE_RADIUS_PX;

            default:
                return false;
        }
    }

    private static double distance(
            float firstX,
            float firstY,
            float secondX,
            float secondY) {
        return Math.sqrt(
                Math.pow(firstX - secondX, 2) +
                        Math.pow(firstY - secondY, 2));
    }

    private void sendAxisScroll(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_SCROLL) {
            return;
        }

        inputSink.sendMouseHighResScroll(
                (short) (
                        event.getAxisValue(
                                MotionEvent.AXIS_VSCROLL) * 120));
        inputSink.sendMouseHighResHScroll(
                (short) (
                        event.getAxisValue(
                                MotionEvent.AXIS_HSCROLL) * 120));
    }

    private void resetRelativeScrollRemaindersIfGestureEnded(
            MotionEvent event,
            int source) {
        if (source == InputDevice.SOURCE_TOUCHPAD &&
                event.getActionMasked() != MotionEvent.ACTION_MOVE) {
            relativeScrollRemainderX = 0;
            relativeScrollRemainderY = 0;
        }
    }

    private void sendChangedButtons(
            int changedButtons,
            int buttonState) {
        sendChangedButton(
                changedButtons,
                buttonState,
                MotionEvent.BUTTON_PRIMARY,
                MouseButtonPacket.BUTTON_LEFT);
        sendChangedButton(
                changedButtons,
                buttonState,
                MotionEvent.BUTTON_SECONDARY |
                        PointerInputCompat.BUTTON_STYLUS_PRIMARY,
                MouseButtonPacket.BUTTON_RIGHT);
        sendChangedButton(
                changedButtons,
                buttonState,
                MotionEvent.BUTTON_TERTIARY |
                        PointerInputCompat.BUTTON_STYLUS_SECONDARY,
                MouseButtonPacket.BUTTON_MIDDLE);

        if (settingsState.get()
                .areMouseNavigationButtonsEnabled()) {
            sendChangedButton(
                    changedButtons,
                    buttonState,
                    MotionEvent.BUTTON_BACK,
                    MouseButtonPacket.BUTTON_X1);
            sendChangedButton(
                    changedButtons,
                    buttonState,
                    MotionEvent.BUTTON_FORWARD,
                    MouseButtonPacket.BUTTON_X2);
        }
    }

    private void sendChangedButton(
            int changedButtons,
            int buttonState,
            int androidButtonMask,
            byte moonlightButton) {
        if ((changedButtons & androidButtonMask) == 0) {
            return;
        }

        if ((buttonState & androidButtonMask) != 0) {
            inputSink.sendMouseButtonDown(moonlightButton);
        }
        else {
            inputSink.sendMouseButtonUp(moonlightButton);
        }
    }

    private void sendStylusContactButton(MotionEvent event) {
        if (event.getPointerCount() != 1 ||
                event.getActionIndex() != 0) {
            return;
        }

        int toolType = event.getToolType(0);
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS &&
                toolType != MotionEvent.TOOL_TYPE_ERASER) {
            return;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastStylusDownTime = event.getEventTime();
            lastStylusDownX = event.getX(0);
            lastStylusDownY = event.getY(0);
            inputSink.sendMouseButtonDown(
                    toolType == MotionEvent.TOOL_TYPE_STYLUS
                            ? MouseButtonPacket.BUTTON_LEFT
                            : MouseButtonPacket.BUTTON_RIGHT);
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            lastStylusUpTime = event.getEventTime();
            lastStylusUpX = event.getX(0);
            lastStylusUpY = event.getY(0);
            inputSink.sendMouseButtonUp(
                    toolType == MotionEvent.TOOL_TYPE_STYLUS
                            ? MouseButtonPacket.BUTTON_LEFT
                            : MouseButtonPacket.BUTTON_RIGHT);
        }
    }

    private boolean shouldUseAbsoluteTouchpadScroll(
            MotionEvent event,
            int buttonState) {
        if (event.getActionMasked() != MotionEvent.ACTION_MOVE ||
                buttonState != 0 ||
                event.getPointerCount() == 0) {
            return false;
        }

        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            return true;
        }

        InputDevice device = event.getDevice();
        if (device == null || device.getName() == null) {
            return false;
        }

        String name = device.getName().toLowerCase(Locale.ROOT);
        return name.contains("touchpad") ||
                name.contains("trackpad") ||
                name.contains("xiaomi");
    }

    private static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }
}
