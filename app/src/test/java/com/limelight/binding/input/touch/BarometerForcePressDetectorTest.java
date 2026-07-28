package com.limelight.binding.input.touch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class BarometerForcePressDetectorTest {
    private BarometerForcePressDetector detector;
    private long timeMs;

    @Before
    public void setUp() {
        detector = new BarometerForcePressDetector();
        for (int i = 0; i < 20; i++) {
            pressure(1000.0f + (i % 2) * 0.002f);
            timeMs += 10;
        }
        assertTrue(detector.hasStableBaseline());
    }

    @Test
    public void pressureSpikeRequiresOneOrTwoActiveTouches() {
        pressure(1000.5f);
        assertFalse(detector.isForcePressed());

        down(1);
        down(2);
        pressure(1000.5f);
        assertTrue(detector.isForcePressed());

        detector.cancelTouchSession();
        down(1);
        down(2);
        down(3);
        pressure(1000.5f);
        assertFalse(detector.isForcePressed());
    }

    @Test
    public void pressureSpikeLatchesUntilOwningPointerLeaves() {
        down(7);
        pressure(1000.3f);

        assertTrue(detector.isForcePressed());
        assertTrue(detector.wasForceTriggeredOnLastSample());
        assertEquals(7, detector.getForcePointerId());

        pressure(1000.0f);
        assertTrue(detector.isForcePressed());

        up(7);
        assertFalse(detector.isForcePressed());
    }

    @Test
    public void rejectedForcePressDoesNotRetriggerDuringSameTouch() {
        down(4);
        pressure(1000.3f);
        assertTrue(detector.isForcePressed());

        detector.blockCurrentTouchSession();
        pressure(1000.4f);
        assertFalse(detector.isForcePressed());

        up(4);
        down(5);
        pressure(1000.3f);
        assertTrue(detector.isForcePressed());
    }

    @Test
    public void configuredThresholdIsFixed() {
        detector.setThresholdHpa(0.4f);
        assertEquals(0.4f, detector.getThresholdHpa(), 0.0001f);

        down(9);
        pressure(1000.3f);
        assertFalse(detector.isForcePressed());

        pressure(1000.5f);
        assertTrue(detector.isForcePressed());
    }

    @Test
    public void twoFingerForcePressReleasesWhenEitherFingerLeaves() {
        down(10);
        down(11);
        pressure(1000.3f);
        assertTrue(detector.isForcePressed());

        pressure(1000.0f);
        assertTrue(detector.isForcePressed());

        up(11);
        assertFalse(detector.isForcePressed());
    }

    @Test
    public void forcePressRequiresMinimumContactSetDuration() {
        detector.setMinimumTouchDurationMs(100);
        assertEquals(100, detector.getMinimumTouchDurationMs());

        down(20);
        timeMs += 99;
        pressure(1000.3f);
        assertFalse(detector.isForcePressed());

        timeMs += 1;
        pressure(1000.3f);
        assertTrue(detector.isForcePressed());
    }

    @Test
    public void secondFingerRestartsMinimumDuration() {
        detector.setMinimumTouchDurationMs(100);
        down(30);
        timeMs += 100;
        down(31);

        pressure(1000.3f);
        assertFalse(detector.isForcePressed());

        timeMs += 100;
        pressure(1000.3f);
        assertTrue(detector.isForcePressed());
    }

    private void down(int pointerId) {
        detector.onPointerDown(pointerId, timeMs);
    }

    private void up(int pointerId) {
        detector.onPointerUp(pointerId, timeMs);
    }

    private void pressure(float pressureHpa) {
        detector.onPressureSample(pressureHpa, timeMs);
    }
}
