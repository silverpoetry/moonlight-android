package com.limelight.settings.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Lifecycle owner for a coalesced group of related default-setting changes.
 *
 * <p>SharedPreferences can notify once per key even when a logical settings
 * update spans several keys. This adapter posts a single main-thread callback
 * so the consumer can rebuild one immutable domain snapshot from the
 * repository instead of applying partial values independently.</p>
 */
public final class AndroidSettingsGroupObserver implements AutoCloseable {
    private final SharedPreferences preferences;
    private final SettingsRepository repository;
    private final Set<String> observedNames;
    private final Handler mainHandler;
    private final Runnable listener;
    private final Runnable publishRunnable;
    private final SharedPreferences.OnSharedPreferenceChangeListener
            preferenceListener;

    private boolean started;
    private boolean callbackPending;

    public AndroidSettingsGroupObserver(
            Context context,
            Collection<? extends SettingKey<?>> keys,
            Runnable listener) {
        Context checkedContext = Objects.requireNonNull(context, "context");
        Objects.requireNonNull(keys, "keys");
        this.listener = Objects.requireNonNull(listener, "listener");

        observedNames = new HashSet<>();
        for (SettingKey<?> key : keys) {
            observedNames.add(Objects.requireNonNull(key, "key").getName());
        }
        if (observedNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one setting key must be observed");
        }

        preferences = AndroidSettingsRepository.defaultPreferences(
                checkedContext);
        repository = new SharedPreferencesSettingsRepository(preferences);
        mainHandler = new Handler(Looper.getMainLooper());
        publishRunnable = this::publishChanges;
        preferenceListener = (source, changedName) -> {
            if (observedNames.contains(changedName)) {
                scheduleCallback();
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
        scheduleCallback();
    }

    @Override
    public void close() {
        if (!started) {
            return;
        }
        started = false;
        preferences.unregisterOnSharedPreferenceChangeListener(
                preferenceListener);
        mainHandler.removeCallbacks(publishRunnable);
        callbackPending = false;
    }

    private void scheduleCallback() {
        if (!started || callbackPending) {
            return;
        }
        callbackPending = true;
        mainHandler.post(publishRunnable);
    }

    private void publishChanges() {
        callbackPending = false;
        if (started) {
            listener.run();
        }
    }
}
