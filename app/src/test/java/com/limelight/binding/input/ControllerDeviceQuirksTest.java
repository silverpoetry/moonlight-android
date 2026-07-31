package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerDeviceQuirksTest {
    private static final float DELTA = 0.0001f;

    @Test
    public void adtOneRemapsBackModeAndTriggerDeadzone() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(0x18d1, 0x2c40)
                                .hasMode(true)
                                .build());

        assertTrue(quirks.isBackStart());
        assertTrue(quirks.isModeSelect());
        assertTrue(quirks.hasSelect());
        assertFalse(quirks.hasMode());
        assertEquals(0.30f, quirks.getTriggerDeadzone(), DELTA);
    }

    @Test
    public void asusWithoutStartOrMenuUsesLegacyMapping() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(0, 0)
                                .deviceName("ASUS Gamepad")
                                .triggerDeadzone(0.13f)
                                .build());

        assertTrue(quirks.isBackStart());
        assertTrue(quirks.isModeSelect());
        assertTrue(quirks.hasSelect());
        assertFalse(quirks.hasMode());
        assertEquals(0.30f, quirks.getTriggerDeadzone(), DELTA);
    }

    @Test
    public void asusWithMenuKeepsButtonsButStillWidensDeadzone() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(0, 0)
                                .deviceName("ASUS Gamepad")
                                .hasMode(true)
                                .hasSelect(false)
                                .hasStartOrMenu(true)
                                .triggerDeadzone(0.13f)
                                .build());

        assertFalse(quirks.isBackStart());
        assertFalse(quirks.isModeSelect());
        assertTrue(quirks.hasMode());
        assertFalse(quirks.hasSelect());
        assertEquals(0.30f, quirks.getTriggerDeadzone(), DELTA);
    }

    @Test
    public void earlyNvidiaControllerMapsSearchToMode() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(0, 0)
                                .deviceName("NVIDIA Controller v01.04")
                                .build());

        assertTrue(quirks.isSearchMode());
        assertTrue(quirks.hasMode());
    }

    @Test
    public void servalEnablesItsNonStandardButtons() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(0, 0)
                                .deviceName("Razer Serval")
                                .build());

        assertTrue(quirks.isServal());
        assertTrue(quirks.hasMode());
        assertTrue(quirks.hasSelect());
    }

    @Test
    public void oldXboxBluetoothMappingRequiresMissingGasAxis() {
        ControllerDeviceQuirks oldFirmware =
                resolve(
                        facts(0, 0)
                                .deviceName("Xbox Wireless Controller")
                                .build());
        ControllerDeviceQuirks newFirmware =
                resolve(
                        facts(0, 0)
                                .deviceName("Xbox Wireless Controller")
                                .hasGasAxis(true)
                                .build());

        assertTrue(oldFirmware.isNonStandardXboxBluetooth());
        assertTrue(oldFirmware.hasMode());
        assertTrue(oldFirmware.hasSelect());
        assertFalse(newFirmware.isNonStandardXboxBluetooth());
    }

    @Test
    public void thrustmasterHomeKeyIsNeverReportedAsMode() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(0x044f, 0xb328)
                                .hasMode(true)
                                .build());

        assertFalse(quirks.hasMode());
    }

    @Test
    public void ordinaryControllerPreservesSampledState() {
        ControllerDeviceQuirks quirks =
                resolve(
                        facts(1, 2)
                                .deviceName("Ordinary Controller")
                                .hasMode(true)
                                .hasSelect(false)
                                .triggerDeadzone(0.17f)
                                .build());

        assertTrue(quirks.hasMode());
        assertFalse(quirks.hasSelect());
        assertFalse(quirks.isServal());
        assertFalse(quirks.isBackStart());
        assertEquals(0.17f, quirks.getTriggerDeadzone(), DELTA);
    }

    private static ControllerDeviceQuirks resolve(
            ControllerDeviceQuirks.Facts facts) {
        return ControllerDeviceQuirks.resolve(facts);
    }

    private static ControllerDeviceQuirks.Facts.Builder facts(
            int vendorId,
            int productId) {
        return ControllerDeviceQuirks.Facts.builder(
                vendorId,
                productId);
    }
}
