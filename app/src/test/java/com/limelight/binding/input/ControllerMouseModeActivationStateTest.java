package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerMouseModeActivationStateTest {
    @Test
    public void disabledFeatureNeverActivates() {
        ControllerMouseModeActivationState state = state();

        assertFalse(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.SPECIAL,
                false,
                1,
                ControllerPacket.SPECIAL_BUTTON_FLAG,
                1000));
    }

    @Test
    public void modeButtonActivatesImmediatelyWhenStillDown() {
        ControllerMouseModeActivationState state = state();

        assertTrue(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.SPECIAL,
                true,
                1,
                ControllerPacket.SPECIAL_BUTTON_FLAG,
                1));
        assertFalse(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.SPECIAL,
                true,
                1,
                0,
                1));
    }

    @Test
    public void playButtonRequiresStrictlyMoreThanThreshold() {
        ControllerMouseModeActivationState state = state();
        state.observeButtonDown(
                ControllerDigitalButtonMapping.Target.PLAY,
                1000,
                0);

        assertFalse(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.PLAY,
                true,
                0,
                ControllerPacket.PLAY_FLAG,
                1750));
        assertTrue(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.PLAY,
                true,
                0,
                ControllerPacket.PLAY_FLAG,
                1751));
    }

    @Test
    public void repeatedPlayDownDoesNotRestartHoldWindow() {
        ControllerMouseModeActivationState state = state();
        state.observeButtonDown(
                ControllerDigitalButtonMapping.Target.PLAY,
                1000,
                0);
        state.observeButtonDown(
                ControllerDigitalButtonMapping.Target.PLAY,
                1700,
                1);

        assertTrue(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.PLAY,
                true,
                0,
                ControllerPacket.PLAY_FLAG,
                1751));
    }

    @Test
    public void configuredButtonAndHeldBitMustMatch() {
        ControllerMouseModeActivationState state = state();
        state.observeButtonDown(
                ControllerDigitalButtonMapping.Target.PLAY,
                1000,
                0);

        assertFalse(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.PLAY,
                true,
                2,
                ControllerPacket.PLAY_FLAG,
                2000));
        assertFalse(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.PLAY,
                true,
                0,
                0,
                2000));
    }

    @Test
    public void selectPathPreservesEstablishedPlayTimestampRule() {
        ControllerMouseModeActivationState state = state();
        state.observeButtonDown(
                ControllerDigitalButtonMapping.Target.PLAY,
                1000,
                0);

        assertTrue(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.BACK,
                true,
                2,
                ControllerPacket.BACK_FLAG,
                1751));
        assertFalse(state.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.BACK,
                true,
                2,
                ControllerPacket.BACK_FLAG,
                1750));
    }

    @Test
    public void unrelatedTargetsNeverActivate() {
        assertFalse(state().shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.A,
                true,
                0,
                ControllerPacket.A_FLAG,
                10000));
    }

    @Test
    public void restorationPreservesActiveHoldWindow() {
        ControllerMouseModeActivationState previous = state();
        previous.observeButtonDown(
                ControllerDigitalButtonMapping.Target.PLAY,
                1000,
                0);
        ControllerMouseModeActivationState restored = state();

        restored.restoreFrom(previous);

        assertTrue(restored.shouldActivateOnRelease(
                ControllerDigitalButtonMapping.Target.PLAY,
                true,
                0,
                ControllerPacket.PLAY_FLAG,
                1751));
    }

    private static ControllerMouseModeActivationState state() {
        return new ControllerMouseModeActivationState();
    }
}
