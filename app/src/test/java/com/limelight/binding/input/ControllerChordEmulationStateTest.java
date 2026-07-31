package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerChordEmulationStateTest {
    @Test
    public void quitChordFinishesOnlyAfterAllButtonsAreReleased() {
        ControllerChordEmulationState state = state(true, true);
        int quitChord = ControllerPacket.BACK_FLAG |
                ControllerPacket.PLAY_FLAG |
                ControllerPacket.LB_FLAG |
                ControllerPacket.RB_FLAG;

        assertEquals(quitChord, state.applyButtonDown(quitChord, 1000));
        assertFalse(state.shouldFinishAfterButtonUp(
                ControllerPacket.PLAY_FLAG));
        assertTrue(state.shouldFinishAfterButtonUp(0));
    }

    @Test
    public void startAndLeftBumperEmulateSelectWithoutPhysicalSelect() {
        ControllerChordEmulationState state = state(false, false);

        assertEquals(
                ControllerPacket.BACK_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG |
                                ControllerPacket.LB_FLAG,
                        1000));
        assertEquals(
                0,
                state.applyButtonUp(ControllerPacket.BACK_FLAG));
    }

    @Test
    public void recentLeftBumperReleaseRetainsOneButtonFallback() {
        ControllerChordEmulationState state = state(false, false);
        state.recordLeftBumperUp(1000);

        assertEquals(
                ControllerPacket.BACK_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG,
                        1100));
        assertEquals(
                ControllerPacket.PLAY_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG,
                        1101));
    }

    @Test
    public void selectAndLeftBumperEmulateRequiredClickpad() {
        ControllerChordEmulationState state = state(false, true);
        state.setClickpadEmulationRequired(true);

        assertTrue(state.isClickpadEmulationRequired());
        assertEquals(
                ControllerPacket.TOUCHPAD_FLAG,
                state.applyButtonDown(
                        ControllerPacket.BACK_FLAG |
                                ControllerPacket.LB_FLAG,
                        1000));
        assertEquals(
                0,
                state.applyButtonUp(
                        ControllerPacket.TOUCHPAD_FLAG));
    }

    @Test
    public void startAndSelectEmulateModeWhenSelectExists() {
        ControllerChordEmulationState state = state(false, true);

        assertEquals(
                ControllerPacket.SPECIAL_BUTTON_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG |
                                ControllerPacket.BACK_FLAG,
                        1000));
        assertEquals(
                0,
                state.applyButtonUp(
                        ControllerPacket.SPECIAL_BUTTON_FLAG));
    }

    @Test
    public void startAndRightBumperEmulateModeWithoutSelect() {
        ControllerChordEmulationState state = state(false, false);

        assertEquals(
                ControllerPacket.SPECIAL_BUTTON_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG |
                                ControllerPacket.RB_FLAG,
                        1000));
    }

    @Test
    public void observedPhysicalButtonsDisableTheirFallbacks() {
        ControllerChordEmulationState state = state(false, false);
        state.observeSelectButton();
        state.observeModeButton();

        assertEquals(
                ControllerPacket.PLAY_FLAG |
                        ControllerPacket.BACK_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG |
                                ControllerPacket.BACK_FLAG,
                        1000));
        assertEquals(
                ControllerPacket.PLAY_FLAG |
                        ControllerPacket.RB_FLAG,
                state.applyButtonDown(
                        ControllerPacket.PLAY_FLAG |
                                ControllerPacket.RB_FLAG,
                        1000));
    }

    private static ControllerChordEmulationState state(
            boolean hasMode,
            boolean hasSelect) {
        return new ControllerChordEmulationState(
                hasMode,
                hasSelect);
    }
}
