package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;

/**
 * Persisted stream-audio settings and their migration-only predecessors.
 */
public final class StreamAudioSettingKeys {
    public static final SettingKey<String> CHANNEL_CONFIGURATION =
            SettingKey.stringKey("list_audio_config", "2");

    public static final SettingKey<Boolean>
            LEGACY_ENABLE_51_SURROUND =
            SettingKey.booleanKey(
                    "checkbox_51_surround",
                    false);

    private StreamAudioSettingKeys() {
    }
}
