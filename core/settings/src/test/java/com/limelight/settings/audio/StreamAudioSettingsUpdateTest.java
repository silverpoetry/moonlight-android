package com.limelight.settings.audio;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings.ChannelConfiguration;

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
    }

    @Test
    public void mutedUpdateChangesOnlyItsOwnedField() {
        StreamAudioSettings original = representativeSettings();
        StreamAudioSettings updated =
                StreamAudioSettingsUpdate.muted(false).applyTo(original);

        assertFalse(updated.isMuted());
        assertEquals(
                original.getChannelConfiguration(),
                updated.getChannelConfiguration());
        assertEquals(
                original.shouldPlayHostAudio(),
                updated.shouldPlayHostAudio());
        assertEquals(
                original.areAudioEffectsEnabled(),
                updated.areAudioEffectsEnabled());
    }

    @Test
    public void updatesPersistOnlyTheirCanonicalKey() {
        FakeRepository repository = new FakeRepository();
        StreamAudioSettingsUpdate update =
                StreamAudioSettingsUpdate.playHostAudio(false);

        StreamAudioSettings updated =
                update.applyTo(representativeSettings());
        update.persist(repository);

        assertFalse(updated.shouldPlayHostAudio());
        assertTrue(updated.areAudioEffectsEnabled());
        assertEquals(
                false,
                repository.values.get(
                        StreamAudioSettingKeys.PLAY_HOST_AUDIO.getName()));
        assertEquals(1, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    private static StreamAudioSettings representativeSettings() {
        return StreamAudioSettings.builder()
                .setChannelConfiguration(ChannelConfiguration.SURROUND_7_1)
                .setPlayHostAudio(true)
                .setAudioEffectsEnabled(true)
                .setMuted(true)
                .build();
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();
        private int applyCount;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public <T> Editor put(SettingKey<T> key, T value) {
                    values.put(key.getName(), key.normalizeValue(value));
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
