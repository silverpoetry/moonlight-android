package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerTriggerDeadzonePolicyTest {
    private static final float DELTA = 0.0001f;

    @Test
    public void enabledCorrectionReplacesTooSmallDeadzone() {
        assertEquals(
                0.13f,
                ControllerTriggerDeadzonePolicy.resolve(
                        0.02f,
                        0.12f,
                        false),
                DELTA);
    }

    @Test
    public void enabledCorrectionPreservesAcceptedDeadzone() {
        assertEquals(
                0.20f,
                ControllerTriggerDeadzonePolicy.resolve(
                        0.20f,
                        0.15f,
                        false),
                DELTA);
        assertEquals(
                0.30f,
                ControllerTriggerDeadzonePolicy.resolve(
                        0.13f,
                        0.30f,
                        false),
                DELTA);
    }

    @Test
    public void enabledCorrectionReplacesTooLargeDeadzone() {
        assertEquals(
                0.13f,
                ControllerTriggerDeadzonePolicy.resolve(
                        0.31f,
                        0.20f,
                        false),
                DELTA);
    }

    @Test
    public void disabledCorrectionPreservesAbsoluteDriverValue() {
        assertEquals(
                0.45f,
                ControllerTriggerDeadzonePolicy.resolve(
                        -0.45f,
                        0.01f,
                        true),
                DELTA);
    }
}
