package com.limelight.binding.input;

import android.os.BatteryManager;

/**
 * Converts SHIELD controller-extension facts into Android battery semantics.
 */
final class ShieldControllerBatteryPolicy {
    enum Connection {
        WIRED,
        WIRELESS,
        BOTH,
        UNKNOWN
    }

    enum Charging {
        CHARGING,
        NOT_CHARGING,
        UNKNOWN
    }

    private ShieldControllerBatteryPolicy() {
    }

    static ControllerBatterySample resolve(
            int batteryPercentage,
            Connection connection,
            Charging charging) {
        float capacity = batteryPercentage < 0
                ? Float.NaN
                : batteryPercentage / 100.f;
        int status;
        switch (connection) {
            case WIRED:
            case BOTH:
                if (batteryPercentage == 100) {
                    status = BatteryManager.BATTERY_STATUS_FULL;
                }
                else if (charging == Charging.NOT_CHARGING) {
                    status = BatteryManager.BATTERY_STATUS_NOT_CHARGING;
                }
                else {
                    status = BatteryManager.BATTERY_STATUS_CHARGING;
                }
                break;
            case WIRELESS:
                status = charging == Charging.CHARGING
                        ? BatteryManager.BATTERY_STATUS_CHARGING
                        : BatteryManager.BATTERY_STATUS_DISCHARGING;
                break;
            case UNKNOWN:
            default:
                if (batteryPercentage == 100) {
                    status = BatteryManager.BATTERY_STATUS_FULL;
                }
                else if (charging == Charging.NOT_CHARGING) {
                    status = BatteryManager.BATTERY_STATUS_DISCHARGING;
                }
                else if (charging == Charging.CHARGING) {
                    status = BatteryManager.BATTERY_STATUS_CHARGING;
                }
                else {
                    status = BatteryManager.BATTERY_STATUS_UNKNOWN;
                }
                break;
        }
        return new ControllerBatterySample(status, capacity);
    }
}
