package com.limelight.binding.input;

import android.os.BatteryManager;

import com.limelight.nvstream.jni.MoonBridge;

/**
 * Immutable protocol report derived from an Android controller battery sample.
 */
final class ControllerBatteryReport {
    private final int androidStatus;
    private final float capacity;
    private final byte protocolState;
    private final byte percentage;

    private ControllerBatteryReport(
            int androidStatus,
            float capacity,
            byte protocolState,
            byte percentage) {
        this.androidStatus = androidStatus;
        this.capacity = capacity;
        this.protocolState = protocolState;
        this.percentage = percentage;
    }

    static ControllerBatteryReport fromAndroidSample(
            int androidStatus,
            float capacity) {
        byte protocolState;
        switch (androidStatus) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                protocolState = MoonBridge.LI_BATTERY_STATE_UNKNOWN;
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                protocolState = MoonBridge.LI_BATTERY_STATE_CHARGING;
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                protocolState = MoonBridge.LI_BATTERY_STATE_DISCHARGING;
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                protocolState = MoonBridge.LI_BATTERY_STATE_NOT_CHARGING;
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                protocolState = MoonBridge.LI_BATTERY_STATE_FULL;
                break;
            default:
                return null;
        }

        byte percentage = Float.isNaN(capacity)
                ? MoonBridge.LI_BATTERY_PERCENTAGE_UNKNOWN
                : (byte) (capacity * 100);
        return new ControllerBatteryReport(
                androidStatus,
                capacity,
                protocolState,
                percentage);
    }

    boolean differsFrom(
            int previousAndroidStatus,
            float previousCapacity) {
        return androidStatus != previousAndroidStatus ||
                !capacitiesEqual(capacity, previousCapacity);
    }

    int getAndroidStatus() {
        return androidStatus;
    }

    float getCapacity() {
        return capacity;
    }

    byte getProtocolState() {
        return protocolState;
    }

    byte getPercentage() {
        return percentage;
    }

    private static boolean capacitiesEqual(
            float first,
            float second) {
        if (!Float.isNaN(first) && !Float.isNaN(second)) {
            return first == second;
        }
        return Float.isNaN(first) == Float.isNaN(second);
    }
}
