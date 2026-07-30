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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public final class PreferenceConfigurationControllerSettingsTest {
    private Context context;
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        preferences =
                PreferenceManager
                        .getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
    }

    @After
    public void tearDown() {
        preferences.edit().clear().commit();
    }

    @Test
    public void legacyUiViewUsesCanonicalControllerSchema() {
        preferences.edit()
                .putInt("seekbar_deadzone", 25)
                .putBoolean("checkbox_multi_controller", false)
                .putBoolean("checkbox_mouse_emulation", false)
                .putInt("mouse_gamepad_sensitity", 175)
                .putBoolean("gameForceGyro", true)
                .putInt("gameForceGyroSensitivity", 150)
                .putString("analog_scrolling", "left")
                .putBoolean("checkbox_enable_audio_haptics", true)
                .putString(
                        "list_audio_haptics_output_target",
                        "controller")
                .commit();

        PreferenceConfiguration configuration =
                PreferenceConfiguration.readPreferences(context);

        assertEquals(25, configuration.deadzonePercentage);
        assertFalse(configuration.multiController);
        assertFalse(configuration.mouseEmulation);
        assertEquals(175, configuration.mouseGamePadSensitity);
        assertTrue(configuration.gameForceGyro);
        assertEquals(150, configuration.gameForceGyroSensitivity);
        assertTrue(configuration.enableAudioHaptics);
        assertEquals(
                "controller",
                configuration.audioHapticsOutputTarget);
    }

    @Test
    public void invalidControllerValuesCannotEscapeTypedBoundary() {
        preferences.edit()
                .putInt("seekbar_deadzone", 5_000)
                .putInt("mouse_gamepad_sensitity", -20)
                .putInt("ax_quick_game_menu_key", 99)
                .putString("analog_scrolling", "broken")
                .putString(
                        "list_audio_haptics_output_target",
                        "broken")
                .commit();

        PreferenceConfiguration configuration =
                PreferenceConfiguration.readPreferences(context);

        assertEquals(50, configuration.deadzonePercentage);
        assertEquals(10, configuration.mouseGamePadSensitity);
        assertEquals(0, configuration.mouseEmulationGameMenu);
        assertEquals(
                "phone",
                configuration.audioHapticsOutputTarget);
    }
}
