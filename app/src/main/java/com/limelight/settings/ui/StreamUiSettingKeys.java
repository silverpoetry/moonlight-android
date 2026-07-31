package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for in-stream UI policy.
 */
public final class StreamUiSettingKeys {
    public static final SettingKey<Boolean>
            FLOATING_CONTROL_ENABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_ax_floating",
                    true);
    public static final SettingKey<Integer> FLOATING_ACTION =
            SettingKey.integerSetKey(
                    "ax_floating_operate",
                    0,
                    0,
                    1,
                    2);
    public static final SettingKey<Boolean>
            REMEMBER_FLOATING_POSITION =
            SettingKey.booleanKey(
                    "ax_floating_postion_auto",
                    false);
    public static final SettingKey<Float> FLOATING_POSITION_X =
            SettingKey.floatKey(
                    "ax_floating_postion_x",
                    -1f,
                    -1f,
                    1_000_000f);
    public static final SettingKey<Float> FLOATING_POSITION_Y =
            SettingKey.floatKey(
                    "ax_floating_postion_y",
                    -1f,
                    -1f,
                    1_000_000f);
    public static final SettingKey<Boolean>
            FLOATING_POSITION_NEAREST_LEFT =
            SettingKey.booleanKey(
                    "ax_floating_postion_isnearestleft",
                    true);

    public static final SettingKey<Boolean>
            PERFORMANCE_OVERLAY_ENABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_perf_overlay",
                    false);
    public static final SettingKey<Boolean>
            COMPACT_PERFORMANCE_OVERLAY =
            SettingKey.booleanKey(
                    "checkbox_enable_perf_overlay_lite",
                    false);
    public static final SettingKey<Boolean>
            COMPACT_PERFORMANCE_DETAILS =
            SettingKey.booleanKey(
                    "checkbox_enable_perf_overlay_lite_ext",
                    true);
    public static final SettingKey<Boolean>
            COMPACT_PERFORMANCE_INTERACTIVE =
            SettingKey.booleanKey(
                    "checkbox_enable_perf_overlay_lite_dialog",
                    false);
    public static final SettingKey<Boolean>
            RUMBLE_OVERLAY_ENABLED =
            SettingKey.booleanKey(
                    "rumble_HUD_show",
                    false);
    public static final SettingKey<Integer>
            COMPACT_PERFORMANCE_SCALE_PERCENT =
            SettingKey.integerKey(
                    "game_setting_pref_zoom",
                    100,
                    50,
                    230);
    public static final SettingKey<Integer>
            COMPACT_PERFORMANCE_MARGIN_TOP_DP =
            SettingKey.integerKey(
                    "performance_overlayLite_magin_top",
                    4,
                    0,
                    100);
    public static final SettingKey<Boolean>
            HIDE_BUILT_IN_SHORTCUTS =
            SettingKey.booleanKey(
                    "checkbox_enable_clear_default_special_button",
                    false);
    public static final SettingKey<Boolean> PICTURE_IN_PICTURE =
            SettingKey.booleanKey(
                    "checkbox_enable_pip",
                    false);
    public static final SettingKey<Boolean>
            CONNECTION_WARNINGS_DISABLED =
            SettingKey.booleanKey(
                    "checkbox_disable_warnings",
                    false);
    public static final SettingKey<Boolean> LATENCY_TOAST =
            SettingKey.booleanKey(
                    "checkbox_enable_post_stream_toast",
                    false);
    public static final SettingKey<Boolean>
            GAME_MODE_INTEGRATION_DISABLED =
            SettingKey.booleanKey(
                    "checkbox_enable_game_manager_quest",
                    false);

    private StreamUiSettingKeys() {
    }
}
