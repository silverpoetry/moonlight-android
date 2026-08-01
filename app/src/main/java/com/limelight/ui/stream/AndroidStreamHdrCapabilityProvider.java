package com.limelight.ui.stream;

import android.app.Activity;
import android.os.Build;
import android.view.Display;

import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;

import com.limelight.settings.android.AndroidHdrCompatibility;
import com.limelight.platform.AndroidDisplayCompat;

import java.util.Objects;

/** Samples Android HDR capabilities once at stream startup. */
public final class AndroidStreamHdrCapabilityProvider {
    private AndroidStreamHdrCapabilityProvider() {
    }

    @MainThread
    public static StreamHdrRequestPolicy.DeviceCapabilities sample(
            Activity activity) {
        Objects.requireNonNull(activity, "activity");

        boolean canQueryHdrCapabilities =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        boolean displaySupportsHdr10 =
                canQueryHdrCapabilities && supportsHdr10(activity);
        return new StreamHdrRequestPolicy.DeviceCapabilities(
                canQueryHdrCapabilities,
                AndroidHdrCompatibility.isHdrStreamingAllowed(),
                displaySupportsHdr10);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private static boolean supportsHdr10(Activity activity) {
        Display display = AndroidDisplayCompat.getActivityDisplay(
                activity);
        int[] supportedHdrTypes =
                AndroidDisplayCompat.getSupportedHdrTypes(display);
        for (int hdrType : supportedHdrTypes) {
            if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                return true;
            }
        }
        return false;
    }
}
