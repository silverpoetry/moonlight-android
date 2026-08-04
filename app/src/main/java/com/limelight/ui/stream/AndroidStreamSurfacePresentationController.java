package com.limelight.ui.stream;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.MainThread;

import java.util.Objects;

/**
 * Controls when the decoder surface becomes part of the visible stream UI.
 *
 * <p>A {@link android.view.SurfaceView} owns a separately composed surface.
 * Some platform compositors continue to show its initial black buffer when
 * the Android view alpha is zero. Parking the surface outside the display
 * keeps its lifecycle intact for decoder startup without covering the
 * translucent launcher window below it.</p>
 */
public final class AndroidStreamSurfacePresentationController {
    private final View surfaceView;
    private final float parkedTranslationX;

    public AndroidStreamSurfacePresentationController(
            View surfaceView,
            Resources resources) {
        this.surfaceView = Objects.requireNonNull(
                surfaceView,
                "surfaceView");
        DisplayMetrics metrics = Objects.requireNonNull(
                resources,
                "resources").getDisplayMetrics();
        parkedTranslationX = -2f * Math.max(
                metrics.widthPixels,
                metrics.heightPixels);
    }

    /** Keeps the render Surface alive but outside the visible display. */
    @MainThread
    public void parkForConnection() {
        surfaceView.setAlpha(0f);
        surfaceView.setTranslationX(parkedTranslationX);
    }

    /** Restores the decoder surface to its normal stream position. */
    @MainThread
    public void revealConnectedStream() {
        surfaceView.setTranslationX(0f);
        surfaceView.setAlpha(1f);
    }
}
