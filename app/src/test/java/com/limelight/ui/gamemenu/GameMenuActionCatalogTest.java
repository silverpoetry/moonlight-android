package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public final class GameMenuActionCatalogTest {
    @Test
    public void builtInActionsUseDistinctSemanticIcons() {
        Set<Integer> icons = new HashSet<>();
        for (GameMenuActionCatalog.Action action :
                GameMenuActionCatalog.all()) {
            icons.add(action.iconRes);
        }

        assertEquals(GameMenuActionCatalog.all().size(), icons.size());
    }

    @Test
    public void softKeyboardUsesTheExistingStreamAction() {
        GameMenuActionCatalog.Action action =
                GameMenuActionCatalog.findById("soft_keyboard");

        assertNotNull(action);
        assertEquals(com.limelight.R.id.btn_soft_keyboard, action.viewId);
    }

    @Test
    public void oneShotActionsDismissBeforeExecution() {
        assertTrue(GameMenuActionCatalog.findById("rotate")
                .dismissesMenuBeforeExecution());
        assertTrue(GameMenuActionCatalog.findById("soft_keyboard")
                .dismissesMenuBeforeExecution());
        assertTrue(GameMenuActionCatalog.findById("hdr")
                .dismissesMenuBeforeExecution());
        assertTrue(GameMenuActionCatalog.findById("clipboard_files")
                .dismissesMenuBeforeExecution());
    }

    @Test
    public void stateActionsKeepMenuVisible() {
        assertFalse(GameMenuActionCatalog.findById("microphone")
                .dismissesMenuBeforeExecution());
        assertFalse(GameMenuActionCatalog.findById("audio_mute")
                .dismissesMenuBeforeExecution());
        assertFalse(GameMenuActionCatalog.findById("video_visibility")
                .dismissesMenuBeforeExecution());
        assertFalse(GameMenuActionCatalog.findById("virtual_gamepad")
                .dismissesMenuBeforeExecution());
        assertFalse(GameMenuActionCatalog.findById("screen_zoom")
                .dismissesMenuBeforeExecution());
    }

    @Test
    public void navigationActionsKeepParentMenuVisible() {
        assertFalse(GameMenuActionCatalog.findById("windows_actions")
                .dismissesMenuBeforeExecution());
    }
}
