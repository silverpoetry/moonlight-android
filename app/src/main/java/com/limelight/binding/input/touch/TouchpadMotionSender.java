package com.limelight.binding.input.touch;

import android.view.View;

import com.limelight.nvstream.NvConnection;
import com.limelight.preferences.PreferenceConfiguration;

// Converts touch-space deltas into stream mouse packets while keeping pointer
// scaling, touchpad sensitivity, and absolute-mouse transport in one place.
final class TouchpadMotionSender {
    private final NvConnection conn;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final PreferenceConfiguration prefConfig;

    private double xFactor;
    private double yFactor;

    TouchpadMotionSender(NvConnection conn, int referenceWidth, int referenceHeight,
                         View targetView, PreferenceConfiguration prefConfig) {
        this.conn = conn;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.targetView = targetView;
        this.prefConfig = prefConfig;
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

    int scaleMouseDeltaX(int deltaX) {
        return applySensitivity(scaleTouchDeltaX(deltaX), prefConfig.mouseTouchPadSensitityX);
    }

    int scaleMouseDeltaY(int deltaY) {
        return applySensitivity(scaleTouchDeltaY(deltaY), prefConfig.mouseTouchPadSensitityY);
    }

    void sendMouseMovePacket(short scaledDeltaX, short scaledDeltaY) {
        if (prefConfig.absoluteMouseMode) {
            conn.sendMouseMoveAsMousePosition(scaledDeltaX, scaledDeltaY,
                    (short) targetView.getWidth(), (short) targetView.getHeight());
        }
        else {
            conn.sendMouseMove(scaledDeltaX, scaledDeltaY);
        }
    }

    void sendHighResScroll(short deltaY) {
        conn.sendMouseHighResScroll(deltaY);
    }

    private static int scaleTouchDelta(int delta, double factor) {
        int scaledDelta = (int) Math.round((double) Math.abs(delta) * factor);
        if (delta < 0) {
            scaledDelta = -scaledDelta;
        }

        return scaledDelta;
    }

    private static int applySensitivity(int delta, int sensitivity) {
        return (short) (delta * sensitivity * 0.01f);
    }
}
