package com.limelight.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.settings.android.SharedPreferencesCustomResolutionRepository;
import com.limelight.settings.stream.CustomResolution;
import com.limelight.settings.stream.CustomResolutionRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class SharedPreferencesCustomResolutionRepositoryTest {
    private static final String PREFERENCES_NAME =
            "custom-resolution-repository-test";
    private static final String RESOLUTIONS_KEY = "resolutions";

    private SharedPreferences preferences;
    private CustomResolutionRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        repository =
                new SharedPreferencesCustomResolutionRepository(
                        preferences);
    }

    @After
    public void tearDown() {
        preferences.edit().clear().commit();
    }

    @Test
    public void loadFiltersMalformedValuesAndReturnsImmutableSnapshot() {
        preferences.edit()
                .putStringSet(
                        RESOLUTIONS_KEY,
                        new HashSet<>(Arrays.asList(
                                "2560x1440",
                                "1920\u00d71080",
                                "malformed",
                                "0x1080")))
                .commit();

        Set<CustomResolution> resolutions = repository.load();

        assertEquals(2, resolutions.size());
        assertTrue(resolutions.contains(
                new CustomResolution(2560, 1440)));
        assertTrue(resolutions.contains(
                new CustomResolution(1920, 1080)));
        try {
            resolutions.clear();
            fail("Repository snapshots must be immutable");
        }
        catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void addAndRemoveUseCanonicalStorageValues() {
        CustomResolution resolution =
                new CustomResolution(3440, 1440);

        repository.add(resolution);

        assertEquals(
                new HashSet<>(Arrays.asList("3440x1440")),
                preferences.getStringSet(
                        RESOLUTIONS_KEY,
                        null));
        assertTrue(repository.load().contains(resolution));

        repository.remove(resolution);

        assertEquals(
                new HashSet<>(),
                preferences.getStringSet(
                        RESOLUTIONS_KEY,
                        new HashSet<>()));
        assertTrue(repository.load().isEmpty());
    }
}
