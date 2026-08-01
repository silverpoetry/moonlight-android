package com.limelight.settings.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AndroidSettingsRepositoryTest {
    @Test
    public void preservesHistoricalDefaultPreferencesFileName() {
        assertEquals(
                "com.example.moonlight_preferences",
                AndroidSettingsRepository.defaultPreferencesName(
                        "com.example.moonlight"));
    }
}
