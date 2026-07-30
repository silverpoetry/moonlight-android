package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Objects;

/**
 * Arbitrates the ambiguous prefix shared by ordinary two-finger input and a
 * configured multi-finger software-keyboard tap.
 *
 * <p>The second contact is held locally only while the gesture can still
 * become the configured keyboard tap. Movement, an early release, the
 * recognition deadline, or a competing press releases the original events
 * through the normal input pipeline in order. When the feature is disabled,
 * this coordinator does not buffer or delay input.</p>
 */
public final class SoftKeyboardGestureCoordinator {
    public interface Listener {
        void dispatchDeferredTouchEvent(View eventView, MotionEvent event);

        void onGesturePrefixDeferred();

        void onKeyboardGestureRecognized();
    }

    private final SoftKeyboardGestureDetector detector;
    private final BufferedTouchEventDispatcher eventDispatcher;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable decisionDeadlineRunnable = this::releaseDeferredGesture;

    private View deferredEventView;

    public SoftKeyboardGestureCoordinator(
            int movementThresholdPx,
            Listener listener) {
        detector = new SoftKeyboardGestureDetector(movementThresholdPx);
        eventDispatcher = new BufferedTouchEventDispatcher();
        this.listener = Objects.requireNonNull(listener);
    }

    /**
     * Handles a touchscreen event before it reaches any host input path.
     *
     * @return true if the event is locally owned or queued for replay
     */
    public boolean onTouchEvent(
            View eventView,
            MotionEvent event,
            int configuredFingerCount) {
        if (eventDispatcher.queueIfReplaying(eventView, event)) {
            return true;
        }

        SoftKeyboardGestureDetector.Result result =
                detector.onTouchEvent(event, configuredFingerCount);
        switch (result) {
            case STARTED:
                deferredEventView = eventView;
                listener.onGesturePrefixDeferred();
                scheduleDecisionDeadline();
                return true;

            case BUFFERING:
                return true;

            case FORWARD:
                releaseBufferedEvents();
                return true;

            case TRIGGERED:
                cancelDecisionDeadline();
                deferredEventView = null;
                listener.onKeyboardGestureRecognized();
                return true;

            case CONSUMED:
                return true;

            case NONE:
            default:
                return false;
        }
    }

    /**
     * Resolves a pending keyboard prefix in favor of normal input.
     *
     * <p>This is used by competing recognizers such as force press, which must
     * first receive the held pointer-down events before they can take
     * ownership.</p>
     */
    public void resolveForCompetingGesture() {
        if (detector.releasePendingGesture() ==
                SoftKeyboardGestureDetector.Result.FORWARD) {
            releaseBufferedEvents();
        }
    }

    public boolean isDispatchingDeferredEvents() {
        return eventDispatcher.isInternalDispatch();
    }

    public void cancel() {
        cancelDecisionDeadline();
        deferredEventView = null;
        detector.reset();
        eventDispatcher.cancel();
    }

    private void scheduleDecisionDeadline() {
        cancelDecisionDeadline();
        long delayMs = Math.max(
                0,
                detector.getDecisionDeadlineMs() -
                        SystemClock.uptimeMillis());
        handler.postDelayed(decisionDeadlineRunnable, delayMs);
    }

    private void releaseDeferredGesture() {
        if (detector.releasePendingGesture() ==
                SoftKeyboardGestureDetector.Result.FORWARD) {
            releaseBufferedEvents();
        }
    }

    private void releaseBufferedEvents() {
        cancelDecisionDeadline();
        View eventView = deferredEventView;
        deferredEventView = null;
        List<MotionEvent> events = detector.takeBufferedEvents();
        eventDispatcher.dispatch(
                eventView,
                events,
                listener::dispatchDeferredTouchEvent);
    }

    private void cancelDecisionDeadline() {
        handler.removeCallbacks(decisionDeadlineRunnable);
    }
}
