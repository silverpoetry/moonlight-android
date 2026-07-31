package com.limelight.settings.audio;

import com.limelight.settings.SettingsRepository;
import java.util.Objects;

/**
 * Builds one validated stream-audio snapshot from persistent storage.
 */
public final class StreamAudioSettingsLoader {
    private StreamAudioSettingsLoader() {
    }

    public static StreamAudioSettings load(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");

        return StreamAudioSettings.builder()
                .setChannelConfiguration(parseChannelConfiguration(
                        repository.get(
                                StreamAudioSettingKeys
                                        .CHANNEL_CONFIGURATION)))
                .setPlayHostAudio(repository.get(
                        StreamAudioSettingKeys.PLAY_HOST_AUDIO))
                .setAudioEffectsEnabled(repository.get(
                        StreamAudioSettingKeys.AUDIO_EFFECTS))
                .setMuted(repository.get(
                        StreamAudioSettingKeys.MUTED))
                .setAudioHaptics(
                        repository.get(
                                StreamAudioSettingKeys
                                        .AUDIO_HAPTICS),
                        parseOutputTarget(repository.get(
                                StreamAudioSettingKeys
                                        .AUDIO_HAPTICS_OUTPUT_TARGET)),
                        repository.get(
                                StreamAudioSettingKeys
                                        .AUDIO_HAPTICS_STRENGTH_PERCENT),
                        parseVoiceFilter(repository.get(
                                StreamAudioSettingKeys
                                        .AUDIO_HAPTICS_VOICE_FILTER)),
                        repository.get(
                                StreamAudioSettingKeys
                                        .KEEP_CONTROLLER_RUMBLE_WITH_AUDIO_HAPTICS))
                .build();
    }

    private static StreamAudioSettings.ChannelConfiguration
            parseChannelConfiguration(String value) {
        return StreamAudioSettingsCodec
                .decodeChannelConfiguration(value);
    }

    private static StreamAudioSettings.HapticsOutputTarget
            parseOutputTarget(String value) {
        return StreamAudioSettingsCodec
                .decodeHapticsOutputTarget(value);
    }

    private static StreamAudioSettings.VoiceFilter
            parseVoiceFilter(String value) {
        return StreamAudioSettingsCodec.decodeVoiceFilter(value);
    }
}
