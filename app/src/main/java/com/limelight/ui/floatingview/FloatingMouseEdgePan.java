package com.limelight.ui.floatingview;

import android.graphics.Matrix;
import android.view.View;

/** Temporary viewport translation that lets an on-screen mouse reach the entire desktop. */
final class FloatingMouseEdgePan {
    private final View panel;
    private final View stream;
    private final Runnable onViewportChanged;
    private final Matrix source = new Matrix();
    private final Matrix target = new Matrix();
    private final Matrix inverse = new Matrix();
    private final float[] points = new float[4];
    private boolean changed;
    private float originalX, originalY, appliedX, appliedY, scaleX, scaleY;
    private int width, height;

    FloatingMouseEdgePan(View panel, View stream, Runnable onViewportChanged) {
        this.panel = panel;
        this.stream = stream;
        this.onViewportChanged = onViewportChanged;
    }

    boolean pan(float dx, float dy, float anchorX, float anchorY) {
        if (!(panel.getParent() instanceof View) || !(stream.getParent() instanceof View)) return false;
        View parent = (View) panel.getParent();
        View viewport = (View) stream.getParent();
        if (stream.getWidth() < 2 || stream.getHeight() < 2) return false;
        // A pinch, layout change or zoom reset supersedes the previous temporary offset.
        if (!changed || externallyChanged()) rememberBaseline();
        source.reset();
        target.reset();
        FloatingMousePanel.toRoot(parent, source);
        FloatingMousePanel.toRoot(viewport, target);
        if (!target.invert(inverse)) return false;
        points[0] = anchorX;
        points[1] = anchorY;
        points[2] = Math.max(0, parent.getWidth() - panel.getWidth()) + anchorX;
        points[3] = Math.max(0, parent.getHeight() - panel.getHeight()) + anchorY;
        source.mapPoints(points);
        inverse.mapPoints(points);
        float x = FloatingMousePanGeometry.clamp(
                stream.getTranslationX() + dx, stream.getLeft(), stream.getWidth(),
                stream.getPivotX(), stream.getScaleX(), points[0], points[2]);
        float y = FloatingMousePanGeometry.clamp(
                stream.getTranslationY() + dy, stream.getTop(), stream.getHeight(),
                stream.getPivotY(), stream.getScaleY(), points[1], points[3]);
        if (x == stream.getTranslationX() && y == stream.getTranslationY()) return false;
        stream.setTranslationX(x);
        stream.setTranslationY(y);
        appliedX = x;
        appliedY = y;
        changed = true;
        // The panel publishes the new pointer immediately after this transform.
        // Reprojecting the old pointer here would present two positions in one frame.
        return true;
    }

    void restore() {
        if (changed && !externallyChanged()) {
            stream.setTranslationX(originalX);
            stream.setTranslationY(originalY);
            onViewportChanged.run();
        }
        changed = false;
    }

    private boolean externallyChanged() {
        return stream.getTranslationX() != appliedX || stream.getTranslationY() != appliedY
                || stream.getScaleX() != scaleX || stream.getScaleY() != scaleY
                || stream.getWidth() != width || stream.getHeight() != height;
    }

    private void rememberBaseline() {
        originalX = appliedX = stream.getTranslationX();
        originalY = appliedY = stream.getTranslationY();
        scaleX = stream.getScaleX();
        scaleY = stream.getScaleY();
        width = stream.getWidth();
        height = stream.getHeight();
    }
}
