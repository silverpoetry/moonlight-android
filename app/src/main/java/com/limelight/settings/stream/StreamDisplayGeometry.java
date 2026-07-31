package com.limelight.settings.stream;

/**
 * Pure display-shape policy shared by settings and stream composition.
 */
public final class StreamDisplayGeometry {
    private static final float SQUARISH_ASPECT_RATIO_LIMIT = 1.3f;

    private StreamDisplayGeometry() {
    }

    public static boolean isSquarish(int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        float longSide = Math.max(width, height);
        float shortSide = Math.min(width, height);
        return longSide / shortSide <
                SQUARISH_ASPECT_RATIO_LIMIT;
    }
}
