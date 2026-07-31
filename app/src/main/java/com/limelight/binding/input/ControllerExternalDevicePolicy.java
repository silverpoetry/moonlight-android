package com.limelight.binding.input;

import java.util.Objects;

/** Resolves product-specific overrides for Android's external-device flag. */
final class ControllerExternalDevicePolicy {
    enum Resolution {
        FORCE_EXTERNAL,
        FORCE_INTERNAL,
        USE_PLATFORM_VALUE
    }

    private ControllerExternalDevicePolicy() {
    }

    static Resolution resolve(
            String deviceModel,
            String deviceName) {
        Objects.requireNonNull(deviceModel, "deviceModel");
        Objects.requireNonNull(deviceName, "deviceName");

        if (deviceModel.equals("Tinker Board")) {
            return Resolution.FORCE_EXTERNAL;
        }
        if (deviceName.contains("gpio") ||
                deviceName.contains("joy_key") ||
                deviceName.contains("keypad") ||
                deviceName.equalsIgnoreCase(
                        "NVIDIA Corporation NVIDIA Controller v01.01") ||
                deviceName.equalsIgnoreCase(
                        "NVIDIA Corporation NVIDIA Controller v01.02") ||
                deviceName.equalsIgnoreCase("GR0006")) {
            return Resolution.FORCE_INTERNAL;
        }
        return Resolution.USE_PLATFORM_VALUE;
    }
}
