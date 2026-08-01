package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;

/**
 * Canonical persisted schema for in-stream UI policy.
 */
public final class StreamUiSettingKeys {
    public static final SettingKey<Boolean>
            FLOATING_CONTROL_ENABLED =
            SettingKey.booleanKey(
                    "stream.ui.floating_control.enabled",
                    true)
                    .renamedFrom("checkbox_enable_ax_floating");
    public static final SettingKey<Integer> FLOATING_ACTION =
            SettingKey.integerSetKey(
                    "stream.ui.floating_control.action",
                    0,
                    0,
                    1,
                    2)
                    .renamedFrom("ax_floating_operate");
    public static final SettingKey<Boolean>
            REMEMBER_FLOATING_POSITION =
            SettingKey.booleanKey(
                    "stream.ui.floating_control.remember_position",
                    false)
                    .renamedFrom("ax_floating_postion_auto");
    public static final SettingKey<Float> FLOATING_POSITION_X =
            SettingKey.floatKey(
                    "stream.ui.floating_control.position_x",
                    -1f,
                    -1f,
                    1_000_000f)
                    .renamedFrom("ax_floating_postion_x");
    public static final SettingKey<Float> FLOATING_POSITION_Y =
            SettingKey.floatKey(
                    "stream.ui.floating_control.position_y",
                    -1f,
                    -1f,
                    1_000_000f)
                    .renamedFrom("ax_floating_postion_y");
    public static final SettingKey<Boolean>
            FLOATING_POSITION_NEAREST_LEFT =
            SettingKey.booleanKey(
                    "stream.ui.floating_control.nearest_left",
                    true)
                    .renamedFrom(
                            "ax_floating_postion_isnearestleft");

    public static final SettingKey<Boolean>
            PERFORMANCE_OVERLAY_ENABLED =
            SettingKey.booleanKey(
                    "stream.ui.performance_overlay.enabled",
                    false)
                    .renamedFrom("checkbox_enable_perf_overlay");
    public static final SettingKey<Boolean>
            COMPACT_PERFORMANCE_OVERLAY =
            SettingKey.booleanKey(
                    "stream.ui.performance_overlay.compact",
                    false)
                    .renamedFrom("checkbox_enable_perf_overlay_lite");
    public static final SettingKey<Boolean>
            COMPACT_PERFORMANCE_DETAILS =
            SettingKey.booleanKey(
                    "stream.ui.performance_overlay.details",
                    true)
                    .renamedFrom(
                            "checkbox_enable_perf_overlay_lite_ext");
    public static final SettingKey<Boolean>
            COMPACT_PERFORMANCE_INTERACTIVE =
            SettingKey.booleanKey(
                    "stream.ui.performance_overlay.interactive",
                    false)
                    .renamedFrom(
                            "checkbox_enable_perf_overlay_lite_dialog");
    public static final SettingKey<Boolean>
            RUMBLE_OVERLAY_ENABLED =
            SettingKey.booleanKey(
                    "stream.ui.rumble_overlay.enabled",
                    false)
                    .renamedFrom("rumble_HUD_show");
    public static final SettingKey<Integer>
            COMPACT_PERFORMANCE_SCALE_PERCENT =
            SettingKey.integerKey(
                    "stream.ui.performance_overlay.scale_percent",
                    100,
                    50,
                    230)
                    .renamedFrom("game_setting_pref_zoom");
    public static final SettingKey<Integer>
            COMPACT_PERFORMANCE_MARGIN_TOP_DP =
            SettingKey.integerKey(
                    "stream.ui.performance_overlay.margin_top_dp",
                    4,
                    0,
                    100)
                    .renamedFrom(
                            "performance_overlayLite_magin_top");
    public static final SettingKey<Boolean>
            HIDE_BUILT_IN_SHORTCUTS =
            SettingKey.booleanKey(
                    "stream.ui.shortcuts.hide_built_in",
                    false)
                    .renamedFrom(
                            "checkbox_enable_clear_default_special_button");
    public static final SettingKey<Boolean> PICTURE_IN_PICTURE =
            SettingKey.booleanKey(
                    "stream.ui.picture_in_picture",
                    false)
                    .renamedFrom("checkbox_enable_pip");
    public static final SettingKey<Boolean>
            CONNECTION_WARNINGS_DISABLED =
            SettingKey.booleanKey(
                    "stream.ui.connection_warnings_disabled",
                    false)
                    .renamedFrom("checkbox_disable_warnings");
    public static final SettingKey<Boolean> LATENCY_TOAST =
            SettingKey.booleanKey(
                    "stream.ui.latency_toast",
                    false)
                    .renamedFrom("checkbox_enable_post_stream_toast");
    public static final SettingKey<Boolean>
            GAME_MODE_INTEGRATION_DISABLED =
            SettingKey.booleanKey(
                    "stream.ui.game_mode_integration_disabled",
                    false)
                    .renamedFrom("checkbox_enable_game_manager_quest");

    private StreamUiSettingKeys() {
    }
}
