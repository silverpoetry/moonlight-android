package com.limelight.binding.input.touch;

import android.os.Build;
import android.util.SparseArray;
import android.view.MotionEvent;

public final class SoftKeyboardGestureDetector {
    public enum Result {
        NONE,
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

    private boolean eligible;
    private boolean armed;
    private boolean suppressRemainder;
    private int configuredFingerCount;

    public SoftKeyboardGestureDetector(int movementThresholdPx) {
        int safeMovementThreshold = Math.max(1, movementThresholdPx);
        movementThresholdSquared = safeMovementThreshold * safeMovementThreshold;
    }

    /**
     * Observes a touch stream without consuming it until an exact multi-finger tap is confirmed.
     *
     * <p>Native touchpad frames must continue flowing while a swipe is still possible. The first
     * pointer-up is the earliest point where a tap can be confirmed and canceled before the host
     * receives any contact-up frame. Only the remaining releases from that confirmed tap are
     * consumed.</p>
     */
    public Result onTouchEvent(MotionEvent event, int requestedFingerCount) {
        if (requestedFingerCount < MIN_GESTURE_FINGER_COUNT) {
            reset();
            return Result.NONE;
        }

        if (suppressRemainder) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                reset();
            }
            return Result.CONSUMED;
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
                updateEligibility(event);
                return Result.NONE;

            case MotionEvent.ACTION_POINTER_UP:
                return handlePointerUp(event, requestedFingerCount);

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                reset();
                return Result.NONE;

            default:
                return Result.NONE;
        }
    }

    public void reset() {
        eligible = false;
        armed = false;
        suppressRemainder = false;
        configuredFingerCount = 0;
        pointerOrigins.clear();
    }

    private Result handlePointerDown(MotionEvent event, int requestedFingerCount) {
        if (!eligible || configuredFingerCount != requestedFingerCount) {
            reset();
            return Result.NONE;
        }

        recordPointerOrigin(event, event.getActionIndex());
        updateEligibility(event);
        if (!eligible) {
            return Result.NONE;
        }

        int pointerCount = event.getPointerCount();
        if (pointerCount > configuredFingerCount) {
            eligible = false;
            armed = false;
            return Result.NONE;
        }

        if (pointerCount == configuredFingerCount) {
            armed = true;
        }

        return Result.NONE;
    }

    private Result handlePointerUp(MotionEvent event, int requestedFingerCount) {
        if (!armed || configuredFingerCount != requestedFingerCount) {
            eligible = false;
            armed = false;
            return Result.NONE;
        }

        updateEligibility(event);
        boolean canceled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0;
        boolean triggered = eligible &&
                !canceled &&
                event.getPointerCount() == configuredFingerCount &&
                event.getEventTime() - event.getDownTime() < TAP_THRESHOLD_MS;

        if (!triggered) {
            eligible = false;
            armed = false;
            return Result.NONE;
        }

        eligible = false;
        armed = false;
        suppressRemainder = true;
        pointerOrigins.clear();
        return Result.TRIGGERED;
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

    private boolean movedBeyondThreshold(float x, float y, PointerOrigin origin) {
        float deltaX = x - origin.x;
        float deltaY = y - origin.y;
        return deltaX * deltaX + deltaY * deltaY > movementThresholdSquared;
    }
}
