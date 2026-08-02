package com.limelight.binding.input.touch;

import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.input.InputSettingsState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owns native touchpad gestures and physical clickpad button state.
 *
 * <p>Standalone single-finger gestures intentionally remain on the legacy mouse path. This keeps
 * local cursor rendering and host cursor movement driven by the same mouse packet. A long press
 * or barometer force press promotes that exact contact to native touchpad ownership before
 * changing the native clickpad button state. Multi-finger gestures enter native ownership as soon
 * as the second contact arrives. When they return to one contact, the tail is either handled as
 * relative movement or suppressed until every contact is lifted.</p>
 */
public final class TouchscreenTouchpadHandler {
    public interface NativeGestureListener {
        void onNativeGestureStarted();
    }

    /**
     * Defines how the remaining contact behaves after a native multi-finger gesture ends.
     */
    public enum SinglePointerRemainderMode {
        RELATIVE,
        SUPPRESS
    }

    private enum GestureState {
        IDLE,
        NATIVE_GESTURE,
        RELATIVE_REMAINDER,
        SUPPRESSED
    }

    private enum PressSource {
        NONE,
        LONG_PRESS,
        FORCE_PRESS
    }

    private static final int CURRENT_SAMPLE = -1;
    private static final float MILLIMETERS_PER_INCH = 25.4f;
    private static final int MIN_TOUCHPAD_SIZE_MM = 40;
    private static final int MAX_TOUCHPAD_SIZE_MM = 200;
    private final NativeTouchpadSender nativeSender;
    private final TouchpadMotionSender remainderMotionSender;
    private TouchpadMotionSender pressedPointerMotionSender;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TouchpadHapticFeedback hapticFeedback;
    private final InputSettingsState settingsState;
    private final float pressSlopSquared;
    private NativeGestureListener nativeGestureListener;
    private boolean nativePressHandlingEnabled;
    private boolean barometerForcePressEnabled;
    private byte buttonState;
    private PressSource pressSource = PressSource.NONE;
    private int forcePressPointerId = -1;
    private final SparseArray<Contact> activeContacts = new SparseArray<>();
    private final SparseArray<PointF> pressPointerOrigins = new SparseArray<>();
    private final List<Contact> frameContacts =
            new ArrayList<>(MoonBridge.LI_TOUCHPAD_MAX_CONTACTS);
    private GestureState gestureState = GestureState.IDLE;
    private short deviceWidthMm;
    private short deviceHeightMm;
    private SinglePointerRemainderMode singlePointerRemainderMode =
            SinglePointerRemainderMode.RELATIVE;
    private int remainderPointerId;
    private int remainderX;
    private int remainderY;
    private View observedView;
    private Contact observedContact;
    private float observedOriginX;
    private float observedOriginY;
    private int observedTouchX;
    private int observedTouchY;
    private int pressedPointerId = -1;
    private int pressedPointerTouchX;
    private int pressedPointerTouchY;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!nativePressHandlingEnabled || barometerForcePressEnabled ||
                    pressSource != PressSource.NONE) {
                return;
            }

            if (gestureState == GestureState.IDLE &&
                    !promoteObservedContactToNative()) {
                return;
            }

            if (gestureState == GestureState.NATIVE_GESTURE &&
                    activeContacts.size() >= 1 &&
                    activeContacts.size() <= 2) {
                pressTouchpadButton(PressSource.LONG_PRESS, -1);
            }
        }
    };

    public TouchscreenTouchpadHandler(PointerInputSink inputSink, View targetView,
                                     int referenceWidth, int referenceHeight,
                                     InputSettingsState settingsState) {
        nativeSender = new NativeTouchpadSender(inputSink);
        this.settingsState = Objects.requireNonNull(settingsState);
        remainderMotionSender = new TouchpadMotionSender(inputSink,
                referenceWidth, referenceHeight,
                targetView, settingsState);
        hapticFeedback = new TouchpadHapticFeedback(targetView);
        int touchSlop = ViewConfiguration.get(targetView.getContext())
                .getScaledTouchSlop();
        int pressSlop = Math.min(
                touchSlop,
                RelativeTouchContext.TAP_MOVEMENT_THRESHOLD);
        pressSlopSquared = pressSlop * pressSlop;
    }

    public void setNativeGestureListener(
            NativeGestureListener nativeGestureListener) {
        this.nativeGestureListener = nativeGestureListener;
    }

    /**
     * Configures how a single contact is handled after native multi-touch has ended.
     *
     * <p>Changing modes cancels any gesture currently owned by this handler, so input from one
     * mode can never leak into another.</p>
     */
    public void setSinglePointerRemainderMode(
            SinglePointerRemainderMode singlePointerRemainderMode) {
        SinglePointerRemainderMode newMode =
                Objects.requireNonNull(singlePointerRemainderMode);
        if (this.singlePointerRemainderMode == newMode) {
            return;
        }

        cancel();
        this.singlePointerRemainderMode = newMode;
    }

    /**
     * Atomically configures physical press handling for standalone pointers.
     *
     * <p>The supplied sender must be the same instance used by the primary legacy touch
     * context. This preserves acceleration history and fractional motion across the exact
     * moment when native touchpad ownership begins.</p>
     */
    public void configureNativePressHandling(
            boolean enabled,
            boolean barometerForcePressEnabled,
            TouchpadMotionSender primaryMotionSender) {
        TouchpadMotionSender newMotionSender = enabled
                ? Objects.requireNonNull(primaryMotionSender)
                : null;
        boolean newBarometerForcePressEnabled =
                enabled && barometerForcePressEnabled;
        if (nativePressHandlingEnabled == enabled &&
                this.barometerForcePressEnabled ==
                        newBarometerForcePressEnabled &&
                pressedPointerMotionSender == newMotionSender) {
            return;
        }

        cancel();
        pressedPointerMotionSender = newMotionSender;
        nativePressHandlingEnabled = enabled;
        this.barometerForcePressEnabled =
                newBarometerForcePressEnabled;
    }

    /**
     * Returns whether this handler owns the current touchscreen gesture.
     */
    public boolean isHandlingGesture() {
        return gestureState == GestureState.NATIVE_GESTURE ||
                gestureState == GestureState.RELATIVE_REMAINDER ||
                gestureState == GestureState.SUPPRESSED;
    }

    /**
     * Handles native multi-finger touchpad input and owns any remaining single-finger tail.
     *
     * @return {@code true} when this handler owns the event, or {@code false} when the caller
     *         should dispatch it through the original touchscreen touchpad implementation.
     */
    public boolean handleMotionEvent(View eventView, MotionEvent event) {
        if (eventView == null) {
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // A standalone finger stays on the legacy mouse path unless a physical press
            // promotes it. Tracking the contact here lets that promotion preserve the exact
            // Android pointer identity and position.
            cancel();
            if (nativePressHandlingEnabled &&
                    event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                updateDeviceDimensions(eventView);
                observeSingleContact(eventView, event);
                scheduleLongPress(event.getEventTime());
            }
            return false;
        }

        if (gestureState == GestureState.SUPPRESSED) {
            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                resetState();
            }
            return true;
        }

        if (event.getPointerCount() > MoonBridge.LI_TOUCHPAD_MAX_CONTACTS) {
            cancelNativeContacts();
            clearObservedContact();
            gestureState = GestureState.SUPPRESSED;
            return true;
        }

        switch (gestureState) {
            case IDLE:
                if (action == MotionEvent.ACTION_POINTER_DOWN &&
                        event.getPointerCount() == 2) {
                    updateDeviceDimensions(eventView);
                    clearObservedContact();
                    if (beginNativeGesture(eventView, event)) {
                        notifyNativeGestureStarted();
                        scheduleLongPress(event.getEventTime());
                        return true;
                    }
                    return false;
                }

                if (nativePressHandlingEnabled &&
                        action == MotionEvent.ACTION_MOVE &&
                        event.getPointerCount() == 1) {
                    updateObservedContact(eventView, event);
                }
                else if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_CANCEL) {
                    clearObservedContact();
                }
                return false;

            case NATIVE_GESTURE:
                updateDeviceDimensions(eventView);
                handleNativeGesture(eventView, event);
                return true;

            case RELATIVE_REMAINDER:
                handleRelativeRemainder(eventView, event);
                return true;

            default:
                return false;
        }
    }

    /**
     * Cancels any active native contacts without affecting the caller's legacy touch contexts.
     */
    public void cancel() {
        cancelNativeContacts();
        resetState();
    }

    /**
     * Pauses a press timer while a local multi-finger gesture recognizer owns
     * the ambiguous two-contact prefix. Replayed input starts the timer again
     * using the original hardware event time.
     */
    public void suspendPendingPressRecognition() {
        cancelLongPress();
    }

    public boolean beginForcePress(int pointerId, int pointerCount) {
        if (!nativePressHandlingEnabled || !barometerForcePressEnabled ||
                pressSource != PressSource.NONE ||
                pointerCount < 1 || pointerCount > 2) {
            return false;
        }

        if (gestureState == GestureState.IDLE &&
                !promoteObservedContactToNative()) {
            return false;
        }

        if (gestureState != GestureState.NATIVE_GESTURE ||
                activeContacts.size() != pointerCount ||
                activeContacts.get(pointerId) == null) {
            return false;
        }

        return pressTouchpadButton(PressSource.FORCE_PRESS, pointerId);
    }

    public boolean endForcePress(int pointerId,
                                 boolean performHapticFeedback) {
        if (pressSource != PressSource.FORCE_PRESS ||
                forcePressPointerId != pointerId) {
            return false;
        }
        return releaseTouchpadButton(performHapticFeedback);
    }

    private void observeSingleContact(View eventView, MotionEvent event) {
        observedView = eventView;
        observedOriginX = event.getX(0);
        observedOriginY = event.getY(0);
        observedTouchX = (int) event.getX(0);
        observedTouchY = (int) event.getY(0);
        observedContact = createContact(eventView, event, 0, CURRENT_SAMPLE,
                MoonBridge.LI_TOUCH_EVENT_DOWN);
    }

    private void updateObservedContact(View eventView, MotionEvent event) {
        if (observedContact == null ||
                event.getPointerId(0) != observedContact.pointerId) {
            clearObservedContact();
            return;
        }

        if (hasMovedBeyondPressSlop(event, 0,
                observedOriginX, observedOriginY)) {
            cancelLongPress();
        }
        observedView = eventView;
        observedTouchX = (int) event.getX(0);
        observedTouchY = (int) event.getY(0);
        observedContact = createContact(eventView, event, 0, CURRENT_SAMPLE,
                MoonBridge.LI_TOUCH_EVENT_DOWN);
    }

    private boolean promoteObservedContactToNative() {
        if (gestureState != GestureState.IDLE ||
                observedView == null || observedContact == null) {
            return false;
        }

        frameContacts.clear();
        frameContacts.add(observedContact);
        if (!nativeSender.sendContacts(frameContacts, (byte) 0,
                deviceWidthMm, deviceHeightMm)) {
            return false;
        }

        activeContacts.clear();
        syncActiveContacts(frameContacts);
        beginPressedPointerMotion(observedContact.pointerId,
                observedTouchX, observedTouchY);
        clearSinglePointerRemainder();
        clearObservedContact();
        gestureState = GestureState.NATIVE_GESTURE;
        notifyNativeGestureStarted();
        return true;
    }

    private boolean hasMovedBeyondPressSlop(MotionEvent event, int pointerIndex,
                                            float originX, float originY) {
        if (movedBeyondPressSlop(event.getX(pointerIndex),
                event.getY(pointerIndex), originX, originY)) {
            return true;
        }

        for (int historyIndex = 0;
             historyIndex < event.getHistorySize();
             historyIndex++) {
            if (movedBeyondPressSlop(
                    event.getHistoricalX(pointerIndex, historyIndex),
                    event.getHistoricalY(pointerIndex, historyIndex),
                    originX, originY)) {
                return true;
            }
        }
        return false;
    }

    private boolean movedBeyondPressSlop(float x, float y,
                                         float originX, float originY) {
        float deltaX = x - originX;
        float deltaY = y - originY;
        return deltaX * deltaX + deltaY * deltaY > pressSlopSquared;
    }

    private void scheduleLongPress(long eventTime) {
        cancelLongPress();
        if (nativePressHandlingEnabled && !barometerForcePressEnabled) {
            long elapsedMs = Math.max(
                    0,
                    SystemClock.uptimeMillis() - eventTime);
            handler.postDelayed(
                    longPressRunnable,
                    Math.max(
                            0,
                            settingsState.get()
                                    .getTouchpadLongPressDurationMs() -
                                    elapsedMs));
        }
    }

    private void cancelLongPress() {
        handler.removeCallbacks(longPressRunnable);
    }

    private void clearObservedContact() {
        cancelLongPress();
        observedView = null;
        observedContact = null;
        observedOriginX = 0;
        observedOriginY = 0;
        observedTouchX = 0;
        observedTouchY = 0;
    }

    private void notifyNativeGestureStarted() {
        if (nativeGestureListener != null) {
            nativeGestureListener.onNativeGestureStarted();
        }
    }

    private boolean pressTouchpadButton(PressSource source,
                                        int pointerId) {
        if (gestureState != GestureState.NATIVE_GESTURE ||
                activeContacts.size() == 0 ||
                buttonState != 0 ||
                source == PressSource.NONE) {
            return false;
        }

        if (!sendButtonState(MoonBridge.LI_TOUCHPAD_BUTTON_PRIMARY)) {
            return false;
        }

        cancelLongPress();
        buttonState = MoonBridge.LI_TOUCHPAD_BUTTON_PRIMARY;
        pressSource = source;
        forcePressPointerId = source == PressSource.FORCE_PRESS
                ? pointerId
                : -1;
        hapticFeedback.performButtonPress();
        return true;
    }

    private boolean releaseTouchpadButton(
            boolean performHapticFeedback) {
        if (buttonState == 0 || pressSource == PressSource.NONE) {
            return false;
        }

        boolean sent = gestureState == GestureState.NATIVE_GESTURE &&
                activeContacts.size() != 0 &&
                sendButtonState((byte) 0);
        buttonState = 0;
        pressSource = PressSource.NONE;
        forcePressPointerId = -1;
        if (sent && performHapticFeedback) {
            hapticFeedback.performButtonRelease();
        }
        return true;
    }

    private boolean sendButtonState(byte newButtonState) {
        frameContacts.clear();
        for (int i = 0; i < activeContacts.size(); i++) {
            frameContacts.add(activeContacts.valueAt(i).withEventType(
                    MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY));
        }
        return nativeSender.sendContacts(frameContacts, newButtonState,
                deviceWidthMm, deviceHeightMm);
    }

    private boolean beginNativeGesture(View eventView, MotionEvent event) {
        frameContacts.clear();
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            frameContacts.add(createContact(eventView, event, pointerIndex, CURRENT_SAMPLE,
                    MoonBridge.LI_TOUCH_EVENT_DOWN));
        }

        if (!nativeSender.sendContacts(frameContacts, (byte) 0,
                deviceWidthMm, deviceHeightMm)) {
            return false;
        }

        activeContacts.clear();
        syncActiveContacts(frameContacts);
        rememberPressOrigins(event);
        clearSinglePointerRemainder();
        gestureState = GestureState.NATIVE_GESTURE;
        return true;
    }

    private void handleNativeGesture(View eventView, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                sendAdditionalContactDown(eventView, event);
                break;

            case MotionEvent.ACTION_MOVE:
                sendNativeMoves(eventView, event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                releaseTouchpadButton(true);
                if (event.getPointerCount() - 1 == 1) {
                    finishNativeGestureWithSinglePointerRemainder(eventView, event);
                }
                else {
                    sendContactUp(eventView, event);
                }
                break;

            case MotionEvent.ACTION_UP:
                releaseTouchpadButton(true);
                sendContactUp(eventView, event);
                if (singlePointerRemainderMode == SinglePointerRemainderMode.RELATIVE) {
                    remainderMotionSender.resendAbsoluteMousePosition();
                }
                resetState();
                break;

            case MotionEvent.ACTION_CANCEL:
                releaseTouchpadButton(false);
                cancelNativeContacts();
                resetState();
                break;

            default:
                break;
        }
    }

    private void sendAdditionalContactDown(View eventView, MotionEvent event) {
        int actionIndex = event.getActionIndex();
        frameContacts.clear();
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            byte eventType = pointerIndex == actionIndex
                    ? MoonBridge.LI_TOUCH_EVENT_DOWN
                    : MoonBridge.LI_TOUCH_EVENT_MOVE;
            frameContacts.add(createContact(eventView, event, pointerIndex, CURRENT_SAMPLE,
                    eventType));
        }

        nativeSender.sendContacts(frameContacts, buttonState,
                deviceWidthMm, deviceHeightMm);
        syncActiveContacts(frameContacts);
        clearPressedPointerMotion();
        pressPointerOrigins.put(event.getPointerId(actionIndex),
                new PointF(event.getX(actionIndex),
                        event.getY(actionIndex)));
        if (event.getPointerCount() > 2) {
            cancelLongPress();
        }
    }

    private void sendNativeMoves(View eventView, MotionEvent event) {
        cancelLongPressIfMoved(event);
        if (sendPressedPointerMotion(event)) {
            // The physical contact remains at its press location while the canonical mouse
            // motion path moves the cursor. Reassert only the clickpad state so Windows does
            // not also accelerate the same finger delta as native touchpad movement.
            sendButtonState(buttonState);
            return;
        }

        for (int historyIndex = 0; historyIndex < event.getHistorySize(); historyIndex++) {
            List<Contact> contacts = createMoveFrame(eventView, event, historyIndex);
            nativeSender.sendContacts(contacts, buttonState,
                    deviceWidthMm, deviceHeightMm);
            syncActiveContacts(contacts);
        }

        List<Contact> contacts = createMoveFrame(eventView, event, CURRENT_SAMPLE);
        nativeSender.sendContacts(contacts, buttonState,
                deviceWidthMm, deviceHeightMm);
        syncActiveContacts(contacts);
    }

    private void sendContactUp(View eventView, MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int actionPointerId = event.getPointerId(actionIndex);
        boolean canceled = isCanceledPointerUp(event);

        frameContacts.clear();
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            byte eventType;
            if (pointerIndex == actionIndex) {
                eventType = canceled
                        ? MoonBridge.LI_TOUCH_EVENT_CANCEL
                        : MoonBridge.LI_TOUCH_EVENT_UP;
            }
            else {
                eventType = MoonBridge.LI_TOUCH_EVENT_MOVE;
            }

            int pointerId = event.getPointerId(pointerIndex);
            Contact activeContact = activeContacts.get(pointerId);
            if (pointerId == pressedPointerId && activeContact != null) {
                // A pressed single pointer moves through the canonical mouse path. Keep its
                // native contact pinned through the terminal frame too, otherwise the UP frame
                // would teleport the host-side clickpad contact to the Android finger position.
                frameContacts.add(activeContact.withEventType(eventType));
            }
            else {
                frameContacts.add(createContact(eventView, event, pointerIndex,
                        CURRENT_SAMPLE, eventType));
            }
        }

        nativeSender.sendContacts(frameContacts, buttonState,
                deviceWidthMm, deviceHeightMm);
        syncActiveContacts(frameContacts);
        activeContacts.remove(actionPointerId);
        pressPointerOrigins.remove(actionPointerId);
        if (pressedPointerId == actionPointerId) {
            clearPressedPointerMotion();
        }
    }

    private void finishNativeGestureWithSinglePointerRemainder(View eventView,
                                                                MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int remainingIndex = actionIndex == 0 ? 1 : 0;
        boolean canceled = isCanceledPointerUp(event);

        // End the complete native frame before leaving native multi-touch. Leaving the remaining
        // contact active would make Windows continue processing an already completed gesture.
        frameContacts.clear();
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            // Finish every native contact atomically. A normal two-finger release must use UP
            // for both contacts so Windows can complete tap and gesture recognition.
            byte eventType = canceled
                    ? MoonBridge.LI_TOUCH_EVENT_CANCEL
                    : MoonBridge.LI_TOUCH_EVENT_UP;
            frameContacts.add(createContact(eventView, event, pointerIndex, CURRENT_SAMPLE,
                    eventType));
        }
        nativeSender.sendContacts(frameContacts, buttonState,
                deviceWidthMm, deviceHeightMm);

        activeContacts.clear();
        pressPointerOrigins.clear();
        clearPressedPointerMotion();
        if (singlePointerRemainderMode == SinglePointerRemainderMode.RELATIVE) {
            beginRelativeRemainder(event, remainingIndex);
        }
        else {
            // An absolute mouse gesture must keep the cursor at the location where Windows
            // evaluates the native touchpad tap. Consume the remaining Android contact until
            // every finger is lifted; the next ACTION_DOWN starts a fresh absolute gesture.
            clearSinglePointerRemainder();
            gestureState = GestureState.SUPPRESSED;
        }
    }

    private void beginRelativeRemainder(MotionEvent event, int pointerIndex) {
        remainderMotionSender.resendAbsoluteMousePosition();
        remainderMotionSender.updateScaleFactors();
        remainderMotionSender.beginPointerMotion(event.getEventTime());
        remainderPointerId = event.getPointerId(pointerIndex);
        remainderX = (int) event.getX(pointerIndex);
        remainderY = (int) event.getY(pointerIndex);
        gestureState = GestureState.RELATIVE_REMAINDER;
    }

    private void handleRelativeRemainder(View eventView, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                updateDeviceDimensions(eventView);
                if (!beginNativeGesture(eventView, event)) {
                    // Native input was already supported earlier in this gesture. If it becomes
                    // unavailable unexpectedly, consume the remainder to avoid duplicate input.
                    gestureState = GestureState.SUPPRESSED;
                }
                else {
                    scheduleLongPress(event.getEventTime());
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(remainderPointerId);
                if (pointerIndex < 0 || event.getPointerCount() != 1) {
                    gestureState = GestureState.SUPPRESSED;
                    return;
                }

                for (int historyIndex = 0;
                     historyIndex < event.getHistorySize();
                     historyIndex++) {
                    sendRelativeRemainderSample(event, pointerIndex, historyIndex);
                }
                sendRelativeRemainderSample(event, pointerIndex, CURRENT_SAMPLE);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetState();
                break;

            default:
                break;
        }
    }

    private void sendRelativeRemainderSample(MotionEvent event, int pointerIndex,
                                             int historyIndex) {
        int eventX = (int) (historyIndex < 0
                ? event.getX(pointerIndex)
                : event.getHistoricalX(pointerIndex, historyIndex));
        int eventY = (int) (historyIndex < 0
                ? event.getY(pointerIndex)
                : event.getHistoricalY(pointerIndex, historyIndex));
        if (eventX == remainderX && eventY == remainderY) {
            return;
        }

        int touchDeltaX = eventX - remainderX;
        int touchDeltaY = eventY - remainderY;
        long eventTime = historyIndex < 0
                ? event.getEventTime()
                : event.getHistoricalEventTime(historyIndex);
        remainderMotionSender.sendTouchpadMove(
                touchDeltaX, touchDeltaY, eventTime);

        // Touch coordinates always follow the hardware sample. Fractional mouse movement is
        // retained by TouchpadMotionSender, which keeps velocity and output accumulation separate.
        remainderX = eventX;
        remainderY = eventY;
    }

    private void beginPressedPointerMotion(int pointerId, int touchX,
                                           int touchY) {
        pressedPointerId = pointerId;
        pressedPointerTouchX = touchX;
        pressedPointerTouchY = touchY;
    }

    private boolean sendPressedPointerMotion(MotionEvent event) {
        if (pressedPointerMotionSender == null ||
                pressedPointerId < 0 ||
                event.getPointerCount() != 1) {
            return false;
        }

        int pointerIndex = event.findPointerIndex(pressedPointerId);
        if (pointerIndex < 0) {
            clearPressedPointerMotion();
            return false;
        }

        for (int historyIndex = 0;
             historyIndex < event.getHistorySize();
             historyIndex++) {
            sendPressedPointerMotionSample(
                    (int) event.getHistoricalX(pointerIndex, historyIndex),
                    (int) event.getHistoricalY(pointerIndex, historyIndex),
                    event.getHistoricalEventTime(historyIndex));
        }
        sendPressedPointerMotionSample((int) event.getX(pointerIndex),
                (int) event.getY(pointerIndex), event.getEventTime());
        return true;
    }

    private void sendPressedPointerMotionSample(int touchX, int touchY,
                                                long eventTime) {
        if (touchX == pressedPointerTouchX &&
                touchY == pressedPointerTouchY) {
            return;
        }

        pressedPointerMotionSender.sendTouchpadMove(
                touchX - pressedPointerTouchX,
                touchY - pressedPointerTouchY,
                eventTime);
        pressedPointerTouchX = touchX;
        pressedPointerTouchY = touchY;
    }

    private void clearPressedPointerMotion() {
        pressedPointerId = -1;
        pressedPointerTouchX = 0;
        pressedPointerTouchY = 0;
    }

    private List<Contact> createMoveFrame(View eventView, MotionEvent event, int historyIndex) {
        frameContacts.clear();
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            frameContacts.add(createContact(eventView, event, pointerIndex, historyIndex,
                    MoonBridge.LI_TOUCH_EVENT_MOVE));
        }
        return frameContacts;
    }

    private Contact createContact(View eventView, MotionEvent event, int pointerIndex,
                                  int historyIndex, byte eventType) {
        float x;
        float y;
        float pressure;
        float touchMajor;
        float touchMinor;

        if (historyIndex < 0) {
            x = event.getX(pointerIndex);
            y = event.getY(pointerIndex);
            pressure = event.getPressure(pointerIndex);
            touchMajor = event.getTouchMajor(pointerIndex);
            touchMinor = event.getTouchMinor(pointerIndex);
        }
        else {
            x = event.getHistoricalX(pointerIndex, historyIndex);
            y = event.getHistoricalY(pointerIndex, historyIndex);
            pressure = event.getHistoricalPressure(pointerIndex, historyIndex);
            touchMajor = event.getHistoricalTouchMajor(pointerIndex, historyIndex);
            touchMinor = event.getHistoricalTouchMinor(pointerIndex, historyIndex);
        }

        // Native touchpad coordinates describe the physical gesture surface,
        // not a point in the streamed image. Normalize in the event View's
        // local space intentionally; viewport mapping would corrupt Windows
        // touchpad geometry when the video is letterboxed or transformed.
        int width = Math.max(1, eventView.getWidth());
        int height = Math.max(1, eventView.getHeight());
        return new Contact(eventType, event.getPointerId(pointerIndex),
                clampUnit(x / width), clampUnit(y / height), clampUnit(pressure),
                clampUnit(touchMajor / width), clampUnit(touchMinor / height));
    }

    private void syncActiveContacts(List<Contact> contacts) {
        for (Contact contact : contacts) {
            if (contact.eventType == MoonBridge.LI_TOUCH_EVENT_UP ||
                    contact.eventType == MoonBridge.LI_TOUCH_EVENT_CANCEL) {
                activeContacts.remove(contact.pointerId);
            }
            else {
                activeContacts.put(contact.pointerId, contact);
            }
        }
    }

    private void rememberPressOrigins(MotionEvent event) {
        pressPointerOrigins.clear();
        for (int pointerIndex = 0;
             pointerIndex < event.getPointerCount();
             pointerIndex++) {
            pressPointerOrigins.put(event.getPointerId(pointerIndex),
                    new PointF(event.getX(pointerIndex),
                            event.getY(pointerIndex)));
        }
    }

    private void cancelLongPressIfMoved(MotionEvent event) {
        if (pressSource != PressSource.NONE) {
            return;
        }

        for (int pointerIndex = 0;
             pointerIndex < event.getPointerCount();
             pointerIndex++) {
            PointF origin = pressPointerOrigins.get(
                    event.getPointerId(pointerIndex));
            if (origin == null ||
                    hasMovedBeyondPressSlop(event, pointerIndex,
                            origin.x, origin.y)) {
                cancelLongPress();
                return;
            }
        }
    }

    private void cancelNativeContacts() {
        releaseTouchpadButton(false);
        if (gestureState == GestureState.NATIVE_GESTURE &&
                activeContacts.size() != 0) {
            nativeSender.cancelAll(deviceWidthMm, deviceHeightMm);
            if (singlePointerRemainderMode == SinglePointerRemainderMode.RELATIVE) {
                remainderMotionSender.resendAbsoluteMousePosition();
            }
        }
        activeContacts.clear();
    }

    private void resetState() {
        clearObservedContact();
        releaseTouchpadButton(false);
        activeContacts.clear();
        pressPointerOrigins.clear();
        clearPressedPointerMotion();
        clearSinglePointerRemainder();
        gestureState = GestureState.IDLE;
    }

    private void clearSinglePointerRemainder() {
        remainderPointerId = 0;
        remainderX = 0;
        remainderY = 0;
    }

    private void updateDeviceDimensions(View eventView) {
        int eventViewWidth = Math.max(1, eventView.getWidth());
        int eventViewHeight = Math.max(1, eventView.getHeight());

        DisplayMetrics metrics = eventView.getResources().getDisplayMetrics();
        deviceWidthMm = pixelsToMillimeters(eventViewWidth, metrics.xdpi);
        deviceHeightMm = pixelsToMillimeters(eventViewHeight, metrics.ydpi);
    }

    private static short pixelsToMillimeters(int pixels, float dotsPerInch) {
        if (pixels <= 0 || !isFinite(dotsPerInch) || dotsPerInch <= 0) {
            return 0;
        }

        int millimeters = Math.round(pixels / dotsPerInch * MILLIMETERS_PER_INCH);
        return (short) Math.max(MIN_TOUCHPAD_SIZE_MM,
                Math.min(MAX_TOUCHPAD_SIZE_MM, millimeters));
    }

    private static boolean isCanceledPointerUp(MotionEvent event) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0;
    }

    private static float clampUnit(float value) {
        if (!isFinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static final class Contact {
        final byte eventType;
        final int pointerId;
        final float x;
        final float y;
        final float pressure;
        final float contactAreaMajor;
        final float contactAreaMinor;

        Contact(byte eventType, int pointerId, float x, float y, float pressure,
                float contactAreaMajor, float contactAreaMinor) {
            this.eventType = eventType;
            this.pointerId = pointerId;
            this.x = x;
            this.y = y;
            this.pressure = pressure;
            this.contactAreaMajor = contactAreaMajor;
            this.contactAreaMinor = contactAreaMinor;
        }

        Contact withEventType(byte eventType) {
            return new Contact(eventType, pointerId, x, y, pressure,
                    contactAreaMajor, contactAreaMinor);
        }
    }

    private final class NativeTouchpadSender {
        private final PointerInputSink inputSink;
        private final byte[] eventTypes = new byte[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final int[] pointerIds = new int[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final float[] x = new float[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final float[] y = new float[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final float[] pressure = new float[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private boolean frameEventsUnsupported;
        private boolean touchpadEventsUnsupported;

        NativeTouchpadSender(PointerInputSink inputSink) {
            this.inputSink = inputSink;
        }

        boolean sendContacts(List<Contact> contacts, byte buttonState,
                             short deviceWidthMm, short deviceHeightMm) {
            if (contacts.isEmpty()) {
                return false;
            }

            if (!frameEventsUnsupported) {
                for (int i = 0; i < contacts.size(); i++) {
                    Contact contact = contacts.get(i);
                    eventTypes[i] = contact.eventType;
                    pointerIds[i] = contact.pointerId;
                    x[i] = contact.x;
                    y[i] = contact.y;
                    pressure[i] = contact.pressure;
                }

                int result = inputSink.sendTouchpadFrameEvent((byte) contacts.size(),
                        eventTypes, pointerIds, x, y, pressure, MoonBridge.LI_ROT_UNKNOWN,
                        deviceWidthMm, deviceHeightMm, buttonState);
                if (result == 0) {
                    return true;
                }
                if (result == MoonBridge.LI_ERR_UNSUPPORTED) {
                    frameEventsUnsupported = true;
                }
                else {
                    return false;
                }
            }

            if (touchpadEventsUnsupported) {
                return false;
            }

            for (Contact contact : contacts) {
                int result = inputSink.sendTouchpadEvent(contact.eventType, contact.pointerId,
                        contact.x, contact.y, contact.pressure,
                        contact.contactAreaMajor, contact.contactAreaMinor,
                        MoonBridge.LI_ROT_UNKNOWN, deviceWidthMm, deviceHeightMm, buttonState);
                if (result == MoonBridge.LI_ERR_UNSUPPORTED) {
                    touchpadEventsUnsupported = true;
                    return false;
                }
                if (result != 0) {
                    return false;
                }
            }
            return true;
        }

        void cancelAll(short deviceWidthMm, short deviceHeightMm) {
            if (touchpadEventsUnsupported) {
                return;
            }

            int result = inputSink.sendTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                    0, 0, 0, 0, 0, 0, MoonBridge.LI_ROT_UNKNOWN,
                    deviceWidthMm, deviceHeightMm, (byte) 0);
            if (result == MoonBridge.LI_ERR_UNSUPPORTED) {
                touchpadEventsUnsupported = true;
            }
        }
    }

}
