package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerArrivalReportTest {
    @Test
    public void fullPlayStationProfileReportsAllCapabilities() {
        ControllerArrivalReport report =
                ControllerArrivalReport.builder(
                                MoonBridge.LI_CTYPE_PS,
                                ControllerPacket.A_FLAG)
                        .hasPaddles(true)
                        .hasShareButton(true)
                        .hasHorizontalHatAxis(true)
                        .hasVerticalHatAxis(true)
                        .hasAdvancedInputDeviceApis(true)
                        .hasQuadVibrators(true)
                        .external(true)
                        .hasRgbLed(true)
                        .hasReliableRgbLedDetection(false)
                        .hasAnalogTriggers(true)
                        .hasAccelerometer(true)
                        .hasGyroscope(true)
                        .requiresGenericMotionControllerType(false)
                        .recognizedByShieldExtensions(true)
                        .hasTouchpad(true)
                        .hasClickpad(true)
                        .build();

        int expectedButtons =
                ControllerPacket.A_FLAG |
                        ControllerPacket.PADDLE1_FLAG |
                        ControllerPacket.PADDLE2_FLAG |
                        ControllerPacket.PADDLE3_FLAG |
                        ControllerPacket.PADDLE4_FLAG |
                        ControllerPacket.MISC_FLAG |
                        ControllerPacket.LEFT_FLAG |
                        ControllerPacket.RIGHT_FLAG |
                        ControllerPacket.UP_FLAG |
                        ControllerPacket.DOWN_FLAG |
                        ControllerPacket.TOUCHPAD_FLAG;
        short expectedCapabilities =
                (short) (
                        MoonBridge.LI_CCAP_RUMBLE |
                                MoonBridge.LI_CCAP_TRIGGER_RUMBLE |
                                MoonBridge.LI_CCAP_BATTERY_STATE |
                                MoonBridge.LI_CCAP_RGB_LED |
                                MoonBridge.LI_CCAP_ANALOG_TRIGGERS |
                                MoonBridge.LI_CCAP_ACCEL |
                                MoonBridge.LI_CCAP_GYRO |
                                MoonBridge.LI_CCAP_TOUCHPAD);

        assertEquals(
                MoonBridge.LI_CTYPE_PS,
                report.getReportedType());
        assertEquals(
                expectedButtons,
                report.getSupportedButtonFlags());
        assertEquals(
                expectedCapabilities,
                report.getCapabilities());
        assertFalse(
                report.isClickpadEmulationRequired());
    }

    @Test
    public void emulatedMotionUsesUnknownProtocolType() {
        ControllerArrivalReport report =
                ControllerArrivalReport.builder(
                                MoonBridge.LI_CTYPE_XBOX,
                                0)
                        .requiresGenericMotionControllerType(true)
                        .hasAccelerometer(true)
                        .build();

        assertEquals(
                MoonBridge.LI_CTYPE_UNKNOWN,
                report.getReportedType());
        assertTrue(
                report.isClickpadEmulationRequired());
        assertEquals(
                MoonBridge.LI_CCAP_ACCEL,
                report.getCapabilities());
    }

    @Test
    public void preAndroidTwelveProfileKeepsLegacyCapabilities() {
        ControllerArrivalReport report =
                ControllerArrivalReport.builder(
                                MoonBridge.LI_CTYPE_XBOX,
                                0)
                        .hasAdvancedInputDeviceApis(false)
                        .hasLegacyVibrator(true)
                        .external(true)
                        .hasRgbLed(true)
                        .hasReliableRgbLedDetection(true)
                        .hasAnalogTriggers(true)
                        .build();

        assertEquals(
                (short) (
                        MoonBridge.LI_CCAP_RUMBLE |
                                MoonBridge.LI_CCAP_ANALOG_TRIGGERS),
                report.getCapabilities());
    }

    @Test
    public void touchpadWithoutClickpadDoesNotClaimButton() {
        ControllerArrivalReport report =
                ControllerArrivalReport.builder(
                                MoonBridge.LI_CTYPE_NINTENDO,
                                ControllerPacket.B_FLAG)
                        .hasTouchpad(true)
                        .hasClickpad(false)
                        .build();

        assertEquals(
                ControllerPacket.B_FLAG,
                report.getSupportedButtonFlags());
        assertEquals(
                MoonBridge.LI_CCAP_TOUCHPAD,
                report.getCapabilities());
    }
}
