package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.Objects;

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
    private boolean primaryLongPressActive;
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
    private final TouchpadHapticFeedback hapticFeedback;
    private boolean nativeTouchpadPressHandlingEnabled;

    private final Runnable primaryClickHoldRunnable = new Runnable() {
        @Override
        public void run() {
            primaryClickHoldElapsed = true;

            if (doubleTapCandidate) {
                return;
            }

            if (primaryPressActive) {
                if (confirmedMove || pointerCount != 1) {
                    beginPrimaryMove();
                }
            }
            else if (waitingForSecondTap) {
                buttonController.releasePrimaryButton();
                clearPrimaryClickState();
            }
        }
    };

    private final Runnable primaryLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!nativeTouchpadPressHandlingEnabled &&
                    !cancelled && primaryPressActive && !confirmedMove &&
                    pointerCount == 1 && !doubleTapCandidate) {
                beginPrimaryLongPress();
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

    private final Runnable secondaryButtonHoldRunnable = new Runnable() {
        @Override
        public void run() {
            if (!nativeTouchpadPressHandlingEnabled &&
                    !cancelled && actionIndex == 0 && pointerCount == 2 &&
                    gestureState.beginSecondaryButtonHold()) {
                if (buttonController.pressButton(MouseButtonPacket.BUTTON_RIGHT)) {
                    hapticFeedback.performButtonPress();
                }
            }
        }
    };

    static final int TAP_MOVEMENT_THRESHOLD = 35;
    private static final int TAP_DISTANCE_THRESHOLD = 45;
    private static final int TAP_TIME_THRESHOLD = 250;
    // Keep the legacy click-release window independent from physical long-press detection.
    private static final int PRIMARY_CLICK_RELEASE_MS = 200;
    static final int PHYSICAL_LONG_PRESS_MS = 300;
    private static final int DOUBLE_TAP_DRAG_HOLD_MS = 300;

    private static final int SCROLL_SPEED_FACTOR = 5;

    public RelativeTouchContext(PointerInputSink inputSink, int actionIndex,
                                int referenceWidth, int referenceHeight,
                                View view, PreferenceConfiguration prefConfig)
    {
        this(inputSink, actionIndex, referenceWidth, referenceHeight, view, prefConfig,
                new TouchpadGestureState());
    }

    public RelativeTouchContext(PointerInputSink inputSink, int actionIndex,
                                int referenceWidth, int referenceHeight,
                                View view, PreferenceConfiguration prefConfig,
                                TouchpadGestureState gestureState)
    {
        this(inputSink, actionIndex, referenceWidth, referenceHeight, view,
                prefConfig, gestureState,
                new TouchpadMotionSender(inputSink, referenceWidth,
                        referenceHeight, view, prefConfig));
    }

    public RelativeTouchContext(PointerInputSink inputSink, int actionIndex,
                                int referenceWidth, int referenceHeight,
                                View view, PreferenceConfiguration prefConfig,
                                TouchpadGestureState gestureState,
                                TouchpadMotionSender motionSender)
    {
        this.actionIndex = actionIndex;
        this.handler = new Handler(Looper.getMainLooper());
        this.gestureState = gestureState;
        this.motionSender = Objects.requireNonNull(motionSender);
        this.dragPrimer = new TouchpadDragPrimer(handler, motionSender, this);
        this.buttonController =
                new TouchpadButtonController(inputSink, handler, this);
        this.hapticFeedback = new TouchpadHapticFeedback(view);
    }

    public void setNativeTouchpadPressHandlingEnabled(boolean enabled) {
        nativeTouchpadPressHandlingEnabled = enabled;
        if (enabled) {
            cancelPrimaryLongPressTimer();
            cancelSecondaryButtonHoldTimer();
        }
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

    private void sendPrimaryMoveTo(int eventX, int eventY, long eventTime) {
        if (eventX == lastTouchX && eventY == lastTouchY) {
            return;
        }

        int previousTouchX = lastTouchX;
        int previousTouchY = lastTouchY;
        int touchDeltaX = eventX - previousTouchX;
        int touchDeltaY = eventY - previousTouchY;

        int scaledTouchDeltaX = motionSender.scaleTouchDeltaX(touchDeltaX);
        int scaledTouchDeltaY = motionSender.scaleTouchDeltaY(touchDeltaY);

        if (pointerCount == 2 && !confirmedDrag &&
                !gestureState.isSecondaryButtonHoldActive()) {
            if (confirmedScroll) {
                motionSender.sendHighResScroll((short)(scaledTouchDeltaY * SCROLL_SPEED_FACTOR));
            }

            // Preserve legacy scroll accumulation independently from pointer acceleration.
            if (scaledTouchDeltaX != 0) {
                lastTouchX = eventX;
            }
            if (scaledTouchDeltaY != 0) {
                lastTouchY = eventY;
            }
        } else {
            motionSender.sendTouchpadMove(touchDeltaX, touchDeltaY, eventTime);
            // Mouse sub-pixels are retained by TouchpadMotionSender, so touch coordinates
            // always advance and velocity remains based on the actual hardware samples.
            lastTouchX = eventX;
            lastTouchY = eventY;
        }
    }

    private void cancelPrimaryClickTimers() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        handler.removeCallbacks(primaryLongPressRunnable);
        handler.removeCallbacks(doubleTapDragRunnable);
    }

    private void cancelPrimaryLongPressTimer() {
        handler.removeCallbacks(primaryLongPressRunnable);
    }

    private void cancelSecondaryButtonHoldTimer() {
        handler.removeCallbacks(secondaryButtonHoldRunnable);
    }

    private void cancelDoubleTapDragPrimerMove() {
        dragPrimer.cancel();
    }

    private void clearPrimaryClickState() {
        primaryPressActive = false;
        primaryClickHoldElapsed = false;
        primaryMoveActive = false;
        primaryLongPressActive = false;
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
        handler.postDelayed(primaryClickHoldRunnable, PRIMARY_CLICK_RELEASE_MS);
        if (!nativeTouchpadPressHandlingEnabled) {
            handler.postDelayed(primaryLongPressRunnable, PHYSICAL_LONG_PRESS_MS);
        }
    }

    private void beginPrimaryMove() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        cancelPrimaryLongPressTimer();
        buttonController.releasePrimaryButton();
        primaryClickHoldElapsed = true;
        primaryMoveActive = true;
        waitingForSecondTap = false;
    }

    private void beginPrimaryLongPress() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        cancelPrimaryLongPressTimer();
        primaryClickHoldElapsed = true;
        primaryLongPressActive = true;
        primaryMoveActive = false;
        waitingForSecondTap = false;

        boolean pressed = buttonController.pressPrimaryButton();
        gestureState.setMouseButtonActive(true);
        if (pressed) {
            hapticFeedback.performButtonPress();
        }
    }

    private void beginSecondPrimaryPress(int eventX, int eventY, long eventTime) {
        handler.removeCallbacks(primaryClickHoldRunnable);
        cancelPrimaryLongPressTimer();
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

    private boolean hasDoubleTapDragMovement(int eventX, int eventY) {
        return eventX != doubleTapStartX || eventY != doubleTapStartY;
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
        gestureState.setMouseButtonActive(true);
    }

    private void sendDragMoveTo(int eventX, int eventY, long eventTime) {
        if (doubleTapDragNeedsPrimerMove) {
            doubleTapDragNeedsPrimerMove = false;
            dragPrimer.begin(lastTouchX, lastTouchY, eventX, eventY, eventTime);
            return;
        }

        if (dragPrimer.isActive()) {
            dragPrimer.updateTarget(eventX, eventY, eventTime);
            return;
        }

        sendPrimaryMoveTo(eventX, eventY, eventTime);
    }

    private void finishDoubleTapDrag() {
        handler.removeCallbacks(doubleTapDragRunnable);
        cancelDoubleTapDragPrimerMove();
        buttonController.releasePrimaryButton();
        confirmedDrag = false;
        gestureState.setMouseButtonActive(false);
        clearPrimaryClickState();
    }

    private void finishDoubleClick() {
        handler.removeCallbacks(doubleTapDragRunnable);
        cancelDoubleTapDragPrimerMove();

        buttonController.releasePrimaryButton();
        clearPrimaryClickState();

        buttonController.scheduleSecondPrimaryClick();
    }

    private void finishPrimaryLongPress(int eventX, int eventY, long eventTime) {
        sendPrimaryMoveTo(eventX, eventY, eventTime);
        boolean released = buttonController.releasePrimaryButton();
        gestureState.setMouseButtonActive(false);
        if (released) {
            hapticFeedback.performButtonRelease();
        }
        clearPrimaryClickState();
    }

    private void resetPrimaryGestureTracking() {
        cancelPrimaryClickTimers();
        cancelSecondaryButtonHoldTimer();
        cancelDoubleTapDragPrimerMove();
        buttonController.releaseAllButtons();
        confirmedDrag = false;
        gestureState.setMouseButtonActive(false);
        clearPrimaryClickState();
    }

    private void enterMultiTouchSession(long eventTime) {
        gestureState.beginMultiTouchSession();
        resetPrimaryGestureTracking();
        beginSecondaryButtonHoldCandidate(eventTime);
    }

    private void beginSecondaryButtonHoldCandidate(long eventTime) {
        cancelSecondaryButtonHoldTimer();
        if (!nativeTouchpadPressHandlingEnabled &&
                actionIndex == 0 &&
                pointerCount == 2) {
            long elapsedMs = Math.max(
                    0,
                    SystemClock.uptimeMillis() - eventTime);
            handler.postDelayed(
                    secondaryButtonHoldRunnable,
                    Math.max(0, PHYSICAL_LONG_PRESS_MS - elapsedMs));
        }
    }

    private boolean finishSecondaryButtonHold() {
        cancelSecondaryButtonHoldTimer();
        if (!gestureState.finishSecondaryButtonHold()) {
            return false;
        }

        if (buttonController.releaseButton(MouseButtonPacket.BUTTON_RIGHT)) {
            hapticFeedback.performButtonRelease();
        }
        return true;
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
            cancelSecondaryButtonHoldTimer();
            return;
        }

        // Check if we've exceeded the maximum distance moved
        distanceMoved += Math.sqrt(Math.pow(eventX - lastTouchX, 2) +
                Math.pow(eventY - lastTouchY, 2));
        if (distanceMoved >= TAP_DISTANCE_THRESHOLD) {
            confirmedMove = true;
            gestureState.cancelTwoFingerTap();
            cancelSecondaryButtonHoldTimer();
        }
    }

    private void checkForConfirmedScroll() {
        // Enter scrolling mode if we've already left the tap zone
        // and we have 2 fingers on screen. Leave scroll mode if
        // we no longer have 2 fingers on screen
        confirmedScroll = (actionIndex == 0 && pointerCount == 2 && confirmedMove &&
                !gestureState.isMouseButtonActive());
    }

    private void finishPrimaryTouch(int eventX, int eventY, long eventTime) {
        if (!primaryLongPressActive) {
            cancelPrimaryLongPressTimer();
        }

        if (doubleTapCandidate) {
            if (eventTime - doubleTapStartTime >= DOUBLE_TAP_DRAG_HOLD_MS) {
                beginDoubleTapDrag();
            }

            if (doubleTapDragActive) {
                sendDragMoveTo(eventX, eventY, eventTime);
                finishDoubleTapDrag();
            }
            else {
                finishDoubleClick();
            }
            return;
        }

        if (doubleTapDragActive || confirmedDrag) {
            sendDragMoveTo(eventX, eventY, eventTime);
            finishDoubleTapDrag();
            return;
        }

        if (primaryLongPressActive) {
            finishPrimaryLongPress(eventX, eventY, eventTime);
            return;
        }

        if (primaryMoveActive) {
            sendPrimaryMoveTo(eventX, eventY, eventTime);
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
        motionSender.beginPointerMotion(eventTime);

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
                    enterMultiTouchSession(eventTime);
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
            if (finishSecondaryButtonHold()) {
                return;
            }

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
            if (actionIndex == 0 && gestureState.isSecondaryButtonHoldActive()) {
                sendPrimaryMoveTo(eventX, eventY, eventTime);
                return true;
            }

            if (actionIndex == 0 && doubleTapCandidate) {
                if (hasDoubleTapDragMovement(eventX, eventY)) {
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
                    sendDragMoveTo(eventX, eventY, eventTime);
                }
                else {
                    sendPrimaryMoveTo(eventX, eventY, eventTime);
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

        cancelSecondaryButtonHoldTimer();
        resetPrimaryGestureTracking();
        gestureState.finishTouchSession();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setPointerCount(int pointerCount) {
        setPointerCount(pointerCount, SystemClock.uptimeMillis());
    }

    @Override
    public void setPointerCount(int pointerCount, long eventTime) {
        int oldPointerCount = this.pointerCount;
        this.pointerCount = pointerCount;

        if (actionIndex == 0 && oldPointerCount < 2 && pointerCount >= 2) {
            enterMultiTouchSession(eventTime);
        }
        else if (actionIndex == 0 && oldPointerCount >= 2 && pointerCount < 2) {
            finishSecondaryButtonHold();
            cancelSecondaryButtonHoldTimer();
        }
    }

    @Override
    public void suspendPendingPressRecognition() {
        cancelPrimaryLongPressTimer();
        cancelSecondaryButtonHoldTimer();
    }
}
