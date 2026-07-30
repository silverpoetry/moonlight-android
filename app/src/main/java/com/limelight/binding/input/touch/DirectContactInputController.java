package com.limelight.binding.input.touch;

import android.util.SparseArray;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.binding.input.PointerInputCompat;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.Objects;

/**
 * Translates Android touchscreen and stylus contacts into Moonlight's direct
 * touch and pen protocols.
 *
 * <p>This controller is confined to the Android input thread. It owns the
 * per-pointer sensitivity state and reuses its geometry scratch object to
 * avoid allocations on move events.</p>
 */
public final class DirectContactInputController {
    private final View streamView;
    private final PointerInputSink inputSink;
    private final PreferenceConfiguration preferences;
    private final SparseArray<SensitivityState> sensitivityStates =
            new SparseArray<>();
    private final ContactGeometry geometry = new ContactGeometry();
    private final int[] eventViewLocation = new int[2];
    private final int[] streamViewLocation = new int[2];

    public DirectContactInputController(
            View streamView,
            PointerInputSink inputSink,
            PreferenceConfiguration preferences) {
        this.streamView = Objects.requireNonNull(
                streamView,
                "streamView");
        this.inputSink = Objects.requireNonNull(inputSink, "inputSink");
        this.preferences = Objects.requireNonNull(
                preferences,
                "preferences");
    }

    public void cancel() {
        sensitivityStates.clear();
    }

    public boolean trySendTouchEvent(
            View eventView,
            MotionEvent event) {
        Objects.requireNonNull(eventView, "eventView");
        Objects.requireNonNull(event, "event");
        if (!isStreamViewReady()) {
            return false;
        }

        byte eventType = getEventType(event);
        if (eventType < 0) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (!sendTouchEventForPointer(
                            eventView,
                            event,
                            eventType,
                            i)) {
                        return false;
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                sensitivityStates.clear();
                return inputSink.sendTouchEvent(
                        MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        MoonBridge.LI_ROT_UNKNOWN) !=
                        MoonBridge.LI_ERR_UNSUPPORTED;

            default:
                return sendTouchEventForPointer(
                        eventView,
                        event,
                        eventType,
                        event.getActionIndex());
        }
    }

    public boolean trySendPenEvent(
            View eventView,
            MotionEvent event) {
        Objects.requireNonNull(eventView, "eventView");
        Objects.requireNonNull(event, "event");
        if (!isStreamViewReady()) {
            return false;
        }

        byte eventType = getEventType(event);
        if (eventType < 0) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                boolean handledStylusEvent = false;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    byte toolType = getStylusToolType(event, i);
                    if (toolType == MoonBridge.LI_TOOL_TYPE_UNKNOWN) {
                        continue;
                    }

                    handledStylusEvent = true;
                    if (!sendPenEventForPointer(
                            eventView,
                            event,
                            eventType,
                            toolType,
                            i)) {
                        return false;
                    }
                }
                return handledStylusEvent;

            case MotionEvent.ACTION_CANCEL:
                return inputSink.sendPenEvent(
                        MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                        MoonBridge.LI_TOOL_TYPE_UNKNOWN,
                        (byte) 0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        MoonBridge.LI_ROT_UNKNOWN,
                        MoonBridge.LI_TILT_UNKNOWN) !=
                        MoonBridge.LI_ERR_UNSUPPORTED;

            default:
                int actionIndex = event.getActionIndex();
                byte toolType = getStylusToolType(
                        event,
                        actionIndex);
                if (toolType == MoonBridge.LI_TOOL_TYPE_UNKNOWN) {
                    return false;
                }
                return sendPenEventForPointer(
                        eventView,
                        event,
                        eventType,
                        toolType,
                        actionIndex);
        }
    }

    private boolean sendTouchEventForPointer(
            View eventView,
            MotionEvent event,
            byte eventType,
            int pointerIndex) {
        updateNormalizedCoordinates(
                eventView,
                event,
                pointerIndex,
                true);
        updateNormalizedContactArea(event, pointerIndex);
        return inputSink.sendTouchEvent(
                eventType,
                event.getPointerId(pointerIndex),
                geometry.x,
                geometry.y,
                getPressureOrDistance(event, pointerIndex),
                geometry.contactAreaMajor,
                geometry.contactAreaMinor,
                getRotationDegrees(event, pointerIndex)) !=
                MoonBridge.LI_ERR_UNSUPPORTED;
    }

    private boolean sendPenEventForPointer(
            View eventView,
            MotionEvent event,
            byte eventType,
            byte toolType,
            int pointerIndex) {
        byte penButtons = 0;
        if ((event.getButtonState() &
                PointerInputCompat.BUTTON_STYLUS_PRIMARY) != 0) {
            penButtons |= MoonBridge.LI_PEN_BUTTON_PRIMARY;
        }
        if ((event.getButtonState() &
                PointerInputCompat.BUTTON_STYLUS_SECONDARY) != 0) {
            penButtons |= MoonBridge.LI_PEN_BUTTON_SECONDARY;
        }

        byte tiltDegrees = MoonBridge.LI_TILT_UNKNOWN;
        InputDevice device = event.getDevice();
        if (device != null &&
                device.getMotionRange(
                        MotionEvent.AXIS_TILT,
                        event.getSource()) != null) {
            tiltDegrees = (byte) Math.toDegrees(
                    event.getAxisValue(
                            MotionEvent.AXIS_TILT,
                            pointerIndex));
        }

        updateNormalizedCoordinates(
                eventView,
                event,
                pointerIndex,
                false);
        updateNormalizedContactArea(event, pointerIndex);
        return inputSink.sendPenEvent(
                eventType,
                toolType,
                penButtons,
                geometry.x,
                geometry.y,
                getPressureOrDistance(event, pointerIndex),
                geometry.contactAreaMajor,
                geometry.contactAreaMinor,
                getRotationDegrees(event, pointerIndex),
                tiltDegrees) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    private void updateNormalizedCoordinates(
            View eventView,
            MotionEvent event,
            int pointerIndex,
            boolean touchEvent) {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        if (touchEvent &&
                preferences.enableTouchSensitivity &&
                (preferences.touchSensitivityX != 100 ||
                        preferences.touchSensitivityY != 100)) {
            updateSensitivityCoordinates(
                    event,
                    pointerIndex,
                    x,
                    y);
            x = geometry.x;
            y = geometry.y;
        }

        if (eventView != streamView) {
            if (streamView.getScaleX() > 1) {
                eventView.getLocationInWindow(eventViewLocation);
                streamView.getLocationInWindow(streamViewLocation);
                int deltaX =
                        streamViewLocation[0] - eventViewLocation[0];
                int deltaY =
                        streamViewLocation[1] - eventViewLocation[1];
                x = (x - deltaX) / streamView.getScaleX();
                y = (y - deltaY) / streamView.getScaleY();
            }
            else {
                x -= streamView.getX();
                y -= streamView.getY();
            }
        }

        geometry.x = clamp(x, 0, streamView.getWidth()) /
                streamView.getWidth();
        geometry.y = clamp(y, 0, streamView.getHeight()) /
                streamView.getHeight();
    }

    private void updateSensitivityCoordinates(
            MotionEvent event,
            int pointerIndex,
            float rawX,
            float rawY) {
        int pointerId = event.getPointerId(pointerIndex);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_POINTER_DOWN) {
            sensitivityStates.put(
                    pointerId,
                    new SensitivityState(rawX));
        }
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_POINTER_UP) {
            sensitivityStates.remove(pointerId);
        }

        geometry.x = rawX;
        geometry.y = rawY;
        if (action != MotionEvent.ACTION_MOVE) {
            return;
        }

        SensitivityState state = sensitivityStates.get(pointerId);
        if (!preferences.touchSensitivityGlobal &&
                (state == null ||
                        state.startDownX <
                                streamView.getWidth() / 2f)) {
            return;
        }
        if (state == null) {
            return;
        }

        float deltaX = 0;
        float deltaY = 0;
        if (state.lastAbsoluteX != -1) {
            deltaX = (rawX - state.lastAbsoluteX) *
                    0.01f * preferences.touchSensitivityX;
            deltaY = (rawY - state.lastAbsoluteY) *
                    0.01f * preferences.touchSensitivityY;
            geometry.x = state.lastRelativeX + deltaX;
            geometry.y = state.lastRelativeY + deltaY;
        }

        if (preferences.touchSensitivityRotationAuto &&
                (geometry.x > streamView.getWidth() ||
                        geometry.x < 0 ||
                        geometry.y > streamView.getHeight() ||
                        geometry.y < 0)) {
            geometry.x -= deltaX;
            geometry.y -= deltaY;
            inputSink.sendTouchEvent(
                    MoonBridge.LI_TOUCH_EVENT_UP,
                    pointerId,
                    geometry.x / streamView.getWidth(),
                    geometry.y / streamView.getHeight(),
                    0.5f,
                    0.5f,
                    0.5f,
                    (short) 0);
            inputSink.sendTouchEvent(
                    MoonBridge.LI_TOUCH_EVENT_DOWN,
                    pointerId,
                    0.5f,
                    0.5f,
                    0.5f,
                    0.5f,
                    0.5f,
                    (short) 0);
            geometry.x =
                    streamView.getWidth() / 2f + deltaX;
            geometry.y =
                    streamView.getHeight() / 2f + deltaY;
        }

        state.lastAbsoluteX = rawX;
        state.lastAbsoluteY = rawY;
        state.lastRelativeX = geometry.x;
        state.lastRelativeY = geometry.y;
    }

    private void updateNormalizedContactArea(
            MotionEvent event,
            int pointerIndex) {
        float orientation;
        InputDevice device = event.getDevice();
        if (device == null ||
                device.getMotionRange(
                        MotionEvent.AXIS_ORIENTATION,
                        event.getSource()) == null) {
            orientation = (float) (Math.PI / 4);
        }
        else {
            orientation = event.getOrientation(pointerIndex);
        }

        float contactAreaMajor;
        float contactAreaMinor;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_HOVER_EXIT:
                contactAreaMajor = event.getToolMajor(pointerIndex);
                contactAreaMinor = event.getToolMinor(pointerIndex);
                break;

            default:
                contactAreaMajor = event.getTouchMajor(pointerIndex);
                contactAreaMinor = event.getTouchMinor(pointerIndex);
                break;
        }

        float majorX = (float) (
                contactAreaMajor * Math.cos(orientation));
        float majorY = (float) (
                contactAreaMajor * Math.sin(orientation));
        float minorOrientation =
                (float) (orientation + Math.PI / 2);
        float minorX = (float) (
                contactAreaMinor * Math.cos(minorOrientation));
        float minorY = (float) (
                contactAreaMinor * Math.sin(minorOrientation));

        majorX = Math.min(
                Math.abs(majorX) / streamView.getScaleX(),
                streamView.getWidth()) / streamView.getWidth();
        majorY = Math.min(
                Math.abs(majorY) / streamView.getScaleY(),
                streamView.getHeight()) / streamView.getHeight();
        minorX = Math.min(
                Math.abs(minorX) / streamView.getScaleX(),
                streamView.getWidth()) / streamView.getWidth();
        minorY = Math.min(
                Math.abs(minorY) / streamView.getScaleY(),
                streamView.getHeight()) / streamView.getHeight();

        geometry.contactAreaMajor = (float) Math.sqrt(
                Math.pow(majorX, 2) + Math.pow(majorY, 2));
        geometry.contactAreaMinor = (float) Math.sqrt(
                Math.pow(minorX, 2) + Math.pow(minorY, 2));
    }

    private boolean isStreamViewReady() {
        return streamView.getWidth() > 0 &&
                streamView.getHeight() > 0 &&
                streamView.getScaleX() != 0 &&
                streamView.getScaleY() != 0;
    }

    private static byte getEventType(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                return MoonBridge.LI_TOUCH_EVENT_DOWN;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                return (event.getFlags() &
                        MotionEvent.FLAG_CANCELED) != 0
                        ? MoonBridge.LI_TOUCH_EVENT_CANCEL
                        : MoonBridge.LI_TOUCH_EVENT_UP;

            case MotionEvent.ACTION_MOVE:
                return MoonBridge.LI_TOUCH_EVENT_MOVE;

            case MotionEvent.ACTION_CANCEL:
                return MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL;

            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
                return MoonBridge.LI_TOUCH_EVENT_HOVER;

            case MotionEvent.ACTION_HOVER_EXIT:
                return MoonBridge.LI_TOUCH_EVENT_HOVER_LEAVE;

            case MotionEvent.ACTION_BUTTON_PRESS:
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY;

            default:
                return -1;
        }
    }

    private static float getPressureOrDistance(
            MotionEvent event,
            int pointerIndex) {
        InputDevice device = event.getDevice();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_HOVER_EXIT:
                if (device != null) {
                    InputDevice.MotionRange distanceRange =
                            device.getMotionRange(
                                    MotionEvent.AXIS_DISTANCE,
                                    event.getSource());
                    if (distanceRange != null) {
                        return normalizeValueInRange(
                                event.getAxisValue(
                                        MotionEvent.AXIS_DISTANCE,
                                        pointerIndex),
                                distanceRange);
                    }
                }
                return 0;

            default:
                return event.getPressure(pointerIndex);
        }
    }

    private static short getRotationDegrees(
            MotionEvent event,
            int pointerIndex) {
        InputDevice device = event.getDevice();
        if (device != null &&
                device.getMotionRange(
                        MotionEvent.AXIS_ORIENTATION,
                        event.getSource()) != null) {
            short rotationDegrees = (short) Math.toDegrees(
                    event.getOrientation(pointerIndex));
            if (rotationDegrees < 0) {
                rotationDegrees += 360;
            }
            return rotationDegrees;
        }
        return MoonBridge.LI_ROT_UNKNOWN;
    }

    private static byte getStylusToolType(
            MotionEvent event,
            int pointerIndex) {
        switch (event.getToolType(pointerIndex)) {
            case MotionEvent.TOOL_TYPE_ERASER:
                return MoonBridge.LI_TOOL_TYPE_ERASER;

            case MotionEvent.TOOL_TYPE_STYLUS:
                return MoonBridge.LI_TOOL_TYPE_PEN;

            default:
                return MoonBridge.LI_TOOL_TYPE_UNKNOWN;
        }
    }

    private static float normalizeValueInRange(
            float value,
            InputDevice.MotionRange range) {
        return (value - range.getMin()) / range.getRange();
    }

    private static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    private static final class ContactGeometry {
        private float x;
        private float y;
        private float contactAreaMajor;
        private float contactAreaMinor;
    }

    private static final class SensitivityState {
        private final float startDownX;
        private float lastAbsoluteX = -1;
        private float lastAbsoluteY = -1;
        private float lastRelativeX = -1;
        private float lastRelativeY = -1;

        private SensitivityState(float startDownX) {
            this.startDownX = startDownX;
        }
    }
}
