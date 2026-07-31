package com.limelight.settings.android;

import android.content.SharedPreferences;

import com.limelight.settings.stream.CustomResolution;
import com.limelight.settings.stream.CustomResolutionRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Android persistence adapter for the custom-resolution catalog.
 */
public final class SharedPreferencesCustomResolutionRepository
        implements CustomResolutionRepository {
    public static final String PREFERENCES_NAME =
            "CustomResolutions";
    private static final String RESOLUTIONS_KEY = "resolutions";
    private static final int MAX_VISIBLE_RESOLUTIONS = 256;

    private final SharedPreferences preferences;

    public SharedPreferencesCustomResolutionRepository(
            SharedPreferences preferences) {
        this.preferences = Objects.requireNonNull(
                preferences,
                "preferences");
    }

    @Override
    public Set<CustomResolution> load() {
        Set<String> stored = readStoredValues();
        LinkedHashSet<CustomResolution> resolutions =
                new LinkedHashSet<>();
        for (String value : stored) {
            CustomResolution resolution =
                    CustomResolution.parse(value);
            if (resolution != null) {
                resolutions.add(resolution);
                if (resolutions.size() >=
                        MAX_VISIBLE_RESOLUTIONS) {
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(resolutions);
    }

    @Override
    public void add(CustomResolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        Set<String> stored = readStoredValues();
        stored.add(resolution.toStorageValue());
        writeStoredValues(stored);
    }

    @Override
    public void remove(CustomResolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        Set<String> stored = readStoredValues();
        if (stored.remove(resolution.toStorageValue())) {
            writeStoredValues(stored);
        }
    }

    private Set<String> readStoredValues() {
        Set<String> stored = preferences.getStringSet(
                RESOLUTIONS_KEY,
                null);
        return stored == null
                ? new HashSet<>()
                : new HashSet<>(stored);
    }

    private void writeStoredValues(Set<String> values) {
        preferences.edit()
                .putStringSet(
                        RESOLUTIONS_KEY,
                        new HashSet<>(values))
                .apply();
    }
}
