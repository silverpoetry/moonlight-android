package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;
import com.limelight.settings.stream.StreamVideoSettings.ScreenOnPolicy;
import com.limelight.settings.stream.StreamVideoSettings.VirtualDisplayMode;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamVideoSettingsLoaderTest {
    private static final StreamResolutionCodec.DisplayAspect
            DISPLAY_ASPECT =
            new StreamResolutionCodec.DisplayAspect(16, 9);

    @Test
    public void missingValuesUseCanonicalDefaults() {
        FakeRepository repository = new FakeRepository();

        StreamVideoSettings settings =
                StreamVideoSettingsLoader.load(
                        repository,
                        DISPLAY_ASPECT);

        assertEquals(1280, settings.getWidth());
        assertEquals(720, settings.getHeight());
        assertEquals(60, settings.getFps());
        assertEquals(10_000, settings.getBitrateKbps());
        assertEquals(VideoFormat.AUTO, settings.getVideoFormat());
        assertFalse(settings.isHdrEnabled());
        assertFalse(settings.isHdrHighBrightnessEnabled());
        assertFalse(settings.shouldIgnoreHdrCapability());
        assertTrue(settings.isLowLatencyExperimentEnabled());
        assertFalse(settings.isPortrait());
        assertFalse(settings.isExternalDisplay());
        assertFalse(settings.isNativeResolution());
        assertTrue(settings.shouldOptimizeGameSettings());
        assertEquals(
                VirtualDisplayMode.DISABLED,
                settings.getVirtualDisplayMode());
        assertFalse(settings.shouldEnforceDisplayMode());
        assertEquals(
                ScreenOnPolicy.DISABLED,
                settings.getScreenOnPolicy());
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void storedValuesBuildOneCoherentSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamResolutionSettingKeys.RESOLUTION,
                "2560x1440");
        repository.put(
                StreamResolutionSettingKeys.SELECTION,
                StreamResolutionCodec.SELECTION_CUSTOM_OR_NATIVE);
        repository.put(
                StreamResolutionSettingKeys.ASPECT_RATIO,
                StreamResolutionCodec.ASPECT_RATIO_16_9);
        repository.put(StreamResolutionSettingKeys.FPS, "120");
        repository.put(
                StreamVideoSettingKeys.BITRATE_KBPS,
                80_000);
        repository.put(
                StreamVideoSettingKeys.VIDEO_FORMAT,
                "forceav1");
        repository.put(
                StreamVideoSettingKeys.HDR_ENABLED,
                true);
        repository.put(
                StreamVideoSettingKeys.HDR_HIGH_BRIGHTNESS,
                true);
        repository.put(
                StreamVideoSettingKeys.IGNORE_HDR_CAPABILITY,
                true);
        repository.put(
                StreamVideoSettingKeys.LOW_LATENCY_EXPERIMENT,
                false);
        repository.put(StreamVideoSettingKeys.PORTRAIT, true);
        repository.put(
                StreamVideoSettingKeys.EXTERNAL_DISPLAY,
                true);
        repository.put(
                StreamDisplaySettingKeys.STRETCH_VIDEO,
                true);
        repository.put(
                StreamDisplaySettingKeys.DISPLAY_CUTOUT,
                true);
        repository.put(
                StreamVideoSettingKeys.OPTIMIZE_GAME_SETTINGS,
                false);
        repository.put(
                StreamVideoSettingKeys.VIRTUAL_DISPLAY_MODE,
                2);
        repository.put(
                StreamVideoSettingKeys.ENFORCE_DISPLAY_MODE,
                true);
        repository.put(
                StreamVideoSettingKeys.SCREEN_ON_POLICY,
                2);
        StreamVideoSettings settings =
                StreamVideoSettingsLoader.load(
                        repository,
                        DISPLAY_ASPECT);

        assertEquals(2560, settings.getWidth());
        assertEquals(1440, settings.getHeight());
        assertEquals(120, settings.getFps());
        assertEquals(80_000, settings.getBitrateKbps());
        assertEquals(
                VideoFormat.FORCE_AV1,
                settings.getVideoFormat());
        assertTrue(settings.isHdrEnabled());
        assertTrue(settings.isHdrHighBrightnessEnabled());
        assertTrue(settings.shouldIgnoreHdrCapability());
        assertFalse(settings.isLowLatencyExperimentEnabled());
        assertTrue(settings.isPortrait());
        assertTrue(settings.isExternalDisplay());
        assertFalse(settings.isNativeResolution());
        assertFalse(settings.shouldOptimizeGameSettings());
        assertEquals(
                VirtualDisplayMode.VIRTUAL_ONLY,
                settings.getVirtualDisplayMode());
        assertTrue(settings.shouldEnforceDisplayMode());
        assertEquals(
                ScreenOnPolicy.ALWAYS,
                settings.getScreenOnPolicy());
    }

    @Test
    public void invalidValuesAreNormalizedAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamVideoSettingKeys.VIDEO_FORMAT.getName(),
                "broken");
        repository.values.put(
                StreamVideoSettingKeys
                        .VIRTUAL_DISPLAY_MODE
                        .getName(),
                99);
        repository.values.put(
                StreamVideoSettingKeys
                        .SCREEN_ON_POLICY
                        .getName(),
                -1);
        repository.values.put(
                StreamVideoSettingKeys.BITRATE_KBPS.getName(),
                Integer.MAX_VALUE);
        StreamVideoSettings settings =
                StreamVideoSettingsLoader.load(
                        repository,
                        DISPLAY_ASPECT);

        assertEquals(VideoFormat.AUTO, settings.getVideoFormat());
        assertEquals(
                VirtualDisplayMode.DISABLED,
                settings.getVirtualDisplayMode());
        assertEquals(
                ScreenOnPolicy.DISABLED,
                settings.getScreenOnPolicy());
        assertEquals(
                StreamVideoSettingKeys.MAX_BITRATE_KBPS,
                settings.getBitrateKbps());
    }

    @Test
    public void customNonPresetDimensionsAreNativeResolution() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamResolutionSettingKeys.RESOLUTION,
                "2400x1080");
        repository.put(
                StreamResolutionSettingKeys.SELECTION,
                StreamResolutionCodec
                        .SELECTION_CUSTOM_OR_NATIVE);

        StreamVideoSettings settings =
                StreamVideoSettingsLoader.load(
                        repository,
                        DISPLAY_ASPECT);

        assertTrue(settings.isNativeResolution());
    }

    @Test
    public void statePublishesWholeReplacementSnapshot() {
        StreamVideoSettingsState state =
                new StreamVideoSettingsState(
                        StreamVideoSettings.builder().build());
        StreamVideoSettings replacement =
                StreamVideoSettings.builder()
                        .setDimensions(2560, 1440)
                        .setFps(120)
                        .setBitrateKbps(80_000)
                        .setHdrEnabled(true)
                        .build();

        state.replace(replacement);

        assertEquals(2560, state.get().getWidth());
        assertEquals(120, state.get().getFps());
        assertEquals(80_000, state.get().getBitrateKbps());
        assertTrue(state.get().isHdrEnabled());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();
        private int commitCount;

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
                }

                @Override
                public boolean commit() {
                    commitCount++;
                    return true;
                }
            };
        }
    }
}
