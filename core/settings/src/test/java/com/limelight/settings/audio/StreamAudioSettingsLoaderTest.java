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

public final class StreamAudioSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        StreamAudioSettings settings =
                StreamAudioSettingsLoader.load(
                        new FakeRepository());

        assertEquals(
                ChannelConfiguration.STEREO,
                settings.getChannelConfiguration());
        assertFalse(settings.shouldPlayHostAudio());
        assertFalse(settings.areAudioEffectsEnabled());
        assertFalse(settings.isMuted());
        assertFalse(settings.areAudioHapticsEnabled());
        assertEquals(
                HapticsOutputTarget.PHONE,
                settings.getHapticsOutputTarget());
        assertEquals(100, settings.getHapticsStrengthPercent());
        assertEquals(VoiceFilter.OFF, settings.getVoiceFilter());
        assertFalse(settings.shouldKeepControllerRumble());
    }

    @Test
    public void storedValuesBuildOneCoherentSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION,
                "71");
        repository.put(
                StreamAudioSettingKeys.PLAY_HOST_AUDIO,
                true);
        repository.put(
                StreamAudioSettingKeys.AUDIO_EFFECTS,
                true);
        repository.put(StreamAudioSettingKeys.MUTED, true);
        repository.put(
                StreamAudioSettingKeys.AUDIO_HAPTICS,
                true);
        repository.put(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_OUTPUT_TARGET,
                "controller");
        repository.put(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_STRENGTH_PERCENT,
                175);
        repository.put(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_VOICE_FILTER,
                "high");
        repository.put(
                StreamAudioSettingKeys
                        .KEEP_CONTROLLER_RUMBLE_WITH_AUDIO_HAPTICS,
                true);

        StreamAudioSettings settings =
                StreamAudioSettingsLoader.load(repository);

        assertEquals(
                ChannelConfiguration.SURROUND_7_1,
                settings.getChannelConfiguration());
        assertTrue(settings.shouldPlayHostAudio());
        assertTrue(settings.areAudioEffectsEnabled());
        assertTrue(settings.isMuted());
        assertTrue(settings.areAudioHapticsEnabled());
        assertTrue(settings.isControllerHapticsTarget());
        assertEquals(175, settings.getHapticsStrengthPercent());
        assertEquals(VoiceFilter.HIGH, settings.getVoiceFilter());
        assertTrue(settings.shouldKeepControllerRumble());
    }

    @Test
    public void invalidValuesAreNormalizedAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamAudioSettingKeys
                        .CHANNEL_CONFIGURATION
                        .getName(),
                "broken");
        repository.values.put(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_OUTPUT_TARGET
                        .getName(),
                "broken");
        repository.values.put(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_STRENGTH_PERCENT
                        .getName(),
                5_000);
        repository.values.put(
                StreamAudioSettingKeys
                        .AUDIO_HAPTICS_VOICE_FILTER
                        .getName(),
                "broken");

        StreamAudioSettings settings =
                StreamAudioSettingsLoader.load(repository);

        assertEquals(
                ChannelConfiguration.STEREO,
                settings.getChannelConfiguration());
        assertEquals(
                HapticsOutputTarget.PHONE,
                settings.getHapticsOutputTarget());
        assertEquals(200, settings.getHapticsStrengthPercent());
        assertEquals(VoiceFilter.OFF, settings.getVoiceFilter());
    }

    @Test
    public void statePublishesWholeReplacementSnapshot() {
        StreamAudioSettings original =
                StreamAudioSettings.builder().build();
        StreamAudioSettings replacement =
                StreamAudioSettings.builder()
                        .setMuted(true)
                        .setAudioHaptics(
                                true,
                                HapticsOutputTarget.CONTROLLER,
                                125,
                                VoiceFilter.MEDIUM,
                                true)
                        .build();
        StreamAudioSettingsState state =
                new StreamAudioSettingsState(original);

        state.replace(replacement);

        assertTrue(state.get().isMuted());
        assertTrue(state.get().isControllerHapticsTarget());
        assertEquals(
                125,
                state.get().getHapticsStrengthPercent());
        assertEquals(
                VoiceFilter.MEDIUM,
                state.get().getVoiceFilter());
        assertTrue(state.get().shouldKeepControllerRumble());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();

        private <T> void put(SettingKey<T> key, T value) {
            values.put(key.getName(), value);
        }

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
            throw new UnsupportedOperationException();
        }
    }
}
