package com.limelight.ui;

import android.graphics.Matrix;
import android.view.View;

/**
 * Maps points between two direct sibling views through their shared parent.
 *
 * This keeps layout position, translation, scaling, and rotation in one
 * coordinate conversion and avoids treating window insets as cursor offsets.
 */
final class ViewCoordinateMapper {
    private ViewCoordinateMapper() {
    }

    static boolean mapPointBetweenSiblings(View source,
                                           View target,
                                           float[] point,
                                           Matrix targetInverse) {
        if (source.getParent() == null ||
                source.getParent() != target.getParent()) {
            return false;
        }

        source.getMatrix().mapPoints(point);
        point[0] += source.getLeft();
        point[1] += source.getTop();

        point[0] -= target.getLeft();
        point[1] -= target.getTop();
        if (!target.getMatrix().invert(targetInverse)) {
            return false;
        }
        targetInverse.mapPoints(point);
        return true;
    }
}
