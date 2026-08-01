package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.ui.StreamUiSettings;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamDecoderSettingsLoaderTest {
    @Test
    public void composesEveryDecoderValueFromTypedDomains() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamDecoderSettingKeys.FRAME_PACING,
                StreamDecoderSettingKeys.FRAME_PACING_CAP_FPS);
        repository.put(
                StreamDecoderSettingKeys.FULL_RANGE,
                true);
        repository.put(
                StreamDecoderSettingKeys.REDUCE_REFRESH_RATE,
                true);
        StreamVideoSettings videoSettings =
                StreamVideoSettings.builder()
                        .setDimensions(3840, 2160)
                        .setFps(120)
                        .setBitrateKbps(80_000)
                        .setVideoFormat(
                                StreamDecoderSettings.VideoFormat
                                        .FORCE_AV1)
                        .setLowLatencyExperimentEnabled(true)
                        .build();
        StreamAudioSettings audioSettings =
                StreamAudioSettings.builder()
                        .setChannelConfiguration(
                                StreamAudioSettings
                                        .ChannelConfiguration
                                        .SURROUND_5_1)
                        .build();
        StreamUiSettings uiSettings =
                StreamUiSettings.builder()
                        .setPerformanceOverlayEnabled(true)
                        .build();

        StreamDecoderSettings settings =
                StreamDecoderSettingsLoader.load(
                        repository,
                        videoSettings,
                        audioSettings,
                        uiSettings);

        assertEquals(3840, settings.getWidth());
        assertEquals(2160, settings.getHeight());
        assertEquals(120, settings.getFps());
        assertEquals(80_000, settings.getBitrateKbps());
        assertEquals(
                StreamDecoderSettings.VideoFormat.FORCE_AV1,
                settings.getVideoFormat());
        assertEquals(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                settings.getFramePacing());
        assertTrue(settings.isFullRange());
        assertTrue(settings.isLowLatencyExperimentEnabled());
        assertTrue(settings.isPerformanceOverlayEnabled());
        assertEquals(6, settings.getAudioChannelCount());
        assertTrue(settings.isRefreshRateReductionEnabled());
    }

    @Test
    public void unknownFramePacingUsesMinimumLatency() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamDecoderSettingKeys.FRAME_PACING.getName(),
                "future-mode");

        StreamDecoderSettings settings =
                StreamDecoderSettingsLoader.load(
                        repository,
                        StreamVideoSettings.builder().build(),
                        StreamAudioSettings.builder().build(),
                        StreamUiSettings.builder().build());

        assertEquals(
                StreamDecoderSettings.FramePacing.MINIMUM_LATENCY,
                settings.getFramePacing());
        assertFalse(settings.isFullRange());
        assertEquals(2, settings.getAudioChannelCount());
        assertFalse(settings.isRefreshRateReductionEnabled());
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
