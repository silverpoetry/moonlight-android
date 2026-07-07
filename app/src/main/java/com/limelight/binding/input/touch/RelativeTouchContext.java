package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

public class RelativeTouchContext implements TouchContext {
    private int lastTouchX = 0;
    private int lastTouchY = 0;
    private int originalTouchX = 0;
    private int originalTouchY = 0;
    private boolean cancelled;
    private boolean confirmedMove;
    private boolean confirmedDrag;
    private boolean confirmedScroll;
    private boolean primaryButtonDown;
    private boolean primaryPressActive;
    private boolean primaryClickHoldElapsed;
    private boolean primaryMoveActive;
    private boolean waitingForSecondTap;
    private boolean doubleTapCandidate;
    private boolean doubleTapDragActive;
    private boolean doubleTapDragNeedsPrimerMove;
    private boolean doubleTapDragPrimerMoveActive;
    private boolean secondClickButtonDown;
    private int doubleTapStartX;
    private int doubleTapStartY;
    private int doubleTapDragPrimerBaseX;
    private int doubleTapDragPrimerBaseY;
    private int doubleTapDragPrimerSentX;
    private int doubleTapDragPrimerSentY;
    private int doubleTapDragPrimerStepIndex;
    private int pendingDoubleTapDragMoveX;
    private int pendingDoubleTapDragMoveY;
    private long firstTapStartTime;
    private long doubleTapStartTime;
    private long currentEventTime;
    private double distanceMoved;
    private double xFactor, yFactor;
    private int pointerCount;
    private final boolean[] pendingButtonUp = new boolean[MouseButtonPacket.BUTTON_X2];

    private final NvConnection conn;
    private final int actionIndex;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final PreferenceConfiguration prefConfig;
    private final Handler handler;
    private final TouchpadGestureState gestureState;

    private final Runnable primaryClickHoldRunnable = new Runnable() {
        @Override
        public void run() {
            currentEventTime = SystemClock.uptimeMillis();
            primaryClickHoldElapsed = true;

            if (doubleTapCandidate) {
                return;
            }

            if (primaryPressActive) {
                beginPrimaryMove();
            }
            else if (waitingForSecondTap) {
                releasePrimaryButton();
                clearPrimaryClickState();
            }
        }
    };

    private final Runnable doubleTapDragRunnable = new Runnable() {
        @Override
        public void run() {
            currentEventTime = SystemClock.uptimeMillis();
            if (doubleTapCandidate && primaryPressActive && !cancelled) {
                beginDoubleTapDrag();
            }
        }
    };

    private final Runnable secondClickDownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!cancelled && !secondClickButtonDown) {
                conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                secondClickButtonDown = true;
            }
        }
    };

    private final Runnable secondClickUpRunnable = new Runnable() {
        @Override
        public void run() {
            releaseSecondClickButton();
        }
    };

    private final Runnable doubleTapDragPrimerMoveRunnable = new Runnable() {
        @Override
        public void run() {
            runDoubleTapDragPrimerMoveStep();
        }
    };

    // Indexed by MouseButtonPacket.BUTTON_XXX - 1
    private final Runnable[] buttonUpRunnables = new Runnable[] {
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_LEFT);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_MIDDLE);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_X1);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_X2);
                }
            }
    };

    private static final int TAP_MOVEMENT_THRESHOLD = 35;
    private static final int TAP_DISTANCE_THRESHOLD = 45;
    private static final int TAP_TIME_THRESHOLD = 250;
    private static final int PRIMARY_CLICK_HOLD_MS = 200;
    private static final int DOUBLE_TAP_DRAG_HOLD_MS = 300;
    private static final int DOUBLE_TAP_DRAG_DISTANCE_THRESHOLD = 8;
    private static final int DOUBLE_TAP_DRAG_PRIMER_INTERVAL_MS = 2;
    private static final int[] DOUBLE_TAP_DRAG_PRIMER_STEPS = new int[] { 1, 3, 5, 7 };
    private static final int SECOND_CLICK_DOWN_DELAY_MS = 30;
    private static final int SECOND_CLICK_UP_DELAY_MS = 80;
    private static final int TAP_CLICK_BUTTON_UP_DELAY = 100;

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
        this.conn = conn;
        this.actionIndex = actionIndex;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.targetView = view;
        this.prefConfig = prefConfig;
        this.handler = new Handler(Looper.getMainLooper());
        this.gestureState = gestureState;
    }

    @Override
    public int getActionIndex()
    {
        return actionIndex;
    }

    private boolean isWithinTapBounds(int touchX, int touchY)
    {
        int xDelta = Math.abs(touchX - originalTouchX);
        int yDelta = Math.abs(touchY - originalTouchY);
        return xDelta <= TAP_MOVEMENT_THRESHOLD &&
                yDelta <= TAP_MOVEMENT_THRESHOLD;
    }

    private int getPendingButtonIndex(byte buttonIndex) {
        return buttonIndex - 1;
    }

    private void sendMouseMovePacket(short scaledDeltaX, short scaledDeltaY) {
        if (prefConfig.absoluteMouseMode) {
            conn.sendMouseMoveAsMousePosition(
                    scaledDeltaX,
                    scaledDeltaY,
                    (short) targetView.getWidth(),
                    (short) targetView.getHeight());
        }
        else {
            conn.sendMouseMove(scaledDeltaX, scaledDeltaY);
        }
    }

    private int scaleTouchDelta(int delta, double factor, int sensitivity) {
        int scaledDelta = (int) Math.round((double) Math.abs(delta) * factor);
        if (delta < 0) {
            scaledDelta = -scaledDelta;
        }

        return (short) (scaledDelta * sensitivity * 0.01f);
    }

    private static int getStepTowards(int remainingDelta, int maxStep) {
        if (remainingDelta == 0) {
            return 0;
        }

        return Integer.signum(remainingDelta) * Math.min(Math.abs(remainingDelta), maxStep);
    }

    private void beginDoubleTapDragPrimerMove(int eventX, int eventY) {
        doubleTapDragPrimerBaseX = lastTouchX;
        doubleTapDragPrimerBaseY = lastTouchY;
        doubleTapDragPrimerSentX = 0;
        doubleTapDragPrimerSentY = 0;
        doubleTapDragPrimerStepIndex = 0;
        pendingDoubleTapDragMoveX = eventX;
        pendingDoubleTapDragMoveY = eventY;
        doubleTapDragPrimerMoveActive = true;

        handler.removeCallbacks(doubleTapDragPrimerMoveRunnable);
        runDoubleTapDragPrimerMoveStep();
    }

    private void runDoubleTapDragPrimerMoveStep() {
        if (!doubleTapDragPrimerMoveActive) {
            return;
        }

        if (cancelled || !(doubleTapDragActive || confirmedDrag)) {
            cancelDoubleTapDragPrimerMove();
            return;
        }

        int targetDeltaX = scaleTouchDelta(
                pendingDoubleTapDragMoveX - doubleTapDragPrimerBaseX,
                xFactor,
                prefConfig.mouseTouchPadSensitityX);
        int targetDeltaY = scaleTouchDelta(
                pendingDoubleTapDragMoveY - doubleTapDragPrimerBaseY,
                yFactor,
                prefConfig.mouseTouchPadSensitityY);

        if (doubleTapDragPrimerStepIndex >= DOUBLE_TAP_DRAG_PRIMER_STEPS.length) {
            finishDoubleTapDragPrimerMove(true);
            return;
        }

        int stepSize = DOUBLE_TAP_DRAG_PRIMER_STEPS[doubleTapDragPrimerStepIndex++];
        int stepX = getStepTowards(targetDeltaX - doubleTapDragPrimerSentX, stepSize);
        int stepY = getStepTowards(targetDeltaY - doubleTapDragPrimerSentY, stepSize);

        if (stepX == 0 && stepY == 0) {
            finishDoubleTapDragPrimerMove(false);
            return;
        }

        sendMouseMovePacket((short) stepX, (short) stepY);
        doubleTapDragPrimerSentX += stepX;
        doubleTapDragPrimerSentY += stepY;

        targetDeltaX = scaleTouchDelta(
                pendingDoubleTapDragMoveX - doubleTapDragPrimerBaseX,
                xFactor,
                prefConfig.mouseTouchPadSensitityX);
        targetDeltaY = scaleTouchDelta(
                pendingDoubleTapDragMoveY - doubleTapDragPrimerBaseY,
                yFactor,
                prefConfig.mouseTouchPadSensitityY);

        if (doubleTapDragPrimerSentX == targetDeltaX &&
                doubleTapDragPrimerSentY == targetDeltaY) {
            finishDoubleTapDragPrimerMove(false);
        }
        else if (doubleTapDragPrimerStepIndex >= DOUBLE_TAP_DRAG_PRIMER_STEPS.length) {
            finishDoubleTapDragPrimerMove(true);
        }
        else {
            handler.postDelayed(doubleTapDragPrimerMoveRunnable,
                    DOUBLE_TAP_DRAG_PRIMER_INTERVAL_MS);
        }
    }

    private void finishDoubleTapDragPrimerMove(boolean completeToTarget) {
        handler.removeCallbacks(doubleTapDragPrimerMoveRunnable);

        if (completeToTarget) {
            int targetDeltaX = scaleTouchDelta(
                    pendingDoubleTapDragMoveX - doubleTapDragPrimerBaseX,
                    xFactor,
                    prefConfig.mouseTouchPadSensitityX);
            int targetDeltaY = scaleTouchDelta(
                    pendingDoubleTapDragMoveY - doubleTapDragPrimerBaseY,
                    yFactor,
                    prefConfig.mouseTouchPadSensitityY);

            int remainingX = targetDeltaX - doubleTapDragPrimerSentX;
            int remainingY = targetDeltaY - doubleTapDragPrimerSentY;
            if (remainingX != 0 || remainingY != 0) {
                sendMouseMovePacket((short) remainingX, (short) remainingY);
            }
        }

        doubleTapDragPrimerMoveActive = false;
        lastTouchX = pendingDoubleTapDragMoveX;
        lastTouchY = pendingDoubleTapDragMoveY;
        doubleTapDragPrimerBaseX = 0;
        doubleTapDragPrimerBaseY = 0;
        doubleTapDragPrimerSentX = 0;
        doubleTapDragPrimerSentY = 0;
        doubleTapDragPrimerStepIndex = 0;
        pendingDoubleTapDragMoveX = 0;
        pendingDoubleTapDragMoveY = 0;
    }

    private void sendPrimaryMoveTo(int eventX, int eventY) {
        if (eventX == lastTouchX && eventY == lastTouchY) {
            return;
        }

        int previousTouchX = lastTouchX;
        int previousTouchY = lastTouchY;
        int deltaX = eventX - previousTouchX;
        int deltaY = eventY - previousTouchY;

        // Scale the deltas based on the factors passed to our constructor
        deltaX = (int) Math.round((double) Math.abs(deltaX) * xFactor);
        deltaY = (int) Math.round((double) Math.abs(deltaY) * yFactor);

        // Fix up the signs
        if (eventX < previousTouchX) {
            deltaX = -deltaX;
        }
        if (eventY < previousTouchY) {
            deltaY = -deltaY;
        }

        if (pointerCount == 2 && !confirmedDrag) {
            if (confirmedScroll) {
                conn.sendMouseHighResScroll((short)(deltaY * SCROLL_SPEED_FACTOR));
            }
        } else {
            short scaledDeltaX = (short) (deltaX * prefConfig.mouseTouchPadSensitityX * 0.01f);
            short scaledDeltaY = (short) (deltaY * prefConfig.mouseTouchPadSensitityY * 0.01f);

            sendMouseMovePacket(scaledDeltaX, scaledDeltaY);
        }

        // If the scaling factor ended up rounding deltas to zero, wait until they are
        // non-zero to update lastTouch that way devices that report small touch events often
        // will work correctly
        if (deltaX != 0) {
            lastTouchX = eventX;
        }
        if (deltaY != 0) {
            lastTouchY = eventY;
        }
    }

    private boolean releasePendingButtonUp(byte buttonIndex) {
        int index = getPendingButtonIndex(buttonIndex);
        if (!pendingButtonUp[index]) {
            return false;
        }

        pendingButtonUp[index] = false;
        conn.sendMouseButtonUp(buttonIndex);
        return true;
    }

    private boolean completePendingTapClick(byte buttonIndex) {
        handler.removeCallbacks(buttonUpRunnables[getPendingButtonIndex(buttonIndex)]);
        return releasePendingButtonUp(buttonIndex);
    }

    private void cancelPendingTapClicks() {
        for (int i = 0; i < buttonUpRunnables.length; i++) {
            handler.removeCallbacks(buttonUpRunnables[i]);
            if (pendingButtonUp[i]) {
                pendingButtonUp[i] = false;
                conn.sendMouseButtonUp((byte) (i + 1));
            }
        }
    }

    private void sendTapClick(byte buttonIndex) {
        completePendingTapClick(buttonIndex);

        conn.sendMouseButtonDown(buttonIndex);

        int index = getPendingButtonIndex(buttonIndex);
        pendingButtonUp[index] = true;
        handler.postDelayed(buttonUpRunnables[index], TAP_CLICK_BUTTON_UP_DELAY);
    }

    private void pressPrimaryButton() {
        if (!primaryButtonDown) {
            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
            primaryButtonDown = true;
        }
    }

    private void releasePrimaryButton() {
        if (primaryButtonDown) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            primaryButtonDown = false;
        }
    }

    private void releaseSecondClickButton() {
        if (secondClickButtonDown) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            secondClickButtonDown = false;
        }
    }

    private void cancelSecondClick() {
        handler.removeCallbacks(secondClickDownRunnable);
        handler.removeCallbacks(secondClickUpRunnable);
        releaseSecondClickButton();
    }

    private void cancelPrimaryClickTimers() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        handler.removeCallbacks(doubleTapDragRunnable);
        handler.removeCallbacks(doubleTapDragPrimerMoveRunnable);
    }

    private void cancelDoubleTapDragPrimerMove() {
        handler.removeCallbacks(doubleTapDragPrimerMoveRunnable);
        doubleTapDragPrimerMoveActive = false;
        doubleTapDragPrimerBaseX = 0;
        doubleTapDragPrimerBaseY = 0;
        doubleTapDragPrimerSentX = 0;
        doubleTapDragPrimerSentY = 0;
        doubleTapDragPrimerStepIndex = 0;
        pendingDoubleTapDragMoveX = 0;
        pendingDoubleTapDragMoveY = 0;
    }

    private void clearPrimaryClickState() {
        primaryPressActive = false;
        primaryClickHoldElapsed = false;
        primaryMoveActive = false;
        waitingForSecondTap = false;
        doubleTapCandidate = false;
        doubleTapDragActive = false;
        doubleTapDragNeedsPrimerMove = false;
        doubleTapDragPrimerMoveActive = false;
        doubleTapDragPrimerBaseX = 0;
        doubleTapDragPrimerBaseY = 0;
        doubleTapDragPrimerSentX = 0;
        doubleTapDragPrimerSentY = 0;
        doubleTapDragPrimerStepIndex = 0;
        doubleTapStartX = 0;
        doubleTapStartY = 0;
        pendingDoubleTapDragMoveX = 0;
        pendingDoubleTapDragMoveY = 0;
        firstTapStartTime = 0;
        doubleTapStartTime = 0;
    }

    private void beginFirstPrimaryPress(int eventX, int eventY) {
        cancelPrimaryClickTimers();
        cancelSecondClick();
        clearPrimaryClickState();

        firstTapStartTime = currentEventTime;
        primaryPressActive = true;
        handler.postDelayed(primaryClickHoldRunnable, PRIMARY_CLICK_HOLD_MS);
    }

    private void beginPrimaryMove() {
        handler.removeCallbacks(primaryClickHoldRunnable);
        releasePrimaryButton();
        primaryClickHoldElapsed = true;
        primaryMoveActive = true;
        waitingForSecondTap = false;
    }

    private void beginSecondPrimaryPress(int eventX, int eventY, long eventTime) {
        handler.removeCallbacks(primaryClickHoldRunnable);
        cancelSecondClick();

        primaryPressActive = true;
        primaryClickHoldElapsed = false;
        waitingForSecondTap = false;
        doubleTapCandidate = true;
        doubleTapDragActive = false;
        doubleTapDragNeedsPrimerMove = false;
        doubleTapDragPrimerMoveActive = false;
        // The left button is already held from the first tap. Track movement from
        // the second contact so the gap between taps is not injected as cursor motion.
        doubleTapStartX = eventX;
        doubleTapStartY = eventY;
        doubleTapStartTime = eventTime;
        originalTouchX = eventX;
        originalTouchY = eventY;
        lastTouchX = eventX;
        lastTouchY = eventY;

        pressPrimaryButton();
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

        pressPrimaryButton();
        gestureState.setPrimaryDragActive(true);
    }

    private void sendDragMoveTo(int eventX, int eventY) {
        if (doubleTapDragNeedsPrimerMove) {
            doubleTapDragNeedsPrimerMove = false;
            beginDoubleTapDragPrimerMove(eventX, eventY);
            return;
        }

        if (doubleTapDragPrimerMoveActive) {
            pendingDoubleTapDragMoveX = eventX;
            pendingDoubleTapDragMoveY = eventY;
            return;
        }

        sendPrimaryMoveTo(eventX, eventY);
    }

    private void finishDoubleTapDrag() {
        handler.removeCallbacks(doubleTapDragRunnable);
        cancelDoubleTapDragPrimerMove();
        releasePrimaryButton();
        confirmedDrag = false;
        gestureState.setPrimaryDragActive(false);
        clearPrimaryClickState();
    }

    private void finishDoubleClick() {
        handler.removeCallbacks(doubleTapDragRunnable);
        cancelDoubleTapDragPrimerMove();

        releasePrimaryButton();
        clearPrimaryClickState();

        handler.postDelayed(secondClickDownRunnable, SECOND_CLICK_DOWN_DELAY_MS);
        handler.postDelayed(secondClickUpRunnable, SECOND_CLICK_UP_DELAY_MS);
    }

    private void cancelPrimaryClickForMultiTouch() {
        cancelPrimaryClickTimers();
        cancelDoubleTapDragPrimerMove();
        cancelSecondClick();
        releasePrimaryButton();
        confirmedDrag = false;
        gestureState.setPrimaryDragActive(false);
        clearPrimaryClickState();
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

        if (!primaryButtonDown) {
            primaryPressActive = false;
            if (primaryClickHoldElapsed) {
                clearPrimaryClickState();
            }
            else {
                pressPrimaryButton();
                waitingForSecondTap = true;
            }
            return;
        }

        primaryPressActive = false;
        if (primaryClickHoldElapsed) {
            releasePrimaryButton();
            clearPrimaryClickState();
        }
        else {
            waitingForSecondTap = true;
        }
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger)
    {
        currentEventTime = eventTime;

        // Get the view dimensions to scale inputs on this touch
        xFactor = referenceWidth / (double)targetView.getWidth();
        yFactor = referenceHeight / (double)targetView.getHeight();

        originalTouchX = lastTouchX = eventX;
        originalTouchY = lastTouchY = eventY;

        if (isNewFinger) {
            cancelled = confirmedDrag = confirmedMove = confirmedScroll = false;
            distanceMoved = 0;

            if (pointerCount == 1) {
                gestureState.resetTouchGesture();
            }
            else if (pointerCount == 2) {
                gestureState.beginTwoFingerTap(eventTime);
            }
            else {
                gestureState.cancelTwoFingerTap();
            }

            if (actionIndex == 0) {
                if (pointerCount == 1) {
                    if (waitingForSecondTap && primaryButtonDown) {
                        beginSecondPrimaryPress(eventX, eventY, eventTime);
                    }
                    else {
                        beginFirstPrimaryPress(eventX, eventY);
                    }
                }
                else {
                    cancelPrimaryClickForMultiTouch();
                }
            }
        }

        return true;
    }

    @Override
    public void touchUpEvent(int eventX, int eventY, long eventTime)
    {
        currentEventTime = eventTime;

        if (cancelled) {
            return;
        }

        int twoFingerTapResult = gestureState.onPointerUp(pointerCount, eventTime,
                TAP_TIME_THRESHOLD);
        if (twoFingerTapResult == TouchpadGestureState.TWO_FINGER_TAP_SUPPRESS) {
            return;
        }
        else if (twoFingerTapResult == TouchpadGestureState.TWO_FINGER_TAP_COMPLETE) {
            sendTapClick(MouseButtonPacket.BUTTON_RIGHT);
            return;
        }

        if (actionIndex == 0) {
            finishPrimaryTouch(eventX, eventY, eventTime);
        }
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime)
    {
        currentEventTime = eventTime;

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

            if (actionIndex == 0 && primaryPressActive && !primaryButtonDown &&
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
        currentEventTime = SystemClock.uptimeMillis();

        cancelPrimaryClickTimers();
        cancelSecondClick();
        cancelPendingTapClicks();
        releasePrimaryButton();
        clearPrimaryClickState();

        if (confirmedDrag) {
            confirmedDrag = false;
            if (actionIndex == 0) {
                gestureState.setPrimaryDragActive(false);
            }
        }
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
            cancelPrimaryClickForMultiTouch();
        }
    }
}
