package com.limelight.binding.input.touch;

import android.util.DisplayMetrics;
import android.view.View;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

// Converts physical finger motion into stream mouse packets while keeping acceleration,
// sub-pixel accumulation, sensitivity, and absolute-mouse transport in one place.
public final class TouchpadMotionSender {
    static final class MotionDelta {
        int x;
        int y;

        void set(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean isEmpty() {
            return x == 0 && y == 0;
        }
    }

    private static final double MILLIMETERS_PER_INCH = 25.4;
    private static final float MIN_VALID_DPI = 50.0f;
    private static final float MAX_VALID_DPI = 2000.0f;

    private final PointerInputSink inputSink;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final InputSettingsState settingsState;
    private final TouchpadPointerAcceleration pointerAcceleration =
            new TouchpadPointerAcceleration();
    private final MotionDelta pendingMotionDelta = new MotionDelta();
    private final double xDpi;
    private final double yDpi;

    private double xFactor;
    private double yFactor;
    private double xRemainder;
    private double yRemainder;

    public TouchpadMotionSender(PointerInputSink inputSink, int referenceWidth,
                                int referenceHeight, View targetView,
                                InputSettingsState settingsState) {
        this.inputSink = inputSink;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.targetView = targetView;
        this.settingsState = settingsState;

        DisplayMetrics metrics = targetView.getResources().getDisplayMetrics();
        xDpi = sanitizeDpi(metrics.xdpi, metrics.densityDpi);
        yDpi = sanitizeDpi(metrics.ydpi, metrics.densityDpi);
    }

    void updateScaleFactors() {
        int viewWidth = targetView.getWidth();
        int viewHeight = targetView.getHeight();
        if (viewWidth > 0 && viewHeight > 0) {
            xFactor = referenceWidth / (double) viewWidth;
            yFactor = referenceHeight / (double) viewHeight;
        }
    }

    int scaleTouchDeltaX(int deltaX) {
        return scaleTouchDelta(deltaX, xFactor);
    }

    int scaleTouchDeltaY(int deltaY) {
        return scaleTouchDelta(deltaY, yFactor);
    }

    void beginPointerMotion(long eventTime) {
        pointerAcceleration.restart(eventTime);
        xRemainder = 0.0;
        yRemainder = 0.0;
    }

    void transformTouchpadMove(int touchDeltaX, int touchDeltaY, long eventTime,
                               MotionDelta outputDelta) {
        if (touchDeltaX == 0 && touchDeltaY == 0) {
            outputDelta.set(0, 0);
            return;
        }

        double acceleration = pointerAcceleration.calculateAcceleration(
                touchDeltaX * MILLIMETERS_PER_INCH / xDpi,
                touchDeltaY * MILLIMETERS_PER_INCH / yDpi,
                eventTime);

        InputSettings settings = settingsState.get();
        xRemainder += touchDeltaX * xFactor *
                settings.getTouchpadPointerSensitivityX() *
                0.01 * acceleration;
        yRemainder += touchDeltaY * yFactor *
                settings.getTouchpadPointerSensitivityY() *
                0.01 * acceleration;

        int mouseDeltaX = takeIntegralPart(xRemainder);
        int mouseDeltaY = takeIntegralPart(yRemainder);
        xRemainder -= mouseDeltaX;
        yRemainder -= mouseDeltaY;
        outputDelta.set(mouseDeltaX, mouseDeltaY);
    }

    void sendTouchpadMove(int touchDeltaX, int touchDeltaY, long eventTime) {
        transformTouchpadMove(touchDeltaX, touchDeltaY, eventTime, pendingMotionDelta);
        if (!pendingMotionDelta.isEmpty()) {
            sendMouseMovePacket(pendingMotionDelta.x, pendingMotionDelta.y);
        }
    }

    void sendMouseMovePacket(int scaledDeltaX, int scaledDeltaY) {
        while (scaledDeltaX != 0 || scaledDeltaY != 0) {
            short packetDeltaX = clampToShort(scaledDeltaX);
            short packetDeltaY = clampToShort(scaledDeltaY);
            sendSingleMouseMovePacket(packetDeltaX, packetDeltaY);
            scaledDeltaX -= packetDeltaX;
            scaledDeltaY -= packetDeltaY;
        }
    }

    private void sendSingleMouseMovePacket(short scaledDeltaX, short scaledDeltaY) {
        if (settingsState.get().isAbsoluteMouseMode()) {
            inputSink.sendMouseMoveAsMousePosition(scaledDeltaX, scaledDeltaY,
                    (short) targetView.getWidth(), (short) targetView.getHeight());
        }
        else {
            inputSink.sendMouseMove(scaledDeltaX, scaledDeltaY);
        }
    }

    void resendAbsoluteMousePosition() {
        if (settingsState.get().isAbsoluteMouseMode()) {
            inputSink.sendMouseMoveAsMousePosition((short) 0, (short) 0,
                    (short) targetView.getWidth(), (short) targetView.getHeight());
        }
    }

    void sendHighResScroll(short deltaY) {
        inputSink.sendMouseHighResScroll(deltaY);
    }

    private static int scaleTouchDelta(int delta, double factor) {
        int scaledDelta = (int) Math.round((double) Math.abs(delta) * factor);
        if (delta < 0) {
            scaledDelta = -scaledDelta;
        }

        return scaledDelta;
    }

    private static double sanitizeDpi(float reportedDpi, int densityDpi) {
        if (Float.isFinite(reportedDpi) &&
                reportedDpi >= MIN_VALID_DPI && reportedDpi <= MAX_VALID_DPI) {
            return reportedDpi;
        }

        return densityDpi > 0 ? densityDpi : DisplayMetrics.DENSITY_DEFAULT;
    }

    private static int takeIntegralPart(double value) {
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return (int) value;
    }

    private static short clampToShort(int value) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }
}
