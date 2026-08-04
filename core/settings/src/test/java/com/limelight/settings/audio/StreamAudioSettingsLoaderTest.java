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

public final class StreamAudioSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        StreamAudioSettings settings =
                StreamAudioSettingsLoader.load(new FakeRepository());

        assertEquals(
                ChannelConfiguration.STEREO,
                settings.getChannelConfiguration());
        assertFalse(settings.shouldPlayHostAudio());
        assertFalse(settings.areAudioEffectsEnabled());
        assertFalse(settings.isMuted());
    }

    @Test
    public void storedValuesBuildOneCoherentSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION,
                "71");
        repository.put(StreamAudioSettingKeys.PLAY_HOST_AUDIO, true);
        repository.put(StreamAudioSettingKeys.AUDIO_EFFECTS, true);
        repository.put(StreamAudioSettingKeys.MUTED, true);

        StreamAudioSettings settings =
                StreamAudioSettingsLoader.load(repository);

        assertEquals(
                ChannelConfiguration.SURROUND_7_1,
                settings.getChannelConfiguration());
        assertTrue(settings.shouldPlayHostAudio());
        assertTrue(settings.areAudioEffectsEnabled());
        assertTrue(settings.isMuted());
    }

    @Test
    public void invalidValuesAreNormalizedAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION.getName(),
                "broken");

        StreamAudioSettings settings =
                StreamAudioSettingsLoader.load(repository);

        assertEquals(
                ChannelConfiguration.STEREO,
                settings.getChannelConfiguration());
    }

    @Test
    public void statePublishesWholeReplacementSnapshot() {
        StreamAudioSettings original =
                StreamAudioSettings.builder().build();
        StreamAudioSettings replacement =
                StreamAudioSettings.builder()
                        .setMuted(true)
                        .setPlayHostAudio(true)
                        .build();
        StreamAudioSettingsState state =
                new StreamAudioSettingsState(original);

        state.replace(replacement);

        assertTrue(state.get().isMuted());
        assertTrue(state.get().shouldPlayHostAudio());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();

        private <T> void put(SettingKey<T> key, T value) {
            values.put(key.getName(), value);
        }

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
            throw new UnsupportedOperationException();
        }
    }
}
