package com.limelight.binding.input.touch;

public interface TouchContext {
    int getActionIndex();
    void setPointerCount(int pointerCount);
    default void setPointerCount(int pointerCount, long eventTime) {
        setPointerCount(pointerCount);
    }
    default void suspendPendingPressRecognition() {
    }
    boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger);
    boolean touchMoveEvent(int eventX, int eventY, long eventTime);
    void touchUpEvent(int eventX, int eventY, long eventTime);
    void cancelTouch();
    boolean isCancelled();
}
