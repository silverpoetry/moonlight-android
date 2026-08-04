package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;

/**
 * Persisted stream-audio settings and their migration-only predecessors.
 */
public final class StreamAudioSettingKeys {
    public static final SettingKey<String> CHANNEL_CONFIGURATION =
            SettingKey.stringSetKey(
                    "stream.audio.channel_layout",
                    "2",
                    "2",
                    "51",
                    "71")
                    .renamedFrom("list_audio_config");
    public static final SettingKey<Boolean> PLAY_HOST_AUDIO =
            SettingKey.booleanKey(
                    "stream.audio.play_on_host",
                    false)
                    .renamedFrom("checkbox_host_audio");
    public static final SettingKey<Boolean> AUDIO_EFFECTS =
            SettingKey.booleanKey(
                    "stream.audio.effects",
                    false)
                    .renamedFrom("checkbox_enable_audiofx");
    public static final SettingKey<Boolean> MUTED =
            SettingKey.booleanKey(
                    "stream.audio.muted",
                    false)
                    .renamedFrom("ax_audio_mute");
    public static final SettingKey<Boolean>
            LEGACY_ENABLE_51_SURROUND =
            SettingKey.booleanKey(
                    "checkbox_51_surround",
                    false);

    private StreamAudioSettingKeys() {
    }
}
