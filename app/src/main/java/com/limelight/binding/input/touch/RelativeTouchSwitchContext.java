package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.settings.input.InputSettingsState;

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

    private final PointerInputSink inputSink;
    private final int actionIndex;
    private final boolean clickEnabled; // 新增：是否启用点击
    private final TouchpadTapDragTracker tapDragTracker;
    private final Handler handler;
    private final TouchpadGestureState gestureState;
    private final TouchpadMotionSender motionSender;
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

    public RelativeTouchSwitchContext(PointerInputSink inputSink, int actionIndex,
                                      int referenceWidth, int referenceHeight,
                                      View view, InputSettingsState settingsState,
                                      boolean clickEnabled)
    {
        this(inputSink, actionIndex, referenceWidth, referenceHeight, view, settingsState,
                clickEnabled, new TouchpadGestureState());
    }

    public RelativeTouchSwitchContext(PointerInputSink inputSink, int actionIndex,
                                      int referenceWidth, int referenceHeight,
                                      View view, InputSettingsState settingsState,
                                      boolean clickEnabled,
                                      TouchpadGestureState gestureState)
    {
        this.inputSink = inputSink;
        this.actionIndex = actionIndex;
        this.clickEnabled = clickEnabled;
        this.tapDragTracker = new TouchpadTapDragTracker();
        this.handler = new Handler(Looper.getMainLooper());
        this.gestureState = gestureState;
        this.motionSender = new TouchpadMotionSender(inputSink, referenceWidth, referenceHeight,
                view, settingsState);
    }

    @Override
    public int getActionIndex() { return actionIndex; }

    private boolean isWithinTapBounds(int touchX, int touchY) {
        return Math.abs(touchX - originalTouchX) <= TAP_MOVEMENT_THRESHOLD &&
                Math.abs(touchY - originalTouchY) <= TAP_MOVEMENT_THRESHOLD;
    }

    private boolean releasePendingLeftButtonUp() {
        if (!pendingLeftButtonUp) {
            return false;
        }

        pendingLeftButtonUp = false;
        inputSink.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
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
            inputSink.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
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

        inputSink.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);

        if (usingHeldTap) {
            sendTapClick();
        }
    }

    private void cancelPendingTapDrag() {
        if (pendingTapDragButtonDown) {
            inputSink.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
        }

        clearPendingTapDragState();
    }

    private void sendTapClick() {
        completePendingTapClick();

        inputSink.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
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
            inputSink.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
        }
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger) {
        if (actionIndex != 0) return true;

        motionSender.updateScaleFactors();
        motionSender.beginPointerMotion(eventTime);

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
            inputSink.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
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
            motionSender.updateScaleFactors();

            if (pendingTapDrag) {
                beginConfirmedDrag();
            }

            if (!confirmedDrag && !confirmedMove && !isWithinTapBounds(eventX, eventY)) {
                confirmedMove = true;
            }

            motionSender.sendTouchpadMove(eventX - lastTouchX, eventY - lastTouchY, eventTime);
            lastTouchX = eventX;
            lastTouchY = eventY;
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
            inputSink.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
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
