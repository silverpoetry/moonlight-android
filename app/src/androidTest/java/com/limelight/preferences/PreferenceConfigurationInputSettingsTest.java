package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public final class PreferenceConfigurationInputSettingsTest {
    private Context context;
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
    }

    @After
    public void tearDown() {
        preferences.edit().clear().commit();
    }

    @Test
    public void virtualTouchpadUsesItsOwnCanonicalSensitivityKeys() {
        preferences.edit()
                .putInt(
                        "seekbar_touchpad_sensitivity_opacity",
                        175)
                .putInt(
                        "seekbar_touchpad_sensitivity_y_opacity",
                        225)
                .putInt(
                        "seekbar_mouse_touchpad_sensitivity_x_opacity",
                        75)
                .putInt(
                        "seekbar_mouse_touchpad_sensitivity_y_opacity",
                        80)
                .commit();

        PreferenceConfiguration configuration =
                PreferenceConfiguration.readPreferences(context);

        assertEquals(175, configuration.touchPadSensitivity);
        assertEquals(225, configuration.touchPadYSensitity);
        assertEquals(75, configuration.mouseTouchPadSensitityX);
        assertEquals(80, configuration.mouseTouchPadSensitityY);
    }
}
