package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;

/**
 * Persisted stream-audio settings and their migration-only predecessors.
 */
public final class StreamAudioSettingKeys {
    public static final SettingKey<String> CHANNEL_CONFIGURATION =
            SettingKey.stringSetKey(
                    "list_audio_config",
                    "2",
                    "2",
                    "51",
                    "71");
    public static final SettingKey<Boolean> PLAY_HOST_AUDIO =
            SettingKey.booleanKey(
                    "checkbox_host_audio",
                    false);
    public static final SettingKey<Boolean> AUDIO_EFFECTS =
            SettingKey.booleanKey(
                    "checkbox_enable_audiofx",
                    false);
    public static final SettingKey<Boolean> MUTED =
            SettingKey.booleanKey(
                    "ax_audio_mute",
                    false);
    public static final SettingKey<Boolean> AUDIO_HAPTICS =
            SettingKey.booleanKey(
                    "checkbox_enable_audio_haptics",
                    false);
    public static final SettingKey<String>
            AUDIO_HAPTICS_OUTPUT_TARGET =
            SettingKey.stringSetKey(
                    "list_audio_haptics_output_target",
                    "phone",
                    "phone",
                    "controller");
    public static final SettingKey<Integer>
            AUDIO_HAPTICS_STRENGTH_PERCENT =
            SettingKey.integerKey(
                    "seekbar_audio_haptics_strength",
                    100,
                    25,
                    200);
    public static final SettingKey<String>
            AUDIO_HAPTICS_VOICE_FILTER =
            SettingKey.stringSetKey(
                    "list_audio_haptics_voice_filter",
                    "off",
                    "off",
                    "low",
                    "medium",
                    "high");
    public static final SettingKey<Boolean>
            KEEP_CONTROLLER_RUMBLE_WITH_AUDIO_HAPTICS =
            SettingKey.booleanKey(
                    "checkbox_audio_haptics_keep_controller_rumble",
                    false);

    public static final SettingKey<Boolean>
            LEGACY_ENABLE_51_SURROUND =
            SettingKey.booleanKey(
                    "checkbox_51_surround",
                    false);

    private StreamAudioSettingKeys() {
    }
}
