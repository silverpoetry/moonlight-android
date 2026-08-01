package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SettingsNavigationStateTest {
    @Test
    public void returningFromDetailRestoresRootPosition() {
        SettingsNavigationState state = new SettingsNavigationState();
        state.captureContentScroll(640);
        state.selectSection("video");
        state.captureContentScroll(180);

        assertTrue(state.returnToRoot());

        assertEquals(640, state.getContentScrollY());
        state.selectSection("video");
        assertEquals(180, state.getContentScrollY());
    }

    @Test
    public void sectionsAndWideRailRetainIndependentPositions() {
        SettingsNavigationState state = new SettingsNavigationState();
        state.selectSection("video");
        state.captureContentScroll(120);
        state.captureSectionRailScroll(300);
        state.selectSection("audio");
        state.captureContentScroll(40);

        assertEquals(40, state.getContentScrollY());
        assertEquals(300, state.getSectionRailScrollY());
        state.selectSection("video");
        assertEquals(120, state.getContentScrollY());
    }

    @Test
    public void rootBackIsNotConsumedAndNegativeOffsetsAreClamped() {
        SettingsNavigationState state = new SettingsNavigationState();
        state.setContentScroll(null, -1);
        state.setSectionRailScrollY(-2);

        assertFalse(state.returnToRoot());
        assertEquals(0, state.getContentScrollY());
        assertEquals(0, state.getSectionRailScrollY());
    }
}
