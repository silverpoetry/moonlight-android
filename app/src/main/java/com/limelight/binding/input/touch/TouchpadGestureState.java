package com.limelight.binding.input.touch;

public final class TouchpadGestureState {
    public static final int TWO_FINGER_TAP_NONE = 0;
    public static final int TWO_FINGER_TAP_COMPLETE = 1;
    public static final int TWO_FINGER_TAP_SUPPRESS = 2;

    private boolean primaryDragActive;
    private boolean twoFingerTapCandidate;
    private boolean twoFingerTapConsumed;
    private long twoFingerTapStartTime;

    public boolean isPrimaryDragActive() {
        return primaryDragActive;
    }

    public void setPrimaryDragActive(boolean primaryDragActive) {
        this.primaryDragActive = primaryDragActive;
    }

    public void resetTwoFingerTap() {
        twoFingerTapCandidate = false;
        twoFingerTapConsumed = false;
        twoFingerTapStartTime = 0;
    }

    public void beginTwoFingerTap(long eventTime) {
        twoFingerTapCandidate = true;
        twoFingerTapConsumed = false;
        twoFingerTapStartTime = eventTime;
    }

    public void cancelTwoFingerTap() {
        twoFingerTapCandidate = false;
    }

    public boolean isTwoFingerTapCandidate() {
        return twoFingerTapCandidate && !twoFingerTapConsumed;
    }

    public int updateTwoFingerTapOnPointerUp(int pointerCountBeforeUp, long eventTime,
                                             int tapTimeThreshold) {
        if (!isTwoFingerTapCandidate()) {
            return TWO_FINGER_TAP_NONE;
        }

        if (eventTime - twoFingerTapStartTime > tapTimeThreshold) {
            resetTwoFingerTap();
            return TWO_FINGER_TAP_SUPPRESS;
        }

        if (pointerCountBeforeUp > 1) {
            return TWO_FINGER_TAP_SUPPRESS;
        }

        twoFingerTapCandidate = false;
        twoFingerTapConsumed = true;
        return TWO_FINGER_TAP_COMPLETE;
    }
}
