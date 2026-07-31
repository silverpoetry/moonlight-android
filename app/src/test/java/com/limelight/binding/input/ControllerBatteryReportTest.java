package com.limelight.binding.input;

import android.os.BatteryManager;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class ControllerBatteryReportTest {
    @Test
    public void androidStatusesMapToProtocolStates() {
        assertState(
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                MoonBridge.LI_BATTERY_STATE_UNKNOWN);
        assertState(
                BatteryManager.BATTERY_STATUS_CHARGING,
                MoonBridge.LI_BATTERY_STATE_CHARGING);
        assertState(
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                MoonBridge.LI_BATTERY_STATE_DISCHARGING);
        assertState(
                BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                MoonBridge.LI_BATTERY_STATE_NOT_CHARGING);
        assertState(
                BatteryManager.BATTERY_STATUS_FULL,
                MoonBridge.LI_BATTERY_STATE_FULL);
    }

    @Test
    public void unknownPlatformStatusIsRejected() {
        assertNull(ControllerBatteryReport.fromAndroidSample(
                Integer.MAX_VALUE,
                0.5f));
    }

    @Test
    public void finiteCapacityMapsToPercentage() {
        ControllerBatteryReport report =
                ControllerBatteryReport.fromAndroidSample(
                        BatteryManager.BATTERY_STATUS_CHARGING,
                        0.73f);

        assertEquals(73, report.getPercentage());
        assertEquals(0.73f, report.getCapacity(), 0);
    }

    @Test
    public void unavailableCapacityMapsToUnknownPercentage() {
        ControllerBatteryReport report =
                ControllerBatteryReport.fromAndroidSample(
                        BatteryManager.BATTERY_STATUS_UNKNOWN,
                        Float.NaN);

        assertEquals(
                MoonBridge.LI_BATTERY_PERCENTAGE_UNKNOWN,
                report.getPercentage());
    }

    @Test
    public void unchangedSamplesIncludingNanAreSuppressed() {
        ControllerBatteryReport finite =
                ControllerBatteryReport.fromAndroidSample(
                        BatteryManager.BATTERY_STATUS_DISCHARGING,
                        0.4f);
        ControllerBatteryReport unavailable =
                ControllerBatteryReport.fromAndroidSample(
                        BatteryManager.BATTERY_STATUS_UNKNOWN,
                        Float.NaN);

        assertFalse(finite.differsFrom(
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                0.4f));
        assertFalse(unavailable.differsFrom(
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                Float.NaN));
    }

    @Test
    public void statusOrCapacityChangeProducesReport() {
        ControllerBatteryReport report =
                ControllerBatteryReport.fromAndroidSample(
                        BatteryManager.BATTERY_STATUS_CHARGING,
                        0.6f);

        assertTrue(report.differsFrom(
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                0.6f));
        assertTrue(report.differsFrom(
                BatteryManager.BATTERY_STATUS_CHARGING,
                0.5f));
    }

    private static void assertState(
            int androidStatus,
            byte expectedProtocolState) {
        ControllerBatteryReport report =
                ControllerBatteryReport.fromAndroidSample(
                        androidStatus,
                        0.5f);

        assertEquals(expectedProtocolState, report.getProtocolState());
        assertEquals(androidStatus, report.getAndroidStatus());
    }
}
