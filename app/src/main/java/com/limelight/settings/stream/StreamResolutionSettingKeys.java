package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Persisted keys that form the atomic stream resolution selection.
 */
public final class StreamResolutionSettingKeys {
    public static final SettingKey<String> RESOLUTION =
            SettingKey.stringKey(
                    "stream.video.resolution",
                    StreamResolutionCodec.DEFAULT_RESOLUTION)
                    .renamedFrom("list_resolution");
    public static final SettingKey<String> SELECTION =
            SettingKey.stringKey(
                    "stream.video.resolution_selection",
                    StreamResolutionCodec.SELECTION_PRESET)
                    .renamedFrom("list_resolution_selection");
    public static final SettingKey<String> ASPECT_RATIO =
            SettingKey.stringKey(
                    "stream.video.aspect_ratio",
                    StreamResolutionCodec.ASPECT_RATIO_16_9)
                    .renamedFrom("list_resolution_aspect_ratio");
    public static final SettingKey<String> FPS =
            SettingKey.stringKey(
                    "stream.video.frame_rate",
                    StreamResolutionCodec.DEFAULT_FPS)
                    .renamedFrom("list_fps");

    public static final SettingKey<String> LEGACY_RESOLUTION_AND_FPS =
            SettingKey.stringKey("list_resolution_fps", "");

    private StreamResolutionSettingKeys() {
    }
}
