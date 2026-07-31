package com.limelight.preferences;

/** Localized text port consumed by the display settings controller. */
interface SettingsDisplayText {
    CharSequence nativeResolutionName(
            SettingsDisplayPolicy.ResolutionOption option);

    CharSequence nativeFrameRateName(String frameRateValue);

    CharSequence hdrFirmwareRequired();
}
