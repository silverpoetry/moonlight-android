package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

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

    public static StreamAudioSettingsUpdate playHostAudio(
            boolean enabled) {
        return single(
                StreamAudioSettingKeys.PLAY_HOST_AUDIO,
                enabled,
                StreamAudioSettings.Builder::setPlayHostAudio);
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
