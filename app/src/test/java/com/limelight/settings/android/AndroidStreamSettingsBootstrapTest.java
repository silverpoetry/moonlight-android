package com.limelight.settings.android;

import android.os.Build;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AndroidStreamSettingsBootstrapTest {
    @Test
    public void android12FreshInstallUsesSafeMotionDefault() {
        assertTrue(
                AndroidStreamSettingsBootstrap
                        .shouldDisableMotionSensorsByDefault(
                                Build.VERSION_CODES.S,
                                false));
    }

    @Test
    public void explicitChoiceAndOtherReleasesArePreserved() {
        assertFalse(
                AndroidStreamSettingsBootstrap
                        .shouldDisableMotionSensorsByDefault(
                                Build.VERSION_CODES.S,
                                true));
        assertFalse(
                AndroidStreamSettingsBootstrap
                        .shouldDisableMotionSensorsByDefault(
                                Build.VERSION_CODES.S + 1,
                                false));
    }
}
