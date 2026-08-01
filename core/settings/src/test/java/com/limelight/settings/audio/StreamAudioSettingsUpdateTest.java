package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings
        .ChannelConfiguration;
import com.limelight.settings.audio.StreamAudioSettings
        .HapticsOutputTarget;
import com.limelight.settings.audio.StreamAudioSettings.VoiceFilter;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamAudioSettingsUpdateTest {
    @Test
    public void copyBuilderPreservesWholeSnapshot() {
        StreamAudioSettings original = representativeSettings();

        StreamAudioSettings copy = original.toBuilder().build();

        assertEquals(
                ChannelConfiguration.SURROUND_7_1,
                copy.getChannelConfiguration());
        assertTrue(copy.shouldPlayHostAudio());
        assertTrue(copy.areAudioEffectsEnabled());
        assertTrue(copy.isMuted());
        assertTrue(copy.areAudioHapticsEnabled());
        assertEquals(
                HapticsOutputTarget.CONTROLLER,
                copy.getHapticsOutputTarget());
        assertEquals(175, copy.getHapticsStrengthPercent());
        assertEquals(VoiceFilter.HIGH, copy.getVoiceFilter());
        assertTrue(copy.shouldKeepControllerRumble());
    }

    @Test
    public void scalarUpdateChangesOnlyItsOwnedField() {
        StreamAudioSettings original = representativeSettings();
        StreamAudioSettingsUpdate update =
                StreamAudioSettingsUpdate.muted(false);

        StreamAudioSettings updated = update.applyTo(original);

        assertFalse(updated.isMuted());
        assertEquals(
                original.getChannelConfiguration(),
                updated.getChannelConfiguration());
        assertEquals(
                original.getHapticsOutputTarget(),
                updated.getHapticsOutputTarget());
        assertEquals(
                original.getHapticsStrengthPercent(),
                updated.getHapticsStrengthPercent());
        assertEquals(
                original.getVoiceFilter(),
                updated.getVoiceFilter());
        assertEquals(
                original.shouldKeepControllerRumble(),
                updated.shouldKeepControllerRumble());
    }

    @Test
    public void hapticsTargetUsesCanonicalCodec() {
        FakeRepository repository = new FakeRepository();
        StreamAudioSettingsUpdate update =
                StreamAudioSettingsUpdate.hapticsOutputTarget(
                        HapticsOutputTarget.PHONE);

        StreamAudioSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertEquals(
                HapticsOutputTarget.PHONE,
                updated.getHapticsOutputTarget());
        assertEquals(
                "phone",
                repository.values.get(
                        StreamAudioSettingKeys
                                .AUDIO_HAPTICS_OUTPUT_TARGET
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void voiceFilterUsesCanonicalCodec() {
        FakeRepository repository = new FakeRepository();
        StreamAudioSettingsUpdate update =
                StreamAudioSettingsUpdate.voiceFilter(
                        VoiceFilter.MEDIUM);

        StreamAudioSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertEquals(VoiceFilter.MEDIUM, updated.getVoiceFilter());
        assertEquals(
                "medium",
                repository.values.get(
                        StreamAudioSettingKeys
                                .AUDIO_HAPTICS_VOICE_FILTER
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void strengthIsNormalizedBeforeStateAndStorage() {
        FakeRepository repository = new FakeRepository();
        StreamAudioSettingsUpdate update =
                StreamAudioSettingsUpdate
                        .hapticsStrengthPercent(5_000);

        StreamAudioSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertEquals(200, updated.getHapticsStrengthPercent());
        assertEquals(
                200,
                repository.values.get(
                        StreamAudioSettingKeys
                                .AUDIO_HAPTICS_STRENGTH_PERCENT
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void hostAudioUpdatePreservesPlaybackPolicy() {
        FakeRepository repository = new FakeRepository();
        StreamAudioSettings original = representativeSettings();
        StreamAudioSettingsUpdate update =
                StreamAudioSettingsUpdate.playHostAudio(false);

        StreamAudioSettings updated = update.applyTo(original);
        update.persist(repository);

        assertFalse(updated.shouldPlayHostAudio());
        assertTrue(updated.areAudioEffectsEnabled());
        assertTrue(updated.areAudioHapticsEnabled());
        assertEquals(
                HapticsOutputTarget.CONTROLLER,
                updated.getHapticsOutputTarget());
        assertEquals(
                false,
                repository.values.get(
                        StreamAudioSettingKeys
                                .PLAY_HOST_AUDIO
                                .getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    private static StreamAudioSettings representativeSettings() {
        return StreamAudioSettings.builder()
                .setChannelConfiguration(
                        ChannelConfiguration.SURROUND_7_1)
                .setPlayHostAudio(true)
                .setAudioEffectsEnabled(true)
                .setMuted(true)
                .setAudioHaptics(
                        true,
                        HapticsOutputTarget.CONTROLLER,
                        175,
                        VoiceFilter.HIGH,
                        true)
                .build();
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();
        private int applyCount;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(
                    values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public <T> Editor put(
                        SettingKey<T> key,
                        T value) {
                    values.put(
                            key.getName(),
                            key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    values.remove(key.getName());
                    return this;
                }

                @Override
                public void apply() {
                    applyCount++;
                }

                @Override
                public boolean commit() {
                    applyCount++;
                    return true;
                }
            };
        }
    }
}
