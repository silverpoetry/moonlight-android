package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Persisted decoder settings and their migration-only predecessors.
 */
public final class StreamDecoderSettingKeys {
    public static final String FRAME_PACING_MINIMUM_LATENCY =
            "latency";
    public static final String FRAME_PACING_BALANCED =
            "balanced";
    public static final String FRAME_PACING_CAP_FPS =
            "cap-fps";
    public static final String FRAME_PACING_MAX_SMOOTHNESS =
            "smoothness";

    public static final SettingKey<String> FRAME_PACING =
            SettingKey.stringSetKey(
                    "stream.video.frame_pacing",
                    FRAME_PACING_MINIMUM_LATENCY,
                    FRAME_PACING_MINIMUM_LATENCY,
                    FRAME_PACING_BALANCED,
                    FRAME_PACING_CAP_FPS,
                    FRAME_PACING_MAX_SMOOTHNESS)
                    .renamedFrom("frame_pacing");

    public static final SettingKey<Boolean>
            LEGACY_DISABLE_FRAME_DROP =
            SettingKey.booleanKey(
                    "checkbox_disable_frame_drop",
                    false);
    public static final SettingKey<Boolean> FULL_RANGE =
            SettingKey.booleanKey(
                    "stream.video.full_range",
                    false)
                    .renamedFrom("checkbox_full_range");
    public static final SettingKey<Boolean>
            REDUCE_REFRESH_RATE =
            SettingKey.booleanKey(
                    "stream.video.reduce_refresh_rate",
                    false)
                    .renamedFrom("checkbox_reduce_refresh_rate");

    private StreamDecoderSettingKeys() {
    }
}
