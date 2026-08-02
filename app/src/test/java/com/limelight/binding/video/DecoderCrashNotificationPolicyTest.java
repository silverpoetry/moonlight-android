package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DecoderCrashNotificationPolicyTest {
    @Test
    public void suppressesEmptyAndAcknowledgedState() {
        assertEquals(
                DecoderCrashNotificationPolicy.Action.NONE,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(0, 0)));
        assertEquals(
                DecoderCrashNotificationPolicy.Action.NONE,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(2, 2)));
    }

    @Test
    public void warnsForUnacknowledgedNonResetCounts() {
        assertEquals(
                DecoderCrashNotificationPolicy.Action.WARN,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(1, 0)));
        assertEquals(
                DecoderCrashNotificationPolicy.Action.WARN,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(4, 3)));
    }

    @Test
    public void resetsSettingsAtEachThirdConsecutiveCrash() {
        assertEquals(
                DecoderCrashNotificationPolicy.Action.RESET_SETTINGS,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(3, 0)));
        assertEquals(
                DecoderCrashNotificationPolicy.Action.RESET_SETTINGS,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(6, 3)));
    }

    @Test
    public void normalizesCorruptNegativeCounts() {
        assertEquals(
                DecoderCrashNotificationPolicy.Action.NONE,
                DecoderCrashNotificationPolicy.evaluate(
                        new DecoderCrashState(-1, -2)));
    }
}
