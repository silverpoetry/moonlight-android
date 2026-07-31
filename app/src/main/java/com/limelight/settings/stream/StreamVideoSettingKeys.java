package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for stream video and launch policy.
 */
public final class StreamVideoSettingKeys {
    public static final int MAX_BITRATE_KBPS = 99_999_000;

    public static final SettingKey<Integer> BITRATE_KBPS =
            SettingKey.integerKey(
                    "seekbar_bitrate_kbps",
                    0,
                    0,
                    MAX_BITRATE_KBPS);
    public static final SettingKey<Integer>
            LEGACY_BITRATE_MBPS =
            SettingKey.integerKey(
                    "seekbar_bitrate",
                    0,
                    0,
                    99_999);
    public static final SettingKey<String> VIDEO_FORMAT =
            SettingKey.stringSetKey(
                    "video_format",
                    "auto",
                    "auto",
                    "forceav1",
                    "forceh265",
                    "neverh265");
    public static final SettingKey<Boolean> HDR_ENABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_hdr",
                    false);
    public static final SettingKey<Boolean>
            HDR_HIGH_BRIGHTNESS =
            SettingKey.booleanKey(
                    "checkbox_enable_hdr_high_brightness",
                    false);
    public static final SettingKey<Boolean>
            IGNORE_HDR_CAPABILITY =
            SettingKey.booleanKey("ignoreCheckHDR", false);
    public static final SettingKey<Boolean>
            LOW_LATENCY_EXPERIMENT =
            SettingKey.booleanKey(
                    "enable_lowLatency_experiment",
                    true);
    public static final SettingKey<Boolean> UNLOCK_FPS =
            SettingKey.booleanKey(
                    "checkbox_unlock_fps",
                    false);
    public static final SettingKey<Boolean> PORTRAIT =
            SettingKey.booleanKey(
                    "checkbox_enable_portrait",
                    false);
    public static final SettingKey<Boolean> EXTERNAL_DISPLAY =
            SettingKey.booleanKey(
                    "checkbox_enable_exdisplay",
                    false);
    public static final SettingKey<Boolean>
            OPTIMIZE_GAME_SETTINGS =
            SettingKey.booleanKey(
                    "checkbox_enable_sops",
                    true);
    public static final SettingKey<Integer>
            VIRTUAL_DISPLAY_MODE =
            SettingKey.integerSetKey(
                    "vdValue",
                    0,
                    0,
                    1,
                    2);
    public static final SettingKey<Boolean>
            ENFORCE_DISPLAY_MODE =
            SettingKey.booleanKey(
                    "checkbox_enforce_display_mode",
                    false);
    public static final SettingKey<Integer> SCREEN_ON_POLICY =
            SettingKey.integerSetKey(
                    "enable_screen_on_auto",
                    0,
                    0,
                    1,
                    2);
    public static final SettingKey<String>
            CUSTOM_RESOLUTION_TEXT =
            SettingKey.boundedStringKey(
                    "edit_diy_w_h",
                    StreamResolutionCodec.RESOLUTION_1080P,
                    32);

    private StreamVideoSettingKeys() {
    }
}
