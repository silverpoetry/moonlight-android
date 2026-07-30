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

    public static final SettingKey<String> FRAME_PACING =
            SettingKey.stringKey(
                    "frame_pacing",
                    FRAME_PACING_MINIMUM_LATENCY);

    public static final SettingKey<Boolean>
            LEGACY_DISABLE_FRAME_DROP =
            SettingKey.booleanKey(
                    "checkbox_disable_frame_drop",
                    false);

    private StreamDecoderSettingKeys() {
    }
}
