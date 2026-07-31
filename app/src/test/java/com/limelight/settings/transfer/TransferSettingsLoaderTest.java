package com.limelight.settings.transfer;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TransferSettingsLoaderTest {
    @Test
    public void missingValuesUseCanonicalDefaults() {
        TransferSettings settings =
                TransferSettingsLoader.load(
                        new FakeRepository());

        assertFalse(settings.isClipboardSyncEnabled());
        assertFalse(settings.hasClipboardFileDirectory());
        assertEquals(
                "",
                settings.getClipboardFileDirectoryUri());
    }

    @Test
    public void storedValuesBuildOneSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.put(
                TransferSettingKeys.CLIPBOARD_SYNC,
                true);
        repository.put(
                TransferSettingKeys
                        .CLIPBOARD_FILE_DIRECTORY_URI,
                "content://documents/tree/downloads");

        TransferSettings settings =
                TransferSettingsLoader.load(repository);

        assertTrue(settings.isClipboardSyncEnabled());
        assertTrue(settings.hasClipboardFileDirectory());
        assertEquals(
                "content://documents/tree/downloads",
                settings.getClipboardFileDirectoryUri());
    }

    @Test
    public void oversizedUriUsesSafeDefault() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(
                TransferSettingKeys
                        .CLIPBOARD_FILE_DIRECTORY_URI
                        .getName(),
                repeat(
                        'x',
                        TransferSettingKeys
                                .MAX_DIRECTORY_URI_LENGTH +
                                1));

        TransferSettings settings =
                TransferSettingsLoader.load(repository);

        assertFalse(settings.hasClipboardFileDirectory());
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
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
