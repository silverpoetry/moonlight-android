package com.limelight.binding.input.touch;

public final class TouchpadGestureState {
    public static final int TWO_FINGER_TAP_NONE = 0;
    public static final int TWO_FINGER_TAP_SUPPRESS = 1;
    public static final int TWO_FINGER_TAP_COMPLETE = 2;

    private boolean primaryDragActive;
    private boolean twoFingerTapCandidate;
    private long twoFingerTapStartTime;

    public boolean isPrimaryDragActive() {
        return primaryDragActive;
    }

    public void setPrimaryDragActive(boolean primaryDragActive) {
        this.primaryDragActive = primaryDragActive;
    }

    public void resetTouchGesture() {
        twoFingerTapCandidate = false;
        twoFingerTapStartTime = 0;
    }

    public void beginTwoFingerTap(long eventTime) {
        twoFingerTapCandidate = true;
        twoFingerTapStartTime = eventTime;
    }

    public void cancelTwoFingerTap() {
        twoFingerTapCandidate = false;
        twoFingerTapStartTime = 0;
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

        cancelTwoFingerTap();
        return TWO_FINGER_TAP_COMPLETE;
    }
}
