package com.limelight.binding.input;

/**
 * Deduplicates and transforms one Android motion-sensor stream without allocating per sample.
 */
final class ControllerMotionSampleTransformer {
    static final int ROTATION_0 = 0;
    static final int ROTATION_90 = 1;
    static final int ROTATION_180 = 2;
    static final int ROTATION_270 = 3;

    private static final float RADIANS_TO_DEGREES = 57.2957795f;

    private boolean hasPreviousSample;
    private float rawX;
    private float rawY;
    private float rawZ;
    private float transformedX;
    private float transformedY;
    private float transformedZ;

    boolean update(
            float x,
            float y,
            float z,
            int displayRotation,
            boolean applyDeviceOrientationCorrection,
            boolean gyroscope) {
        if (hasPreviousSample &&
                x == rawX &&
                y == rawY &&
                z == rawZ) {
            return false;
        }

        hasPreviousSample = true;
        rawX = x;
        rawY = y;
        rawZ = z;

        if (!applyDeviceOrientationCorrection) {
            transformedX = x;
            transformedY = y;
            transformedZ = z;
        }
        else {
            transformDeviceCoordinates(
                    x,
                    y,
                    z,
                    displayRotation);
        }

        if (gyroscope) {
            transformedX *= RADIANS_TO_DEGREES;
            transformedY *= RADIANS_TO_DEGREES;
            transformedZ *= RADIANS_TO_DEGREES;
        }
        return true;
    }

    float getRawX() {
        return rawX;
    }

    float getRawY() {
        return rawY;
    }

    float getRawZ() {
        return rawZ;
    }

    float getTransformedX() {
        return transformedX;
    }

    float getTransformedY() {
        return transformedY;
    }

    float getTransformedZ() {
        return transformedZ;
    }

    private void transformDeviceCoordinates(
            float x,
            float y,
            float z,
            int displayRotation) {
        switch (displayRotation) {
            case ROTATION_0:
                transformedX = x;
                transformedY = z;
                transformedZ = -y;
                break;
            case ROTATION_90:
                transformedX = -y;
                transformedY = z;
                transformedZ = -x;
                break;
            case ROTATION_180:
                transformedX = -x;
                transformedY = z;
                transformedZ = y;
                break;
            case ROTATION_270:
                transformedX = y;
                transformedY = z;
                transformedZ = x;
                break;
            default:
                transformedX = x;
                transformedY = y;
                transformedZ = z;
                break;
        }
    }
}
