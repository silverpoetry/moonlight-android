package com.limelight.settings.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/** Lifecycle owner for observing one typed value in default settings. */
public final class AndroidSettingObserver<T> implements AutoCloseable {
    public interface Listener<T> {
        void onValueChanged(T value);
    }

    private final SharedPreferences preferences;
    private final SettingsRepository repository;
    private final SettingKey<T> key;
    private final Listener<T> listener;
    private final SharedPreferences.OnSharedPreferenceChangeListener
            preferenceListener;
    private boolean started;

    public AndroidSettingObserver(
            Context context,
            SettingKey<T> key,
            Listener<T> listener) {
        preferences = AndroidSettingsRepository.defaultPreferences(
                Objects.requireNonNull(context, "context"));
        repository = new SharedPreferencesSettingsRepository(preferences);
        this.key = Objects.requireNonNull(key, "key");
        this.listener = Objects.requireNonNull(listener, "listener");
        preferenceListener = (source, changedKey) -> {
            if (this.key.getName().equals(changedKey)) {
                publishCurrentValue();
            }
        };
    }

    public void start() {
        if (started) {
            return;
        }
        SettingsMigrationRunner.migrate(repository);
        preferences.registerOnSharedPreferenceChangeListener(
                preferenceListener);
        started = true;
        publishCurrentValue();
    }

    @Override
    public void close() {
        if (!started) {
            return;
        }
        started = false;
        preferences.unregisterOnSharedPreferenceChangeListener(
                preferenceListener);
    }

    private void publishCurrentValue() {
        if (started) {
            listener.onValueChanged(repository.get(key));
        }
    }
}
