package com.limelight.computers.apps.android;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.computers.apps.HiddenAppSelection;
import com.limelight.computers.model.HostId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class SharedPreferencesHiddenAppRepositoryTest {
    private Context context;
    private SharedPreferences preferences;

    @Before
    public void clearBefore() {
        context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(
                SharedPreferencesHiddenAppRepository.PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @After
    public void clearAfter() {
        preferences.edit().clear().commit();
    }

    @Test
    public void savesAndLoadsCanonicalHostSelection() {
        HostId hostId = HostId.of("host-a");
        SharedPreferencesHiddenAppRepository repository =
                new SharedPreferencesHiddenAppRepository(context);

        repository.save(
                hostId,
                HiddenAppSelection.of(Arrays.asList(9, 2, 4)));
        HiddenAppSelection restored =
                new SharedPreferencesHiddenAppRepository(context)
                        .load(hostId);

        assertEquals(
                new HashSet<>(Arrays.asList(2, 4, 9)),
                restored.getAppIds());
    }

    @Test
    public void migratesCaseVariantLegacyKeyBeforeCleanup() {
        preferences.edit()
                .putStringSet(
                        "HOST-A",
                        new HashSet<>(Arrays.asList(
                                "4", "invalid", "-1", "2")))
                .commit();

        HiddenAppSelection restored =
                new SharedPreferencesHiddenAppRepository(context)
                        .load(HostId.of("host-a"));

        assertEquals(
                new HashSet<>(Arrays.asList(-1, 2, 4)),
                restored.getAppIds());
        assertTrue(preferences.contains("host-a"));
        assertFalse(preferences.contains("HOST-A"));
    }

    @Test
    public void canonicalValueWinsAndCleansStaleAlias() {
        preferences.edit()
                .putStringSet(
                        "host-a",
                        new HashSet<>(Arrays.asList("3")))
                .putStringSet(
                        "HOST-A",
                        new HashSet<>(Arrays.asList("8")))
                .commit();

        HiddenAppSelection restored =
                new SharedPreferencesHiddenAppRepository(context)
                        .load(HostId.of("host-a"));

        assertEquals(
                new HashSet<>(Arrays.asList(3)),
                restored.getAppIds());
        assertFalse(preferences.contains("HOST-A"));
    }

    @Test
    public void wrongTypedCanonicalValueFailsClosed() {
        preferences.edit().putInt("host-a", 7).commit();

        HiddenAppSelection restored =
                new SharedPreferencesHiddenAppRepository(context)
                        .load(HostId.of("host-a"));

        assertTrue(restored.getAppIds().isEmpty());
    }

    @Test
    public void deleteRemovesCanonicalAndCaseVariantKeys() {
        preferences.edit()
                .putStringSet(
                        "host-a",
                        new HashSet<>(Arrays.asList("3")))
                .putStringSet(
                        "HOST-A",
                        new HashSet<>(Arrays.asList("8")))
                .commit();

        new SharedPreferencesHiddenAppRepository(context)
                .delete(HostId.of("host-a"));

        assertFalse(preferences.contains("host-a"));
        assertFalse(preferences.contains("HOST-A"));
    }
}
