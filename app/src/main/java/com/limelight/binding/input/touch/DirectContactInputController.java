package com.limelight.binding.input.touch;

import android.graphics.Matrix;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.binding.input.PointerInputCompat;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.utils.ViewCoordinateMapper;

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
    private final InputSettingsState settingsState;
    private final SparseArray<SensitivityState> sensitivityStates =
            new SparseArray<>();
    private final ContactGeometry geometry = new ContactGeometry();
    private final float[] mappedPosition = new float[2];
    private final float[] contactBasis = new float[4];
    private final Matrix streamViewInverse = new Matrix();

    public DirectContactInputController(
            View streamView,
            PointerInputSink inputSink,
            InputSettingsState settingsState) {
        this.streamView = Objects.requireNonNull(
                streamView,
                "streamView");
        this.inputSink = Objects.requireNonNull(inputSink, "inputSink");
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
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
        if (!updateNormalizedCoordinates(
                eventView,
                event,
                pointerIndex,
                true)) {
            return false;
        }
        if (!updateNormalizedContactGeometry(
                eventView,
                event,
                pointerIndex)) {
            return false;
        }
        return inputSink.sendTouchEvent(
                eventType,
                event.getPointerId(pointerIndex),
                geometry.x,
                geometry.y,
                getPressureOrDistance(event, pointerIndex),
                geometry.contactAreaMajor,
                geometry.contactAreaMinor,
                geometry.rotation) !=
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

        if (!updateNormalizedCoordinates(
                eventView,
                event,
                pointerIndex,
                false)) {
            return false;
        }
        if (!updateNormalizedContactGeometry(
                eventView,
                event,
                pointerIndex)) {
            return false;
        }
        return inputSink.sendPenEvent(
                eventType,
                toolType,
                penButtons,
                geometry.x,
                geometry.y,
                getPressureOrDistance(event, pointerIndex),
                geometry.contactAreaMajor,
                geometry.contactAreaMinor,
                geometry.rotation,
                tiltDegrees) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    private boolean updateNormalizedCoordinates(
            View eventView,
            MotionEvent event,
            int pointerIndex,
            boolean touchEvent) {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        if (eventView != streamView) {
            mappedPosition[0] = x;
            mappedPosition[1] = y;
            if (!ViewCoordinateMapper.mapPointBetweenSiblings(
                    eventView,
                    streamView,
                    mappedPosition,
                    streamViewInverse)) {
                return false;
            }
            x = mappedPosition[0];
            y = mappedPosition[1];
        }

        InputSettings settings = settingsState.get();
        if (touchEvent &&
                settings.isDirectTouchSensitivityEnabled() &&
                (settings.getDirectTouchSensitivityX() != 100 ||
                        settings.getDirectTouchSensitivityY() != 100)) {
            updateSensitivityCoordinates(
                    event,
                    pointerIndex,
                    x,
                    y,
                    settings);
            x = geometry.x;
            y = geometry.y;
        }

        geometry.x = clamp(x, 0, streamView.getWidth()) /
                streamView.getWidth();
        geometry.y = clamp(y, 0, streamView.getHeight()) /
                streamView.getHeight();
        return true;
    }

    private void updateSensitivityCoordinates(
            MotionEvent event,
            int pointerIndex,
            float rawX,
            float rawY,
            InputSettings settings) {
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
        if (!settings.isDirectTouchSensitivityGlobal() &&
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
                    0.01f *
                    settings.getDirectTouchSensitivityX();
            deltaY = (rawY - state.lastAbsoluteY) *
                    0.01f *
                    settings.getDirectTouchSensitivityY();
            geometry.x = state.lastRelativeX + deltaX;
            geometry.y = state.lastRelativeY + deltaY;
        }

        if (settings.isDirectTouchRecenterEnabled() &&
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

    private boolean updateNormalizedContactGeometry(
            View eventView,
            MotionEvent event,
            int pointerIndex) {
        float orientation;
        boolean orientationKnown;
        InputDevice device = event.getDevice();
        if (device == null ||
                device.getMotionRange(
                        MotionEvent.AXIS_ORIENTATION,
                        event.getSource()) == null) {
            orientation = (float) (Math.PI / 4);
            orientationKnown = false;
        }
        else {
            orientation = event.getOrientation(pointerIndex);
            orientationKnown = true;
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

        // Android transforms event X/Y and orientation into the receiving
        // View's local coordinates, but TOUCH/TOOL_MAJOR and MINOR remain in
        // display pixels. Reconstruct each physical direction through the
        // source View, normalize it to one display pixel, then transform that
        // vector into stream-local coordinates. This keeps position, contact
        // area, and rotation in the same reference space.
        contactBasis[0] = (float) Math.cos(orientation);
        contactBasis[1] = (float) Math.sin(orientation);
        float minorOrientation =
                (float) (orientation + Math.PI / 2);
        contactBasis[2] = (float) Math.cos(minorOrientation);
        contactBasis[3] = (float) Math.sin(minorOrientation);

        eventView.getMatrix().mapVectors(contactBasis);
        if (!normalizeDirection(contactBasis, 0) ||
                !normalizeDirection(contactBasis, 2) ||
                !streamView.getMatrix().invert(streamViewInverse)) {
            return false;
        }
        streamViewInverse.mapVectors(contactBasis);

        geometry.rotation = orientationKnown
                ? rotationDegrees(contactBasis[0], contactBasis[1])
                : MoonBridge.LI_ROT_UNKNOWN;

        contactAreaMajor = sanitizeContactLength(contactAreaMajor);
        contactAreaMinor = sanitizeContactLength(contactAreaMinor);
        float majorX = normalizedContactComponent(
                contactBasis[0] * contactAreaMajor,
                streamView.getWidth());
        float majorY = normalizedContactComponent(
                contactBasis[1] * contactAreaMajor,
                streamView.getHeight());
        float minorX = normalizedContactComponent(
                contactBasis[2] * contactAreaMinor,
                streamView.getWidth());
        float minorY = normalizedContactComponent(
                contactBasis[3] * contactAreaMinor,
                streamView.getHeight());

        geometry.contactAreaMajor =
                (float) Math.hypot(majorX, majorY);
        geometry.contactAreaMinor =
                (float) Math.hypot(minorX, minorY);
        return true;
    }

    private static boolean normalizeDirection(
            float[] vectors,
            int offset) {
        float magnitude = (float) Math.hypot(
                vectors[offset],
                vectors[offset + 1]);
        if (!Float.isFinite(magnitude) || magnitude <= 0) {
            return false;
        }
        vectors[offset] /= magnitude;
        vectors[offset + 1] /= magnitude;
        return true;
    }

    private static float sanitizeContactLength(float value) {
        return Float.isFinite(value) && value > 0 ? value : 0;
    }

    private static float normalizedContactComponent(
            float component,
            int extent) {
        return Math.min(Math.abs(component), extent) / extent;
    }

    private static short rotationDegrees(float x, float y) {
        short degrees = (short) Math.toDegrees(Math.atan2(y, x));
        if (degrees < 0) {
            degrees += 360;
        }
        return degrees;
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
        private short rotation;
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
