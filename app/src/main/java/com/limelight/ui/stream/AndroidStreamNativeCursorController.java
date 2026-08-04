package com.limelight.ui.stream;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;

import com.limelight.ui.NativeCursorOverlayView;
import com.limelight.ui.StreamView;

import java.util.Objects;

/** Owns the optional native-cursor overlay and its thread confinement. */
public final class AndroidStreamNativeCursorController {
    private static final String LOG_TAG = "MoonlightCursor";
    private final Activity activity;
    private final StreamView streamView;
    private final int encodedWidth;
    private final int encodedHeight;
    private final NativeCursorOverlayView overlayView;
    private final View.OnLayoutChangeListener layoutChangeListener;
    private boolean enabled;
    private boolean streamPresented;
    private boolean initialPositionMapped;
    private int captureScaleX = 1 << 16;
    private int captureScaleY = 1 << 16;
    private volatile boolean destroyed;
    private volatile CursorPosition cursorPosition;

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
        this.enabled = enabled;
        if (!(streamParent instanceof FrameLayout)) {
            overlayView = null;
            layoutChangeListener = null;
            trace("Cursor overlay unavailable: stream parent is not a FrameLayout");
            return;
        }

        FrameLayout parent = (FrameLayout) streamParent;
        overlayView = new NativeCursorOverlayView(activity);
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        parent.addView(overlayView, layoutParams);
        overlayView.setVisibility(enabled
                ? NativeCursorOverlayView.VISIBLE
                : NativeCursorOverlayView.INVISIBLE);
        layoutChangeListener = (view, left, top, right, bottom,
                                oldLeft, oldTop, oldRight, oldBottom) ->
                refreshOverlayGeometry();
        streamView.addOnLayoutChangeListener(layoutChangeListener);
        trace("Cursor overlay created; enabled=" + enabled);
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
        cursorPosition = new CursorPosition(
                x,
                y,
                referenceWidth,
                referenceHeight);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            applyCachedPosition();
        }
        else {
            activity.runOnUiThread(this::applyCachedPosition);
        }
    }

    /** Makes cached local cursor coordinates eligible for visual presentation. */
    @MainThread
    public void onStreamPresented() {
        if (destroyed) {
            return;
        }
        streamPresented = true;
        trace("Stream presented; cached position=" + formatPosition(cursorPosition));
        refreshOverlayGeometry();
        // When Windows' secure desktop is active, Sunshine cannot probe any cursor state.
        // Give the local, predicted pointer a visible shape until a host state is available.
        overlayView.showFallbackCursorIfNativeStateUnavailable();
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
        trace("Native cursor update: visible=" + visible +
                ", shapeChanged=" + shapeChanged +
                ", format=" + format +
                ", size=" + width + "x" + height +
                ", captureScale=" + scaleX + "x" + scaleY +
                ", cachedPosition=" + formatPosition(cursorPosition));
        captureScaleX = scaleX;
        captureScaleY = scaleY;
        overlayView.bringToFront();
        refreshCursorScale();
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

    /** Enables the negotiated native-cursor overlay without rebuilding it. */
    @MainThread
    public void setEnabled(boolean enabled) {
        if (destroyed || overlayView == null || this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        trace("Cursor overlay enabled=" + enabled);
        overlayView.setVisibility(enabled
                ? NativeCursorOverlayView.VISIBLE
                : NativeCursorOverlayView.INVISIBLE);
        if (enabled) {
            refreshOverlayGeometry();
        }
    }

    @MainThread
    private void refreshOverlayGeometry() {
        refreshCursorScale();
        applyCachedPosition();
    }

    @MainThread
    private void refreshCursorScale() {
        if (destroyed || overlayView == null) {
            return;
        }
        overlayView.setCursorScaleFromStream(
                streamView,
                encodedWidth,
                encodedHeight,
                captureScaleX,
                captureScaleY);
    }

    @MainThread
    private void applyCachedPosition() {
        CursorPosition position = cursorPosition;
        if (destroyed || !streamPresented || !enabled ||
                overlayView == null || position == null) {
            return;
        }
        overlayView.bringToFront();
        boolean mapped = overlayView.setCursorPositionFromReference(
                streamView,
                position.x,
                position.y,
                position.referenceWidth,
                position.referenceHeight);
        if (!initialPositionMapped || !mapped) {
            trace("Cursor position map=" + mapped +
                    ", source=" + formatPosition(position) +
                    ", stream=" + streamView.getWidth() + "x" +
                    streamView.getHeight());
        }
        initialPositionMapped |= mapped;
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (layoutChangeListener != null) {
            streamView.removeOnLayoutChangeListener(layoutChangeListener);
        }
        if (overlayView != null) {
            overlayView.clearCursor();
            if (overlayView.getParent() instanceof FrameLayout) {
                ((FrameLayout) overlayView.getParent())
                        .removeView(overlayView);
            }
        }
    }

    private static final class CursorPosition {
        private final int x;
        private final int y;
        private final int referenceWidth;
        private final int referenceHeight;

        private CursorPosition(
                int x,
                int y,
                int referenceWidth,
                int referenceHeight) {
            this.x = x;
            this.y = y;
            this.referenceWidth = referenceWidth;
            this.referenceHeight = referenceHeight;
        }
    }

    private static String formatPosition(CursorPosition position) {
        if (position == null) {
            return "none";
        }
        return position.x + "," + position.y + "/" +
                position.referenceWidth + "x" + position.referenceHeight;
    }

    private static void trace(String message) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, message);
        }
    }
}
