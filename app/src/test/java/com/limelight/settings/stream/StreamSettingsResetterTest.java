package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamSettingsResetterTest {
    @Test
    public void removesOnlyCrashSensitiveSettings() {
        FakeRepository repository = new FakeRepository();
        repository.put(StreamVideoSettingKeys.BITRATE_KBPS, 20_000);
        repository.put(
                StreamResolutionSettingKeys.RESOLUTION,
                StreamResolutionCodec.RESOLUTION_4K);
        repository.put(StreamResolutionSettingKeys.FPS, "120");
        repository.put(
                StreamResolutionSettingKeys.ASPECT_RATIO,
                StreamResolutionCodec.ASPECT_RATIO_NATIVE);
        repository.put(StreamVideoSettingKeys.HDR_ENABLED, true);
        repository.put(
                StreamDecoderSettingKeys.FULL_RANGE,
                true);

        StreamSettingsResetter.resetAfterDecoderCrashes(
                repository);

        assertFalse(repository.contains(
                StreamVideoSettingKeys.BITRATE_KBPS));
        assertFalse(repository.contains(
                StreamResolutionSettingKeys.RESOLUTION));
        assertFalse(repository.contains(
                StreamResolutionSettingKeys.FPS));
        assertFalse(repository.contains(
                StreamVideoSettingKeys.HDR_ENABLED));
        assertFalse(repository.contains(
                StreamDecoderSettingKeys.FULL_RANGE));
        assertTrue(repository.contains(
                StreamResolutionSettingKeys.ASPECT_RATIO));
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();

        private <T> void put(SettingKey<T> key, T value) {
            values.put(key.getName(), key.normalizeValue(value));
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
                    return true;
                }
            };
        }
    }
}
