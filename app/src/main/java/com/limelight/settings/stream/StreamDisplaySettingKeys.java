package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Persisted keys used to build a stream display settings snapshot.
 */
public final class StreamDisplaySettingKeys {
    public static final SettingKey<Boolean> STRETCH_VIDEO =
            SettingKey.booleanKey(
                    "checkbox_stretch_video",
                    false);
    public static final SettingKey<Boolean> DISPLAY_CUTOUT =
            SettingKey.booleanKey(
                    "checkbox_cutout_mode_video",
                    false);
    public static final SettingKey<String> FSR_TARGET =
            SettingKey.stringKey("list_fsr_target", "off");
    public static final SettingKey<String> FSR_SHARPNESS =
            SettingKey.stringKey(
                    "list_fsr_sharpness",
                    "standard");
    public static final SettingKey<String> FSR_HDR_OUTPUT =
            SettingKey.stringKey(
                    "list_fsr_hdr_output",
                    "native");
    public static final SettingKey<String> GRAVITY =
            SettingKey.stringKey("screen_gravity_list", "0");

    private StreamDisplaySettingKeys() {
    }
}
