package com.limelight.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.settings.android.SharedPreferencesSettingsRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SharedPreferencesSettingsRepositoryTest {
    private static final String PREFERENCES_NAME =
            "typed-settings-repository-test";

    private SharedPreferences preferences;
    private SettingsRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        repository =
                new SharedPreferencesSettingsRepository(preferences);
    }

    @After
    public void tearDown() {
        preferences.edit().clear().commit();
    }

    @Test
    public void missingAndWrongTypeValuesUseSchemaDefault() {
        SettingKey<Integer> count =
                SettingKey.integerKey("count", 4, 0, 10);

        assertFalse(repository.contains(count));
        assertEquals(Integer.valueOf(4), repository.get(count));
        preferences.edit().putString("count", "9").commit();
        assertTrue(repository.contains(count));
        assertEquals(Integer.valueOf(4), repository.get(count));
    }

    @Test
    public void editorNormalizesAndCommitsAtomically() {
        SettingKey<Integer> count =
                SettingKey.integerKey("count", 4, 0, 10);
        SettingKey<Boolean> enabled =
                SettingKey.booleanKey("enabled", false);

        assertTrue(repository.edit()
                .put(count, 99)
                .put(enabled, true)
                .commit());

        assertEquals(Integer.valueOf(10), repository.get(count));
        assertTrue(repository.get(enabled));
    }

    @Test
    public void editorCannotBeReusedAfterTerminalOperation() {
        SettingsRepository.Editor editor = repository.edit();
        editor.apply();

        boolean rejected = false;
        try {
            editor.remove(SettingKey.booleanKey("enabled", false));
        }
        catch (IllegalStateException expected) {
            rejected = true;
        }
        assertTrue(rejected);
        assertFalse(preferences.contains("enabled"));
    }
}
