package com.limelight.utils;

import android.graphics.Rect;
import android.view.View;

/**
 * Converts View geometry into the coordinate space of its containing window.
 */
public final class ViewWindowGeometry {
    private ViewWindowGeometry() {
    }

    /**
     * Writes the transformed and clipped visible bounds of {@code view} in
     * window coordinates.
     *
     * @param expectedWindowRoot root View of the window that will consume the
     *                           resulting bounds
     * @param windowLocationScratch a reusable array with at least two entries
     * @return {@code false} when the View belongs to another window or has no
     * visible bounds
     */
    public static boolean getVisibleBoundsInWindow(
            View view,
            View expectedWindowRoot,
            Rect outBounds,
            int[] windowLocationScratch) {
        if (windowLocationScratch.length < 2) {
            throw new IllegalArgumentException(
                    "Window location scratch array must contain two entries");
        }
        if (view.getRootView() != expectedWindowRoot) {
            return false;
        }
        if (!view.getGlobalVisibleRect(outBounds)) {
            return false;
        }

        expectedWindowRoot.getLocationInWindow(windowLocationScratch);
        outBounds.offset(
                windowLocationScratch[0],
                windowLocationScratch[1]);
        return true;
    }
}
