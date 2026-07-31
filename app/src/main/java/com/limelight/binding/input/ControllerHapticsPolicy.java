package com.limelight.binding.input;

import com.limelight.settings.audio.StreamAudioSettings;

/**
 * Resolves ownership between audio haptics and ordinary controller rumble.
 */
final class ControllerHapticsPolicy {
    private ControllerHapticsPolicy() {
    }

    static boolean shouldUseAudioHaptics(
            StreamAudioSettings settings) {
        return settings.areAudioHapticsEnabled() &&
                settings.isControllerHapticsTarget();
    }

    static boolean shouldSuppressStandardRumble(
            StreamAudioSettings settings,
            boolean selectiveDeviceSuppression,
            boolean deviceReceivesAudioHaptics) {
        if (!shouldUseAudioHaptics(settings) ||
                settings.shouldKeepControllerRumble()) {
            return false;
        }
        return !selectiveDeviceSuppression ||
                deviceReceivesAudioHaptics;
    }
}
