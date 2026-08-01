package com.limelight.utils;

import android.os.Build;

/** Stable device identity strings used in host and virtual-display metadata. */
public final class DeviceUtils {
    private DeviceUtils() {
    }

    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getModel() {
        return Build.MODEL;
    }
}
