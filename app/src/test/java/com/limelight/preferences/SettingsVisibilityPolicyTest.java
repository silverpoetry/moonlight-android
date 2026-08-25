package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.platform.PlatformIntegrationSettingKeys;
import com.limelight.settings.ui.StreamUiSettingKeys;

import org.junit.Test;

import java.util.Set;

public final class SettingsVisibilityPolicyTest {
    @Test
    public void completeDeviceKeepsEveryCapabilitySettingVisible() {
        SettingsVisibilityPolicy.Result result =
                SettingsVisibilityPolicy.evaluate(
                        completeCapabilities().build(),
                        true);

        assertTrue(result.getHiddenSectionIds().isEmpty());
        assertTrue(result.getHiddenItemIds().isEmpty());
    }

    @Test
    public void unavailableHardwareHidesOnlyAffectedSettings() {
        SettingsVisibilityPolicy.Result result =
                SettingsVisibilityPolicy.evaluate(
                        SettingsDeviceCapabilities.builder().build(),
                        false);

        assertEquals(1, result.getHiddenSectionIds().size());
        assertTrue(result.getHiddenSectionIds().contains(
                SettingsScreenIds.SECTION_VIRTUAL_CONTROLS));

        Set<String> hidden = result.getHiddenItemIds();
        assertEquals(13, hidden.size());
        assertContains(hidden,
                InputSettingKeys.ABSOLUTE_MOUSE_MODE.getName(),
                InputSettingKeys.BAROMETER_FORCE_PRESS.getName(),
                InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD.getName(),
                InputSettingKeys
                        .BAROMETER_FORCE_PRESS_MINIMUM_DURATION
                        .getName(),
                ControllerSettingKeys.MOTION_SENSORS.getName(),
                ControllerSettingKeys
                        .MOTION_SENSORS_FALLBACK_TO_DEVICE
                        .getName(),
                ControllerSettingKeys.CLAIM_ALL_USB_DEVICES.getName(),
                ControllerSettingKeys.USB_DRIVER.getName(),
                StreamUiSettingKeys.PICTURE_IN_PICTURE.getName(),
                ControllerSettingKeys.FALLBACK_DEVICE_RUMBLE.getName(),
                ControllerSettingKeys
                        .FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT
                        .getName(),
                ControllerSettingKeys.ONSCREEN_RUMBLE.getName());
        assertTrue(hidden.contains(
                PlatformIntegrationSettingKeys
                        .XIAOMI_REFRESH_RATE_LIMIT_SUPPRESSION
                        .getName()));
    }

    @Test
    public void disabledBarometerModeHidesOnlyItsDetailRows() {
        SettingsVisibilityPolicy.Result result =
                SettingsVisibilityPolicy.evaluate(
                        completeCapabilities().build(),
                        false);

        Set<String> hidden = result.getHiddenItemIds();
        assertEquals(2, hidden.size());
        assertFalse(hidden.contains(
                InputSettingKeys.BAROMETER_FORCE_PRESS.getName()));
        assertContains(hidden,
                InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD.getName(),
                InputSettingKeys
                        .BAROMETER_FORCE_PRESS_MINIMUM_DURATION
                        .getName());
    }

    @Test
    public void vibratorWithoutAmplitudeControlHidesOnlyStrength() {
        SettingsVisibilityPolicy.Result result =
                SettingsVisibilityPolicy.evaluate(
                        completeCapabilities()
                                .vibrationAmplitudeControlAvailable(false)
                                .build(),
                        true);

        assertEquals(1, result.getHiddenItemIds().size());
        assertTrue(result.getHiddenItemIds().contains(
                ControllerSettingKeys
                        .FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT
                        .getName()));
    }

    private static SettingsDeviceCapabilities.Builder
            completeCapabilities() {
        return SettingsDeviceCapabilities.builder()
                .touchscreenAvailable(true)
                .absoluteMouseModeAvailable(true)
                .barometerAvailable(true)
                .controllerMotionSensorsAvailable(true)
                .deviceMotionSensorsAvailable(true)
                .usbHostAvailable(true)
                .pictureInPictureAvailable(true)
                .vibratorAvailable(true)
                .vibrationAmplitudeControlAvailable(true)
                .xiaomiRefreshRateOverrideAvailable(true);

    }

    private static void assertContains(
            Set<String> actual,
            String... expected) {
        for (String value : expected) {
            assertTrue("Missing " + value, actual.contains(value));
        }
    }
}
