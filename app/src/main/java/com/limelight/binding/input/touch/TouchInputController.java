package com.limelight.binding.input.touch;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.Objects;

/**
 * Owns touchscreen gesture arbitration, active touch contexts, and force-press
 * lifecycle for one streaming session.
 *
 * <p>Events are handled synchronously on the Android input thread. This class
 * does not queue protocol output; deferred events exist only for the bounded
 * multi-finger keyboard gesture decision.</p>
 */
public final class TouchInputController {
    private static final int MAX_LEGACY_CONTACTS = 2;
    private static final int REFERENCE_WIDTH = 1_280;
    private static final int REFERENCE_HEIGHT = 720;

    /**
     * Activity boundary for operations that still belong to the Android
     * composition layer.
     */
    public interface Host {
        boolean trySendDirectTouchEvent(View eventView, MotionEvent event);

        void showSoftKeyboard();
    }

    private final View streamView;
    private final PointerInputSink inputSink;
    private final PreferenceConfiguration preferences;
    private final Host host;
    private final TouchContext[] touchContexts =
            new TouchContext[MAX_LEGACY_CONTACTS];
    private final SoftKeyboardGestureCoordinator keyboardGestureCoordinator;
    private final BarometerForcePressController forcePressController;
    private final TouchscreenTouchpadHandler nativeTouchpadHandler;

    private boolean nativeTouchpadInputEnabled;
    private boolean disabled = true;
    private boolean inputSuspended;
    private boolean destroyed;

    public TouchInputController(
            Context context,
            View streamView,
            PointerInputSink inputSink,
            PreferenceConfiguration preferences,
            Host host) {
        this.streamView = Objects.requireNonNull(streamView, "streamView");
        this.inputSink = Objects.requireNonNull(inputSink, "inputSink");
        this.preferences = Objects.requireNonNull(
                preferences,
                "preferences");
        this.host = Objects.requireNonNull(host, "host");

        nativeTouchpadHandler = new TouchscreenTouchpadHandler(
                inputSink,
                streamView,
                REFERENCE_WIDTH,
                REFERENCE_HEIGHT,
                preferences);
        nativeTouchpadHandler.setNativeGestureListener(
                this::cancelLegacyTouchContextsForNativeGesture);

        keyboardGestureCoordinator = new SoftKeyboardGestureCoordinator(
                ViewConfiguration.get(
                        Objects.requireNonNull(context, "context"))
                        .getScaledTouchSlop(),
                new SoftKeyboardGestureCoordinator.Listener() {
                    @Override
                    public void dispatchDeferredTouchEvent(
                            View eventView,
                            MotionEvent event) {
                        handleMotionEvent(eventView, event);
                    }

                    @Override
                    public void onGesturePrefixDeferred() {
                        suspendPendingPressRecognition();
                    }

                    @Override
                    public void onKeyboardGestureRecognized() {
                        completeKeyboardGesture();
                    }
                });

        forcePressController = new BarometerForcePressController(
                context,
                new BarometerForcePressController.Listener() {
                    @Override
                    public boolean onForcePressDown(
                            int pointerId,
                            int pointerCount) {
                        keyboardGestureCoordinator
                                .resolveForCompetingGesture();
                        return nativeTouchpadHandler.beginForcePress(
                                pointerId,
                                pointerCount);
                    }

                    @Override
                    public void onForcePressUp(
                            int pointerId,
                            boolean cancelled) {
                        nativeTouchpadHandler.endForcePress(
                                pointerId,
                                !cancelled);
                    }
                });
        forcePressController.setThresholdHpa(
                preferences.barometerForcePressThresholdHpa);
        forcePressController.setMinimumTouchDurationMs(
                preferences.barometerForcePressMinimumDurationMs);
        preferences.enableBarometerForcePress =
                preferences.enableBarometerForcePress &&
                        forcePressController.isAvailable();
    }

    public void start() {
        if (!destroyed) {
            forcePressController.start();
        }
    }

    public void stop() {
        forcePressController.stop();
    }

    public void destroy() {
        if (destroyed) {
            return;
        }

        destroyed = true;
        forcePressController.stop();
        keyboardGestureCoordinator.cancel();
        nativeTouchpadHandler.cancel();
        cancelLegacyTouchContexts();
    }

    public void cancelActiveInput() {
        keyboardGestureCoordinator.cancel();
        forcePressController.cancelTouchSession();
        nativeTouchpadHandler.cancel();
        cancelLegacyTouchContexts();
    }

    public void setInputSuspended(boolean inputSuspended) {
        requireNotDestroyed();
        if (this.inputSuspended == inputSuspended) {
            return;
        }

        this.inputSuspended = inputSuspended;
        if (inputSuspended) {
            cancelActiveInput();
        }
    }

    public void setMode(TouchInputMode mode) {
        requireNotDestroyed();
        Objects.requireNonNull(mode, "mode");

        cancelActiveInput();
        forcePressController.setEnabled(false);
        disabled = false;
        nativeTouchpadInputEnabled = false;

        switch (mode) {
            case MULTI_TOUCH:
                preferences.enableMultiTouchScreen = true;
                preferences.touchscreenTrackpad = false;
                break;

            case ABSOLUTE_MOUSE:
            case ABSOLUTE_MOUSE_SWAPPED:
                preferences.enableMultiTouchScreen = false;
                preferences.touchscreenTrackpad = false;
                nativeTouchpadInputEnabled = true;
                break;

            case NATIVE_TOUCHPAD:
                preferences.enableMultiTouchScreen = false;
                preferences.touchscreenTrackpad = true;
                nativeTouchpadInputEnabled = true;
                break;

            case DISABLED:
                disabled = true;
                return;

            case TOUCHPAD_MOVE_ONLY:
            case TOUCHPAD_MOVE_AND_CLICK:
                preferences.enableMultiTouchScreen = false;
                preferences.touchscreenTrackpad = true;
                break;

            default:
                throw new AssertionError("Unhandled touch input mode: " + mode);
        }

        TouchpadMotionSender pressedPointerMotionSender =
                mode == TouchInputMode.NATIVE_TOUCHPAD
                        ? new TouchpadMotionSender(
                                inputSink,
                                REFERENCE_WIDTH,
                                REFERENCE_HEIGHT,
                                streamView,
                                preferences)
                        : null;
        nativeTouchpadHandler.setSinglePointerRemainderMode(
                mode == TouchInputMode.ABSOLUTE_MOUSE ||
                        mode == TouchInputMode.ABSOLUTE_MOUSE_SWAPPED
                        ? TouchscreenTouchpadHandler
                                .SinglePointerRemainderMode.SUPPRESS
                        : TouchscreenTouchpadHandler
                                .SinglePointerRemainderMode.RELATIVE);
        nativeTouchpadHandler.configureNativePressHandling(
                mode == TouchInputMode.NATIVE_TOUCHPAD,
                preferences.enableBarometerForcePress,
                pressedPointerMotionSender);

        TouchpadGestureState gestureState = new TouchpadGestureState();
        for (int i = 0; i < touchContexts.length; i++) {
            touchContexts[i] = createTouchContext(
                    mode,
                    i,
                    gestureState,
                    pressedPointerMotionSender);
        }

        forcePressController.setEnabled(
                mode == TouchInputMode.NATIVE_TOUCHPAD &&
                        preferences.enableBarometerForcePress);
    }

    /**
     * Handles a touchscreen-finger event after source and virtual-control
     * filtering.
     */
    public boolean handleMotionEvent(View eventView, MotionEvent event) {
        if (destroyed || disabled || inputSuspended) {
            return true;
        }

        if (!keyboardGestureCoordinator.isDispatchingDeferredEvents()) {
            forcePressController.onTouchEvent(event);
        }

        float xOffset;
        float yOffset;
        if (eventView != streamView && !preferences.touchscreenTrackpad) {
            xOffset = -streamView.getX();
            yOffset = -streamView.getY();
        }
        else {
            xOffset = 0;
            yOffset = 0;
        }

        if (keyboardGestureCoordinator.onTouchEvent(
                eventView,
                event,
                preferences.quickSoftKeyboardFingers)) {
            return true;
        }

        if (nativeTouchpadInputEnabled &&
                nativeTouchpadHandler.handleMotionEvent(
                        eventView,
                        event)) {
            return true;
        }

        if (preferences.enableMultiTouchScreen &&
                !preferences.touchscreenTrackpad &&
                host.trySendDirectTouchEvent(eventView, event)) {
            return true;
        }

        return dispatchLegacyTouchEvent(event, xOffset, yOffset);
    }

    private TouchContext createTouchContext(
            TouchInputMode mode,
            int actionIndex,
            TouchpadGestureState gestureState,
            TouchpadMotionSender pressedPointerMotionSender) {
        if (!preferences.touchscreenTrackpad) {
            if (mode == TouchInputMode.ABSOLUTE_MOUSE_SWAPPED) {
                return new AbsoluteTouchSwitchContext(
                        inputSink,
                        actionIndex,
                        streamView);
            }
            return new AbsoluteTouchContext(
                    inputSink,
                    actionIndex,
                    streamView);
        }

        if (mode == TouchInputMode.TOUCHPAD_MOVE_ONLY ||
                mode == TouchInputMode.TOUCHPAD_MOVE_AND_CLICK) {
            return new RelativeTouchSwitchContext(
                    inputSink,
                    actionIndex,
                    REFERENCE_WIDTH,
                    REFERENCE_HEIGHT,
                    streamView,
                    preferences,
                    mode == TouchInputMode.TOUCHPAD_MOVE_AND_CLICK,
                    gestureState);
        }

        RelativeTouchContext touchContext;
        if (actionIndex == 0 && pressedPointerMotionSender != null) {
            touchContext = new RelativeTouchContext(
                    inputSink,
                    actionIndex,
                    REFERENCE_WIDTH,
                    REFERENCE_HEIGHT,
                    streamView,
                    preferences,
                    gestureState,
                    pressedPointerMotionSender);
        }
        else {
            touchContext = new RelativeTouchContext(
                    inputSink,
                    actionIndex,
                    REFERENCE_WIDTH,
                    REFERENCE_HEIGHT,
                    streamView,
                    preferences,
                    gestureState);
        }
        touchContext.setNativeTouchpadPressHandlingEnabled(
                mode == TouchInputMode.NATIVE_TOUCHPAD);
        return touchContext;
    }

    private boolean dispatchLegacyTouchEvent(
            MotionEvent event,
            float xOffset,
            float yOffset) {
        int actionIndex = event.getActionIndex();
        TouchContext context = getTouchContext(actionIndex);
        if (context == null) {
            return false;
        }

        int eventX = (int) (event.getX(actionIndex) + xOffset);
        int eventY = (int) (event.getY(actionIndex) + yOffset);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                updatePointerCount(
                        event.getPointerCount(),
                        event.getEventTime());
                context.touchDownEvent(
                        eventX,
                        eventY,
                        event.getEventTime(),
                        true);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                    context.cancelTouch();
                }
                else {
                    context.touchUpEvent(
                            eventX,
                            eventY,
                            event.getEventTime());
                }

                updatePointerCount(
                        event.getPointerCount() - 1,
                        event.getEventTime());
                if (actionIndex == 0 &&
                        event.getPointerCount() > 1 &&
                        !context.isCancelled()) {
                    context.touchDownEvent(
                            (int) (event.getX(1) + xOffset),
                            (int) (event.getY(1) + yOffset),
                            event.getEventTime(),
                            false);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                dispatchMoveEvents(event, xOffset, yOffset);
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelLegacyTouchContexts();
                return true;

            default:
                return false;
        }
    }

    private void dispatchMoveEvents(
            MotionEvent event,
            float xOffset,
            float yOffset) {
        for (int historyIndex = 0;
             historyIndex < event.getHistorySize();
            historyIndex++) {
            for (TouchContext context : touchContexts) {
                if (context == null) {
                    continue;
                }
                int actionIndex = context.getActionIndex();
                if (actionIndex < event.getPointerCount()) {
                    context.touchMoveEvent(
                            (int) (event.getHistoricalX(
                                    actionIndex,
                                    historyIndex) + xOffset),
                            (int) (event.getHistoricalY(
                                    actionIndex,
                                    historyIndex) + yOffset),
                            event.getHistoricalEventTime(historyIndex));
                }
            }
        }

        for (TouchContext context : touchContexts) {
            if (context == null) {
                continue;
            }
            int actionIndex = context.getActionIndex();
            if (actionIndex < event.getPointerCount()) {
                context.touchMoveEvent(
                        (int) (event.getX(actionIndex) + xOffset),
                        (int) (event.getY(actionIndex) + yOffset),
                        event.getEventTime());
            }
        }
    }

    private TouchContext getTouchContext(int actionIndex) {
        if (actionIndex < 0 || actionIndex >= touchContexts.length) {
            return null;
        }
        return touchContexts[actionIndex];
    }

    private void updatePointerCount(int pointerCount, long eventTime) {
        for (TouchContext context : touchContexts) {
            if (context != null) {
                context.setPointerCount(pointerCount, eventTime);
            }
        }
    }

    private void suspendPendingPressRecognition() {
        nativeTouchpadHandler.suspendPendingPressRecognition();
        for (TouchContext context : touchContexts) {
            if (context != null) {
                context.suspendPendingPressRecognition();
            }
        }
    }

    private void completeKeyboardGesture() {
        forcePressController.cancelTouchSession();
        nativeTouchpadHandler.cancel();
        cancelLegacyTouchContexts();
        host.showSoftKeyboard();
    }

    private void cancelLegacyTouchContextsForNativeGesture() {
        cancelLegacyTouchContexts();
    }

    private void cancelLegacyTouchContexts() {
        for (TouchContext context : touchContexts) {
            if (context != null) {
                context.cancelTouch();
                context.setPointerCount(0);
            }
        }
    }

    private void requireNotDestroyed() {
        if (destroyed) {
            throw new IllegalStateException(
                    "TouchInputController is destroyed");
        }
    }
}
