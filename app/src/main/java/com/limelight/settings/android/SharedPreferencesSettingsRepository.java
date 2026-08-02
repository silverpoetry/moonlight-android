package com.limelight.settings.android;

import android.content.SharedPreferences;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Android persistence adapter for the typed settings schema.
 */
public final class SharedPreferencesSettingsRepository
        implements SettingsRepository {
    private final SharedPreferences preferences;

    public SharedPreferencesSettingsRepository(
            SharedPreferences preferences) {
        this.preferences = Objects.requireNonNull(
                preferences,
                "preferences");
    }

    @Override
    public boolean contains(SettingKey<?> key) {
        return preferences.contains(
                Objects.requireNonNull(key, "key").getName());
    }

    @Override
    public <T> T get(SettingKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value;
        try {
            switch (key.getStorageType()) {
                case BOOLEAN:
                    value = preferences.getBoolean(
                            key.getName(),
                            (Boolean) key.getDefaultValue());
                    break;
                case INTEGER:
                    value = preferences.getInt(
                            key.getName(),
                            (Integer) key.getDefaultValue());
                    break;
                case LONG:
                    value = preferences.getLong(
                            key.getName(),
                            (Long) key.getDefaultValue());
                    break;
                case FLOAT:
                    value = preferences.getFloat(
                            key.getName(),
                            (Float) key.getDefaultValue());
                    break;
                case STRING:
                    value = preferences.getString(
                            key.getName(),
                            (String) key.getDefaultValue());
                    break;
                case STRING_SET:
                    value = preferences.getStringSet(
                            key.getName(),
                            asStringSet(key.getDefaultValue()));
                    break;
                default:
                    throw new AssertionError(
                            "Unhandled storage type: " +
                                    key.getStorageType());
            }
        }
        catch (ClassCastException corruptedValue) {
            value = key.getDefaultValue();
        }
        return key.normalizeStoredValue(value);
    }

    @Override
    public Editor edit() {
        return new SharedPreferencesEditor(preferences.edit());
    }

    private static final class SharedPreferencesEditor implements Editor {
        private final SharedPreferences.Editor editor;
        private boolean closed;

        SharedPreferencesEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public <T> Editor put(SettingKey<T> key, T value) {
            ensureOpen();
            Objects.requireNonNull(key, "key");
            Object normalized = key.normalizeValue(value);
            switch (key.getStorageType()) {
                case BOOLEAN:
                    editor.putBoolean(
                            key.getName(),
                            (Boolean) normalized);
                    break;
                case INTEGER:
                    editor.putInt(
                            key.getName(),
                            (Integer) normalized);
                    break;
                case LONG:
                    editor.putLong(
                            key.getName(),
                            (Long) normalized);
                    break;
                case FLOAT:
                    editor.putFloat(
                            key.getName(),
                            (Float) normalized);
                    break;
                case STRING:
                    editor.putString(
                            key.getName(),
                            (String) normalized);
                    break;
                case STRING_SET:
                    editor.putStringSet(
                            key.getName(),
                            new HashSet<>(
                                    asStringSet(normalized)));
                    break;
                default:
                    throw new AssertionError(
                            "Unhandled storage type: " +
                                    key.getStorageType());
            }
            return this;
        }

        @Override
        public Editor remove(SettingKey<?> key) {
            ensureOpen();
            editor.remove(Objects.requireNonNull(
                    key,
                    "key").getName());
            return this;
        }

        @Override
        public void apply() {
            ensureOpen();
            closed = true;
            editor.apply();
        }

        @Override
        public boolean commit() {
            ensureOpen();
            closed = true;
            return editor.commit();
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException(
                        "Settings editor is already closed");
            }
        }
    }

    private static Set<String> asStringSet(Object value) {
        if (!(value instanceof Set<?>)) {
            throw new ClassCastException(
                    "Expected a string set but found " +
                            (value == null ? "null" :
                                    value.getClass().getName()));
        }
        Set<String> strings = new HashSet<>();
        for (Object entry : (Set<?>) value) {
            if (!(entry instanceof String)) {
                throw new ClassCastException(
                        "String set contains a non-string entry");
            }
            strings.add((String) entry);
        }
        return strings;
    }
}
