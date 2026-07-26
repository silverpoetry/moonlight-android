package com.limelight.binding.input.touch;

import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.LimeLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Forwards touchscreen gestures with two or more contacts as native touchpad input.
 *
 * <p>Standalone single-finger gestures intentionally remain on the legacy mouse path. This keeps
 * local cursor rendering and host cursor movement driven by the same mouse packet. When a native
 * multi-finger gesture returns to one contact, the remaining contact sends mouse movement only
 * until it is lifted, preventing a discontinuity or a synthetic tap.</p>
 */
public final class TouchscreenTouchpadHandler {
    private enum GestureState {
        IDLE,
        NATIVE_MULTITOUCH,
        MOUSE_REMAINDER,
        SUPPRESSED
    }

    private static final int CURRENT_SAMPLE = -1;
    private static final float MILLIMETERS_PER_INCH = 25.4f;
    private static final int MIN_TOUCHPAD_SIZE_MM = 40;
    private static final int MAX_TOUCHPAD_SIZE_MM = 200;
    private static final long TRACE_WINDOW_MS = 500;

    private final NativeTouchpadSender nativeSender;
    private final TouchpadMotionSender mouseMotionSender;
    private final SparseArray<Contact> activeContacts = new SparseArray<>();
    private final List<Contact> frameContacts =
            new ArrayList<>(MoonBridge.LI_TOUCHPAD_MAX_CONTACTS);
    private GestureState gestureState = GestureState.IDLE;
    private short deviceWidthMm;
    private short deviceHeightMm;
    private int mousePointerId;
    private int mouseX;
    private int mouseY;
    private int traceGestureId;
    private boolean traceGestureActive;
    private long traceWindowStartMs;
    private long traceLastCallbackMs;
    private long traceLastSampleTimeMs;
    private long traceSourceGapTotalMs;
    private long traceSourceGapMaxMs;
    private long traceCallbackGapMaxMs;
    private long traceDeliveryLagTotalMs;
    private long traceDeliveryLagMaxMs;
    private int traceCallbackCount;
    private int traceSampleCount;
    private int traceSourceIntervalCount;
    private int traceHistoricalSampleCount;
    private int traceMaxHistorySize;
    private int traceSendFailures;

    public TouchscreenTouchpadHandler(NvConnection connection, View targetView,
                                     int referenceWidth, int referenceHeight,
                                     PreferenceConfiguration prefConfig) {
        nativeSender = new NativeTouchpadSender(connection);
        mouseMotionSender = new TouchpadMotionSender(connection, referenceWidth, referenceHeight,
                targetView, prefConfig);
    }

    /**
     * Returns whether this handler owns the current touchscreen gesture.
     */
    public boolean isHandlingGesture() {
        return gestureState != GestureState.IDLE;
    }

    /**
     * Handles native multi-finger touchpad input and the movement-only single-finger tail.
     *
     * @return {@code true} when this handler owns the event, or {@code false} when the caller
     *         should dispatch it through the original touchscreen touchpad implementation.
     */
    public boolean handleMotionEvent(View eventView, MotionEvent event) {
        if (eventView == null) {
            return false;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // A new gesture always starts on the original single-finger mouse path.
            cancel();
            return false;
        }

        if (gestureState == GestureState.SUPPRESSED) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                resetState();
            }
            return true;
        }

        if (event.getPointerCount() > MoonBridge.LI_TOUCHPAD_MAX_CONTACTS) {
            cancelNativeContacts();
            gestureState = GestureState.SUPPRESSED;
            return true;
        }

        switch (gestureState) {
            case IDLE:
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN &&
                        event.getPointerCount() >= 2) {
                    updateDeviceDimensions(eventView);
                    return beginNativeGesture(eventView, event);
                }
                return false;

            case NATIVE_MULTITOUCH:
                updateDeviceDimensions(eventView);
                handleNativeGesture(eventView, event);
                return true;

            case MOUSE_REMAINDER:
                handleMouseRemainder(eventView, event);
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

    private boolean beginNativeGesture(View eventView, MotionEvent event) {
        beginTraceGesture(event);
        frameContacts.clear();
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            frameContacts.add(createContact(eventView, event, pointerIndex, CURRENT_SAMPLE,
                    MoonBridge.LI_TOUCH_EVENT_DOWN));
        }

        boolean sent = nativeSender.sendContacts(frameContacts, (byte) 0,
                deviceWidthMm, deviceHeightMm, event.getEventTime());
        recordTraceSample(event.getEventTime(), false, sent);
        if (!sent) {
            finishTraceGesture("unsupported");
            return false;
        }

        activeContacts.clear();
        syncActiveContacts(frameContacts);
        clearMouseRemainder();
        gestureState = GestureState.NATIVE_MULTITOUCH;
        return true;
    }

    private void handleNativeGesture(View eventView, MotionEvent event) {
        recordTraceCallback(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                sendAdditionalContactDown(eventView, event);
                break;

            case MotionEvent.ACTION_MOVE:
                sendNativeMoves(eventView, event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() - 1 == 1) {
                    finishNativeGestureWithMouseRemainder(eventView, event);
                }
                else {
                    sendContactUp(eventView, event);
                }
                break;

            case MotionEvent.ACTION_UP:
                sendContactUp(eventView, event);
                mouseMotionSender.resendAbsoluteMousePosition();
                resetState();
                break;

            case MotionEvent.ACTION_CANCEL:
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

        boolean sent = nativeSender.sendContacts(frameContacts, (byte) 0,
                deviceWidthMm, deviceHeightMm, event.getEventTime());
        recordTraceSample(event.getEventTime(), false, sent);
        syncActiveContacts(frameContacts);
    }

    private void sendNativeMoves(View eventView, MotionEvent event) {
        for (int historyIndex = 0; historyIndex < event.getHistorySize(); historyIndex++) {
            List<Contact> contacts = createMoveFrame(eventView, event, historyIndex);
            long sampleTimeMs = event.getHistoricalEventTime(historyIndex);
            boolean sent = nativeSender.sendContacts(contacts, (byte) 0,
                    deviceWidthMm, deviceHeightMm, sampleTimeMs);
            recordTraceSample(sampleTimeMs, true, sent);
            syncActiveContacts(contacts);
        }

        List<Contact> contacts = createMoveFrame(eventView, event, CURRENT_SAMPLE);
        boolean sent = nativeSender.sendContacts(contacts, (byte) 0,
                deviceWidthMm, deviceHeightMm, event.getEventTime());
        recordTraceSample(event.getEventTime(), false, sent);
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
            frameContacts.add(createContact(eventView, event, pointerIndex, CURRENT_SAMPLE,
                    eventType));
        }

        boolean sent = nativeSender.sendContacts(frameContacts, (byte) 0,
                deviceWidthMm, deviceHeightMm, event.getEventTime());
        recordTraceSample(event.getEventTime(), false, sent);
        syncActiveContacts(frameContacts);
        activeContacts.remove(actionPointerId);
    }

    private void finishNativeGestureWithMouseRemainder(View eventView, MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int remainingIndex = actionIndex == 0 ? 1 : 0;
        boolean canceled = isCanceledPointerUp(event);

        // End the complete native frame before resuming mouse movement. Leaving the remaining
        // contact active would make Windows move the host cursor from both input paths.
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
        boolean sent = nativeSender.sendContacts(frameContacts, (byte) 0,
                deviceWidthMm, deviceHeightMm, event.getEventTime());
        recordTraceSample(event.getEventTime(), false, sent);
        mouseMotionSender.resendAbsoluteMousePosition();
        finishTraceGesture("native-end");

        activeContacts.clear();
        beginMouseRemainder(event, remainingIndex);
    }

    private void beginMouseRemainder(MotionEvent event, int pointerIndex) {
        mouseMotionSender.updateScaleFactors();
        mouseMotionSender.beginPointerMotion(event.getEventTime());
        mousePointerId = event.getPointerId(pointerIndex);
        mouseX = (int) event.getX(pointerIndex);
        mouseY = (int) event.getY(pointerIndex);
        gestureState = GestureState.MOUSE_REMAINDER;
    }

    private void handleMouseRemainder(View eventView, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                updateDeviceDimensions(eventView);
                if (!beginNativeGesture(eventView, event)) {
                    // Native input was already supported earlier in this gesture. If it becomes
                    // unavailable unexpectedly, consume the remainder to avoid duplicate input.
                    gestureState = GestureState.SUPPRESSED;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(mousePointerId);
                if (pointerIndex < 0 || event.getPointerCount() != 1) {
                    gestureState = GestureState.SUPPRESSED;
                    return;
                }

                for (int historyIndex = 0;
                     historyIndex < event.getHistorySize();
                     historyIndex++) {
                    sendMouseRemainderSample(event, pointerIndex, historyIndex);
                }
                sendMouseRemainderSample(event, pointerIndex, CURRENT_SAMPLE);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetState();
                break;

            default:
                break;
        }
    }

    private void sendMouseRemainderSample(MotionEvent event, int pointerIndex, int historyIndex) {
        int eventX = (int) (historyIndex < 0
                ? event.getX(pointerIndex)
                : event.getHistoricalX(pointerIndex, historyIndex));
        int eventY = (int) (historyIndex < 0
                ? event.getY(pointerIndex)
                : event.getHistoricalY(pointerIndex, historyIndex));
        if (eventX == mouseX && eventY == mouseY) {
            return;
        }

        int touchDeltaX = eventX - mouseX;
        int touchDeltaY = eventY - mouseY;
        long eventTime = historyIndex < 0
                ? event.getEventTime()
                : event.getHistoricalEventTime(historyIndex);
        mouseMotionSender.sendTouchpadMove(touchDeltaX, touchDeltaY, eventTime);

        // Touch coordinates always follow the hardware sample. Fractional mouse movement is
        // retained by TouchpadMotionSender, which keeps velocity and output accumulation separate.
        mouseX = eventX;
        mouseY = eventY;
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

    private void cancelNativeContacts() {
        if (gestureState == GestureState.NATIVE_MULTITOUCH && activeContacts.size() != 0) {
            nativeSender.cancelAll(deviceWidthMm, deviceHeightMm);
            mouseMotionSender.resendAbsoluteMousePosition();
        }
        activeContacts.clear();
    }

    private void resetState() {
        finishTraceGesture("end");
        activeContacts.clear();
        clearMouseRemainder();
        gestureState = GestureState.IDLE;
    }

    private void clearMouseRemainder() {
        mousePointerId = 0;
        mouseX = 0;
        mouseY = 0;
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

    private void beginTraceGesture(MotionEvent event) {
        traceGestureActive = true;
        traceGestureId++;
        traceWindowStartMs = SystemClock.elapsedRealtime();
        traceLastCallbackMs = traceWindowStartMs;
        traceLastSampleTimeMs = 0;
        resetTraceWindow();
        traceCallbackCount = 1;
        traceMaxHistorySize = event.getHistorySize();
        LimeLog.info("TPTRACE_ANDROID_GESTURE phase=begin gesture=" + traceGestureId +
                " t_ms=" + traceWindowStartMs + " event_ms=" + event.getEventTime() +
                " pointers=" + event.getPointerCount());
    }

    private void recordTraceCallback(MotionEvent event) {
        if (!traceGestureActive) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long callbackGap = now - traceLastCallbackMs;
        traceCallbackGapMaxMs = Math.max(traceCallbackGapMaxMs, callbackGap);
        traceLastCallbackMs = now;
        traceCallbackCount++;
        traceMaxHistorySize = Math.max(traceMaxHistorySize, event.getHistorySize());
    }

    private void recordTraceSample(long sampleTimeMs, boolean historical, boolean sent) {
        if (!traceGestureActive) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (traceLastSampleTimeMs != 0) {
            long sourceGap = sampleTimeMs - traceLastSampleTimeMs;
            if (sourceGap >= 0) {
                traceSourceGapTotalMs += sourceGap;
                traceSourceGapMaxMs = Math.max(traceSourceGapMaxMs, sourceGap);
                traceSourceIntervalCount++;
            }
        }
        traceLastSampleTimeMs = sampleTimeMs;
        traceSampleCount++;
        if (historical) {
            traceHistoricalSampleCount++;
        }
        if (!sent) {
            traceSendFailures++;
        }

        long deliveryLag = Math.max(0, now - sampleTimeMs);
        traceDeliveryLagTotalMs += deliveryLag;
        traceDeliveryLagMaxMs = Math.max(traceDeliveryLagMaxMs, deliveryLag);

        if (now - traceWindowStartMs >= TRACE_WINDOW_MS) {
            logTraceWindow(now, false);
        }
    }

    private void finishTraceGesture(String reason) {
        if (!traceGestureActive) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        logTraceWindow(now, true);
        LimeLog.info("TPTRACE_ANDROID_GESTURE phase=end gesture=" + traceGestureId +
                " t_ms=" + now + " reason=" + reason);
        traceGestureActive = false;
    }

    private void logTraceWindow(long now, boolean finalWindow) {
        long elapsedMs = Math.max(1, now - traceWindowStartMs);
        if (traceSampleCount != 0 || finalWindow) {
            double rateHz = traceSampleCount * 1000.0 / elapsedMs;
            double sourceGapAverage = traceSourceIntervalCount == 0 ? 0 :
                    traceSourceGapTotalMs / (double) traceSourceIntervalCount;
            double deliveryLagAverage = traceSampleCount == 0 ? 0 :
                    traceDeliveryLagTotalMs / (double) traceSampleCount;
            LimeLog.info("TPTRACE_ANDROID_SOURCE t_ms=" + now +
                    " window_ms=" + elapsedMs + " gesture=" + traceGestureId +
                    " samples=" + traceSampleCount + " rate_hz=" + rateHz +
                    " callbacks=" + traceCallbackCount +
                    " historical=" + traceHistoricalSampleCount +
                    " history_max=" + traceMaxHistorySize +
                    " source_gap_avg_ms=" + sourceGapAverage +
                    " source_gap_max_ms=" + traceSourceGapMaxMs +
                    " callback_gap_max_ms=" + traceCallbackGapMaxMs +
                    " delivery_lag_avg_ms=" + deliveryLagAverage +
                    " delivery_lag_max_ms=" + traceDeliveryLagMaxMs +
                    " send_failures=" + traceSendFailures +
                    " final=" + finalWindow);
        }
        traceWindowStartMs = now;
        resetTraceWindow();
    }

    private void resetTraceWindow() {
        traceSourceGapTotalMs = 0;
        traceSourceGapMaxMs = 0;
        traceCallbackGapMaxMs = 0;
        traceDeliveryLagTotalMs = 0;
        traceDeliveryLagMaxMs = 0;
        traceCallbackCount = 0;
        traceSampleCount = 0;
        traceSourceIntervalCount = 0;
        traceHistoricalSampleCount = 0;
        traceMaxHistorySize = 0;
        traceSendFailures = 0;
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
    }

    private final class NativeTouchpadSender {
        private final NvConnection connection;
        private final byte[] eventTypes = new byte[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final int[] pointerIds = new int[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final float[] x = new float[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final float[] y = new float[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private final float[] pressure = new float[MoonBridge.LI_TOUCHPAD_MAX_CONTACTS];
        private boolean frameEventsUnsupported;
        private boolean touchpadEventsUnsupported;

        NativeTouchpadSender(NvConnection connection) {
            this.connection = connection;
        }

        boolean sendContacts(List<Contact> contacts, byte buttonState,
                             short deviceWidthMm, short deviceHeightMm, long eventTimeMs) {
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

                int result = connection.sendTouchpadFrameEvent((byte) contacts.size(),
                        eventTypes, pointerIds, x, y, pressure, eventTimeMs,
                        MoonBridge.LI_ROT_UNKNOWN,
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
                int result = connection.sendTouchpadEvent(contact.eventType, contact.pointerId,
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

            int result = connection.sendTouchpadEvent(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                    0, 0, 0, 0, 0, 0, MoonBridge.LI_ROT_UNKNOWN,
                    deviceWidthMm, deviceHeightMm, (byte) 0);
            if (result == MoonBridge.LI_ERR_UNSUPPORTED) {
                touchpadEventsUnsupported = true;
            }
        }
    }

}
