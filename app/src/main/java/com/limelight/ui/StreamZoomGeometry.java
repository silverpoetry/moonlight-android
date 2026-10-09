package com.limelight.ui;

/** Pixel-space viewport transform math, independent of Android event dispatch. */
public final class StreamZoomGeometry {
    private StreamZoomGeometry() {
    }

    public static float clampScale(float scale) {
        return Float.isFinite(scale) ? Math.max(1f, Math.min(15f, scale)) : 1f;
    }

    /** Keeps the content under the old focus under the new focus after scaling. */
    public static float translationAroundFocus(
            float translation, float layoutStart, float pivot,
            float previousFocus, float focus, float scaleRatio) {
        float anchor = layoutStart + pivot;
        return focus - anchor - scaleRatio * (previousFocus - anchor - translation);
    }

    public static float clampTranslation(
            float translation, float layoutStart, float size, float pivot,
            float scale, float viewportStart, float viewportEnd) {
        if (scale == 1f || size <= 0f || viewportEnd <= viewportStart) {
            return 0f;
        }
        float scaledStart = layoutStart + pivot * (1f - scale);
        float scaledEnd = scaledStart + size * scale;
        float min = viewportEnd - scaledEnd;
        float max = viewportStart - scaledStart;
        if (min > max) {
            // A letterboxed axis retains its layout alignment until it fills
            // the viewport, while keeping all of its content visible.
            return Math.max(max, Math.min(0f, min));
        }
        return Math.max(min, Math.min(translation, max));
    }
}
