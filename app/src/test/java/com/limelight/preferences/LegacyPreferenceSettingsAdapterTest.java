package com.limelight.preferences;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamDisplaySettingKeys;
import com.limelight.settings.stream.StreamDisplaySettings;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyPreferenceSettingsAdapterTest {
    @Test
    public void mapsLegacyBagAndTypedStorageIntoOneImmutableSnapshot() {
        PreferenceConfiguration legacy = new PreferenceConfiguration();
        legacy.width = 2400;
        legacy.height = 1080;
        legacy.resolutionSelection =
                PreferenceConfiguration.ResolutionSelection
                        .CUSTOM_OR_NATIVE;
        legacy.stretchVideo = true;
        legacy.enableCutoutModeVideo = true;
        legacy.enableExDisplay = false;
        legacy.enableHdr = true;

        InMemoryRepository repository = new InMemoryRepository();
        repository.putRaw(StreamDisplaySettingKeys.GRAVITY, "6");
        repository.putRaw(
                StreamDisplaySettingKeys.FSR_TARGET,
                "4k");
        repository.putRaw(
                StreamDisplaySettingKeys.FSR_SHARPNESS,
                "strong");
        repository.putRaw(
                StreamDisplaySettingKeys.FSR_HDR_OUTPUT,
                "native");

        StreamDisplaySettings settings =
                LegacyPreferenceSettingsAdapter
                        .loadStreamDisplaySettings(
                                legacy,
                                repository);

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
    public void malformedGravityCannotCrashStreamComposition() {
        PreferenceConfiguration legacy = new PreferenceConfiguration();
        legacy.width = 1920;
        legacy.height = 1080;
        legacy.resolutionSelection =
                PreferenceConfiguration.ResolutionSelection.PRESET;

        InMemoryRepository repository = new InMemoryRepository();
        repository.putRaw(
                StreamDisplaySettingKeys.GRAVITY,
                "not-an-integer");

        StreamDisplaySettings settings =
                LegacyPreferenceSettingsAdapter
                        .loadStreamDisplaySettings(
                                legacy,
                                repository);

        assertEquals(
                StreamDisplaySettings.Gravity.DEFAULT,
                settings.getGravity());
    }

    private static final class InMemoryRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();

        <T> void putRaw(SettingKey<T> key, Object value) {
            values.put(key.getName(), value);
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
