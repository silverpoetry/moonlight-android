package com.limelight.binding.input;

/** Decides whether Android input-device motion APIs may be probed safely. */
final class ControllerMotionSensorPolicy {
    private static final int ANDROID_S = 31;
    private static final int ANDROID_TIRAMISU = 33;
    private static final int SONY_VENDOR_ID = 0x054c;
    private static final int NINTENDO_VENDOR_ID = 0x057e;

    private ControllerMotionSensorPolicy() {
    }

    static boolean shouldProbeInputDeviceSensors(
            int sdkInt,
            int vendorId,
            boolean motionSensorsEnabled) {
        if (!motionSensorsEnabled) {
            return false;
        }
        if (sdkInt >= ANDROID_TIRAMISU) {
            return true;
        }
        return sdkInt == ANDROID_S &&
                (vendorId == SONY_VENDOR_ID ||
                        vendorId == NINTENDO_VENDOR_ID);
    }
}
