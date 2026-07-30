package com.limelight.ui;

/**
 * Pure coordinate conversion for the native cursor overlay.
 */
final class CursorGeometry {
    private CursorGeometry() {
    }

    static float mapReferenceCoordinate(int coordinate,
                                        int referenceSize,
                                        int targetSize) {
        if (referenceSize <= 1 || targetSize <= 1) {
            return 0f;
        }

        int clampedCoordinate =
                Math.max(0, Math.min(referenceSize - 1, coordinate));
        return clampedCoordinate * (targetSize - 1f) / (referenceSize - 1f);
    }
}
