package com.limelight.ui.stream;

import android.app.Activity;
import android.os.Looper;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;

import com.limelight.ui.NativeCursorOverlayView;
import com.limelight.ui.StreamView;

import java.util.Objects;

/** Owns the optional native-cursor overlay and its thread confinement. */
public final class AndroidStreamNativeCursorController {
    private final Activity activity;
    private final StreamView streamView;
    private final int encodedWidth;
    private final int encodedHeight;
    private final NativeCursorOverlayView overlayView;
    private boolean destroyed;

    public AndroidStreamNativeCursorController(
            Activity activity,
            StreamView streamView,
            ViewParent streamParent,
            boolean enabled,
            int encodedWidth,
            int encodedHeight) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.streamView = Objects.requireNonNull(
                streamView,
                "streamView");
        this.encodedWidth = encodedWidth;
        this.encodedHeight = encodedHeight;
        if (!enabled || !(streamParent instanceof FrameLayout)) {
            overlayView = null;
            return;
        }

        FrameLayout parent = (FrameLayout) streamParent;
        overlayView = new NativeCursorOverlayView(activity);
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        parent.addView(overlayView, layoutParams);
    }

    @AnyThread
    public void updatePositionFromReference(
            short x,
            short y,
            short referenceWidth,
            short referenceHeight) {
        if (destroyed || overlayView == null ||
                referenceWidth <= 1 || referenceHeight <= 1) {
            return;
        }
        Runnable update = () -> {
            if (!destroyed) {
                overlayView.setCursorPositionFromReference(
                        streamView,
                        x,
                        y,
                        referenceWidth,
                        referenceHeight);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            update.run();
        }
        else {
            activity.runOnUiThread(update);
        }
    }

    @MainThread
    public void onNativeCursor(
            boolean visible,
            boolean shapeChanged,
            int format,
            int width,
            int height,
            int hotspotX,
            int hotspotY,
            int shapeId,
            int scaleX,
            int scaleY,
            byte[] imageData) {
        if (destroyed || overlayView == null) {
            return;
        }
        overlayView.setCursorScaleFromStream(
                streamView,
                encodedWidth,
                encodedHeight,
                scaleX,
                scaleY);
        overlayView.updateCursor(
                visible,
                shapeChanged,
                format,
                width,
                height,
                hotspotX,
                hotspotY,
                shapeId,
                imageData);
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (overlayView != null) {
            overlayView.clearCursor();
        }
    }
}
