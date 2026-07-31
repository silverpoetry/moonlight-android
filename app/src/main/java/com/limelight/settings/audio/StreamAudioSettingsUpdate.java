package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings
        .HapticsOutputTarget;
import com.limelight.settings.audio.StreamAudioSettings.VoiceFilter;

import java.util.Objects;

/**
 * One type-safe live stream-audio settings intent.
 */
public final class StreamAudioSettingsUpdate {
    private interface Applier {
        StreamAudioSettings apply(StreamAudioSettings settings);
    }

    private interface Persister {
        void persist(SettingsRepository.Editor editor);
    }

    private final Applier applier;
    private final Persister persister;

    private StreamAudioSettingsUpdate(
            Applier applier,
            Persister persister) {
        this.applier = Objects.requireNonNull(applier, "applier");
        this.persister =
                Objects.requireNonNull(persister, "persister");
    }

    public static StreamAudioSettingsUpdate muted(
            boolean muted) {
        return single(
                StreamAudioSettingKeys.MUTED,
                muted,
                StreamAudioSettings.Builder::setMuted);
    }

    public static StreamAudioSettingsUpdate hapticsEnabled(
            boolean enabled) {
        return single(
                StreamAudioSettingKeys.AUDIO_HAPTICS,
                enabled,
                StreamAudioSettings.Builder
                        ::setAudioHapticsEnabled);
    }

    public static StreamAudioSettingsUpdate hapticsOutputTarget(
            HapticsOutputTarget target) {
        HapticsOutputTarget normalized =
                Objects.requireNonNull(target, "target");
        String stored = StreamAudioSettingsCodec
                .encodeHapticsOutputTarget(normalized);
        stored = StreamAudioSettingKeys
                .AUDIO_HAPTICS_OUTPUT_TARGET
                .normalizeValue(stored);
        HapticsOutputTarget applied =
                StreamAudioSettingsCodec
                        .decodeHapticsOutputTarget(stored);
        String persisted = stored;
        return new StreamAudioSettingsUpdate(
                settings -> settings.toBuilder()
                        .setHapticsOutputTarget(applied)
                        .build(),
                editor -> editor.put(
                        StreamAudioSettingKeys
                                .AUDIO_HAPTICS_OUTPUT_TARGET,
                        persisted));
    }

    public static StreamAudioSettingsUpdate hapticsStrengthPercent(
            int percent) {
        return single(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_STRENGTH_PERCENT,
                percent,
                StreamAudioSettings.Builder
                        ::setHapticsStrengthPercent);
    }

    public static StreamAudioSettingsUpdate voiceFilter(
            VoiceFilter filter) {
        VoiceFilter normalized =
                Objects.requireNonNull(filter, "filter");
        String stored = StreamAudioSettingsCodec
                .encodeVoiceFilter(normalized);
        stored = StreamAudioSettingKeys
                .AUDIO_HAPTICS_VOICE_FILTER
                .normalizeValue(stored);
        VoiceFilter applied =
                StreamAudioSettingsCodec.decodeVoiceFilter(stored);
        String persisted = stored;
        return new StreamAudioSettingsUpdate(
                settings -> settings.toBuilder()
                        .setVoiceFilter(applied)
                        .build(),
                editor -> editor.put(
                        StreamAudioSettingKeys
                                .AUDIO_HAPTICS_VOICE_FILTER,
                        persisted));
    }

    public static StreamAudioSettingsUpdate keepControllerRumble(
            boolean keep) {
        return single(
                StreamAudioSettingKeys
                        .KEEP_CONTROLLER_RUMBLE_WITH_AUDIO_HAPTICS,
                keep,
                StreamAudioSettings.Builder
                        ::setKeepControllerRumble);
    }

    public StreamAudioSettings applyTo(
            StreamAudioSettings settings) {
        return applier.apply(
                Objects.requireNonNull(settings, "settings"));
    }

    public void persist(SettingsRepository repository) {
        SettingsRepository.Editor editor =
                Objects.requireNonNull(repository, "repository")
                        .edit();
        persister.persist(editor);
        editor.apply();
    }

    private interface ValueApplier<T> {
        StreamAudioSettings.Builder apply(
                StreamAudioSettings.Builder builder,
                T value);
    }

    private static <T> StreamAudioSettingsUpdate single(
            SettingKey<T> key,
            T value,
            ValueApplier<T> applier) {
        T normalized = key.normalizeValue(value);
        return new StreamAudioSettingsUpdate(
                settings -> applier.apply(
                                settings.toBuilder(),
                                normalized)
                        .build(),
                editor -> editor.put(key, normalized));
    }

}
