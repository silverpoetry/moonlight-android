package com.limelight.utils;

/**
 * Resolves stream orientation intent without depending on Activity state.
 */
public final class StreamOrientationPolicy {
    public enum Mode {
        FOLLOW_USER,
        FOLLOW_USER_ALL_ROTATIONS,
        USER_LANDSCAPE,
        USER_PORTRAIT,
        SENSOR_PORTRAIT
    }

    private StreamOrientationPolicy() {
    }

    public static Mode resolveGameMode(
            boolean adaptiveWindow,
            int windowWidthDp,
            int windowHeightDp,
            boolean onScreenControllerEnabled,
            boolean nativeResolution,
            int streamWidth,
            int streamHeight,
            boolean portraitRequested,
            boolean automaticOrientationEnabled) {
        if (adaptiveWindow) {
            return Mode.FOLLOW_USER_ALL_ROTATIONS;
        }

        if (isSquarish(windowWidthDp, windowHeightDp)) {
            if (nativeResolution) {
                return streamWidth > streamHeight
                        ? Mode.USER_LANDSCAPE
                        : Mode.USER_PORTRAIT;
            }
            if (onScreenControllerEnabled) {
                return Mode.USER_LANDSCAPE;
            }
            return Mode.FOLLOW_USER_ALL_ROTATIONS;
        }

        if (portraitRequested) {
            return Mode.SENSOR_PORTRAIT;
        }
        if (automaticOrientationEnabled) {
            return Mode.FOLLOW_USER;
        }
        return Mode.USER_LANDSCAPE;
    }

    static boolean isSquarish(int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        float longDimension = Math.max(width, height);
        float shortDimension = Math.min(width, height);
        return longDimension / shortDimension < 1.3f;
    }
}
