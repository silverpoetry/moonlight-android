package com.limelight.ui.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamPictureInPictureStateTest {
    @Test
    public void autoEnterRequiresEnabledConnectedUnsuppressedSession() {
        StreamPictureInPictureState state =
                new StreamPictureInPictureState(true);

        assertFalse(state.shouldAutoEnter());
        state.setConnected(true);
        assertTrue(state.shouldAutoEnter());
        state.setEnabled(false);
        assertFalse(state.shouldAutoEnter());
        state.setEnabled(true);
        assertTrue(state.shouldAutoEnter());
        state.setConnected(false);
        assertFalse(state.shouldAutoEnter());
    }

    @Test
    public void nestedSuppressionsMustAllBeReleased() {
        StreamPictureInPictureState state =
                new StreamPictureInPictureState(true);
        state.setConnected(true);

        state.acquireSuppression();
        state.acquireSuppression();
        assertFalse(state.shouldAutoEnter());
        state.releaseSuppression();
        assertFalse(state.shouldAutoEnter());
        state.releaseSuppression();
        assertTrue(state.shouldAutoEnter());
    }

    @Test
    public void unmatchedReleaseCannotUnderflowSuppression() {
        StreamPictureInPictureState state =
                new StreamPictureInPictureState(true);
        state.setConnected(true);

        state.releaseSuppression();

        assertEquals(0, state.getSuppressionCount());
        assertTrue(state.shouldAutoEnter());
    }
}
