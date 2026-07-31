package com.limelight.binding.input;

import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.limelight.nvstream.jni.MoonBridge;

import java.util.Map;
import java.util.Objects;

/** Samples Android controller capabilities for one host arrival report. */
final class AndroidControllerArrivalProbe {
    interface ShieldDeviceRecognizer {
        boolean isRecognized(InputDevice device);
    }

    private final ControllerTypeResolver.Fallback typeFallback;
    private final ShieldDeviceRecognizer shieldDeviceRecognizer;

    AndroidControllerArrivalProbe(
            ControllerTypeResolver.Fallback typeFallback,
            ShieldDeviceRecognizer shieldDeviceRecognizer) {
        this.typeFallback = Objects.requireNonNull(
                typeFallback,
                "typeFallback");
        this.shieldDeviceRecognizer = Objects.requireNonNull(
                shieldDeviceRecognizer,
                "shieldDeviceRecognizer");
    }

    ControllerArrivalReport probe(
            InputDevice device,
            RuntimeCapabilities runtimeCapabilities) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(
                runtimeCapabilities,
                "runtimeCapabilities");

        byte type = ControllerTypeResolver.resolve(
                device.getVendorId(),
                device.getProductId(),
                typeFallback);
        int supportedButtonFlags = 0;
        for (Map.Entry<Integer, Integer> entry :
                ControllerButtonMapper
                        .getProtocolButtonMappings()
                        .entrySet()) {
            if (device.hasKeys(entry.getKey())[0]) {
                supportedButtonFlags |= entry.getValue();
            }
        }

        boolean hasTouchpad =
                (device.getSources() & InputDevice.SOURCE_TOUCHPAD) ==
                        InputDevice.SOURCE_TOUCHPAD;
        return ControllerArrivalReport.builder(
                        type,
                        supportedButtonFlags)
                .hasPaddles(runtimeCapabilities.hasPaddles)
                .hasShareButton(
                        runtimeCapabilities.hasShareButton)
                .hasHorizontalHatAxis(
                        hasJoystickAxis(
                                device,
                                MotionEvent.AXIS_HAT_X))
                .hasVerticalHatAxis(
                        hasJoystickAxis(
                                device,
                                MotionEvent.AXIS_HAT_Y))
                .hasAdvancedInputDeviceApis(
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.S)
                .hasQuadVibrators(
                        runtimeCapabilities.hasQuadVibrators)
                .hasVibratorManager(
                        runtimeCapabilities.hasVibratorManager)
                .hasLegacyVibrator(
                        runtimeCapabilities.hasLegacyVibrator)
                .external(runtimeCapabilities.external)
                .hasRgbLed(runtimeCapabilities.hasRgbLed)
                .hasReliableRgbLedDetection(
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                .hasAnalogTriggers(
                        runtimeCapabilities.hasAnalogTriggers)
                .hasAccelerometer(
                        runtimeCapabilities.hasAccelerometer)
                .hasGyroscope(runtimeCapabilities.hasGyroscope)
                .requiresGenericMotionControllerType(
                        type != MoonBridge.LI_CTYPE_PS &&
                                runtimeCapabilities.hasMotionManager)
                .recognizedByShieldExtensions(
                        shieldDeviceRecognizer.isRecognized(device))
                .hasTouchpad(hasTouchpad)
                .hasClickpad(
                        hasTouchpad &&
                                hasButtonUnderTouchpad(device, type))
                .build();
    }

    private static boolean hasJoystickAxis(
            InputDevice device,
            int axis) {
        return device.getMotionRange(
                        axis,
                        InputDevice.SOURCE_JOYSTICK) != null ||
                device.getMotionRange(
                        axis,
                        InputDevice.SOURCE_GAMEPAD) != null;
    }

    private static boolean hasButtonUnderTouchpad(
            InputDevice device,
            byte type) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            try {
                return (Boolean) device.getClass()
                        .getMethod("hasButtonUnderPad")
                        .invoke(device);
            }
            catch (ReflectiveOperationException |
                    ClassCastException ignored) {
                // The hidden API is optional. Fall through to type inference.
            }
        }

        return type == MoonBridge.LI_CTYPE_PS;
    }

    static final class RuntimeCapabilities {
        private final boolean hasPaddles;
        private final boolean hasShareButton;
        private final boolean hasQuadVibrators;
        private final boolean hasVibratorManager;
        private final boolean hasLegacyVibrator;
        private final boolean external;
        private final boolean hasRgbLed;
        private final boolean hasAnalogTriggers;
        private final boolean hasAccelerometer;
        private final boolean hasGyroscope;
        private final boolean hasMotionManager;

        private RuntimeCapabilities(Builder builder) {
            hasPaddles = builder.hasPaddles;
            hasShareButton = builder.hasShareButton;
            hasQuadVibrators = builder.hasQuadVibrators;
            hasVibratorManager = builder.hasVibratorManager;
            hasLegacyVibrator = builder.hasLegacyVibrator;
            external = builder.external;
            hasRgbLed = builder.hasRgbLed;
            hasAnalogTriggers = builder.hasAnalogTriggers;
            hasAccelerometer = builder.hasAccelerometer;
            hasGyroscope = builder.hasGyroscope;
            hasMotionManager = builder.hasMotionManager;
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private boolean hasPaddles;
            private boolean hasShareButton;
            private boolean hasQuadVibrators;
            private boolean hasVibratorManager;
            private boolean hasLegacyVibrator;
            private boolean external;
            private boolean hasRgbLed;
            private boolean hasAnalogTriggers;
            private boolean hasAccelerometer;
            private boolean hasGyroscope;
            private boolean hasMotionManager;

            Builder hasPaddles(boolean value) {
                hasPaddles = value;
                return this;
            }

            Builder hasShareButton(boolean value) {
                hasShareButton = value;
                return this;
            }

            Builder hasQuadVibrators(boolean value) {
                hasQuadVibrators = value;
                return this;
            }

            Builder hasVibratorManager(boolean value) {
                hasVibratorManager = value;
                return this;
            }

            Builder hasLegacyVibrator(boolean value) {
                hasLegacyVibrator = value;
                return this;
            }

            Builder external(boolean value) {
                external = value;
                return this;
            }

            Builder hasRgbLed(boolean value) {
                hasRgbLed = value;
                return this;
            }

            Builder hasAnalogTriggers(boolean value) {
                hasAnalogTriggers = value;
                return this;
            }

            Builder hasAccelerometer(boolean value) {
                hasAccelerometer = value;
                return this;
            }

            Builder hasGyroscope(boolean value) {
                hasGyroscope = value;
                return this;
            }

            Builder hasMotionManager(boolean value) {
                hasMotionManager = value;
                return this;
            }

            RuntimeCapabilities build() {
                return new RuntimeCapabilities(this);
            }
        }
    }
}
