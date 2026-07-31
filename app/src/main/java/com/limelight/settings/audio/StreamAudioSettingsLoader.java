package com.limelight.settings.audio;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings
        .ChannelConfiguration;
import com.limelight.settings.audio.StreamAudioSettings
        .HapticsOutputTarget;
import com.limelight.settings.audio.StreamAudioSettings.VoiceFilter;

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

    private static ChannelConfiguration
            parseChannelConfiguration(String value) {
        if ("71".equals(value)) {
            return ChannelConfiguration.SURROUND_7_1;
        }
        if ("51".equals(value)) {
            return ChannelConfiguration.SURROUND_5_1;
        }
        return ChannelConfiguration.STEREO;
    }

    private static HapticsOutputTarget parseOutputTarget(
            String value) {
        return "controller".equals(value)
                ? HapticsOutputTarget.CONTROLLER
                : HapticsOutputTarget.PHONE;
    }

    private static VoiceFilter parseVoiceFilter(String value) {
        if ("low".equals(value)) {
            return VoiceFilter.LOW;
        }
        if ("medium".equals(value)) {
            return VoiceFilter.MEDIUM;
        }
        if ("high".equals(value)) {
            return VoiceFilter.HIGH;
        }
        return VoiceFilter.OFF;
    }
}
