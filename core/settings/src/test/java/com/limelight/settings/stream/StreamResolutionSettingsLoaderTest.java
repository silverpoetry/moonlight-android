package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamResolutionSettingsLoaderTest {
    private static final StreamResolutionCodec.DisplayAspect DISPLAY =
            new StreamResolutionCodec.DisplayAspect(1920, 1080);

    @Test
    public void legacyAggregateIsReplacedInOneCommittedBatch() {
        FakeRepository repository = new FakeRepository();
        repository.values.put("list_resolution_fps", "1080p60");

        StreamResolutionCodec.Result result =
                StreamResolutionSettingsLoader.load(
                        repository,
                        DISPLAY);

        assertEquals(1920, result.getWidth());
        assertEquals(1080, result.getHeight());
        assertEquals(60, result.getFps());
        assertFalse(repository.values.containsKey(
                "list_resolution_fps"));
        assertEquals("1920x1080", repository.values.get(
                StreamResolutionSettingKeys.RESOLUTION.getName()));
        assertEquals(1, repository.commitCount);
        assertEquals(0, repository.applyCount);
    }

    @Test
    public void missingSelectionPreservesCustomResolutionSemantics() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamResolutionSettingKeys.RESOLUTION.getName(),
                "2000x1000");
        repository.values.put(
                StreamResolutionSettingKeys.FPS.getName(),
                "90");
        repository.values.put(
                StreamResolutionSettingKeys.ASPECT_RATIO.getName(),
                "native");

        StreamResolutionCodec.Result result =
                StreamResolutionSettingsLoader.load(
                        repository,
                        DISPLAY);

        assertEquals(
                StreamResolutionCodec.Selection.CUSTOM_OR_NATIVE,
                result.getSelection());
        assertEquals(2000, result.getWidth());
        assertEquals(1000, result.getHeight());
        assertEquals(1, repository.commitCount);
    }

    @Test
    public void canonicalAggregateDoesNotWriteDuringRead() {
        FakeRepository repository = canonicalRepository();

        StreamResolutionCodec.Result result =
                StreamResolutionSettingsLoader.load(
                        repository,
                        DISPLAY);

        assertFalse(result.isRepairRequired());
        assertEquals(0, repository.commitCount);
        assertEquals(0, repository.applyCount);
    }

    @Test
    public void corruptAggregateRepairsIdempotently() {
        FakeRepository repository = canonicalRepository();
        repository.values.put(
                StreamResolutionSettingKeys.RESOLUTION.getName(),
                "broken");
        repository.values.put(
                StreamResolutionSettingKeys.FPS.getName(),
                "-5");

        StreamResolutionSettingsLoader.load(repository, DISPLAY);
        assertEquals(1, repository.commitCount);
        assertEquals("1280x720", repository.values.get(
                StreamResolutionSettingKeys.RESOLUTION.getName()));
        assertEquals("60", repository.values.get(
                StreamResolutionSettingKeys.FPS.getName()));

        StreamResolutionCodec.Result reread =
                StreamResolutionSettingsLoader.load(
                        repository,
                        DISPLAY);
        assertFalse(reread.isRepairRequired());
        assertEquals(1, repository.commitCount);
    }

    private static FakeRepository canonicalRepository() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                StreamResolutionSettingKeys.RESOLUTION.getName(),
                "1280x720");
        repository.values.put(
                StreamResolutionSettingKeys.SELECTION.getName(),
                "preset");
        repository.values.put(
                StreamResolutionSettingKeys.ASPECT_RATIO.getName(),
                "16_9");
        repository.values.put(
                StreamResolutionSettingKeys.FPS.getName(),
                "60");
        return repository;
    }

    private static final class FakeRepository
            implements SettingsRepository {
        final Map<String, Object> values = new HashMap<>();
        int applyCount;
        int commitCount;

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
            return new FakeEditor();
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> updates =
                    new HashMap<>();
            private final Map<String, Boolean> removals =
                    new HashMap<>();

            @Override
            public <T> Editor put(SettingKey<T> key, T value) {
                updates.put(
                        key.getName(),
                        key.normalizeValue(value));
                removals.remove(key.getName());
                return this;
            }

            @Override
            public Editor remove(SettingKey<?> key) {
                removals.put(key.getName(), true);
                updates.remove(key.getName());
                return this;
            }

            @Override
            public void apply() {
                applyCount++;
                commit();
                commitCount--;
            }

            @Override
            public boolean commit() {
                commitCount++;
                for (String key : removals.keySet()) {
                    values.remove(key);
                }
                values.putAll(updates);
                return true;
            }
        }
    }
}
