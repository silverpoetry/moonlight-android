package com.limelight.settings.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Objects;

/**
 * Resolves Android device-specific defaults for application presentation.
 */
public final class AndroidAppPresentationDefaults {
    private static final int SMALL_ICON_MAX_WIDTH_DP = 499;

    private AndroidAppPresentationDefaults() {
    }

    public static boolean shouldUseSmallAppIcons(Context context) {
        Objects.requireNonNull(context, "context");
        PackageManager packageManager =
                context.getPackageManager();
        if (packageManager != null &&
                (packageManager.hasSystemFeature(
                        PackageManager.FEATURE_TELEVISION) ||
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.LOLLIPOP_MR1 &&
                                packageManager.hasSystemFeature(
                                        PackageManager
                                                .FEATURE_LEANBACK))) {
            return false;
        }

        return context.getResources()
                .getConfiguration()
                .smallestScreenWidthDp <=
                SMALL_ICON_MAX_WIDTH_DP;
    }
}
