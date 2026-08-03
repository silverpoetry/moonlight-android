package com.limelight.settings.app;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AppPresentationSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        AppPresentationSettings settings =
                AppPresentationSettingsLoader.load(
                        new FakeRepository());

        assertTrue(settings.usesSystemLanguage());
        assertFalse(settings.usesSmallAppIcons());
        assertTrue(settings.usesLightTheme());
    }

    @Test
    public void storedValuesBuildOneCoherentSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                AppPresentationSettingKeys.LANGUAGE,
                "zh-CN");
        repository.put(
                AppPresentationSettingKeys.SMALL_APP_ICONS,
                true);
        repository.put(
                AppPresentationSettingKeys.LIGHT_THEME,
                false);
        AppPresentationSettings settings =
                AppPresentationSettingsLoader.load(repository);

        assertEquals("zh-CN", settings.getLanguage());
        assertFalse(settings.usesSystemLanguage());
        assertTrue(settings.usesSmallAppIcons());
        assertFalse(settings.usesLightTheme());
    }

    @Test
    public void oversizedStringsFallBackAtSchemaBoundary() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                AppPresentationSettingKeys.LANGUAGE.getName(),
                repeat('x', 65));
        AppPresentationSettings settings =
                AppPresentationSettingsLoader.load(repository);

        assertTrue(settings.usesSystemLanguage());
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
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
