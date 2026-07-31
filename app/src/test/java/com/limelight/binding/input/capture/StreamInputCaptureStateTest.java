package com.limelight.binding.input.capture;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamInputCaptureStateTest {
    @Test
    public void streamStartsGrabbedWithLocalCursorHidden() {
        StreamInputCaptureState state =
                new StreamInputCaptureState();

        assertTrue(state.isInputGrabbed());
        assertFalse(state.isLocalCursorVisible());
    }

    @Test
    public void grabChangesDoNotOverwriteCursorPreference() {
        StreamInputCaptureState state =
                new StreamInputCaptureState();
        state.toggleLocalCursorVisibility();

        state.setInputGrabbed(false);
        state.setInputGrabbed(true);

        assertTrue(state.isInputGrabbed());
        assertTrue(state.isLocalCursorVisible());
    }

    @Test
    public void togglingCursorReentersGrabbedInput() {
        StreamInputCaptureState state =
                new StreamInputCaptureState();
        state.setInputGrabbed(false);

        state.toggleLocalCursorVisibility();

        assertTrue(state.isInputGrabbed());
        assertTrue(state.isLocalCursorVisible());
    }
}
