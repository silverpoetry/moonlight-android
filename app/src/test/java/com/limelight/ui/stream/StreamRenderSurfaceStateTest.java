package com.limelight.ui.stream;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class StreamRenderSurfaceStateTest {
    @Test
    public void startRequiresCreatedValidSurfaceAndSession() {
        StreamRenderSurfaceState state =
                new StreamRenderSurfaceState();

        assertFalse(state.canStart(true));
        state.onCreated();
        assertFalse(state.canStart(true));
        state.onChanged(true);
        assertFalse(state.canStart(false));
        assertTrue(state.canStart(true));
    }

    @Test
    public void invalidChangeRevokesReadiness() {
        StreamRenderSurfaceState state =
                new StreamRenderSurfaceState();
        state.onCreated();
        state.onChanged(true);
        state.onChanged(false);

        assertFalse(state.canStart(true));
    }

    @Test
    public void recreatedSurfaceMustBecomeValidAgain() {
        StreamRenderSurfaceState state =
                new StreamRenderSurfaceState();
        state.onCreated();
        state.onChanged(true);
        state.onDestroyed();
        state.onCreated();

        assertFalse(state.canStart(true));
        state.onChanged(true);
        assertTrue(state.canStart(true));
    }

    @Test
    public void outOfOrderCallbacksAreRejected() {
        StreamRenderSurfaceState state =
                new StreamRenderSurfaceState();

        assertThrows(
                IllegalStateException.class,
                () -> state.onChanged(true));
        assertThrows(
                IllegalStateException.class,
                state::onDestroyed);
    }
}
