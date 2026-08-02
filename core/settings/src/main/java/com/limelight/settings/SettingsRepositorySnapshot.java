package com.limelight.settings;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable value snapshot for detecting repository changes across a lifecycle boundary. */
public final class SettingsRepositorySnapshot {
    private final Map<String, Object> values;

    private SettingsRepositorySnapshot(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public static SettingsRepositorySnapshot capture(
            SettingsRepository repository,
            Collection<? extends SettingKey<?>> keys) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(keys, "keys");
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (SettingKey<?> key : keys) {
            SettingKey<?> checkedKey = Objects.requireNonNull(key, "key");
            values.put(
                    checkedKey.getName(),
                    read(repository, checkedKey));
        }
        return new SettingsRepositorySnapshot(values);
    }

    private static <T> T read(
            SettingsRepository repository,
            SettingKey<T> key) {
        return repository.get(key);
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
                other instanceof SettingsRepositorySnapshot &&
                        values.equals(
                                ((SettingsRepositorySnapshot) other)
                                        .values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
