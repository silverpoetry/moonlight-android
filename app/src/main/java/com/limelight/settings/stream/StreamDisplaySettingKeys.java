package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;

/**
 * Persisted keys used to build a stream display settings snapshot.
 */
public final class StreamDisplaySettingKeys {
    public static final SettingKey<Boolean> STRETCH_VIDEO =
            SettingKey.booleanKey(
                    "stream.display.stretch_video",
                    false)
                    .renamedFrom("checkbox_stretch_video");
    public static final SettingKey<Boolean> DISPLAY_CUTOUT =
            SettingKey.booleanKey(
                    "stream.display.use_cutout_area",
                    false)
                    .renamedFrom("checkbox_cutout_mode_video");
    public static final SettingKey<String> GRAVITY =
            SettingKey.stringKey(
                    "stream.display.gravity",
                    "0")
                    .renamedFrom("screen_gravity_list");

    private StreamDisplaySettingKeys() {
    }
}
