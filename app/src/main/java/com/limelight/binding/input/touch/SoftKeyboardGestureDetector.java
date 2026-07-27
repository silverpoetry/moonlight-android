package com.limelight.binding.input.touch;

import android.os.Build;
import android.util.SparseArray;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public final class SoftKeyboardGestureDetector {
    public enum Result {
        NONE,
        STARTED,
        BUFFERING,
        FORWARD,
        CONSUMED,
        TRIGGERED
    }

    private static final int MIN_GESTURE_FINGER_COUNT = 3;
    private static final int TAP_THRESHOLD_MS = 300;

    private static final class PointerOrigin {
        final float x;
        final float y;

        PointerOrigin(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private final float movementThresholdSquared;
    private final SparseArray<PointerOrigin> pointerOrigins = new SparseArray<>();
    private final List<MotionEvent> bufferedEvents = new ArrayList<>();

    private boolean eligible;
    private boolean armed;
    private boolean buffering;
    private boolean passthrough;
    private boolean suppressRemainder;
    private int configuredFingerCount;

    public SoftKeyboardGestureDetector(int movementThresholdPx) {
        int safeMovementThreshold = Math.max(1, movementThresholdPx);
        movementThresholdSquared = safeMovementThreshold * safeMovementThreshold;
    }

    /**
     * Defers multi-pointer events only while they can still form the configured keyboard tap.
     *
     * <p>The first finger remains on the normal mouse path. Starting with the second pointer-down,
     * events are copied into a short-lived buffer. Movement, timeout, an early release, or an
     * unexpected finger count releases the complete buffer in original order. An exact tap drops
     * the buffer, so the host never receives contacts that could trigger its own gesture.</p>
     */
    public Result onTouchEvent(MotionEvent event, int requestedFingerCount) {
        if (passthrough) {
            if (isGestureEnd(event)) {
                reset();
            }
            return Result.NONE;
        }

        if (suppressRemainder) {
            if (isGestureEnd(event)) {
                reset();
            }
            return Result.CONSUMED;
        }

        if (requestedFingerCount < MIN_GESTURE_FINGER_COUNT) {
            if (buffering) {
                buffer(event);
                beginPassthrough();
                return Result.FORWARD;
            }
            reset();
            return Result.NONE;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                reset();
                configuredFingerCount = requestedFingerCount;
                eligible = true;
                recordPointerOrigin(event, 0);
                return Result.NONE;

            case MotionEvent.ACTION_POINTER_DOWN:
                return handlePointerDown(event, requestedFingerCount);

            case MotionEvent.ACTION_MOVE:
                return handleMove(event);

            case MotionEvent.ACTION_POINTER_UP:
                return handlePointerUp(event, requestedFingerCount);

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (buffering) {
                    buffer(event);
                    beginPassthrough();
                    return Result.FORWARD;
                }
                reset();
                return Result.NONE;

            default:
                if (buffering) {
                    buffer(event);
                    return Result.BUFFERING;
                }
                return Result.NONE;
        }
    }

    /**
     * Transfers ownership of events returned with {@link Result#FORWARD}.
     * The caller must recycle each event after dispatch.
     */
    public List<MotionEvent> takeBufferedEvents() {
        List<MotionEvent> events = new ArrayList<>(bufferedEvents);
        bufferedEvents.clear();
        return events;
    }

    public void reset() {
        recycleBufferedEvents();
        eligible = false;
        armed = false;
        buffering = false;
        passthrough = false;
        suppressRemainder = false;
        configuredFingerCount = 0;
        pointerOrigins.clear();
    }

    private Result handlePointerDown(MotionEvent event, int requestedFingerCount) {
        if (!eligible || configuredFingerCount != requestedFingerCount) {
            if (buffering) {
                buffer(event);
                beginPassthrough();
                return Result.FORWARD;
            }
            eligible = false;
            armed = false;
            return Result.NONE;
        }

        recordPointerOrigin(event, event.getActionIndex());
        updateEligibility(event);

        boolean started = !buffering;
        if (event.getPointerCount() >= 2) {
            buffering = true;
            buffer(event);
        }

        if (!eligible || event.getPointerCount() > configuredFingerCount) {
            beginPassthrough();
            return Result.FORWARD;
        }

        if (event.getPointerCount() == configuredFingerCount) {
            armed = true;
        }

        return started ? Result.STARTED : Result.BUFFERING;
    }

    private Result handleMove(MotionEvent event) {
        updateEligibility(event);
        if (!buffering) {
            return Result.NONE;
        }

        buffer(event);
        if (!eligible) {
            beginPassthrough();
            return Result.FORWARD;
        }
        return Result.BUFFERING;
    }

    private Result handlePointerUp(MotionEvent event, int requestedFingerCount) {
        if (!buffering) {
            eligible = false;
            armed = false;
            return Result.NONE;
        }

        updateEligibility(event);
        boolean canceled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0;
        boolean triggered = armed &&
                eligible &&
                configuredFingerCount == requestedFingerCount &&
                !canceled &&
                event.getPointerCount() == configuredFingerCount &&
                event.getEventTime() - event.getDownTime() < TAP_THRESHOLD_MS;

        if (triggered) {
            recycleBufferedEvents();
            eligible = false;
            armed = false;
            buffering = false;
            suppressRemainder = true;
            pointerOrigins.clear();
            return Result.TRIGGERED;
        }

        buffer(event);
        beginPassthrough();
        return Result.FORWARD;
    }

    private void recordPointerOrigin(MotionEvent event, int pointerIndex) {
        pointerOrigins.put(
                event.getPointerId(pointerIndex),
                new PointerOrigin(event.getX(pointerIndex), event.getY(pointerIndex)));
    }

    private void updateEligibility(MotionEvent event) {
        if (!eligible) {
            return;
        }

        if (event.getEventTime() - event.getDownTime() >= TAP_THRESHOLD_MS ||
                event.getPointerCount() > configuredFingerCount) {
            eligible = false;
            armed = false;
            return;
        }

        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            PointerOrigin origin = pointerOrigins.get(event.getPointerId(pointerIndex));
            if (origin == null ||
                    movedBeyondThreshold(event.getX(pointerIndex), event.getY(pointerIndex), origin)) {
                eligible = false;
                armed = false;
                return;
            }

            for (int historyIndex = 0;
                 historyIndex < event.getHistorySize();
                 historyIndex++) {
                if (movedBeyondThreshold(
                        event.getHistoricalX(pointerIndex, historyIndex),
                        event.getHistoricalY(pointerIndex, historyIndex),
                        origin)) {
                    eligible = false;
                    armed = false;
                    return;
                }
            }
        }
    }

    private void beginPassthrough() {
        eligible = false;
        armed = false;
        buffering = false;
        passthrough = true;
        pointerOrigins.clear();
    }

    private void buffer(MotionEvent event) {
        bufferedEvents.add(MotionEvent.obtain(event));
    }

    private void recycleBufferedEvents() {
        for (MotionEvent event : bufferedEvents) {
            event.recycle();
        }
        bufferedEvents.clear();
    }

    private static boolean isGestureEnd(MotionEvent event) {
        return event.getActionMasked() == MotionEvent.ACTION_UP ||
                event.getActionMasked() == MotionEvent.ACTION_CANCEL;
    }

    private boolean movedBeyondThreshold(float x, float y, PointerOrigin origin) {
        float deltaX = x - origin.x;
        float deltaY = y - origin.y;
        return deltaX * deltaX + deltaY * deltaY > movementThresholdSquared;
    }
}
