package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerAssignmentPolicyTest {
    @Test
    public void internalInputAlwaysUsesPlayerOne() {
        assertEquals(
                ControllerAssignmentPolicy.Strategy.FIXED_PLAYER_ONE,
                ControllerAssignmentPolicy.forInputDevice(
                        false,
                        true,
                        true));
        assertEquals(
                ControllerAssignmentPolicy.Strategy.FIXED_PLAYER_ONE,
                ControllerAssignmentPolicy.forInputDevice(
                        false,
                        false,
                        false));
    }

    @Test
    public void externalJoystickReservesOnlyInMultiControllerMode() {
        assertEquals(
                ControllerAssignmentPolicy.Strategy.RESERVE_NEXT,
                ControllerAssignmentPolicy.forInputDevice(
                        true,
                        true,
                        true));
        assertEquals(
                ControllerAssignmentPolicy.Strategy.FIXED_PLAYER_ONE,
                ControllerAssignmentPolicy.forInputDevice(
                        true,
                        true,
                        false));
    }

    @Test
    public void externalAuxiliaryInputAlwaysSearchesForItsJoystick() {
        assertEquals(
                ControllerAssignmentPolicy.Strategy.FIND_ASSOCIATED_JOYSTICK,
                ControllerAssignmentPolicy.forInputDevice(
                        true,
                        false,
                        true));
        assertEquals(
                ControllerAssignmentPolicy.Strategy.FIND_ASSOCIATED_JOYSTICK,
                ControllerAssignmentPolicy.forInputDevice(
                        true,
                        false,
                        false));
    }

    @Test
    public void usbControllerReservesOnlyInMultiControllerMode() {
        assertEquals(
                ControllerAssignmentPolicy.Strategy.RESERVE_NEXT,
                ControllerAssignmentPolicy.forUsbController(true));
        assertEquals(
                ControllerAssignmentPolicy.Strategy.FIXED_PLAYER_ONE,
                ControllerAssignmentPolicy.forUsbController(false));
    }
}
