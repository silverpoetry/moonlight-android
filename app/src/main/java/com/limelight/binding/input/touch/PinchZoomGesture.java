package com.limelight.binding.input.touch;

/**
 * Classifies only the opening movement of a two-finger gesture. Once ordinary
 * input wins, adding fingers or changing direction cannot steal that sequence.
 * Coordinates and movement thresholds are in the unscaled input surface.
 */
final class PinchZoomGesture {
    enum Action { DOWN, POINTER_DOWN, MOVE, POINTER_UP, UP, CANCEL }
    enum Result { FORWARD, DEFER, BEGIN_ZOOM, REBASE_ZOOM, ZOOM, CONSUME }
    private enum State { IDLE, SINGLE, PENDING, REMOTE, ZOOM, ZOOM_REGRIP, TAIL }

    private final float touchSlop;
    private final long decisionTimeoutMs;
    private State state = State.IDLE;
    private long deadlineMs;
    private float downX;
    private float downY;
    private float initialDx;
    private float initialDy;
    private float initialSpan;
    private float initialFocusX;
    private float initialFocusY;

    PinchZoomGesture(float touchSlop, long decisionTimeoutMs) {
        this.touchSlop = Math.max(1f, touchSlop);
        this.decisionTimeoutMs = Math.max(1L, decisionTimeoutMs);
    }

    Result onTouch(Action action, long timeMs, int count,
                   float x0, float y0, float x1, float y1) {
        if (action == Action.DOWN) {
            state = State.SINGLE;
            downX = x0;
            downY = y0;
            deadlineMs = timeMs + decisionTimeoutMs;
            return Result.FORWARD;
        }
        if (action == Action.UP || action == Action.CANCEL) {
            boolean consumed = isConsuming();
            reset();
            return consumed ? Result.CONSUME : Result.FORWARD;
        }
        if (state == State.TAIL) {
            return Result.CONSUME;
        }
        if (state == State.ZOOM_REGRIP) {
            if (count == 2 && (action == Action.POINTER_DOWN || action == Action.MOVE)) {
                state = State.ZOOM;
                return Result.REBASE_ZOOM;
            }
            return Result.CONSUME;
        }
        if (action == Action.POINTER_UP ||
                state == State.ZOOM && count != 2) {
            if (isConsuming()) {
                state = State.ZOOM_REGRIP;
                return Result.CONSUME;
            }
            state = State.REMOTE;
            return Result.FORWARD;
        }
        if (action == Action.POINTER_DOWN) {
            if (state != State.SINGLE || count != 2 || timeMs >= deadlineMs ||
                    distance(x0 - downX, y0 - downY) > touchSlop) {
                state = State.REMOTE;
                return Result.FORWARD;
            }
            initialDx = x1 - x0;
            initialDy = y1 - y0;
            initialSpan = distance(initialDx, initialDy);
            if (initialSpan < touchSlop) {
                state = State.REMOTE;
                return Result.FORWARD;
            }
            initialFocusX = (x0 + x1) / 2f;
            initialFocusY = (y0 + y1) / 2f;
            deadlineMs = timeMs + decisionTimeoutMs;
            state = State.PENDING;
            return Result.DEFER;
        }
        if (state == State.ZOOM) {
            return Result.ZOOM;
        }
        if (state == State.SINGLE) {
            if (timeMs >= deadlineMs ||
                    distance(x0 - downX, y0 - downY) > touchSlop) {
                state = State.REMOTE;
            }
            return Result.FORWARD;
        }
        if (state != State.PENDING) {
            return Result.FORWARD;
        }
        if (count != 2 || timeMs >= deadlineMs) {
            state = State.REMOTE;
            return Result.FORWARD;
        }

        float dx = x1 - x0;
        float dy = y1 - y0;
        float spanChange = Math.abs(distance(dx, dy) - initialSpan);
        float focusMovement = distance(
                (x0 + x1) / 2f - initialFocusX,
                (y0 + y1) / 2f - initialFocusY);
        float spanThreshold = 2f * touchSlop;
        float relativeMovement = distance(dx - initialDx, dy - initialDy);
        // Compare relative finger motion with common translation. An anchored
        // finger makes these equal, even when the moving finger follows an arc.
        // At least half the relative motion must change the span, with a slop
        // margin, to distinguish pinching from rotation. These distances are
        // invariant under reversing the path and independent of video scale.
        if (spanChange >= spanThreshold &&
                relativeMovement + spanThreshold >= 2f * focusMovement &&
                2f * spanChange + touchSlop >= relativeMovement) {
            state = State.ZOOM;
            return Result.BEGIN_ZOOM;
        }
        if ((focusMovement > touchSlop &&
                2f * focusMovement > relativeMovement + spanThreshold) ||
                (relativeMovement > spanThreshold &&
                        relativeMovement > 2f * spanChange + touchSlop)) {
            state = State.REMOTE;
            return Result.FORWARD;
        }
        return Result.DEFER;
    }

    boolean isPending() {
        return state == State.PENDING;
    }

    boolean isConsuming() {
        return state == State.ZOOM || state == State.ZOOM_REGRIP || state == State.TAIL;
    }

    long getDeadlineMs() {
        return deadlineMs;
    }

    void useRemoteInput() {
        state = isConsuming() ? State.TAIL : State.REMOTE;
    }

    void reset() {
        state = State.IDLE;
    }

    private static float distance(float x, float y) {
        return (float) Math.hypot(x, y);
    }
}
