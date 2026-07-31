package com.limelight.binding.input;

import android.os.BatteryManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ShieldControllerBatteryPolicyTest {
    @Test
    public void wiredFullControllerIsFull() {
        assertSample(
                100,
                ShieldControllerBatteryPolicy.Connection.WIRED,
                ShieldControllerBatteryPolicy.Charging.CHARGING,
                BatteryManager.BATTERY_STATUS_FULL,
                1.f);
    }

    @Test
    public void wiredNotChargingControllerRetainsNotChargingState() {
        assertSample(
                55,
                ShieldControllerBatteryPolicy.Connection.BOTH,
                ShieldControllerBatteryPolicy.Charging.NOT_CHARGING,
                BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                0.55f);
    }

    @Test
    public void wiredUnknownChargeStateIsAssumedCharging() {
        assertSample(
                55,
                ShieldControllerBatteryPolicy.Connection.WIRED,
                ShieldControllerBatteryPolicy.Charging.UNKNOWN,
                BatteryManager.BATTERY_STATUS_CHARGING,
                0.55f);
    }

    @Test
    public void wirelessChargeStateSelectsChargingOrDischarging() {
        assertSample(
                40,
                ShieldControllerBatteryPolicy.Connection.WIRELESS,
                ShieldControllerBatteryPolicy.Charging.CHARGING,
                BatteryManager.BATTERY_STATUS_CHARGING,
                0.4f);
        assertSample(
                40,
                ShieldControllerBatteryPolicy.Connection.WIRELESS,
                ShieldControllerBatteryPolicy.Charging.UNKNOWN,
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                0.4f);
    }

    @Test
    public void unknownConnectionUsesAvailableChargeFacts() {
        assertSample(
                100,
                ShieldControllerBatteryPolicy.Connection.UNKNOWN,
                ShieldControllerBatteryPolicy.Charging.UNKNOWN,
                BatteryManager.BATTERY_STATUS_FULL,
                1.f);
        assertSample(
                70,
                ShieldControllerBatteryPolicy.Connection.UNKNOWN,
                ShieldControllerBatteryPolicy.Charging.NOT_CHARGING,
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                0.7f);
        assertSample(
                70,
                ShieldControllerBatteryPolicy.Connection.UNKNOWN,
                ShieldControllerBatteryPolicy.Charging.CHARGING,
                BatteryManager.BATTERY_STATUS_CHARGING,
                0.7f);
        assertSample(
                70,
                ShieldControllerBatteryPolicy.Connection.UNKNOWN,
                ShieldControllerBatteryPolicy.Charging.UNKNOWN,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                0.7f);
    }

    @Test
    public void unavailablePercentageProducesNanCapacity() {
        ControllerBatterySample sample =
                ShieldControllerBatteryPolicy.resolve(
                        -1,
                        ShieldControllerBatteryPolicy.Connection.UNKNOWN,
                        ShieldControllerBatteryPolicy.Charging.UNKNOWN);

        assertTrue(Float.isNaN(sample.getCapacity()));
    }

    private static void assertSample(
            int percentage,
            ShieldControllerBatteryPolicy.Connection connection,
            ShieldControllerBatteryPolicy.Charging charging,
            int expectedStatus,
            float expectedCapacity) {
        ControllerBatterySample sample =
                ShieldControllerBatteryPolicy.resolve(
                        percentage,
                        connection,
                        charging);

        assertEquals(expectedStatus, sample.getStatus());
        assertEquals(expectedCapacity, sample.getCapacity(), 0);
    }
}
