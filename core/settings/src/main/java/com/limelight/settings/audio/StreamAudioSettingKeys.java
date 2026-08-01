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
    public static final SettingKey<Boolean> AUDIO_HAPTICS =
            SettingKey.booleanKey(
                    "stream.haptics.audio.enabled",
                    false)
                    .renamedFrom("checkbox_enable_audio_haptics");
    public static final SettingKey<String>
            AUDIO_HAPTICS_OUTPUT_TARGET =
            SettingKey.stringSetKey(
                    "stream.haptics.audio.output_target",
                    "phone",
                    "phone",
                    "controller")
                    .renamedFrom("list_audio_haptics_output_target");
    public static final SettingKey<Integer>
            AUDIO_HAPTICS_STRENGTH_PERCENT =
            SettingKey.integerKey(
                    "stream.haptics.audio.strength_percent",
                    100,
                    25,
                    200)
                    .renamedFrom("seekbar_audio_haptics_strength");
    public static final SettingKey<String>
            AUDIO_HAPTICS_VOICE_FILTER =
            SettingKey.stringSetKey(
                    "stream.haptics.audio.voice_filter",
                    "off",
                    "off",
                    "low",
                    "medium",
                    "high")
                    .renamedFrom("list_audio_haptics_voice_filter");
    public static final SettingKey<Boolean>
            KEEP_CONTROLLER_RUMBLE_WITH_AUDIO_HAPTICS =
            SettingKey.booleanKey(
                    "stream.haptics.audio.keep_controller_rumble",
                    false)
                    .renamedFrom(
                            "checkbox_audio_haptics_keep_controller_rumble");

    public static final SettingKey<Boolean>
            LEGACY_ENABLE_51_SURROUND =
            SettingKey.booleanKey(
                    "checkbox_51_surround",
                    false);

    private StreamAudioSettingKeys() {
    }
}
