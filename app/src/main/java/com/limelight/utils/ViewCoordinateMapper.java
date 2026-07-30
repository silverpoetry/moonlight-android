package com.limelight.utils;

import android.graphics.Matrix;
import android.view.View;

/**
 * Maps points and basis vectors between two direct sibling views.
 *
 * <p>The shared-parent transform keeps layout position, translation, scaling,
 * and rotation in one conversion. Window insets are deliberately absent
 * because the siblings already share the inset content rectangle.</p>
 */
public final class ViewCoordinateMapper {
    private ViewCoordinateMapper() {
    }

    public static boolean mapPointBetweenSiblings(
            View source,
            View target,
            float[] point,
            Matrix targetInverse) {
        if (point.length < 2 ||
                source.getParent() == null ||
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

    public static boolean mapBasisBetweenSiblings(
            View source,
            View target,
            float[] basis,
            Matrix targetInverse) {
        if (basis.length < 4 ||
                source.getParent() == null ||
                source.getParent() != target.getParent()) {
            return false;
        }

        source.getMatrix().mapVectors(basis);
        if (!target.getMatrix().invert(targetInverse)) {
            return false;
        }
        targetInverse.mapVectors(basis);
        return true;
    }
}
