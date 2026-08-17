package com.limelight.computers.network;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Objects;

/** Android 17 local-network capability boundary. */
public final class AndroidLocalNetworkAccess {
    public static final String PERMISSION =
            Manifest.permission.ACCESS_LOCAL_NETWORK;

    private AndroidLocalNetworkAccess() {
    }

    public static boolean isGranted(Context context) {
        Objects.requireNonNull(context, "context");
        return !requiresRuntimePermission(Build.VERSION.SDK_INT) ||
                context.checkSelfPermission(PERMISSION) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    static boolean requiresRuntimePermission(int sdkInt) {
        return sdkInt >= Build.VERSION_CODES.CINNAMON_BUN;
    }
}
