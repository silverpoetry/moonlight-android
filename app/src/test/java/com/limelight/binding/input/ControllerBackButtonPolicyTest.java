package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerBackButtonPolicyTest {
    @Test
    public void servalAndAxislessRemoteLeaveBackToAndroid() {
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .IGNORE_AS_CONTROLLER_INPUT,
                resolve("Razer Serval", true, true, true));
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .IGNORE_AS_CONTROLLER_INPUT,
                resolve("Living Room REMOTE", true, false, false));
    }

    @Test
    public void remoteNameDoesNotOverrideRealJoystickAxes() {
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .HANDLE_AS_CONTROLLER_INPUT,
                resolve("Remote Gamepad", true, true, false));
    }

    @Test
    public void externalControllerRequiresAxesOrGamepadButtons() {
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .IGNORE_AS_CONTROLLER_INPUT,
                resolve("Accessory", true, false, false));
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .HANDLE_AS_CONTROLLER_INPUT,
                resolve("Accessory", true, true, false));
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .HANDLE_AS_CONTROLLER_INPUT,
                resolve("Accessory", true, false, true));
    }

    @Test
    public void ordinaryInternalDeviceRequestsInventoryInspection() {
        assertEquals(
                ControllerBackButtonPolicy.DeviceResolution
                        .INSPECT_INTERNAL_INVENTORY,
                resolve("gpio-keys", false, false, false));
    }

    @Test
    public void internalInventoryPreservesNavigationRules() {
        assertTrue(ControllerBackButtonPolicy
                .shouldIgnoreForInternalInventory(false, false));
        assertTrue(ControllerBackButtonPolicy
                .shouldIgnoreForInternalInventory(true, true));
        assertFalse(ControllerBackButtonPolicy
                .shouldIgnoreForInternalInventory(true, false));
    }

    private static ControllerBackButtonPolicy.DeviceResolution resolve(
            String name,
            boolean external,
            boolean hasAxes,
            boolean hasButtons) {
        return ControllerBackButtonPolicy.resolveDevice(
                name,
                external,
                hasAxes,
                hasButtons);
    }
}
