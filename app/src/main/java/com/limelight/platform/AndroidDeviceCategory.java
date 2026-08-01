package com.limelight.platform;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

import java.util.Objects;

/** Android form-factor classification used by settings and stream policy. */
public final class AndroidDeviceCategory {
    private AndroidDeviceCategory() {
    }

    public static boolean isTelevision(Context context) {
        Objects.requireNonNull(context, "context");
        UiModeManager uiModeManager = (UiModeManager)
                context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null &&
                uiModeManager.getCurrentModeType() ==
                        Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        PackageManager packageManager = context.getPackageManager();
        return packageManager != null &&
                packageManager.hasSystemFeature(
                        PackageManager.FEATURE_LEANBACK);
    }
}
