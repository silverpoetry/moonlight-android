package com.limelight.preferences;

import android.content.Context;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidSettingsRepository;

import java.math.BigDecimal;
import java.util.Objects;

/** Typed persistence adapter used by the settings screen. */
final class SettingsStore implements SettingsValueReader {
    final SettingsRepository repository;

    SettingsStore(Context context) {
        this(AndroidSettingsRepository.create(context));
    }

    SettingsStore(SettingsRepository repository) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository");
    }

    @Override
    public boolean getBoolean(SettingsItem item) {
        return repository.get(item.booleanKey());
    }

    <T> T get(SettingKey<T> key) {
        return repository.get(key);
    }

    @Override
    public int getInt(SettingsItem item) {
        int value = repository.get(item.integerKey());
        if (value == 0 && item.displayDefaultInteger != null) {
            return item.displayDefaultInteger;
        }
        return value;
    }

    @Override
    public String getString(SettingsItem item) {
        return repository.get(item.stringKey());
    }

    @Override
    public String getText(SettingsItem item) {
        if (!item.isCustomBitrateEditor()) {
            return getString(item);
        }
        int bitrateKbps = getInt(item);
        return BigDecimal.valueOf(bitrateKbps, 3)
                .stripTrailingZeros()
                .toPlainString();
    }

    void putBoolean(SettingsItem item, boolean value) {
        put(item.booleanKey(), value);
    }

    void putInt(SettingsItem item, int value) {
        put(item.integerKey(), value);
    }

    void putString(SettingsItem item, String value) {
        put(item.stringKey(), value);
    }

    <T> void put(SettingKey<T> key, T value) {
        repository.edit()
                .put(key, value)
                .apply();
    }
}
