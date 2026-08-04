package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for stream video and launch policy.
 */
public final class StreamVideoSettingKeys {
    public static final int MAX_BITRATE_KBPS = 99_999_000;

    public static final SettingKey<Integer> BITRATE_KBPS =
            SettingKey.integerKey(
                    "stream.video.bitrate_kbps",
                    0,
                    0,
                    MAX_BITRATE_KBPS)
                    .renamedFrom("seekbar_bitrate_kbps");
    public static final SettingKey<Integer>
            LEGACY_BITRATE_MBPS =
            SettingKey.integerKey(
                    "seekbar_bitrate",
                    0,
                    0,
                    99_999);
    public static final SettingKey<String> VIDEO_FORMAT =
            SettingKey.stringSetKey(
                    "stream.video.codec",
                    "auto",
                    "auto",
                    "forceav1",
                    "forceh265",
                    "neverh265")
                    .renamedFrom("video_format");
    public static final SettingKey<Boolean> HDR_ENABLED =
            SettingKey.booleanKey(
                    "stream.video.hdr.enabled",
                    false)
                    .renamedFrom("checkbox_enable_hdr");
    public static final SettingKey<Boolean>
            HDR_HIGH_BRIGHTNESS =
            SettingKey.booleanKey(
                    "stream.video.hdr.high_brightness",
                    false)
                    .renamedFrom(
                            "checkbox_enable_hdr_high_brightness");
    public static final SettingKey<Boolean>
            IGNORE_HDR_CAPABILITY =
            SettingKey.booleanKey(
                    "stream.video.hdr.ignore_device_capability",
                    false)
                    .renamedFrom("ignoreCheckHDR");
    public static final SettingKey<Boolean>
            LOW_LATENCY_EXPERIMENT =
            SettingKey.booleanKey(
                    "stream.video.low_latency_decode",
                    true)
                    .renamedFrom("enable_lowLatency_experiment");
    public static final SettingKey<Boolean> UNLOCK_FPS =
            SettingKey.booleanKey(
                    "stream.video.unlock_frame_rates",
                    false)
                    .renamedFrom("checkbox_unlock_fps");
    public static final SettingKey<Boolean> PORTRAIT =
            SettingKey.booleanKey(
                    "stream.display.portrait",
                    false)
                    .renamedFrom("checkbox_enable_portrait");
    public static final SettingKey<Boolean> EXTERNAL_DISPLAY =
            SettingKey.booleanKey(
                    "stream.display.external_display",
                    false)
                    .renamedFrom("checkbox_enable_exdisplay");
    public static final SettingKey<Boolean>
            OPTIMIZE_GAME_SETTINGS =
            SettingKey.booleanKey(
                    "stream.host.optimize_game_settings",
                    true)
                    .renamedFrom("checkbox_enable_sops");
    public static final SettingKey<Integer>
            VIRTUAL_DISPLAY_MODE =
            SettingKey.integerSetKey(
                    "stream.display.virtual_display_mode",
                    0,
                    0,
                    1,
                    2)
                    .renamedFrom("vdValue");
    public static final SettingKey<Boolean>
            ENFORCE_DISPLAY_MODE =
            SettingKey.booleanKey(
                    "stream.display.enforce_mode",
                    false)
                    .renamedFrom("checkbox_enforce_display_mode");
    public static final SettingKey<Integer> SCREEN_ON_POLICY =
            SettingKey.integerSetKey(
                    "stream.display.device_screen_policy",
                    0,
                    0,
                    1,
                    2)
                    .renamedFrom("enable_screen_on_auto");
    public static final SettingKey<String>
            LEGACY_CUSTOM_RESOLUTION_TEXT =
            SettingKey.boundedStringKey(
                    "stream.video.custom_resolution",
                    "",
                    32)
                    .renamedFrom("edit_diy_w_h");

    private StreamVideoSettingKeys() {
    }
}
