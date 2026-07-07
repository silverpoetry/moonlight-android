package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

public class RelativeTouchContext implements TouchContext, TouchpadDragPrimer.Listener,
        TouchpadButtonController.CancellationProvider {
    private int lastTouchX = 0;
    private int lastTouchY = 0;
    private int originalTouchX = 0;
    private int originalTouchY = 0;
    private boolean cancelled;
    private boolean confirmedMove;
    private boolean confirmedDrag;
    private boolean confirmedScroll;
    private boolean primaryPressActive;
    private boolean primaryClickHoldElapsed;
    private boolean primaryMoveActive;
    private boolean waitingForSecondTap;
    private boolean doubleTapCandidate;
    private boolean doubleTapDragActive;
    private boolean doubleTapDragNeedsPrimerMove;
    private int doubleTapStartX;
    private int doubleTapStartY;
    private long doubleTapStartTime;
    private double distanceMoved;
    private int pointerCount;

    private final int actionIndex;
    private final Handler handler;
    private final TouchpadGestureState gestureState;
    private final TouchpadMotionSender motionSender;
    private final TouchpadDragPrimer dragPrimer;
    private final TouchpadButtonController buttonController;

    private final Runnable primaryClickHoldRunnable = new Runnable() {
        @Override
        public void run() {
            primaryClickHoldElapsed = true;

            if (doubleTapCandidate) {
                return;
            }

            if (primaryPressActive) {
                beginPrimaryMove();
            }
            else if (waitingForSecondTap) {
                buttonController.releasePrimaryButton();
                clearPrimaryClickState();
            }
        }
    };

    private final Runnable doubleTapDragRunnable = new Runnable() {
        @Override
        public void run() {
            if (doubleTapCandidate && primaryPressActive && !cancelled) {
                beginDoubleTapDrag();
            }
        }
    };

    private static final int TAP_MOVEMENT_THRESHOLD = 35;
    private static final int TAP_DISTANCE_THRESHOLD = 45;
    private static final int TAP_TIME_THRESHOLD = 250;
    private static final int PRIMARY_CLICK_HOLD_MS = 200;
    private static final int DOUBLE_TAP_DRAG_HOLD_MS = 300;
    private static final int DOUBLE_TAP_DRAG_DISTANCE_THRESHOLD = 8;

    private static final int SCROLL_SPEED_FACTOR = 5;

    public RelativeTouchContext(NvConnection conn, int actionIndex,
                                int referenceWidth, int referenceHeight,
                                View view, PreferenceConfiguration prefConfig)
    {
        this(conn, actionIndex, referenceWidth, referenceHeight, view, prefConfig,
                new TouchpadGestureState());
    }

    public RelativeTouchContext(NvConnection conn, int actionIndex,
                                int referenceWidth, int referenceHeight,
                                View view, PreferenceConfiguration prefConfig,
                                TouchpadGestureState gestureState)
    {
        this.actionIndex = actionIndex;
        this.handler = new Handler(Looper.getMainLooper());
        this.gestureState = gestureState;
        this.motionSender = new TouchpadMotionSender(conn, referenceWidth, referenceHeight,
                view, prefConfig);
        this.dragPrimer = new TouchpadDragPrimer(handler, motionSender, this);
        this.buttonController = new TouchpadButtonController(conn, handler, this);
    }

    @Override
    public int getActionIndex()
    {
        return actionIndex;
    }

    @Override
    public boolean isDragStillActive() {
        return !cancelled && (doubleTapDragActive || confirmedDrag);
    }

    @Override
    public void onDragPrimerFinished(int touchX, int touchY) {
        lastTouchX = touchX;
        lastTouchY = touchY;
    }

    private boolean isWithinTapBounds(int touchX, int touchY)
    {
        int xDelta = Math.abs(touchX - originalTouchX);
        int yDelta = Math.abs(touchY - originalTouchY);
        return xDelta <= TAP_MOVEMENT_THRESHOLD &&
                yDelta <= TAP_MOVEMENT_THRESHOLD;
    }

    private void sendPrimaryMoveTo(int eventX, int eventY) {
        if (eventX == lastTouchX && eventY == lastTouchY) {
            return;
        }

        int previousTouchX = lastTouchX;
        int previousTouchY = lastTouchY;
        int touchDeltaX = eventX - previousTouchX;
        int touchDeltaY = eventY - previousTouchY;

        int scaledTouchDeltaX = motionSender.scaleTouchDeltaX(touchDeltaX);
        int scaledTouchDeltaY = motionSender.scaleTouchDeltaY(touchDeltaY);

        if (pointerCount == 2 && !confirmedDrag) {
            if (confirmedScroll) {
                motionSender.sendHighResScroll((short)(scaledTouchDeltaY * SCROLL_SPEED_FACTOR));
            }
        } else {
            motionSender.sendMouseMovePacket(
                    (short) motionSender.scaleMouseDeltaX(touchDeltaX),
                    (short) motionSender.scaleMouseDeltaY(touchDeltaY));
        }

        // If reference scaling rounds a delta to zero, keep accumulating raw touch
        // movement until there is enough motion to produce a packet.
        if (scaledTouchDeltaX != 0) {
            lastTouchX = eventX;
        }
        if (scaledTouchDeltaY != 0) {
            lastTouchY = eventY;
        }
    }

    private void cancelPrimaryClickTimers() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        handler.removeCallbacks(doubleTapDragRunnable);
    }

    private void cancelDoubleTapDragPrimerMove() {
        dragPrimer.cancel();
    }

    private void clearPrimaryClickState() {
        primaryPressActive = false;
        primaryClickHoldElapsed = false;
        primaryMoveActive = false;
        waitingForSecondTap = false;
        doubleTapCandidate = false;
        doubleTapDragActive = false;
        doubleTapDragNeedsPrimerMove = false;
        doubleTapStartX = 0;
        doubleTapStartY = 0;
        doubleTapStartTime = 0;
    }

    private void beginFirstPrimaryPress() {
        cancelPrimaryClickTimers();
        buttonController.cancelSecondClick();
        clearPrimaryClickState();

        primaryPressActive = true;
        handler.postDelayed(primaryClickHoldRunnable, PRIMARY_CLICK_HOLD_MS);
    }

    private void beginPrimaryMove() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        buttonController.releasePrimaryButton();
        primaryClickHoldElapsed = true;
        primaryMoveActive = true;
        waitingForSecondTap = false;
    }

    private void beginSecondPrimaryPress(int eventX, int eventY, long eventTime) {
        handler.removeCallbacks(primaryClickHoldRunnable);
        buttonController.cancelSecondClick();

        primaryPressActive = true;
        primaryClickHoldElapsed = false;
        waitingForSecondTap = false;
        doubleTapCandidate = true;
        doubleTapDragActive = false;
        doubleTapDragNeedsPrimerMove = false;
        // The left button is already held from the first tap. Track movement from
        // the second contact so the gap between taps is not injected as cursor motion.
        doubleTapStartX = eventX;
        doubleTapStartY = eventY;
        doubleTapStartTime = eventTime;
        originalTouchX = eventX;
        originalTouchY = eventY;
        lastTouchX = eventX;
        lastTouchY = eventY;

        buttonController.pressPrimaryButton();
        handler.postDelayed(doubleTapDragRunnable, DOUBLE_TAP_DRAG_HOLD_MS);
    }

    private double getDoubleTapDragDistance(int eventX, int eventY) {
        int xDelta = eventX - doubleTapStartX;
        int yDelta = eventY - doubleTapStartY;
        return Math.sqrt((xDelta * xDelta) + (yDelta * yDelta));
    }

    private boolean hasDoubleTapDragDistance(int eventX, int eventY) {
        return getDoubleTapDragDistance(eventX, eventY) >= DOUBLE_TAP_DRAG_DISTANCE_THRESHOLD;
    }

    private void beginDoubleTapDrag() {
        if (!doubleTapCandidate) {
            return;
        }

        handler.removeCallbacks(doubleTapDragRunnable);
        doubleTapCandidate = false;
        doubleTapDragActive = true;
        confirmedDrag = true;
        confirmedMove = false;
        confirmedScroll = false;
        doubleTapDragNeedsPrimerMove = true;

        buttonController.pressPrimaryButton();
        gestureState.setPrimaryDragActive(true);
    }

    private void sendDragMoveTo(int eventX, int eventY) {
        if (doubleTapDragNeedsPrimerMove) {
            doubleTapDragNeedsPrimerMove = false;
            dragPrimer.begin(lastTouchX, lastTouchY, eventX, eventY);
            return;
        }

        if (dragPrimer.isActive()) {
            dragPrimer.updateTarget(eventX, eventY);
            return;
        }

        sendPrimaryMoveTo(eventX, eventY);
    }

    private void finishDoubleTapDrag() {
        handler.removeCallbacks(doubleTapDragRunnable);
        cancelDoubleTapDragPrimerMove();
        buttonController.releasePrimaryButton();
        confirmedDrag = false;
        gestureState.setPrimaryDragActive(false);
        clearPrimaryClickState();
    }

    private void finishDoubleClick() {
        handler.removeCallbacks(doubleTapDragRunnable);
        cancelDoubleTapDragPrimerMove();

        buttonController.releasePrimaryButton();
        clearPrimaryClickState();

        buttonController.scheduleSecondPrimaryClick();
    }

    private void resetPrimaryGestureTracking() {
        cancelPrimaryClickTimers();
        cancelDoubleTapDragPrimerMove();
        buttonController.releaseAllButtons();
        confirmedDrag = false;
        gestureState.setPrimaryDragActive(false);
        clearPrimaryClickState();
    }

    private void enterMultiTouchSession() {
        gestureState.beginMultiTouchSession();
        resetPrimaryGestureTracking();
    }

    private boolean consumePrimaryReleaseFromMultiTouch() {
        boolean consumed = gestureState.consumePrimaryReleaseFromMultiTouch(pointerCount);
        if (consumed) {
            resetPrimaryGestureTracking();
        }
        return consumed;
    }

    private void checkForConfirmedMove(int eventX, int eventY) {
        // If we've already confirmed something, get out now
        if (confirmedMove || confirmedDrag) {
            return;
        }

        // If it leaves the tap bounds, it's a move.
        if (!isWithinTapBounds(eventX, eventY)) {
            confirmedMove = true;
            gestureState.cancelTwoFingerTap();
            return;
        }

        // Check if we've exceeded the maximum distance moved
        distanceMoved += Math.sqrt(Math.pow(eventX - lastTouchX, 2) +
                Math.pow(eventY - lastTouchY, 2));
        if (distanceMoved >= TAP_DISTANCE_THRESHOLD) {
            confirmedMove = true;
            gestureState.cancelTwoFingerTap();
        }
    }

    private void checkForConfirmedScroll() {
        // Enter scrolling mode if we've already left the tap zone
        // and we have 2 fingers on screen. Leave scroll mode if
        // we no longer have 2 fingers on screen
        confirmedScroll = (actionIndex == 0 && pointerCount == 2 && confirmedMove &&
                !gestureState.isPrimaryDragActive());
    }

    private void finishPrimaryTouch(int eventX, int eventY, long eventTime) {
        if (doubleTapCandidate) {
            if (eventTime - doubleTapStartTime >= DOUBLE_TAP_DRAG_HOLD_MS) {
                beginDoubleTapDrag();
            }

            if (doubleTapDragActive) {
                sendDragMoveTo(eventX, eventY);
                finishDoubleTapDrag();
            }
            else {
                finishDoubleClick();
            }
            return;
        }

        if (doubleTapDragActive || confirmedDrag) {
            sendDragMoveTo(eventX, eventY);
            finishDoubleTapDrag();
            return;
        }

        if (primaryMoveActive) {
            sendPrimaryMoveTo(eventX, eventY);
            clearPrimaryClickState();
            return;
        }

        if (!buttonController.isPrimaryButtonDown()) {
            primaryPressActive = false;
            if (primaryClickHoldElapsed) {
                clearPrimaryClickState();
            }
            else {
                buttonController.pressPrimaryButton();
                waitingForSecondTap = true;
            }
            return;
        }

        primaryPressActive = false;
        if (primaryClickHoldElapsed) {
            buttonController.releasePrimaryButton();
            clearPrimaryClickState();
        }
        else {
            waitingForSecondTap = true;
        }
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger)
    {
        motionSender.updateScaleFactors();

        originalTouchX = lastTouchX = eventX;
        originalTouchY = lastTouchY = eventY;

        if (isNewFinger) {
            cancelled = confirmedDrag = confirmedMove = confirmedScroll = false;
            distanceMoved = 0;

            if (pointerCount == 1) {
                gestureState.beginSingleTouchSession();
            }
            else if (pointerCount == 2) {
                gestureState.beginTwoFingerTap(eventTime);
            }
            else {
                gestureState.cancelTwoFingerTap();
            }

            if (actionIndex == 0) {
                if (pointerCount == 1) {
                    if (waitingForSecondTap && buttonController.isPrimaryButtonDown()) {
                        beginSecondPrimaryPress(eventX, eventY, eventTime);
                    }
                    else {
                        beginFirstPrimaryPress();
                    }
                }
                else {
                    enterMultiTouchSession();
                }
            }
        }

        return true;
    }

    @Override
    public void touchUpEvent(int eventX, int eventY, long eventTime)
    {
        if (cancelled) {
            return;
        }

        int twoFingerTapResult = gestureState.onPointerUp(pointerCount, eventTime,
                TAP_TIME_THRESHOLD);
        if (twoFingerTapResult == TouchpadGestureState.TWO_FINGER_TAP_SUPPRESS) {
            return;
        }
        else if (twoFingerTapResult == TouchpadGestureState.TWO_FINGER_TAP_COMPLETE) {
            resetPrimaryGestureTracking();
            buttonController.sendTapClick(MouseButtonPacket.BUTTON_RIGHT);
            return;
        }

        if (actionIndex == 0) {
            if (consumePrimaryReleaseFromMultiTouch()) {
                return;
            }
            finishPrimaryTouch(eventX, eventY, eventTime);
        }
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime)
    {
        if (cancelled) {
            return true;
        }

        if (eventX != lastTouchX || eventY != lastTouchY)
        {
            if (actionIndex == 0 && doubleTapCandidate) {
                if (hasDoubleTapDragDistance(eventX, eventY)) {
                    beginDoubleTapDrag();
                }
                else {
                    return true;
                }
            }

            checkForConfirmedMove(eventX, eventY);

            if (actionIndex == 0 && primaryPressActive && !buttonController.isPrimaryButtonDown() &&
                    !doubleTapCandidate && !doubleTapDragActive && !waitingForSecondTap &&
                    confirmedMove) {
                beginPrimaryMove();
            }

            checkForConfirmedScroll();

            // We only send moves and drags for the primary touch point
            if (actionIndex == 0) {
                if (confirmedDrag) {
                    sendDragMoveTo(eventX, eventY);
                }
                else {
                    sendPrimaryMoveTo(eventX, eventY);
                }
            }
            else {
                lastTouchX = eventX;
                lastTouchY = eventY;
            }
        }

        return true;
    }

    @Override
    public void cancelTouch() {
        cancelled = true;

        resetPrimaryGestureTracking();
        gestureState.finishTouchSession();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setPointerCount(int pointerCount) {
        int oldPointerCount = this.pointerCount;
        this.pointerCount = pointerCount;

        if (actionIndex == 0 && oldPointerCount < 2 && pointerCount >= 2) {
            enterMultiTouchSession();
        }
    }
}
