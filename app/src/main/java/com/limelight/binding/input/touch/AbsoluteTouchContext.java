package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;

public class AbsoluteTouchContext implements TouchContext {
    private int lastTouchDownX = 0;
    private int lastTouchDownY = 0;
    private long lastTouchDownTime = 0;
    private int lastTouchUpX = 0;
    private int lastTouchUpY = 0;
    private long lastTouchUpTime = 0;
    private int lastTouchLocationX = 0;
    private int lastTouchLocationY = 0;
    private boolean cancelled;
    private boolean confirmedLongPress;
    private boolean confirmedTap;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            confirmedLongPress = true;
            if (cancelled) {
                return;
            }

            updatePosition(lastTouchDownX, lastTouchDownY);
            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                }
            }, 100);
        }
    };

    private final NvConnection conn;
    private final int actionIndex;
    private final View targetView;
    private final Handler handler;

    private final Runnable leftButtonUpRunnable = new Runnable() {
        @Override
        public void run() {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
        }
    };

    private static final int SCROLL_SPEED_FACTOR = 3;

    private static final int LONG_PRESS_TIME_THRESHOLD = 650;
    private static final int LONG_PRESS_DISTANCE_THRESHOLD = 30;

    private static final int DOUBLE_TAP_TIME_THRESHOLD = 250;
    private static final int DOUBLE_TAP_DISTANCE_THRESHOLD = 60;

    private static final int TOUCH_DOWN_DEAD_ZONE_TIME_THRESHOLD = 100;
    private static final int TOUCH_DOWN_DEAD_ZONE_DISTANCE_THRESHOLD = 20;

    public AbsoluteTouchContext(NvConnection conn, int actionIndex, View view)
    {
        this.conn = conn;
        this.actionIndex = actionIndex;
        this.targetView = view;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int getActionIndex()
    {
        return actionIndex;
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger)
    {
        if (!isNewFinger) {
            // We don't handle finger transitions for absolute mode
            return true;
        }

        lastTouchLocationX = lastTouchDownX = eventX;
        lastTouchLocationY = lastTouchDownY = eventY;
        lastTouchDownTime = eventTime;
        cancelled = confirmedTap = confirmedLongPress = false;

        if (actionIndex == 0) {
            startLongPressTimer();
        }

        return true;
    }

    private boolean distanceExceeds(int deltaX, int deltaY, double limit) {
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2)) > limit;
    }

    private void updatePosition(int eventX, int eventY) {
        // We may get values slightly outside our view region on ACTION_HOVER_ENTER and ACTION_HOVER_EXIT.
        // Normalize these to the view size. We can't just drop them because we won't always get an event
        // right at the boundary of the view, so dropping them would result in our cursor never really
        // reaching the sides of the screen.
        eventX = Math.min(Math.max(eventX, 0), targetView.getWidth());
        eventY = Math.min(Math.max(eventY, 0), targetView.getHeight());

        conn.sendMousePosition((short)eventX, (short)eventY, (short)targetView.getWidth(), (short)targetView.getHeight());
    }

    @Override
    public void touchUpEvent(int eventX, int eventY, long eventTime)
    {
        if (cancelled) {
            return;
        }

        if (actionIndex == 0) {
            cancelLongPressTimer();

            if (confirmedLongPress) {
                // Right click is synthesized automatically by longPressRunnable.
            }
            else if (confirmedTap) {
                conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            }
            else {
                tapConfirmed();

                handler.removeCallbacks(leftButtonUpRunnable);
                handler.postDelayed(leftButtonUpRunnable, 100);
            }
        }

        lastTouchLocationX = lastTouchUpX = eventX;
        lastTouchLocationY = lastTouchUpY = eventY;
        lastTouchUpTime = eventTime;
    }

    private void startLongPressTimer() {
        cancelLongPressTimer();
        handler.postDelayed(longPressRunnable, LONG_PRESS_TIME_THRESHOLD);
    }

    private void cancelLongPressTimer() {
        handler.removeCallbacks(longPressRunnable);
    }

    private void tapConfirmed() {
        if (confirmedTap || confirmedLongPress) {
            return;
        }

        confirmedTap = true;

        // Left button down at original position
        if (lastTouchDownTime - lastTouchUpTime > DOUBLE_TAP_TIME_THRESHOLD ||
                distanceExceeds(lastTouchDownX - lastTouchUpX, lastTouchDownY - lastTouchUpY, DOUBLE_TAP_DISTANCE_THRESHOLD)) {
            // Don't reposition for finger down events within the deadzone. This makes double-clicking easier.
            updatePosition(lastTouchDownX, lastTouchDownY);
        }
        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime)
    {
        if (cancelled) {
            return true;
        }

        if (actionIndex == 0) {
            if (confirmedLongPress) {
                lastTouchLocationX = eventX;
                lastTouchLocationY = eventY;
                return true;
            }

            if (distanceExceeds(eventX - lastTouchDownX, eventY - lastTouchDownY, LONG_PRESS_DISTANCE_THRESHOLD)) {
                cancelLongPressTimer();
            }

            if (confirmedTap || distanceExceeds(eventX - lastTouchDownX, eventY - lastTouchDownY, TOUCH_DOWN_DEAD_ZONE_DISTANCE_THRESHOLD)) {
                tapConfirmed();
                updatePosition(eventX, eventY);
            }
        }
        else if (actionIndex == 1) {
            conn.sendMouseHighResScroll((short)((eventY - lastTouchLocationY) * SCROLL_SPEED_FACTOR));
        }

        lastTouchLocationX = eventX;
        lastTouchLocationY = eventY;

        return true;
    }

    @Override
    public void cancelTouch() {
        cancelled = true;

        cancelLongPressTimer();
        handler.removeCallbacksAndMessages(null);

        if (!confirmedLongPress && confirmedTap) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setPointerCount(int pointerCount) {
        if (actionIndex == 0 && pointerCount > 1) {
            cancelTouch();
        }
    }
}
