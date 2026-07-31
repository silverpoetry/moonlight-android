package com.limelight.binding.input;

import android.hardware.BatteryState;
import android.os.Build;
import android.view.InputDevice;

import org.cgutman.shieldcontrollerextensions.SceChargingState;
import org.cgutman.shieldcontrollerextensions.SceConnectionType;
import org.cgutman.shieldcontrollerextensions.SceManager;

import java.util.Objects;

/**
 * Samples controller battery state from Android or SHIELD extensions.
 */
final class AndroidControllerBatterySource
        implements ControllerBatteryReporter.Source {
    private final InputDevice inputDevice;
    private final SceManager sceManager;

    AndroidControllerBatterySource(
            InputDevice inputDevice,
            SceManager sceManager) {
        this.inputDevice = Objects.requireNonNull(
                inputDevice,
                "inputDevice");
        this.sceManager = Objects.requireNonNull(
                sceManager,
                "sceManager");
    }

    @Override
    public ControllerBatterySample sample() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BatteryState batteryState = inputDevice.getBatteryState();
            if (batteryState.isPresent()) {
                return new ControllerBatterySample(
                        batteryState.getStatus(),
                        batteryState.getCapacity());
            }
        }

        if (!sceManager.isRecognizedDevice(inputDevice)) {
            return null;
        }

        return ShieldControllerBatteryPolicy.resolve(
                sceManager.getBatteryPercentage(inputDevice),
                mapConnection(
                        sceManager.getConnectionType(inputDevice)),
                mapCharging(
                        sceManager.getChargingState(inputDevice)));
    }

    private static ShieldControllerBatteryPolicy.Connection mapConnection(
            SceConnectionType connection) {
        if (connection == SceConnectionType.WIRED) {
            return ShieldControllerBatteryPolicy.Connection.WIRED;
        }
        if (connection == SceConnectionType.WIRELESS) {
            return ShieldControllerBatteryPolicy.Connection.WIRELESS;
        }
        if (connection == SceConnectionType.BOTH) {
            return ShieldControllerBatteryPolicy.Connection.BOTH;
        }
        return ShieldControllerBatteryPolicy.Connection.UNKNOWN;
    }

    private static ShieldControllerBatteryPolicy.Charging mapCharging(
            SceChargingState charging) {
        if (charging == SceChargingState.CHARGING) {
            return ShieldControllerBatteryPolicy.Charging.CHARGING;
        }
        if (charging == SceChargingState.NOT_CHARGING) {
            return ShieldControllerBatteryPolicy.Charging.NOT_CHARGING;
        }
        return ShieldControllerBatteryPolicy.Charging.UNKNOWN;
    }
}
