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
public final class
        PreferenceConfigurationVirtualControlSettingsTest {
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
    public void legacyUiViewUsesCanonicalVirtualControlSchema() {
        preferences.edit()
                .putInt("seekbar_osc_opacity", 70)
                .putInt("seekbar_keyboard_axi_opacity", 65)
                .putInt("seekbar_keyboard_axi_height", 280)
                .putBoolean("checkbox_vibrate_keyboard", true)
                .putInt("onscreen_game_pad_skin", 2)
                .putBoolean(
                        "checkbox_enable_keyboard_square",
                        true)
                .putBoolean(
                        "checkbox_enable_analog_stick_new",
                        true)
                .putInt(
                        "seekbar_osc_free_analog_stick_opacity",
                        45)
                .putBoolean(
                        "checkbox_enable_analog_stick_new_fixed",
                        true)
                .putInt(
                        "virtual_key_view_normal_color",
                        0xF0000000)
                .putInt("virtualGamePadScaleFactor", 150)
                .putBoolean("checkbox_rocker_click_L3R3", true)
                .putBoolean(
                        "checkbox_auto_screen_orientation",
                        true)
                .putBoolean(
                        "checkbox_enable_keyboard_axi_combination",
                        true)
                .commit();

        PreferenceConfiguration configuration =
                PreferenceConfiguration.readPreferences(context);

        assertEquals(70, configuration.oscOpacity);
        assertEquals(65, configuration.oscKeyboardOpacity);
        assertEquals(280, configuration.oscKeyboardHeight);
        assertTrue(configuration.enableKeyboardVibrate);
        assertEquals(2, configuration.gamepad_skin);
        assertTrue(configuration.enableKeyboardSquare);
        assertTrue(configuration.enableNewAnalogStick);
        assertEquals(
                45,
                configuration.senableNewAnalogStickOpacity);
        assertTrue(
                configuration.senableNewAnalogStickOpacityFixed);
        assertEquals(
                0xF0000000,
                configuration.virtualkeyViewNormalColor);
        assertEquals(
                150,
                configuration.virtualGamePadScaleFactor);
        assertTrue(configuration.disableRockerClickL3R3);
        assertTrue(configuration.autoScreenOrientation);
        assertTrue(configuration.keyboard_axi_combination);
    }

    @Test
    public void invalidVirtualControlValuesCannotEscapeTypedBoundary() {
        preferences.edit()
                .putInt("seekbar_osc_opacity", 500)
                .putInt("seekbar_keyboard_axi_height", -1)
                .putInt("onscreen_game_pad_skin", 99)
                .putInt("virtualGamePadScaleFactor", 1)
                .commit();

        PreferenceConfiguration configuration =
                PreferenceConfiguration.readPreferences(context);

        assertEquals(100, configuration.oscOpacity);
        assertEquals(100, configuration.oscKeyboardHeight);
        assertEquals(0, configuration.gamepad_skin);
        assertEquals(
                20,
                configuration.virtualGamePadScaleFactor);
        assertFalse(configuration.enableKeyboardVibrate);
    }
}
