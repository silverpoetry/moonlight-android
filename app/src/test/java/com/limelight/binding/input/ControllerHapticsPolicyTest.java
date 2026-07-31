package com.limelight.binding.input;

import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettings.HapticsOutputTarget;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerHapticsPolicyTest {
    @Test
    public void controllerTargetEnablesAudioHaptics() {
        StreamAudioSettings settings =
                StreamAudioSettings.builder()
                        .setAudioHaptics(
                                true,
                                HapticsOutputTarget.CONTROLLER,
                                100,
                                StreamAudioSettings.VoiceFilter.OFF,
                                false)
                        .build();

        assertTrue(
                ControllerHapticsPolicy
                        .shouldUseAudioHaptics(settings));
        assertTrue(
                ControllerHapticsPolicy
                        .shouldSuppressStandardRumble(
                                settings,
                                false,
                                false));
    }

    @Test
    public void phoneTargetDoesNotTakeControllerRumble() {
        StreamAudioSettings settings =
                StreamAudioSettings.builder()
                        .setAudioHapticsEnabled(true)
                        .setHapticsOutputTarget(
                                HapticsOutputTarget.PHONE)
                        .build();

        assertFalse(
                ControllerHapticsPolicy
                        .shouldUseAudioHaptics(settings));
        assertFalse(
                ControllerHapticsPolicy
                        .shouldSuppressStandardRumble(
                                settings,
                                false,
                                true));
    }

    @Test
    public void selectiveSuppressionOnlyAffectsAudioDevice() {
        StreamAudioSettings settings =
                StreamAudioSettings.builder()
                        .setAudioHapticsEnabled(true)
                        .setHapticsOutputTarget(
                                HapticsOutputTarget.CONTROLLER)
                        .setKeepControllerRumble(false)
                        .build();

        assertFalse(
                ControllerHapticsPolicy
                        .shouldSuppressStandardRumble(
                                settings,
                                true,
                                false));
        assertTrue(
                ControllerHapticsPolicy
                        .shouldSuppressStandardRumble(
                                settings,
                                true,
                                true));
    }

    @Test
    public void keepRumbleOverridesAudioOwnership() {
        StreamAudioSettings settings =
                StreamAudioSettings.builder()
                        .setAudioHapticsEnabled(true)
                        .setHapticsOutputTarget(
                                HapticsOutputTarget.CONTROLLER)
                        .setKeepControllerRumble(true)
                        .build();

        assertFalse(
                ControllerHapticsPolicy
                        .shouldSuppressStandardRumble(
                                settings,
                                false,
                                true));
    }
}
