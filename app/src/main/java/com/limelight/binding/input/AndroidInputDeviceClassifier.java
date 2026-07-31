package com.limelight.binding.input;

import android.os.Build;
import android.view.InputDevice;

import com.limelight.LimeLog;

/** Applies controller overrides around Android's external-device API. */
final class AndroidInputDeviceClassifier {
    private AndroidInputDeviceClassifier() {
    }

    static boolean isExternal(InputDevice device) {
        ControllerExternalDevicePolicy.Resolution resolution =
                ControllerExternalDevicePolicy.resolve(
                        Build.MODEL,
                        device.getName());
        switch (resolution) {
            case FORCE_EXTERNAL:
                return true;
            case FORCE_INTERNAL:
                LimeLog.info(
                        device.getName() +
                                " is internal by hardcoded mapping");
                return false;
            case USE_PLATFORM_VALUE:
                return readPlatformValue(device);
            default:
                throw new AssertionError(
                        "Unhandled external-device resolution: " +
                                resolution);
        }
    }

    private static boolean readPlatformValue(InputDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return device.isExternal();
        }
        try {
            return (Boolean) device.getClass()
                    .getMethod("isExternal")
                    .invoke(device);
        }
        catch (ReflectiveOperationException |
                ClassCastException ignored) {
            // Unknown legacy devices are treated as external so their Back
            // button cannot unexpectedly navigate out of the stream.
            return true;
        }
    }
}
