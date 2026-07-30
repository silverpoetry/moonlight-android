package com.limelight.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;

import com.limelight.preferences.PreferenceConfiguration;

/**
 * Applies compact-screen orientation preferences while keeping adaptive windows free.
 */
public final class StreamOrientationController {
    private static final int ADAPTIVE_SMALLEST_WIDTH_DP = 600;

    private StreamOrientationController() {
    }

    public static void applyGameOrientation(
            Activity activity,
            PreferenceConfiguration preferences,
            boolean temporaryPortraitRequest) {
        Configuration configuration =
                activity.getResources().getConfiguration();
        StreamOrientationPolicy.Mode mode =
                StreamOrientationPolicy.resolveGameMode(
                        isAdaptiveWindow(activity, configuration),
                        configuration.screenWidthDp,
                        configuration.screenHeightDp,
                        preferences.onscreenController,
                        preferences.isNativeResolution(),
                        preferences.width,
                        preferences.height,
                        preferences.enablePortrait ||
                                temporaryPortraitRequest,
                        preferences.autoScreenOrientation);
        apply(activity, mode);
    }

    public static void applySbsOrientation(Activity activity) {
        Configuration configuration =
                activity.getResources().getConfiguration();
        apply(activity, StreamOrientationPolicy.resolveSbsMode(
                isAdaptiveWindow(activity, configuration)));
    }

    private static boolean isAdaptiveWindow(
            Activity activity, Configuration configuration) {
        if (configuration.smallestScreenWidthDp >=
                ADAPTIVE_SMALLEST_WIDTH_DP) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                activity.isInMultiWindowMode()) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.isInPictureInPictureMode();
    }

    /**
     * Fixed requests are limited to compact, full-screen windows. Large, freeform,
     * split-screen, and picture-in-picture windows are explicitly released to the
     * user's orientation preference.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private static void apply(
            Activity activity, StreamOrientationPolicy.Mode mode) {
        switch (mode) {
            case FOLLOW_USER:
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
            case FOLLOW_USER_ALL_ROTATIONS:
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                break;
            case USER_LANDSCAPE:
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                break;
            case USER_PORTRAIT:
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                break;
            case SENSOR_PORTRAIT:
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unhandled orientation mode: " + mode);
        }
    }
}
