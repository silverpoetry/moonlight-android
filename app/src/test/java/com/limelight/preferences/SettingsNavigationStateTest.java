package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SettingsNavigationStateTest {
    @Test
    public void returningFromDetailRestoresRootPosition() {
        SettingsNavigationState state = new SettingsNavigationState();
        state.captureContentScroll(SettingsScrollPosition.of(6, 40));
        state.selectSection("video");
        state.captureContentScroll(SettingsScrollPosition.of(1, 80));

        assertTrue(state.returnToRoot());

        assertEquals(
                SettingsScrollPosition.of(6, 40),
                state.getContentScrollPosition());
        state.selectSection("video");
        assertEquals(
                SettingsScrollPosition.of(1, 80),
                state.getContentScrollPosition());
    }

    @Test
    public void sectionsAndWideRailRetainIndependentPositions() {
        SettingsNavigationState state = new SettingsNavigationState();
        state.selectSection("video");
        state.captureContentScroll(SettingsScrollPosition.of(1, 20));
        state.captureSectionRailScroll(SettingsScrollPosition.of(3, 0));
        state.selectSection("audio");
        state.captureContentScroll(SettingsScrollPosition.of(0, 40));

        assertEquals(
                SettingsScrollPosition.of(0, 40),
                state.getContentScrollPosition());
        assertEquals(
                SettingsScrollPosition.of(3, 0),
                state.getSectionRailScrollPosition());
        state.selectSection("video");
        assertEquals(
                SettingsScrollPosition.of(1, 20),
                state.getContentScrollPosition());
    }

    @Test
    public void rootBackIsNotConsumedAndNegativeOffsetsAreClamped() {
        SettingsNavigationState state = new SettingsNavigationState();
        state.setContentScroll(
                null,
                SettingsScrollPosition.of(-1, -1));
        state.setSectionRailScroll(
                SettingsScrollPosition.of(-2, -2));

        assertFalse(state.returnToRoot());
        assertEquals(
                SettingsScrollPosition.START,
                state.getContentScrollPosition());
        assertEquals(
                SettingsScrollPosition.START,
                state.getSectionRailScrollPosition());
    }
}
