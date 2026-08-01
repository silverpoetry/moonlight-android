package com.limelight.platform;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * Centralizes display API level differences without leaking deprecated
 * platform calls into display policy and rendering code.
 */
public final class AndroidDisplayCompat {
    private AndroidDisplayCompat() {
    }

    /** Returns the display that currently owns the Activity window. */
    public static Display getActivityDisplay(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Display display = activity.getDisplay();
            if (display != null) {
                return display;
            }
        }
        return getLegacyActivityDisplay(activity);
    }

    /** Returns the physical size of the current display mode. */
    public static Point getPhysicalDisplaySize(Context context) {
        Objects.requireNonNull(context, "context");
        Display display = context instanceof Activity
                ? getActivityDisplay((Activity) context)
                : getDefaultDisplay(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = display.getMode();
            return new Point(
                    mode.getPhysicalWidth(),
                    mode.getPhysicalHeight());
        }
        return getLegacyRealSize(display);
    }

    /** Returns the drawable size of the Activity window. */
    public static Point getWindowSize(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getCurrentWindowSize(activity);
        }
        return getLegacyWindowSize(getActivityDisplay(activity));
    }

    /**
     * Returns HDR types supported by the active mode. Android 14 moved this
     * capability from {@link Display.HdrCapabilities} to {@link Display.Mode}.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public static int[] getSupportedHdrTypes(Display display) {
        Objects.requireNonNull(display, "display");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            int[] types = display.getMode().getSupportedHdrTypes();
            return types == null ? new int[0] : types;
        }
        return getLegacySupportedHdrTypes(display);
    }

    private static Display getDefaultDisplay(Context context) {
        DisplayManager displayManager = (DisplayManager)
                context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            throw new IllegalStateException(
                    "Display service is unavailable");
        }
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            throw new IllegalStateException(
                    "Default display is unavailable");
        }
        return display;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static Point getCurrentWindowSize(Activity activity) {
        Rect bounds = activity.getWindowManager()
                .getCurrentWindowMetrics()
                .getBounds();
        return new Point(bounds.width(), bounds.height());
    }

    @SuppressWarnings("deprecation")
    private static Display getLegacyActivityDisplay(Activity activity) {
        return activity.getWindowManager().getDefaultDisplay();
    }

    @SuppressWarnings("deprecation")
    private static Point getLegacyRealSize(Display display) {
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    @SuppressWarnings("deprecation")
    private static Point getLegacyWindowSize(Display display) {
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressWarnings("deprecation")
    private static int[] getLegacySupportedHdrTypes(Display display) {
        Display.HdrCapabilities capabilities = display.getHdrCapabilities();
        if (capabilities == null) {
            return new int[0];
        }
        int[] types = capabilities.getSupportedHdrTypes();
        return types == null ? new int[0] : types;
    }
}
