package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

public class RelativeTouchContext implements TouchContext {
    private int lastTouchX = 0;
    private int lastTouchY = 0;
    private int originalTouchX = 0;
    private int originalTouchY = 0;
    private long originalTouchTime = 0;
    private boolean cancelled;
    private boolean confirmedMove;
    private boolean confirmedDrag;
    private boolean confirmedScroll;
    private boolean pendingTapDrag;
    private boolean pendingTapDragButtonDown;
    private boolean pendingTapDragUsingHeldTap;
    private double distanceMoved;
    private double xFactor, yFactor;
    private int pointerCount;
    private int maxPointerCountInGesture;
    private final boolean[] pendingButtonUp = new boolean[MouseButtonPacket.BUTTON_X2];
    private final TouchpadTapDragTracker tapDragTracker;

    private final NvConnection conn;
    private final int actionIndex;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final PreferenceConfiguration prefConfig;
    private final Handler handler;
    private final TouchpadGestureState gestureState;

    private final Runnable dragTimerRunnable = new Runnable() {
        @Override
        public void run() {
            // Check if someone already set move
            if (confirmedMove) {
                return;
            }

            // The drag should only be processed for the primary finger
            if (actionIndex != maxPointerCountInGesture - 1) {
                return;
            }

            // We haven't been cancelled before the timer expired so begin dragging
            beginConfirmedDrag(getMouseButtonIndex());
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
    private static final int DRAG_TIME_THRESHOLD = 650;
    private static final int TAP_CLICK_BUTTON_UP_DELAY = 160;

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
        this.tapDragTracker = new TouchpadTapDragTracker();
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

    private boolean isTap(long eventTime)
    {
        if (confirmedDrag || confirmedMove || confirmedScroll) {
            return false;
        }

        if (actionIndex != 0 && gestureState.isPrimaryDragActive()) {
            return false;
        }

        // If this input wasn't the last finger down, do not report
        // a tap. This ensures we don't report duplicate taps for each
        // finger on a multi-finger tap gesture
        if (actionIndex + 1 != maxPointerCountInGesture) {
            return false;
        }

        long timeDelta = eventTime - originalTouchTime;
        return isWithinTapBounds(lastTouchX, lastTouchY) && timeDelta <= TAP_TIME_THRESHOLD;
    }

    private byte getMouseButtonIndex()
    {
        if (actionIndex == 1) {
            return MouseButtonPacket.BUTTON_RIGHT;
        }
        else {
            return MouseButtonPacket.BUTTON_LEFT;
        }
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

    private boolean promotePendingTapToDrag(byte buttonIndex) {
        int index = getPendingButtonIndex(buttonIndex);
        if (!pendingButtonUp[index]) {
            return false;
        }

        handler.removeCallbacks(buttonUpRunnables[index]);
        pendingButtonUp[index] = false;
        return true;
    }

    private void beginPendingTapDrag(byte buttonIndex) {
        pendingTapDrag = true;
        pendingTapDragUsingHeldTap = promotePendingTapToDrag(buttonIndex);
        pendingTapDragButtonDown = true;

        if (!pendingTapDragUsingHeldTap) {
            conn.sendMouseButtonDown(buttonIndex);
        }
    }

    private void clearPendingTapDragState() {
        pendingTapDrag = false;
        pendingTapDragButtonDown = false;
        pendingTapDragUsingHeldTap = false;
    }

    private void finishPendingTapDragAsTap(byte buttonIndex, int eventX, int eventY,
                                           long eventTime) {
        if (!pendingTapDragButtonDown) {
            clearPendingTapDragState();
            return;
        }

        boolean usingHeldTap = pendingTapDragUsingHeldTap;
        clearPendingTapDragState();

        conn.sendMouseButtonUp(buttonIndex);

        if (usingHeldTap) {
            sendTapClick(buttonIndex);
        }
    }

    private void cancelPendingTapDrag(byte buttonIndex) {
        if (pendingTapDragButtonDown) {
            conn.sendMouseButtonUp(buttonIndex);
        }

        clearPendingTapDragState();
    }

    private void sendTapClick(byte buttonIndex) {
        completePendingTapClick(buttonIndex);

        conn.sendMouseButtonDown(buttonIndex);

        int index = getPendingButtonIndex(buttonIndex);
        pendingButtonUp[index] = true;
        handler.postDelayed(buttonUpRunnables[index], getTapClickButtonUpDelay(buttonIndex));
    }

    private int getTapClickButtonUpDelay(byte buttonIndex) {
        return buttonIndex == MouseButtonPacket.BUTTON_LEFT ?
                TAP_CLICK_BUTTON_UP_DELAY : 100;
    }

    private void beginConfirmedDrag(byte buttonIndex) {
        boolean alreadyDown = pendingTapDragButtonDown || promotePendingTapToDrag(buttonIndex);
        cancelDragTimer();

        clearPendingTapDragState();
        confirmedDrag = true;
        if (actionIndex == 0 && buttonIndex == MouseButtonPacket.BUTTON_LEFT) {
            gestureState.setPrimaryDragActive(true);
        }
        if (!alreadyDown) {
            conn.sendMouseButtonDown(buttonIndex);
        }
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger)
    {
        // Get the view dimensions to scale inputs on this touch
        xFactor = referenceWidth / (double)targetView.getWidth();
        yFactor = referenceHeight / (double)targetView.getHeight();

        originalTouchX = lastTouchX = eventX;
        originalTouchY = lastTouchY = eventY;

        if (isNewFinger) {
            maxPointerCountInGesture = pointerCount;
            originalTouchTime = eventTime;
            cancelled = confirmedDrag = confirmedMove = confirmedScroll = false;
            clearPendingTapDragState();
            distanceMoved = 0;

            if (pointerCount <= 1) {
                gestureState.resetTwoFingerTap();
            }
            else if (pointerCount == 2) {
                gestureState.beginTwoFingerTap(eventTime);
            }
            else {
                gestureState.cancelTwoFingerTap();
            }

            if (actionIndex == 0) {
                if (tapDragTracker.isTapDragStart(eventX, eventY, eventTime)) {
                    tapDragTracker.clear();
                    beginPendingTapDrag(MouseButtonPacket.BUTTON_LEFT);
                    startDragTimer();
                }
                else {
                    // Start the timer for engaging a drag
                    startDragTimer();
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

        // Cancel the drag timer
        cancelDragTimer();

        byte buttonIndex = getMouseButtonIndex();
        int twoFingerTapResult = gestureState.updateTwoFingerTapOnPointerUp(pointerCount,
                eventTime, TAP_TIME_THRESHOLD);
        if (twoFingerTapResult == TouchpadGestureState.TWO_FINGER_TAP_COMPLETE) {
            sendTapClick(MouseButtonPacket.BUTTON_RIGHT);
            return;
        }
        else if (twoFingerTapResult == TouchpadGestureState.TWO_FINGER_TAP_SUPPRESS) {
            return;
        }

        if (confirmedDrag) {
            // Raise the button after a drag
            conn.sendMouseButtonUp(buttonIndex);
            confirmedDrag = false;
            if (actionIndex == 0 && buttonIndex == MouseButtonPacket.BUTTON_LEFT) {
                gestureState.setPrimaryDragActive(false);
            }
        }
        else if (pendingTapDrag) {
            if (isTap(eventTime)) {
                if (buttonIndex == MouseButtonPacket.BUTTON_LEFT) {
                    tapDragTracker.recordTap(eventX, eventY, eventTime);
                }
                finishPendingTapDragAsTap(buttonIndex, eventX, eventY, eventTime);
            }
            else {
                cancelPendingTapDrag(buttonIndex);
            }
        }
        else if (isTap(eventTime))
        {
            if (buttonIndex == MouseButtonPacket.BUTTON_LEFT) {
                tapDragTracker.recordTap(eventX, eventY, eventTime);
            }

            sendTapClick(buttonIndex);
        }
    }

    private void startDragTimer() {
        cancelDragTimer();
        handler.postDelayed(dragTimerRunnable, DRAG_TIME_THRESHOLD);
    }

    private void cancelDragTimer() {
        handler.removeCallbacks(dragTimerRunnable);
    }

    private void checkForConfirmedMove(int eventX, int eventY) {
        // If we've already confirmed something, get out now
        if (confirmedMove || confirmedDrag) {
            return;
        }

        // If it leaves the tap bounds before the drag time expires, it's a move.
        if (!isWithinTapBounds(eventX, eventY)) {
            confirmedMove = true;
            gestureState.cancelTwoFingerTap();
            cancelDragTimer();
            return;
        }

        // Check if we've exceeded the maximum distance moved
        distanceMoved += Math.sqrt(Math.pow(eventX - lastTouchX, 2) + Math.pow(eventY - lastTouchY, 2));
        if (distanceMoved >= TAP_DISTANCE_THRESHOLD) {
            confirmedMove = true;
            gestureState.cancelTwoFingerTap();
            cancelDragTimer();
            return;
        }
    }

    private void checkForConfirmedScroll() {
        // Enter scrolling mode if we've already left the tap zone
        // and we have 2 fingers on screen. Leave scroll mode if
        // we no longer have 2 fingers on screen
        confirmedScroll = (actionIndex == 0 && pointerCount == 2 && confirmedMove &&
                !gestureState.isPrimaryDragActive());
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime)
    {
        if (cancelled) {
            return true;
        }

        if (eventX != lastTouchX || eventY != lastTouchY)
        {
            if (pendingTapDrag && actionIndex == 0) {
                beginConfirmedDrag(MouseButtonPacket.BUTTON_LEFT);
            }

            checkForConfirmedMove(eventX, eventY);
            checkForConfirmedScroll();

            // We only send moves and drags for the primary touch point
            if (actionIndex == 0) {
                int deltaX = eventX - lastTouchX;
                int deltaY = eventY - lastTouchY;

                // Scale the deltas based on the factors passed to our constructor
                deltaX = (int) Math.round((double) Math.abs(deltaX) * xFactor);
                deltaY = (int) Math.round((double) Math.abs(deltaY) * yFactor);

                // Fix up the signs
                if (eventX < lastTouchX) {
                    deltaX = -deltaX;
                }
                if (eventY < lastTouchY) {
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

        // Cancel the drag timer
        cancelDragTimer();
        cancelPendingTapDrag(MouseButtonPacket.BUTTON_LEFT);

        // If it was a confirmed drag, we'll need to raise the button now
        if (confirmedDrag) {
            conn.sendMouseButtonUp(getMouseButtonIndex());
            confirmedDrag = false;
            if (actionIndex == 0 && getMouseButtonIndex() == MouseButtonPacket.BUTTON_LEFT) {
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
        this.pointerCount = pointerCount;

        if (pointerCount > maxPointerCountInGesture) {
            maxPointerCountInGesture = pointerCount;
        }
    }
}
