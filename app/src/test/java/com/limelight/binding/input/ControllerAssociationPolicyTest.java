package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerAssociationPolicyTest {
    private static final ControllerAssociationPolicy.DeviceFacts ORIGINAL =
            facts("Wireless Controller Touchpad", "descriptor", false);

    @Test
    public void absentCandidateDoesNotMatch() {
        assertFalse(
                ControllerAssociationPolicy.isAssociatedJoystick(
                        ORIGINAL,
                        null));
    }

    @Test
    public void nonJoystickCandidateDoesNotMatch() {
        assertFalse(
                ControllerAssociationPolicy.isAssociatedJoystick(
                        ORIGINAL,
                        facts(
                                "Wireless Controller",
                                "descriptor",
                                false)));
    }

    @Test
    public void equalNameDoesNotMatchAnotherControllerInstance() {
        assertFalse(
                ControllerAssociationPolicy.isAssociatedJoystick(
                        ORIGINAL,
                        facts(
                                "Wireless Controller Touchpad",
                                "descriptor",
                                true)));
    }

    @Test
    public void differentDescriptorDoesNotMatch() {
        assertFalse(
                ControllerAssociationPolicy.isAssociatedJoystick(
                        ORIGINAL,
                        facts(
                                "Wireless Controller",
                                "other-descriptor",
                                true)));
    }

    @Test
    public void joystickWithDifferentNameAndSameDescriptorMatches() {
        assertTrue(
                ControllerAssociationPolicy.isAssociatedJoystick(
                        ORIGINAL,
                        facts(
                                "Wireless Controller",
                                "descriptor",
                                true)));
    }

    private static ControllerAssociationPolicy.DeviceFacts facts(
            String name,
            String descriptor,
            boolean joystick) {
        return new ControllerAssociationPolicy.DeviceFacts(
                name,
                descriptor,
                joystick);
    }
}
