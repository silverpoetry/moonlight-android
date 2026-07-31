package com.limelight.binding.input;

import java.util.Objects;

/**
 * Matches an auxiliary input device, such as a controller touchpad, to the
 * joystick device that owns its protocol player slot.
 */
final class ControllerAssociationPolicy {
    private ControllerAssociationPolicy() {
    }

    static boolean isAssociatedJoystick(
            DeviceFacts original,
            DeviceFacts candidate) {
        Objects.requireNonNull(original, "original");
        if (candidate == null || !candidate.joystick) {
            return false;
        }

        // Equal names can identify another instance of the same controller.
        // Split devices instead expose different names with one descriptor.
        return !candidate.name.equals(original.name) &&
                candidate.descriptor.equals(original.descriptor);
    }

    static final class DeviceFacts {
        private final String name;
        private final String descriptor;
        private final boolean joystick;

        DeviceFacts(
                String name,
                String descriptor,
                boolean joystick) {
            this.name = Objects.requireNonNull(name, "name");
            this.descriptor = Objects.requireNonNull(
                    descriptor,
                    "descriptor");
            this.joystick = joystick;
        }
    }
}
