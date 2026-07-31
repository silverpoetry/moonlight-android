package com.limelight.binding.input;

/**
 * Converts device gyroscope samples into the customized right-stick output.
 */
final class ControllerGyroStickTranslator {
    private static final float SMOOTHING_ALPHA = 0.3f;
    private static final float DEADZONE = 0.015f;
    private static final float MINIMUM_OUTPUT = 0.05f;
    private static final float HORIZONTAL_SENSITIVITY = 1.2f;
    private static final float VERTICAL_SENSITIVITY = 1.0f;

    private float filteredX;
    private float filteredY;
    private short rightStickX;
    private short rightStickY;

    void update(
            float rawX,
            float rawY,
            int displayRotation,
            boolean swapAxes,
            int sensitivityPercent) {
        float gyroX;
        float gyroY;
        if (isLandscape(displayRotation)) {
            gyroX = rawX;
            gyroY = rawY;
        }
        else {
            gyroX = rawY;
            gyroY = rawX;
        }

        if (swapAxes) {
            float temporary = gyroX;
            gyroX = gyroY;
            gyroY = temporary;
        }

        float sensitivityScale =
                Math.max(0, sensitivityPercent) * 0.01f;
        float targetX =
                shapeAxis(
                        gyroX,
                        HORIZONTAL_SENSITIVITY,
                        sensitivityScale);
        float targetY =
                shapeAxis(
                        gyroY,
                        VERTICAL_SENSITIVITY,
                        sensitivityScale);
        filteredX += SMOOTHING_ALPHA * (targetX - filteredX);
        filteredY += SMOOTHING_ALPHA * (targetY - filteredY);

        rightStickX = toInvertedStickValue(filteredX);
        rightStickY = toInvertedStickValue(filteredY);
    }

    void reset() {
        filteredX = 0;
        filteredY = 0;
        rightStickX = 0;
        rightStickY = 0;
    }

    short getRightStickX() {
        return rightStickX;
    }

    short getRightStickY() {
        return rightStickY;
    }

    private static boolean isLandscape(int displayRotation) {
        return displayRotation == ControllerMotionSampleTransformer.ROTATION_90 ||
                displayRotation == ControllerMotionSampleTransformer.ROTATION_270;
    }

    private static float shapeAxis(
            float raw,
            float axisSensitivity,
            float sensitivityScale) {
        float absoluteValue = Math.abs(raw);
        if (absoluteValue < DEADZONE) {
            return 0;
        }

        float normalized =
                (absoluteValue - DEADZONE) /
                        (1.0f - DEADZONE);
        float curved = (float) Math.pow(normalized, 1.5);
        float output =
                (curved + MINIMUM_OUTPUT) *
                        axisSensitivity *
                        sensitivityScale;
        return raw > 0 ? output : -output;
    }

    private static short toInvertedStickValue(float value) {
        float clamped = Math.max(-1f, Math.min(1f, value));
        return (short) -(clamped * Short.MAX_VALUE);
    }
}
