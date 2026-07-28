package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Replays touch events released by a local gesture recognizer without collapsing a tap into
 * back-to-back down/up reports.
 *
 * <p>Movement-disqualified gestures are forwarded immediately so scrolling remains responsive.
 * A buffer released by a pointer-up preserves its recorded timing and holds native contacts long
 * enough for the host touchpad injector to emit a sustained-contact report. Input arriving during
 * that short replay window is queued and drained afterward in order.</p>
 */
public final class BufferedTouchEventDispatcher {
    @FunctionalInterface
    public interface Dispatcher {
        void dispatch(View eventView, MotionEvent event);
    }

    // Sunshine refreshes held synthetic touchpad contacts every 50 ms. Leave scheduling margin
    // so the refresh occurs before an UP packet released from the local gesture buffer.
    static final long MIN_NATIVE_TAP_HOLD_MS = 60;
    private static final long MAX_REPLAY_DURATION_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ReplayEvent> replayEvents = new ArrayList<>();
    private final List<QueuedInputEvent> queuedInputEvents = new ArrayList<>();

    private Dispatcher dispatcher;
    private boolean replaying;
    private boolean internalDispatch;

    /**
     * Queues an input event if a timed replay is active.
     *
     * @return {@code true} if ownership of a copy was taken and the caller must consume the event.
     */
    public boolean queueIfReplaying(View eventView, MotionEvent event) {
        if (!replaying || internalDispatch) {
            return false;
        }

        queuedInputEvents.add(new QueuedInputEvent(eventView, MotionEvent.obtain(event)));
        return true;
    }

    /**
     * Returns true while buffered events are being replayed through the normal
     * input pipeline. Side-channel observers that already saw the original
     * hardware event must skip the replay to avoid counting it twice.
     */
    public boolean isInternalDispatch() {
        return internalDispatch;
    }

    /**
     * Takes ownership of {@code events} and dispatches each event exactly once.
     */
    public void dispatch(View eventView, List<MotionEvent> events, Dispatcher dispatcher) {
        Objects.requireNonNull(events);
        Objects.requireNonNull(dispatcher);
        if (events.isEmpty()) {
            return;
        }

        cancel();
        this.dispatcher = dispatcher;

        if (!requiresTimedReplay(events)) {
            try {
                dispatchImmediately(eventView, events, dispatcher);
            }
            finally {
                this.dispatcher = null;
            }
            return;
        }

        replaying = true;
        long firstEventTime = events.get(0).getEventTime();
        int lastIndex = events.size() - 1;
        for (int i = 0; i < events.size(); i++) {
            MotionEvent event = events.get(i);
            long delayMs = Math.max(0, event.getEventTime() - firstEventTime);
            delayMs = Math.min(delayMs, MAX_REPLAY_DURATION_MS);
            if (i == lastIndex) {
                delayMs = Math.max(delayMs, MIN_NATIVE_TAP_HOLD_MS);
            }

            ReplayEvent replayEvent = new ReplayEvent(eventView, event, delayMs);
            replayEvents.add(replayEvent);
        }

        for (ReplayEvent replayEvent : new ArrayList<>(replayEvents)) {
            if (replayEvent.delayMs == 0) {
                replayEvent.run();
            }
            else {
                handler.postDelayed(replayEvent, replayEvent.delayMs);
            }
        }
    }

    /**
     * Cancels pending replay and recycles every event still owned by this dispatcher.
     */
    public void cancel() {
        for (ReplayEvent replayEvent : replayEvents) {
            handler.removeCallbacks(replayEvent);
            replayEvent.recycle();
        }
        replayEvents.clear();

        for (QueuedInputEvent queuedEvent : queuedInputEvents) {
            queuedEvent.event.recycle();
        }
        queuedInputEvents.clear();

        dispatcher = null;
        replaying = false;
        internalDispatch = false;
    }

    private static boolean requiresTimedReplay(List<MotionEvent> events) {
        int action = events.get(events.size() - 1).getActionMasked();
        return action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_UP;
    }

    private void dispatchImmediately(View eventView, List<MotionEvent> events,
                                     Dispatcher dispatcher) {
        internalDispatch = true;
        try {
            for (MotionEvent event : events) {
                try {
                    dispatcher.dispatch(eventView, event);
                }
                finally {
                    event.recycle();
                }
            }
        }
        finally {
            internalDispatch = false;
        }
    }

    private void dispatchReplayEvent(ReplayEvent replayEvent) {
        if (!replayEvents.remove(replayEvent)) {
            return;
        }

        internalDispatch = true;
        try {
            dispatcher.dispatch(replayEvent.eventView, replayEvent.event);
        }
        finally {
            replayEvent.recycle();
            internalDispatch = false;
        }

        if (replayEvents.isEmpty()) {
            finishReplay();
        }
    }

    private void finishReplay() {
        Dispatcher completedDispatcher = dispatcher;
        dispatcher = null;
        replaying = false;

        internalDispatch = true;
        try {
            for (QueuedInputEvent queuedEvent : queuedInputEvents) {
                try {
                    completedDispatcher.dispatch(queuedEvent.eventView, queuedEvent.event);
                }
                finally {
                    queuedEvent.event.recycle();
                }
            }
        }
        finally {
            queuedInputEvents.clear();
            internalDispatch = false;
        }
    }

    private final class ReplayEvent implements Runnable {
        final View eventView;
        final MotionEvent event;
        final long delayMs;
        boolean recycled;

        ReplayEvent(View eventView, MotionEvent event, long delayMs) {
            this.eventView = eventView;
            this.event = event;
            this.delayMs = delayMs;
        }

        @Override
        public void run() {
            dispatchReplayEvent(this);
        }

        void recycle() {
            if (!recycled) {
                recycled = true;
                event.recycle();
            }
        }
    }

    private static final class QueuedInputEvent {
        final View eventView;
        final MotionEvent event;

        QueuedInputEvent(View eventView, MotionEvent event) {
            this.eventView = eventView;
            this.event = event;
        }
    }
}
