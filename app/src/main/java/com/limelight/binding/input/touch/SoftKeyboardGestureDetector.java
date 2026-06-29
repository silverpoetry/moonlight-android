package com.limelight.binding.input.touch;

import android.view.MotionEvent;

public final class SoftKeyboardGestureDetector {
    public enum Result {
        NONE,
        STARTED,
        CONSUMED,
        TRIGGERED
    }

    private static final int MIN_GESTURE_FINGER_COUNT = 3;
    private static final int TAP_THRESHOLD_MS = 300;

    private boolean pending;
    private boolean armed;
    private int configuredFingerCount;
    private int maxPointerCount;
    private long armedEventTime;

    public Result onTouchEvent(MotionEvent event, int configuredFingerCount) {
        if (configuredFingerCount < MIN_GESTURE_FINGER_COUNT) {
            reset();
            return Result.NONE;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                reset();
                return Result.NONE;

            case MotionEvent.ACTION_POINTER_DOWN:
                return handlePointerDown(event, configuredFingerCount);

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                return handlePointerUp(event);

            case MotionEvent.ACTION_CANCEL:
                return handleCancel(event);

            default:
                return pending ? Result.CONSUMED : Result.NONE;
        }
    }

    public void reset() {
        pending = false;
        armed = false;
        configuredFingerCount = 0;
        maxPointerCount = 0;
        armedEventTime = 0;
    }

    private Result handlePointerDown(MotionEvent event, int configuredFingerCount) {
        int pointerCount = event.getPointerCount();

        if (pointerCount < MIN_GESTURE_FINGER_COUNT) {
            return pending ? Result.CONSUMED : Result.NONE;
        }

        if (pointerCount > configuredFingerCount) {
            Result result = pending ? Result.CONSUMED : Result.NONE;
            reset();
            return result;
        }

        boolean justStarted = !pending;
        pending = true;
        this.configuredFingerCount = configuredFingerCount;
        maxPointerCount = Math.max(maxPointerCount, pointerCount);

        if (pointerCount == configuredFingerCount) {
            armed = true;
            armedEventTime = event.getEventTime();
        }

        return justStarted ? Result.STARTED : Result.CONSUMED;
    }

    private Result handlePointerUp(MotionEvent event) {
        if (!pending) {
            return Result.NONE;
        }

        maxPointerCount = Math.max(maxPointerCount, event.getPointerCount());
        if (event.getPointerCount() == 1) {
            return completeIfTriggered(event);
        }

        return Result.CONSUMED;
    }

    private Result handleCancel(MotionEvent event) {
        if (!pending) {
            return Result.NONE;
        }

        return completeIfTriggered(event);
    }

    private Result completeIfTriggered(MotionEvent event) {
        boolean triggered = armed &&
                maxPointerCount == configuredFingerCount &&
                event.getEventTime() - armedEventTime < TAP_THRESHOLD_MS;
        reset();
        return triggered ? Result.TRIGGERED : Result.CONSUMED;
    }
}
