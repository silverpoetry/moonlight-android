package com.limelight.binding.input.touch;

import android.os.Handler;

// Sends the first pixel of a double-tap drag before completing to the latest
// target. This avoids the first real drag packet being interpreted as a cursor
// jump by the host side.
final class TouchpadDragPrimer {
    interface Listener {
        boolean isDragStillActive();
        void onDragPrimerFinished(int touchX, int touchY);
    }

    private static final int STEP_INTERVAL_MS = 2;
    private static final int[] STEP_SEQUENCE = new int[] { 1 };

    private final Handler handler;
    private final TouchpadMotionSender motionSender;
    private final Listener listener;
    private final TouchpadMotionSender.MotionDelta transformedDelta =
            new TouchpadMotionSender.MotionDelta();
    private final Runnable stepRunnable = new Runnable() {
        @Override
        public void run() {
            runStep();
        }
    };

    private boolean active;
    private int sentDeltaX;
    private int sentDeltaY;
    private int stepIndex;
    private int pendingTouchX;
    private int pendingTouchY;
    private int targetDeltaX;
    private int targetDeltaY;

    TouchpadDragPrimer(Handler handler, TouchpadMotionSender motionSender, Listener listener) {
        this.handler = handler;
        this.motionSender = motionSender;
        this.listener = listener;
    }

    boolean isActive() {
        return active;
    }

    void begin(int baseTouchX, int baseTouchY, int targetTouchX, int targetTouchY,
               long eventTime) {
        sentDeltaX = 0;
        sentDeltaY = 0;
        stepIndex = 0;
        pendingTouchX = targetTouchX;
        pendingTouchY = targetTouchY;
        motionSender.transformTouchpadMove(targetTouchX - baseTouchX,
                targetTouchY - baseTouchY, eventTime, transformedDelta);
        targetDeltaX = transformedDelta.x;
        targetDeltaY = transformedDelta.y;
        active = true;

        handler.removeCallbacks(stepRunnable);
        runStep();
    }

    void updateTarget(int targetTouchX, int targetTouchY, long eventTime) {
        motionSender.transformTouchpadMove(targetTouchX - pendingTouchX,
                targetTouchY - pendingTouchY, eventTime, transformedDelta);
        targetDeltaX += transformedDelta.x;
        targetDeltaY += transformedDelta.y;
        pendingTouchX = targetTouchX;
        pendingTouchY = targetTouchY;
    }

    void cancel() {
        handler.removeCallbacks(stepRunnable);
        clear();
    }

    private void runStep() {
        if (!active) {
            return;
        }

        if (!listener.isDragStillActive()) {
            cancel();
            return;
        }

        int targetDeltaX = getTargetDeltaX();
        int targetDeltaY = getTargetDeltaY();

        if (stepIndex >= STEP_SEQUENCE.length) {
            finish(true);
            return;
        }

        int stepSize = STEP_SEQUENCE[stepIndex++];
        int stepX = getStepTowards(targetDeltaX - sentDeltaX, stepSize);
        int stepY = getStepTowards(targetDeltaY - sentDeltaY, stepSize);

        if (stepX == 0 && stepY == 0) {
            finish(false);
            return;
        }

        motionSender.sendMouseMovePacket(stepX, stepY);
        sentDeltaX += stepX;
        sentDeltaY += stepY;

        targetDeltaX = getTargetDeltaX();
        targetDeltaY = getTargetDeltaY();
        if (sentDeltaX == targetDeltaX && sentDeltaY == targetDeltaY) {
            finish(false);
        }
        else if (stepIndex >= STEP_SEQUENCE.length) {
            finish(true);
        }
        else {
            handler.postDelayed(stepRunnable, STEP_INTERVAL_MS);
        }
    }

    private int getTargetDeltaX() {
        return targetDeltaX;
    }

    private int getTargetDeltaY() {
        return targetDeltaY;
    }

    private void finish(boolean completeToTarget) {
        handler.removeCallbacks(stepRunnable);

        if (completeToTarget) {
            int remainingX = getTargetDeltaX() - sentDeltaX;
            int remainingY = getTargetDeltaY() - sentDeltaY;
            if (remainingX != 0 || remainingY != 0) {
                motionSender.sendMouseMovePacket(remainingX, remainingY);
            }
        }

        int completedTouchX = pendingTouchX;
        int completedTouchY = pendingTouchY;
        clear();
        listener.onDragPrimerFinished(completedTouchX, completedTouchY);
    }

    private void clear() {
        active = false;
        sentDeltaX = 0;
        sentDeltaY = 0;
        stepIndex = 0;
        pendingTouchX = 0;
        pendingTouchY = 0;
        targetDeltaX = 0;
        targetDeltaY = 0;
    }

    private static int getStepTowards(int remainingDelta, int maxStep) {
        if (remainingDelta == 0) {
            return 0;
        }

        return Integer.signum(remainingDelta) * Math.min(Math.abs(remainingDelta), maxStep);
    }
}
