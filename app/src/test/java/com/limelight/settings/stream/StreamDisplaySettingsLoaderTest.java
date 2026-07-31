package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamDisplaySettingsLoaderTest {
    @Test
    public void projectsVideoAndLayoutPolicyWithoutLegacyBag() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                StreamDisplaySettingKeys.GRAVITY,
                "6");
        StreamVideoSettings videoSettings =
                StreamVideoSettings.builder()
                        .setDimensions(2400, 1080)
                        .setNativeResolution(true)
                        .setStretchVideo(true)
                        .setDisplayCutoutEnabled(true)
                        .setExternalDisplay(false)
                        .setHdrEnabled(true)
                        .setFsrTarget(
                                StreamDisplaySettings.FsrTarget
                                        .OUTPUT_4K)
                        .setFsrSharpness(
                                StreamDisplaySettings.FsrSharpness
                                        .STRONG)
                        .setFsrHdrOutput(
                                StreamDisplaySettings.FsrHdrOutput
                                        .NATIVE)
                        .build();

        StreamDisplaySettings settings =
                StreamDisplaySettingsLoader.load(
                        repository,
                        videoSettings);

        assertEquals(2400, settings.getStreamWidth());
        assertEquals(1080, settings.getStreamHeight());
        assertTrue(settings.isNativeResolution());
        assertTrue(settings.isStretchVideo());
        assertTrue(settings.isDisplayCutoutEnabled());
        assertFalse(settings.isExternalDisplayEnabled());
        assertEquals(
                StreamDisplaySettings.Gravity.BOTTOM_RIGHT,
                settings.getGravity());
        assertEquals(
                StreamDisplaySettings.FsrTarget.OUTPUT_4K,
                settings.getFsrTarget());
        assertEquals(
                StreamDisplaySettings.FsrSharpness.STRONG,
                settings.getFsrSharpness());
        assertTrue(settings.isNativeHdrOutputEnabled());
    }

    @Test
    public void malformedGravityFallsBackAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamDisplaySettingKeys.GRAVITY.getName(),
                "future-value");

        StreamDisplaySettings settings =
                StreamDisplaySettingsLoader.load(
                        repository,
                        StreamVideoSettings.builder().build());

        assertEquals(
                StreamDisplaySettings.Gravity.DEFAULT,
                settings.getGravity());
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
