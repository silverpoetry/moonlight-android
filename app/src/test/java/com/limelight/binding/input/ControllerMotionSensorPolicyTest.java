package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerMotionSensorPolicyTest {
    @Test
    public void disabledSettingNeverProbesDeviceSensors() {
        assertFalse(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                35,
                                0x054c,
                                false));
    }

    @Test
    public void preAndroidTwelveNeverProbesDeviceSensors() {
        assertFalse(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                30,
                                0x054c,
                                true));
    }

    @Test
    public void androidTwelveAllowsOnlyKnownMotionControllerVendors() {
        assertTrue(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                31,
                                0x054c,
                                true));
        assertTrue(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                31,
                                0x057e,
                                true));
        assertFalse(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                31,
                                0x045e,
                                true));
    }

    @Test
    public void androidTwelveLRetainsOriginalNoProbeBehavior() {
        assertFalse(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                32,
                                0x054c,
                                true));
    }

    @Test
    public void androidThirteenAndLaterAllowAllVendors() {
        assertTrue(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                33,
                                0x045e,
                                true));
        assertTrue(
                ControllerMotionSensorPolicy
                        .shouldProbeInputDeviceSensors(
                                35,
                                0,
                                true));
    }
}
