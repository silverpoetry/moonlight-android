package com.limelight.settings.android;

import android.os.Build;

/**
 * Android-device compatibility policy that must stay outside the pure typed
 * settings domain.
 */
public final class AndroidHdrCompatibility {
    private static final String BROKEN_SHIELD_FINGERPRINT =
            "PPR1.180610.011/4079208_2235.1395";

    private AndroidHdrCompatibility() {
    }

    public static boolean isHdrStreamingAllowed() {
        return !isKnownBrokenShieldFirmware(
                Build.MANUFACTURER,
                Build.FINGERPRINT);
    }

    static boolean isKnownBrokenShieldFirmware(
            String manufacturer,
            String fingerprint) {
        return manufacturer != null &&
                manufacturer.equalsIgnoreCase("NVIDIA") &&
                fingerprint != null &&
                fingerprint.contains(
                        BROKEN_SHIELD_FINGERPRINT);
    }
}
