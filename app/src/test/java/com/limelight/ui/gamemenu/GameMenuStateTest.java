package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;

import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.shortcuts.GameMenuShortcut;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class GameMenuStateTest {
    @Test
    public void normalizesBatteryAndDefensivelyCopiesShortcuts() {
        List<GameMenuShortcut> shortcuts = new ArrayList<>();
        shortcuts.add(GameMenuShortcut.moonlightChord(
                "test",
                "Test",
                "",
                new short[]{1},
                true));

        GameMenuState state = createState(101, shortcuts);
        shortcuts.clear();

        assertEquals(
                GameMenuState.UNKNOWN_BATTERY_PERCENT,
                state.getBatteryPercent());
        assertEquals(1, state.getShortcuts().size());
    }

    private static GameMenuState createState(
            int batteryPercent,
            List<GameMenuShortcut> shortcuts) {
        return new GameMenuState(
                true,
                false,
                false,
                true,
                false,
                false,
                batteryPercent,
                GameMenuCardLayoutLoadResult.absent(),
                shortcuts,
                InputSettings.builder().build(),
                ControllerSettings.builder().build(),
                StreamAudioSettings.builder().build(),
                StreamUiSettings.builder().build(),
                VirtualControlSettings.builder().build(),
                VirtualControlEditMode.NONE,
                VirtualControlEditMode.NONE);
    }
}
