package com.limelight.binding.input.touch;

import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.limelight.ui.StreamZoomGeometry;

import java.util.ArrayList;
import java.util.List;

/** Owns a bounded two-contact prefix and hands all other input back unchanged. */
final class StreamPinchZoomController {
    interface Listener {
        void dispatchDeferredTouchEvent(View eventView, MotionEvent event);
        void suspendPendingPressRecognition();
        void cancelRemoteTouchInput();
        void onViewportChanged();
    }

    private static final int MAX_PENDING_EVENTS = 128;
    private final View streamView;
    private final Listener listener;
    private final PinchZoomGesture gesture;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable deadline = this::resolveForCompetingGesture;
    private final BufferedTouchEventDispatcher dispatcher = new BufferedTouchEventDispatcher();
    private final List<MotionEvent> pendingEvents = new ArrayList<>();
    private final Matrix eventToParent = new Matrix();
    private final float[] points = new float[4];
    private final View.OnLayoutChangeListener layoutListener;
    private View pendingEventView;
    private int firstPointerId = -1;
    private int secondPointerId = -1;
    private boolean enabled;
    private float previousSpan;
    private float previousFocusX;
    private float previousFocusY;

    StreamPinchZoomController(View streamView, Listener listener) {
        this.streamView = streamView;
        this.listener = listener;
        gesture = new PinchZoomGesture(
                ViewConfiguration.get(streamView.getContext()).getScaledTouchSlop(),
                ViewConfiguration.getDoubleTapTimeout());
        layoutListener = (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                cancel();
                listener.cancelRemoteTouchInput();
                constrainViewport();
                listener.onViewportChanged();
            }
        };
        streamView.addOnLayoutChangeListener(layoutListener);
    }

    void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            resolveForCompetingGesture();
            this.enabled = enabled;
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean isConsuming() {
        return gesture.isConsuming();
    }

    boolean isDispatchingDeferredEvents() {
        return dispatcher.isInternalDispatch();
    }

    boolean onTouchEvent(View eventView, MotionEvent event) {
        if (dispatcher.queueIfReplaying(eventView, event)) {
            return true;
        }
        if (dispatcher.isReplayingBufferedEvents() || !enabled && !gesture.isConsuming()) {
            return false;
        }
        PinchZoomGesture.Action action = actionOf(event.getActionMasked());
        if (action == null) {
            return false;
        }
        if (!isFingerEvent(event) || eventView == null ||
                streamView.getParent() == null ||
                eventView.getParent() != streamView.getParent()) {
            resolveForCompetingGesture();
            return gesture.isConsuming();
        }

        // Freeze the source transform for this entire event, including history.
        // Recognition never runs in the moving/scaling video's coordinate space.
        eventToParent.set(eventView.getMatrix());
        eventToParent.postTranslate(eventView.getLeft(), eventView.getTop());
        boolean wasPending = gesture.isPending();
        boolean viewportChanged = false;
        PinchZoomGesture.Result result = PinchZoomGesture.Result.FORWARD;
        int firstSample = action == PinchZoomGesture.Action.MOVE ? 0 : event.getHistorySize();
        for (int sample = firstSample; sample <= event.getHistorySize(); sample++) {
            readPoints(event, sample);
            long time = sample == event.getHistorySize()
                    ? event.getEventTime() : event.getHistoricalEventTime(sample);
            result = gesture.onTouch(action, time, event.getPointerCount(),
                    points[0], points[1], points[2], points[3]);
            if (result == PinchZoomGesture.Result.BEGIN_ZOOM) {
                discardPendingEvents();
                listener.cancelRemoteTouchInput();
                applyZoom();
                viewportChanged = true;
            }
            else if (result == PinchZoomGesture.Result.ZOOM) {
                applyZoom();
                viewportChanged = true;
            }
            else if (result == PinchZoomGesture.Result.REBASE_ZOOM) {
                rememberFocus();
            }
        }
        if (viewportChanged) {
            listener.onViewportChanged();
        }

        if (result == PinchZoomGesture.Result.DEFER) {
            if (!wasPending) {
                pendingEventView = eventView;
                rememberFocus();
                listener.suspendPendingPressRecognition();
                handler.postAtTime(deadline, Math.max(
                        SystemClock.uptimeMillis(), gesture.getDeadlineMs()));
            }
            pendingEvents.add(MotionEvent.obtain(event));
            if (pendingEvents.size() >= MAX_PENDING_EVENTS) {
                resolveForCompetingGesture();
            }
            return true;
        }
        if (result == PinchZoomGesture.Result.FORWARD) {
            if (!pendingEvents.isEmpty()) {
                pendingEvents.add(MotionEvent.obtain(event));
                releasePendingEvents();
                return true;
            }
            return false;
        }
        return true;
    }

    void resolveForCompetingGesture() {
        gesture.useRemoteInput();
        releasePendingEvents();
    }

    void cancel() {
        discardPendingEvents();
        dispatcher.cancel();
        gesture.reset();
        firstPointerId = -1;
        secondPointerId = -1;
    }

    void destroy() {
        cancel();
        streamView.removeOnLayoutChangeListener(layoutListener);
    }

    private void releasePendingEvents() {
        handler.removeCallbacks(deadline);
        if (pendingEvents.isEmpty()) {
            return;
        }
        View eventView = pendingEventView;
        List<MotionEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        pendingEventView = null;
        dispatcher.dispatch(eventView, events, listener::dispatchDeferredTouchEvent);
    }

    private void discardPendingEvents() {
        handler.removeCallbacks(deadline);
        for (MotionEvent event : pendingEvents) {
            event.recycle();
        }
        pendingEvents.clear();
        pendingEventView = null;
    }

    private void readPoints(MotionEvent event, int sample) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            firstPointerId = event.getPointerId(0);
            secondPointerId = -1;
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN &&
                event.getPointerCount() == 2) {
            firstPointerId = event.getPointerId(1 - event.getActionIndex());
            secondPointerId = event.getPointerId(event.getActionIndex());
        }
        int firstIndex = event.findPointerIndex(firstPointerId);
        if (firstIndex < 0) {
            firstIndex = 0;
            firstPointerId = event.getPointerId(firstIndex);
        }
        int secondIndex = event.findPointerIndex(secondPointerId);
        if (event.getPointerCount() < 2) {
            secondIndex = firstIndex;
        }
        else if (secondIndex < 0 || secondIndex == firstIndex) {
            secondIndex = firstIndex == 0 ? 1 : 0;
            secondPointerId = event.getPointerId(secondIndex);
        }
        for (int index = 0; index < 2; index++) {
            int pointerIndex = index == 0 ? firstIndex : secondIndex;
            points[2 * index] = sample == event.getHistorySize()
                    ? event.getX(pointerIndex) : event.getHistoricalX(pointerIndex, sample);
            points[2 * index + 1] = sample == event.getHistorySize()
                    ? event.getY(pointerIndex) : event.getHistoricalY(pointerIndex, sample);
        }
        eventToParent.mapPoints(points);
    }

    private void rememberFocus() {
        previousSpan = (float) Math.hypot(points[2] - points[0], points[3] - points[1]);
        previousFocusX = (points[0] + points[2]) / 2f;
        previousFocusY = (points[1] + points[3]) / 2f;
    }

    private void applyZoom() {
        float oldSpan = previousSpan;
        float oldFocusX = previousFocusX;
        float oldFocusY = previousFocusY;
        rememberFocus();
        if (oldSpan <= 0f || previousSpan <= 0f ||
                !(streamView.getParent() instanceof View)) {
            return;
        }
        float oldScale = streamView.getScaleX();
        float scale = StreamZoomGeometry.clampScale(oldScale * previousSpan / oldSpan);
        float ratio = scale / oldScale;
        float x = StreamZoomGeometry.translationAroundFocus(
                streamView.getTranslationX(), streamView.getLeft(), streamView.getPivotX(),
                oldFocusX, previousFocusX, ratio);
        float y = StreamZoomGeometry.translationAroundFocus(
                streamView.getTranslationY(), streamView.getTop(), streamView.getPivotY(),
                oldFocusY, previousFocusY, ratio);
        streamView.setScaleX(scale);
        streamView.setScaleY(scale);
        streamView.setTranslationX(x);
        streamView.setTranslationY(y);
        constrainViewport();
    }

    private void constrainViewport() {
        if (!(streamView.getParent() instanceof View)) {
            return;
        }
        View viewport = (View) streamView.getParent();
        float scale = streamView.getScaleX();
        streamView.setTranslationX(StreamZoomGeometry.clampTranslation(
                streamView.getTranslationX(), streamView.getLeft(), streamView.getWidth(), streamView.getPivotX(),
                scale, viewport.getPaddingLeft(), viewport.getWidth() - viewport.getPaddingRight()));
        streamView.setTranslationY(StreamZoomGeometry.clampTranslation(
                streamView.getTranslationY(), streamView.getTop(), streamView.getHeight(), streamView.getPivotY(),
                scale, viewport.getPaddingTop(), viewport.getHeight() - viewport.getPaddingBottom()));
    }

    private static boolean isFingerEvent(MotionEvent event) {
        if (!event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)) {
            return false;
        }
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (event.getToolType(i) != MotionEvent.TOOL_TYPE_FINGER) {
                return false;
            }
        }
        return true;
    }

    private static PinchZoomGesture.Action actionOf(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: return PinchZoomGesture.Action.DOWN;
            case MotionEvent.ACTION_POINTER_DOWN: return PinchZoomGesture.Action.POINTER_DOWN;
            case MotionEvent.ACTION_MOVE: return PinchZoomGesture.Action.MOVE;
            case MotionEvent.ACTION_POINTER_UP: return PinchZoomGesture.Action.POINTER_UP;
            case MotionEvent.ACTION_UP: return PinchZoomGesture.Action.UP;
            case MotionEvent.ACTION_CANCEL: return PinchZoomGesture.Action.CANCEL;
            default: return null;
        }
    }
}
