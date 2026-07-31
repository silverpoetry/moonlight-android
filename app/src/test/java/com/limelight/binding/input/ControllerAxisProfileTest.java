package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerAxisProfileTest {
    @Test
    public void commonControllerUsesDedicatedTriggersAndZRightStick() {
        ControllerAxisProfile profile =
                resolve(
                        0x045e,
                        false,
                        capabilities()
                                .xAndY(true)
                                .leftTriggerAndRightTrigger(true)
                                .zAndRz(true)
                                .build());

        assertTrue(profile.hasLeftStick());
        assertAxes(
                profile,
                ControllerAxisProfile.Axis.Z,
                ControllerAxisProfile.Axis.RZ,
                ControllerAxisProfile.Axis.LEFT_TRIGGER,
                ControllerAxisProfile.Axis.RIGHT_TRIGGER);
        assertFalse(profile.areTriggersIdleNegative());
    }

    @Test
    public void brakeGasPrecedesBrakeThrottle() {
        ControllerAxisProfile profile =
                resolve(
                        0,
                        false,
                        capabilities()
                                .brakeAndGas(true)
                                .brakeAndThrottle(true)
                                .build());

        assertAxes(
                profile,
                ControllerAxisProfile.Axis.NONE,
                ControllerAxisProfile.Axis.NONE,
                ControllerAxisProfile.Axis.BRAKE,
                ControllerAxisProfile.Axis.GAS);
    }

    @Test
    public void xiaomiStyleBrakeThrottlePairIsPreserved() {
        ControllerAxisProfile profile =
                resolve(
                        0,
                        false,
                        capabilities()
                                .brakeAndThrottle(true)
                                .rxAndRy(true)
                                .build());

        assertAxes(
                profile,
                ControllerAxisProfile.Axis.RX,
                ControllerAxisProfile.Axis.RY,
                ControllerAxisProfile.Axis.BRAKE,
                ControllerAxisProfile.Axis.THROTTLE);
    }

    @Test
    public void oldDualShockUsesRxRyTriggersAndZRightStick() {
        ControllerAxisProfile profile =
                resolve(
                        0x054c,
                        true,
                        capabilities()
                                .rxAndRy(true)
                                .zAndRz(true)
                                .build());

        assertTrue(profile.isNonStandardDualShock4());
        assertFalse(profile.hasLinuxStandardFaceButtons());
        assertTrue(profile.areTriggersIdleNegative());
        assertAxes(
                profile,
                ControllerAxisProfile.Axis.Z,
                ControllerAxisProfile.Axis.RZ,
                ControllerAxisProfile.Axis.RX,
                ControllerAxisProfile.Axis.RY);
    }

    @Test
    public void linuxDualShockUsesRxRyStickAndZTriggers() {
        ControllerAxisProfile profile =
                resolve(
                        0x054c,
                        false,
                        capabilities()
                                .rxAndRy(true)
                                .zAndRz(true)
                                .build());

        assertFalse(profile.isNonStandardDualShock4());
        assertTrue(profile.hasLinuxStandardFaceButtons());
        assertAxes(
                profile,
                ControllerAxisProfile.Axis.RX,
                ControllerAxisProfile.Axis.RY,
                ControllerAxisProfile.Axis.Z,
                ControllerAxisProfile.Axis.RZ);
    }

    @Test
    public void unnamedSonyDeviceDoesNotApplyDualShockHeuristic() {
        ControllerAxisProfile profile =
                ControllerAxisProfile.resolve(
                        0x054c,
                        false,
                        true,
                        capabilities()
                                .rxAndRy(true)
                                .zAndRz(true)
                                .build());

        assertFalse(profile.isNonStandardDualShock4());
        assertFalse(profile.hasLinuxStandardFaceButtons());
        assertAxes(
                profile,
                ControllerAxisProfile.Axis.Z,
                ControllerAxisProfile.Axis.RZ,
                ControllerAxisProfile.Axis.NONE,
                ControllerAxisProfile.Axis.NONE);
    }

    @Test
    public void hatsRequireBothAxes() {
        ControllerAxisProfile withoutHat =
                resolve(0, false, capabilities().build());
        ControllerAxisProfile withHat =
                resolve(
                        0,
                        false,
                        capabilities()
                                .hatXAndHatY(true)
                                .build());

        assertEquals(
                ControllerAxisProfile.Axis.NONE,
                withoutHat.getHatX());
        assertEquals(
                ControllerAxisProfile.Axis.NONE,
                withoutHat.getHatY());
        assertEquals(
                ControllerAxisProfile.Axis.HAT_X,
                withHat.getHatX());
        assertEquals(
                ControllerAxisProfile.Axis.HAT_Y,
                withHat.getHatY());
    }

    private static ControllerAxisProfile resolve(
            int vendorId,
            boolean hasButtonC,
            ControllerAxisProfile.Capabilities capabilities) {
        return ControllerAxisProfile.resolve(
                vendorId,
                true,
                hasButtonC,
                capabilities);
    }

    private static ControllerAxisProfile.Capabilities.Builder capabilities() {
        return ControllerAxisProfile.Capabilities.builder();
    }

    private static void assertAxes(
            ControllerAxisProfile profile,
            ControllerAxisProfile.Axis rightStickX,
            ControllerAxisProfile.Axis rightStickY,
            ControllerAxisProfile.Axis leftTrigger,
            ControllerAxisProfile.Axis rightTrigger) {
        assertEquals(rightStickX, profile.getRightStickX());
        assertEquals(rightStickY, profile.getRightStickY());
        assertEquals(leftTrigger, profile.getLeftTrigger());
        assertEquals(rightTrigger, profile.getRightTrigger());
    }
}
