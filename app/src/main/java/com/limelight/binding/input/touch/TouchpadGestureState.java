package com.limelight.binding.input.touch;

// Shared state for all touch contexts that participate in the same touchpad
// gesture. This keeps multi-touch session rules out of individual pointer
// handlers, so a two-finger scroll cannot fall back into single-finger click
// handling when the final finger is lifted.
public final class TouchpadGestureState {
    public static final int TWO_FINGER_TAP_NONE = 0;
    public static final int TWO_FINGER_TAP_SUPPRESS = 1;
    public static final int TWO_FINGER_TAP_COMPLETE = 2;

    private boolean primaryDragActive;
    private boolean multiTouchSessionActive;
    private boolean twoFingerTapCandidate;
    private long twoFingerTapStartTime;

    public boolean isPrimaryDragActive() {
        return primaryDragActive;
    }

    public void setPrimaryDragActive(boolean primaryDragActive) {
        this.primaryDragActive = primaryDragActive;
    }

    public void beginSingleTouchSession() {
        multiTouchSessionActive = false;
        primaryDragActive = false;
        resetTwoFingerTap();
    }

    public void beginMultiTouchSession() {
        multiTouchSessionActive = true;
    }

    public boolean consumePrimaryReleaseFromMultiTouch(int pointerCountBeforeUp) {
        if (!multiTouchSessionActive) {
            return false;
        }

        if (pointerCountBeforeUp <= 1) {
            finishTouchSession();
        }

        return true;
    }

    public void finishTouchSession() {
        multiTouchSessionActive = false;
        primaryDragActive = false;
        resetTwoFingerTap();
    }

    private void resetTwoFingerTap() {
        twoFingerTapCandidate = false;
        twoFingerTapStartTime = 0;
    }

    public void beginTwoFingerTap(long eventTime) {
        beginMultiTouchSession();
        twoFingerTapCandidate = true;
        twoFingerTapStartTime = eventTime;
    }

    public void cancelTwoFingerTap() {
        resetTwoFingerTap();
    }

    public int onPointerUp(int pointerCountBeforeUp, long eventTime, int tapTimeThreshold) {
        if (!twoFingerTapCandidate) {
            return TWO_FINGER_TAP_NONE;
        }

        if (eventTime - twoFingerTapStartTime > tapTimeThreshold) {
            cancelTwoFingerTap();
            return TWO_FINGER_TAP_NONE;
        }

        if (pointerCountBeforeUp > 1) {
            return TWO_FINGER_TAP_SUPPRESS;
        }

        finishTouchSession();
        return TWO_FINGER_TAP_COMPLETE;
    }
}
