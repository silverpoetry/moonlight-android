package com.limelight.ui.hosts;

import android.app.Activity;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.limelight.LimeLog;
import com.limelight.settings.app.AppPresentationSettings;

import java.io.File;
import java.util.Objects;

/**
 * Renders the optional host/app-list background from one immutable snapshot.
 */
public final class ScreenBackgroundPresenter {
    private static final float BLUR_RADIUS_PX = 25f;

    private ScreenBackgroundPresenter() {
    }

    public static void apply(
            Activity activity,
            ImageView backgroundView,
            AppPresentationSettings settings) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(backgroundView, "backgroundView");
        Objects.requireNonNull(settings, "settings");

        clear(backgroundView);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                !settings.isBackgroundEnabled()) {
            return;
        }

        File imageFile = new File(
                activity.getFilesDir(),
                settings.getBackgroundFile());
        if (!imageFile.isFile()) {
            return;
        }

        try {
            Glide.with(backgroundView)
                    .load(imageFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(backgroundView);
            backgroundView.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    settings.isBackgroundBlurEnabled()) {
                backgroundView.setRenderEffect(
                        RenderEffect.createBlurEffect(
                                BLUR_RADIUS_PX,
                                BLUR_RADIUS_PX,
                                Shader.TileMode.CLAMP));
            }
        }
        catch (RuntimeException error) {
            clear(backgroundView);
            LimeLog.warning(
                    "Unable to render screen background: " +
                            error.getMessage());
        }
    }

    private static void clear(ImageView backgroundView) {
        backgroundView.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            backgroundView.setRenderEffect(null);
        }
    }
}
