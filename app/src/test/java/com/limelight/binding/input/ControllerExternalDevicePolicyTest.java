package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerExternalDevicePolicyTest {
    @Test
    public void tinkerBoardForcesExternalClassification() {
        assertEquals(
                ControllerExternalDevicePolicy.Resolution.FORCE_EXTERNAL,
                ControllerExternalDevicePolicy.resolve(
                        "Tinker Board",
                        "gpio-keys"));
    }

    @Test
    public void embeddedControllerNamesForceInternalClassification() {
        assertForcedInternal("gpio-keys");
        assertForcedInternal("archos-joy_key");
        assertForcedInternal("xperia-keypad");
        assertForcedInternal(
                "NVIDIA Corporation NVIDIA Controller v01.01");
        assertForcedInternal(
                "nvidia corporation nvidia controller V01.02");
        assertForcedInternal("gr0006");
    }

    @Test
    public void ordinaryControllerUsesPlatformClassification() {
        assertEquals(
                ControllerExternalDevicePolicy
                        .Resolution.USE_PLATFORM_VALUE,
                ControllerExternalDevicePolicy.resolve(
                        "Pixel 9",
                        "Xbox Wireless Controller"));
    }

    @Test
    public void hardcodedSubstringRulesRemainCaseSensitive() {
        assertEquals(
                ControllerExternalDevicePolicy
                        .Resolution.USE_PLATFORM_VALUE,
                ControllerExternalDevicePolicy.resolve(
                        "Pixel 9",
                        "GPIO-Keys"));
    }

    private static void assertForcedInternal(String deviceName) {
        assertEquals(
                ControllerExternalDevicePolicy.Resolution.FORCE_INTERNAL,
                ControllerExternalDevicePolicy.resolve(
                        "Pixel 9",
                        deviceName));
    }
}
