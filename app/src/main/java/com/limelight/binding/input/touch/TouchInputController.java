package com.limelight.binding.input.touch;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.utils.ViewCoordinateMapper;

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
        void showSoftKeyboard();
    }

    private final View streamView;
    private final PointerInputSink inputSink;
    private final DirectContactInputController directContactInputController;
    private final InputSettingsState settingsState;
    private final Host host;
    private final TouchContext[] touchContexts =
            new TouchContext[MAX_LEGACY_CONTACTS];
    private final SoftKeyboardGestureCoordinator keyboardGestureCoordinator;
    private final BarometerForcePressController forcePressController;
    private final TouchscreenTouchpadHandler nativeTouchpadHandler;
    private final float[] mappedLegacyPosition = new float[2];
    private final Matrix streamViewInverse = new Matrix();

    private TouchInputMode currentMode = TouchInputMode.DISABLED;
    private TouchpadMotionSender pressedPointerMotionSender;
    private boolean nativeTouchpadInputEnabled;
    private boolean directContactInputEnabled;
    private boolean legacyTouchpadInputEnabled;
    private boolean disabled = true;
    private boolean inputSuspended;
    private boolean destroyed;

    public TouchInputController(
            Context context,
            View streamView,
            PointerInputSink inputSink,
            DirectContactInputController directContactInputController,
            InputSettingsState settingsState,
            Host host) {
        this.streamView = Objects.requireNonNull(streamView, "streamView");
        this.inputSink = Objects.requireNonNull(inputSink, "inputSink");
        this.directContactInputController = Objects.requireNonNull(
                directContactInputController,
                "directContactInputController");
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
        this.host = Objects.requireNonNull(host, "host");

        nativeTouchpadHandler = new TouchscreenTouchpadHandler(
                inputSink,
                streamView,
                REFERENCE_WIDTH,
                REFERENCE_HEIGHT,
                settingsState);
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
        applyForcePressSettings(settingsState.get());
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
        directContactInputController.cancel();
        cancelLegacyTouchContexts();
    }

    public void cancelActiveInput() {
        keyboardGestureCoordinator.cancel();
        forcePressController.cancelTouchSession();
        nativeTouchpadHandler.cancel();
        directContactInputController.cancel();
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
        currentMode = mode;
        pressedPointerMotionSender = null;
        disabled = false;
        nativeTouchpadInputEnabled = false;
        directContactInputEnabled = false;
        legacyTouchpadInputEnabled = false;

        switch (mode) {
            case MULTI_TOUCH:
                directContactInputEnabled = true;
                break;

            case ABSOLUTE_MOUSE:
            case ABSOLUTE_MOUSE_SWAPPED:
                nativeTouchpadInputEnabled = true;
                break;

            case NATIVE_TOUCHPAD:
                nativeTouchpadInputEnabled = true;
                legacyTouchpadInputEnabled = true;
                break;

            case DISABLED:
                disabled = true;
                applyForcePressSettings(settingsState.get());
                return;

            case TOUCHPAD_MOVE_ONLY:
            case TOUCHPAD_MOVE_AND_CLICK:
                legacyTouchpadInputEnabled = true;
                break;

            default:
                throw new AssertionError("Unhandled touch input mode: " + mode);
        }

        pressedPointerMotionSender =
                mode == TouchInputMode.NATIVE_TOUCHPAD
                        ? new TouchpadMotionSender(
                                inputSink,
                                REFERENCE_WIDTH,
                                REFERENCE_HEIGHT,
                                streamView,
                                settingsState)
                        : null;
        nativeTouchpadHandler.setSinglePointerRemainderMode(
                mode == TouchInputMode.ABSOLUTE_MOUSE ||
                        mode == TouchInputMode.ABSOLUTE_MOUSE_SWAPPED
                        ? TouchscreenTouchpadHandler
                                .SinglePointerRemainderMode.SUPPRESS
                        : TouchscreenTouchpadHandler
                                .SinglePointerRemainderMode.RELATIVE);
        TouchpadGestureState gestureState = new TouchpadGestureState();
        for (int i = 0; i < touchContexts.length; i++) {
            touchContexts[i] = createTouchContext(
                    mode,
                    i,
                    gestureState,
                    pressedPointerMotionSender);
        }

        applyForcePressSettings(settingsState.get());
    }

    /**
     * Applies the force-press subset of a newly published input snapshot.
     * Other touch settings are read directly from {@link InputSettingsState}
     * by their consumers and require no duplicated controller state.
     */
    public void onInputSettingsChanged(
            InputSettings previous,
            InputSettings current) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        if (destroyed || !forcePressSettingsChanged(previous, current)) {
            return;
        }
        applyForcePressSettings(current);
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

        if (keyboardGestureCoordinator.onTouchEvent(
                eventView,
                event,
                settingsState.get()
                        .getSoftKeyboardGestureFingers())) {
            return true;
        }

        if (nativeTouchpadInputEnabled &&
                nativeTouchpadHandler.handleMotionEvent(
                        eventView,
                        event)) {
            return true;
        }

        if (directContactInputEnabled &&
                directContactInputController.trySendTouchEvent(
                        eventView,
                        event)) {
            return true;
        }

        return dispatchLegacyTouchEvent(
                eventView,
                event,
                eventView != streamView &&
                        !legacyTouchpadInputEnabled);
    }

    private TouchContext createTouchContext(
            TouchInputMode mode,
            int actionIndex,
            TouchpadGestureState gestureState,
            TouchpadMotionSender pressedPointerMotionSender) {
        if (!legacyTouchpadInputEnabled) {
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
                    settingsState,
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
                    settingsState,
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
                    settingsState,
                    gestureState);
        }
        touchContext.setNativeTouchpadPressHandlingEnabled(
                mode == TouchInputMode.NATIVE_TOUCHPAD);
        return touchContext;
    }

    private boolean dispatchLegacyTouchEvent(
            View eventView,
            MotionEvent event,
            boolean mapToStreamView) {
        int actionIndex = event.getActionIndex();
        TouchContext context = getTouchContext(actionIndex);
        if (context == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!updateLegacyPosition(
                        eventView,
                        event.getX(actionIndex),
                        event.getY(actionIndex),
                        mapToStreamView)) {
                    return true;
                }
                updatePointerCount(
                        event.getPointerCount(),
                        event.getEventTime());
                context.touchDownEvent(
                        (int) mappedLegacyPosition[0],
                        (int) mappedLegacyPosition[1],
                        event.getEventTime(),
                        true);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (!updateLegacyPosition(
                        eventView,
                        event.getX(actionIndex),
                        event.getY(actionIndex),
                        mapToStreamView)) {
                    cancelLegacyTouchContexts();
                    return true;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                    context.cancelTouch();
                }
                else {
                    context.touchUpEvent(
                            (int) mappedLegacyPosition[0],
                            (int) mappedLegacyPosition[1],
                            event.getEventTime());
                }

                updatePointerCount(
                        event.getPointerCount() - 1,
                        event.getEventTime());
                if (actionIndex == 0 &&
                        event.getPointerCount() > 1 &&
                        !context.isCancelled()) {
                    if (!updateLegacyPosition(
                            eventView,
                            event.getX(1),
                            event.getY(1),
                            mapToStreamView)) {
                        cancelLegacyTouchContexts();
                        return true;
                    }
                    context.touchDownEvent(
                            (int) mappedLegacyPosition[0],
                            (int) mappedLegacyPosition[1],
                            event.getEventTime(),
                            false);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                dispatchMoveEvents(
                        eventView,
                        event,
                        mapToStreamView);
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelLegacyTouchContexts();
                return true;

            default:
                return false;
        }
    }

    private void dispatchMoveEvents(
            View eventView,
            MotionEvent event,
            boolean mapToStreamView) {
        for (int historyIndex = 0;
             historyIndex < event.getHistorySize();
             historyIndex++) {
            for (TouchContext context : touchContexts) {
                if (context == null) {
                    continue;
                }
                int actionIndex = context.getActionIndex();
                if (actionIndex < event.getPointerCount()) {
                    if (!updateLegacyPosition(
                            eventView,
                            event.getHistoricalX(
                                    actionIndex,
                                    historyIndex),
                            event.getHistoricalY(
                                    actionIndex,
                                    historyIndex),
                            mapToStreamView)) {
                        cancelLegacyTouchContexts();
                        return;
                    }
                    context.touchMoveEvent(
                            (int) mappedLegacyPosition[0],
                            (int) mappedLegacyPosition[1],
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
                if (!updateLegacyPosition(
                        eventView,
                        event.getX(actionIndex),
                        event.getY(actionIndex),
                        mapToStreamView)) {
                    cancelLegacyTouchContexts();
                    return;
                }
                context.touchMoveEvent(
                        (int) mappedLegacyPosition[0],
                        (int) mappedLegacyPosition[1],
                        event.getEventTime());
            }
        }
    }

    private boolean updateLegacyPosition(
            View eventView,
            float x,
            float y,
            boolean mapToStreamView) {
        mappedLegacyPosition[0] = x;
        mappedLegacyPosition[1] = y;
        return !mapToStreamView ||
                eventView != null &&
                        ViewCoordinateMapper.mapPointBetweenSiblings(
                                eventView,
                                streamView,
                                mappedLegacyPosition,
                                streamViewInverse);
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

    private void applyForcePressSettings(InputSettings settings) {
        forcePressController.setThresholdHpa(
                settings.getBarometerForcePressThresholdHpa());
        forcePressController.setMinimumTouchDurationMs(
                settings.getBarometerForcePressMinimumDurationMs());

        boolean nativeTouchpadMode =
                currentMode == TouchInputMode.NATIVE_TOUCHPAD;
        boolean forcePressEnabled =
                nativeTouchpadMode &&
                        settings.isBarometerForcePressEnabled() &&
                        forcePressController.isAvailable();
        nativeTouchpadHandler.configureNativePressHandling(
                nativeTouchpadMode,
                forcePressEnabled,
                pressedPointerMotionSender);
        forcePressController.setEnabled(forcePressEnabled);
    }

    private static boolean forcePressSettingsChanged(
            InputSettings previous,
            InputSettings current) {
        return previous.isBarometerForcePressEnabled() !=
                        current.isBarometerForcePressEnabled() ||
                Float.compare(
                        previous.getBarometerForcePressThresholdHpa(),
                        current.getBarometerForcePressThresholdHpa()) != 0 ||
                previous.getBarometerForcePressMinimumDurationMs() !=
                        current.getBarometerForcePressMinimumDurationMs();
    }
}
