package com.limelight.binding.input.virtual_controller.keyboard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AnalogStickGestureTimingTest {
    @Test
    public void acceptsSecondTapAtTimeoutBoundary() {
        assertTrue(AnalogStickGestureTiming.isDoubleTap(1_000, 1_350, 350));
    }

    @Test
    public void rejectsMissingPreviousTap() {
        assertFalse(AnalogStickGestureTiming.isDoubleTap(-1, 100, 350));
    }

    @Test
    public void rejectsTapAfterTimeout() {
        assertFalse(AnalogStickGestureTiming.isDoubleTap(1_000, 1_351, 350));
    }

    @Test
    public void rejectsMismatchedClockOrigin() {
        assertFalse(AnalogStickGestureTiming.isDoubleTap(10_000, 1_000, 350));
    }
}
