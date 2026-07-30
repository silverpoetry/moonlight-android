package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Persisted keys that form the atomic stream resolution selection.
 */
public final class StreamResolutionSettingKeys {
    public static final SettingKey<String> RESOLUTION =
            SettingKey.stringKey(
                    "list_resolution",
                    StreamResolutionCodec.DEFAULT_RESOLUTION);
    public static final SettingKey<String> SELECTION =
            SettingKey.stringKey(
                    "list_resolution_selection",
                    StreamResolutionCodec.SELECTION_PRESET);
    public static final SettingKey<String> ASPECT_RATIO =
            SettingKey.stringKey(
                    "list_resolution_aspect_ratio",
                    StreamResolutionCodec.ASPECT_RATIO_16_9);
    public static final SettingKey<String> FPS =
            SettingKey.stringKey(
                    "list_fps",
                    StreamResolutionCodec.DEFAULT_FPS);

    public static final SettingKey<String> LEGACY_RESOLUTION_AND_FPS =
            SettingKey.stringKey("list_resolution_fps", "");

    private StreamResolutionSettingKeys() {
    }
}
