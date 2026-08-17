package com.limelight.settings.android;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class AndroidAppLanguageSettingsLoaderTest {
    @Test
    public void freshInstallUsesSystemLanguage() {
        FakeRepository repository = new FakeRepository();

        assertEquals(
                AppPresentationSettingKeys.SYSTEM_LANGUAGE,
                AndroidAppLanguageSettingsLoader.load(repository));
    }

    @Test
    public void legacyLanguageAliasIsMigratedBeforeLoading() {
        FakeRepository repository = new FakeRepository();
        repository.values.put("list_languages", "zh-Hans");

        assertEquals(
                "zh-Hans",
                AndroidAppLanguageSettingsLoader.load(repository));
        assertEquals(
                "zh-Hans",
                repository.get(AppPresentationSettingKeys.LANGUAGE));
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();

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
