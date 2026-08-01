package com.limelight.settings.android;

import android.content.Context;

import java.util.Objects;

import com.limelight.platform.AndroidDeviceCategory;

/**
 * Resolves Android device-specific defaults for application presentation.
 */
public final class AndroidAppPresentationDefaults {
    private static final int SMALL_ICON_MAX_WIDTH_DP = 499;

    private AndroidAppPresentationDefaults() {
    }

    public static boolean shouldUseSmallAppIcons(Context context) {
        Objects.requireNonNull(context, "context");
        if (AndroidDeviceCategory.isTelevision(context)) {
            return false;
        }

        return context.getResources()
                .getConfiguration()
                .smallestScreenWidthDp <=
                SMALL_ICON_MAX_WIDTH_DP;
    }
}
