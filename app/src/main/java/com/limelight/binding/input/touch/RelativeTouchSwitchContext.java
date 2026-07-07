package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

public class RelativeTouchSwitchContext implements TouchContext {
    private int lastTouchX = 0;
    private int lastTouchY = 0;
    private int originalTouchX = 0;
    private int originalTouchY = 0;
    private long originalTouchTime = 0;
    private boolean cancelled;
    private boolean confirmedMove;
    private boolean confirmedDrag;
    private boolean pendingTapDrag;
    private boolean pendingTapDragButtonDown;
    private boolean pendingTapDragUsingHeldTap;
    private double xFactor, yFactor;

    private final NvConnection conn;
    private final int actionIndex;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final PreferenceConfiguration prefConfig;
    private final boolean clickEnabled; // 新增：是否启用点击
    private final TouchpadTapDragTracker tapDragTracker;
    private final Handler handler;
    private final TouchpadGestureState gestureState;
    private boolean pendingLeftButtonUp;

    private final Runnable leftButtonUpRunnable = new Runnable() {
        @Override
        public void run() {
            releasePendingLeftButtonUp();
        }
    };

    private static final int TAP_MOVEMENT_THRESHOLD = 35;
    private static final int TAP_TIME_THRESHOLD = 150;
    private static final int TAP_CLICK_BUTTON_UP_DELAY = 160;

    public RelativeTouchSwitchContext(NvConnection conn, int actionIndex,
                                      int referenceWidth, int referenceHeight,
                                      View view, PreferenceConfiguration prefConfig,
                                      boolean clickEnabled)
    {
        this(conn, actionIndex, referenceWidth, referenceHeight, view, prefConfig,
                clickEnabled, new TouchpadGestureState());
    }

    public RelativeTouchSwitchContext(NvConnection conn, int actionIndex,
                                      int referenceWidth, int referenceHeight,
                                      View view, PreferenceConfiguration prefConfig,
                                      boolean clickEnabled,
                                      TouchpadGestureState gestureState)
    {
        this.conn = conn;
        this.actionIndex = actionIndex;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.targetView = view;
        this.prefConfig = prefConfig;
        this.clickEnabled = clickEnabled;
        this.tapDragTracker = new TouchpadTapDragTracker();
        this.handler = new Handler(Looper.getMainLooper());
        this.gestureState = gestureState;
    }

    @Override
    public int getActionIndex() { return actionIndex; }

    private void sendMouseMovePacket(short scaledDeltaX, short scaledDeltaY) {
        if (prefConfig.absoluteMouseMode) {
            conn.sendMouseMoveAsMousePosition(scaledDeltaX, scaledDeltaY,
                    (short) targetView.getWidth(), (short) targetView.getHeight());
        } else {
            conn.sendMouseMove(scaledDeltaX, scaledDeltaY);
        }
    }

    private void updateScaleFactors() {
        int viewWidth = targetView.getWidth();
        int viewHeight = targetView.getHeight();

        if (viewWidth > 0 && viewHeight > 0) {
            xFactor = (double) referenceWidth / viewWidth;
            yFactor = (double) referenceHeight / viewHeight;
        }
    }

    private boolean isWithinTapBounds(int touchX, int touchY) {
        return Math.abs(touchX - originalTouchX) <= TAP_MOVEMENT_THRESHOLD &&
                Math.abs(touchY - originalTouchY) <= TAP_MOVEMENT_THRESHOLD;
    }

    private boolean releasePendingLeftButtonUp() {
        if (!pendingLeftButtonUp) {
            return false;
        }

        pendingLeftButtonUp = false;
        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
        return true;
    }

    private boolean completePendingTapClick() {
        handler.removeCallbacks(leftButtonUpRunnable);
        return releasePendingLeftButtonUp();
    }

    private boolean promotePendingTapToDrag() {
        if (!pendingLeftButtonUp) {
            return false;
        }

        handler.removeCallbacks(leftButtonUpRunnable);
        pendingLeftButtonUp = false;
        return true;
    }

    private void beginPendingTapDrag() {
        pendingTapDrag = true;
        pendingTapDragUsingHeldTap = promotePendingTapToDrag();
        pendingTapDragButtonDown = true;

        if (!pendingTapDragUsingHeldTap) {
            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
        }
    }

    private void clearPendingTapDragState() {
        pendingTapDrag = false;
        pendingTapDragButtonDown = false;
        pendingTapDragUsingHeldTap = false;
    }

    private void finishPendingTapDragAsTap(int eventX, int eventY, long eventTime) {
        if (!pendingTapDragButtonDown) {
            clearPendingTapDragState();
            return;
        }

        boolean usingHeldTap = pendingTapDragUsingHeldTap;
        clearPendingTapDragState();

        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);

        if (usingHeldTap) {
            sendTapClick();
        }
    }

    private void cancelPendingTapDrag() {
        if (pendingTapDragButtonDown) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
        }

        clearPendingTapDragState();
    }

    private void sendTapClick() {
        completePendingTapClick();

        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
        pendingLeftButtonUp = true;
        handler.postDelayed(leftButtonUpRunnable, TAP_CLICK_BUTTON_UP_DELAY);
    }

    private void beginConfirmedDrag() {
        boolean alreadyDown = pendingTapDragButtonDown || promotePendingTapToDrag();
        clearPendingTapDragState();
        confirmedDrag = true;
        if (actionIndex == 0) {
            gestureState.setMouseButtonActive(true);
        }
        if (!alreadyDown) {
            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
        }
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger) {
        if (actionIndex != 0) return true;

        updateScaleFactors();

        originalTouchX = lastTouchX = eventX;
        originalTouchY = lastTouchY = eventY;

        if (isNewFinger) {
            originalTouchTime = eventTime;
            cancelled = confirmedMove = confirmedDrag = false;
            clearPendingTapDragState();
            if (clickEnabled && tapDragTracker.isTapDragStart(eventX, eventY, eventTime)) {
                tapDragTracker.clear();
                beginPendingTapDrag();
            }
        }
        return true;
    }

    @Override
    public void touchUpEvent(int eventX, int eventY, long eventTime) {
        // 如果禁用了点击，或者不是主手指，直接返回
        if (cancelled || actionIndex != 0 || !clickEnabled) return;

        if (confirmedDrag) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            confirmedDrag = false;
            gestureState.setMouseButtonActive(false);
            return;
        }

        long timeDelta = eventTime - originalTouchTime;
        if (pendingTapDrag && !confirmedMove && timeDelta <= TAP_TIME_THRESHOLD &&
                isWithinTapBounds(eventX, eventY)) {
            tapDragTracker.recordTap(eventX, eventY, eventTime);
            finishPendingTapDragAsTap(eventX, eventY, eventTime);
        }
        else if (!pendingTapDrag && !confirmedMove && timeDelta <= TAP_TIME_THRESHOLD &&
                isWithinTapBounds(eventX, eventY)) {
            tapDragTracker.recordTap(eventX, eventY, eventTime);
            sendTapClick();
        }
        else {
            if (pendingTapDrag) {
                cancelPendingTapDrag();
            }

        }
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime) {
        if (cancelled || actionIndex != 0) return true;

        if (eventX != lastTouchX || eventY != lastTouchY) {
            updateScaleFactors();

            if (pendingTapDrag) {
                beginConfirmedDrag();
            }

            if (!confirmedDrag && !confirmedMove && !isWithinTapBounds(eventX, eventY)) {
                confirmedMove = true;
            }

            int deltaX = (int) Math.round((eventX - lastTouchX) * xFactor);
            int deltaY = (int) Math.round((eventY - lastTouchY) * yFactor);

            if (deltaX != 0 || deltaY != 0) {
                short scaledDeltaX = (short) (deltaX * prefConfig.mouseTouchPadSensitityX * 0.01f);
                short scaledDeltaY = (short) (deltaY * prefConfig.mouseTouchPadSensitityY * 0.01f);

                sendMouseMovePacket(scaledDeltaX, scaledDeltaY);
                if (deltaX != 0) {
                    lastTouchX = eventX;
                }
                if (deltaY != 0) {
                    lastTouchY = eventY;
                }
            }
        }
        return true;
    }

    @Override
    public void cancelTouch() {
        cancelled = true;
        handler.removeCallbacks(leftButtonUpRunnable);
        releasePendingLeftButtonUp();
        cancelPendingTapDrag();
        if (confirmedDrag) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            confirmedDrag = false;
            gestureState.setMouseButtonActive(false);
        }
    }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setPointerCount(int pointerCount) {
        // 多指逻辑已完全剥离
    }
}
