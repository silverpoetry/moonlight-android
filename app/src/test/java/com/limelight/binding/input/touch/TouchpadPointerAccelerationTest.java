package com.limelight.binding.input.touch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes the acceleration profile used by every legacy touchpad mouse
 * movement path.
 */
public final class TouchpadPointerAccelerationTest {
    private static final double EPSILON = 0.000_001;

    @Test
    public void lowSpeedMotionUsesPrecisionDeceleration() {
        TouchpadPointerAcceleration acceleration =
                new TouchpadPointerAcceleration();
        acceleration.restart(0);

        double factor = acceleration.calculateAcceleration(
                0.1, 0.0, 10);

        assertTrue(factor >= 1.0 / 3.0);
        assertTrue(factor < 1.0);
    }

    @Test
    public void ordinaryMotionUsesUnityPlateau() {
        TouchpadPointerAcceleration acceleration =
                new TouchpadPointerAcceleration();
        acceleration.restart(0);

        double factor = acceleration.calculateAcceleration(
                0.8, 0.0, 10);

        assertEquals(1.0, factor, EPSILON);
    }

    @Test
    public void fastMotionUsesBoundedAcceleration() {
        TouchpadPointerAcceleration acceleration =
                new TouchpadPointerAcceleration();
        acceleration.restart(0);

        double factor = acceleration.calculateAcceleration(
                4.0, 0.0, 10);

        assertTrue(factor > 1.0);
        assertTrue(factor < 6.0);
    }

    @Test
    public void directionReversalDropsPreviousFastHistory() {
        TouchpadPointerAcceleration acceleration =
                new TouchpadPointerAcceleration();
        acceleration.restart(0);
        acceleration.calculateAcceleration(4.0, 0.0, 10);

        double reversedFactor = acceleration.calculateAcceleration(
                -0.1, 0.0, 20);

        assertTrue(reversedFactor < 1.0);
    }

    @Test
    public void longPauseDropsPreviousFastHistory() {
        TouchpadPointerAcceleration acceleration =
                new TouchpadPointerAcceleration();
        acceleration.restart(0);
        acceleration.calculateAcceleration(4.0, 0.0, 10);

        double resumedFactor = acceleration.calculateAcceleration(
                0.1, 0.0, 2_000);

        assertTrue(resumedFactor < 1.0);
    }
}
