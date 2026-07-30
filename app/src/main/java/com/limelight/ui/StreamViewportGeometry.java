package com.limelight.ui;

/**
 * Pure conversions between pixel grids used by a stream viewport.
 *
 * <p>Pixel positions map endpoint-to-endpoint because both coordinate spaces
 * address pixel centers. Dimensions use a simple size ratio because they
 * describe lengths rather than addressable coordinates.</p>
 */
final class StreamViewportGeometry {
    private static final float FIXED_POINT_SCALE = 65536f;

    private StreamViewportGeometry() {
    }

    static float mapPixelCoordinate(
            int coordinate,
            int sourceSize,
            int targetSize) {
        if (sourceSize <= 1 || targetSize <= 1) {
            return 0f;
        }

        int clampedCoordinate =
                Math.max(0, Math.min(sourceSize - 1, coordinate));
        return clampedCoordinate *
                (targetSize - 1f) /
                (sourceSize - 1f);
    }

    static float mapScaledDimension(
            int fixedPointScale,
            int encodedSize,
            int viewportSize) {
        if (encodedSize <= 0 || viewportSize <= 0) {
            return 1f;
        }
        float captureToEncoded = fixedPointScale > 0
                ? fixedPointScale / FIXED_POINT_SCALE
                : 1f;
        return captureToEncoded *
                viewportSize /
                encodedSize;
    }
}
