package com.limelight.binding.input.touch;

import android.view.ViewConfiguration;

final class TouchpadTapDragTracker {
    private final int tapDragTimeThreshold;

    private int lastTapX;
    private int lastTapY;
    private long lastTapTime;

    TouchpadTapDragTracker() {
        tapDragTimeThreshold = ViewConfiguration.getDoubleTapTimeout();
    }

    public boolean isTapDragStart(int touchX, int touchY, long eventTime) {
        if (lastTapTime == 0) {
            return false;
        }

        long timeDelta = eventTime - lastTapTime;
        return timeDelta <= tapDragTimeThreshold;
    }

    public void recordTap(int touchX, int touchY, long eventTime) {
        lastTapX = touchX;
        lastTapY = touchY;
        lastTapTime = eventTime;
    }

    public void clear() {
        lastTapTime = 0;
    }
}
