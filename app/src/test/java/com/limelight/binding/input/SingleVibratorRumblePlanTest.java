package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SingleVibratorRumblePlanTest {
    @Test
    public void zeroReportCancelsOrdinaryVibrator() {
        SingleVibratorRumblePlan plan = create(
                (short) 0,
                (short) 0,
                false,
                false,
                true,
                true);

        assertEquals(
                SingleVibratorRumblePlan.Action.CANCEL,
                plan.getAction());
    }

    @Test
    public void zeroReportAddsConfiguredDeviceStopPulse() {
        SingleVibratorRumblePlan plan = create(
                (short) 0,
                (short) 0,
                true,
                false,
                true,
                true);

        assertEquals(
                SingleVibratorRumblePlan.Action
                        .CANCEL_WITH_STOP_PULSE,
                plan.getAction());
    }

    @Test
    public void forceStrongDeviceModePrecedesAmplitudeControl() {
        SingleVibratorRumblePlan plan = create(
                (short) 0x4000,
                (short) 0x2000,
                true,
                true,
                false,
                true);

        assertEquals(
                SingleVibratorRumblePlan.Action.STRONG_CONTINUOUS,
                plan.getAction());
    }

    @Test
    public void amplitudeControlUsesMixedProtocolAmplitude() {
        short low = (short) 0x6000;
        short high = (short) 0x3000;
        SingleVibratorRumblePlan plan = create(
                low,
                high,
                false,
                false,
                false,
                true);

        assertEquals(
                SingleVibratorRumblePlan.Action
                        .AMPLITUDE_CONTINUOUS,
                plan.getAction());
        assertEquals(
                ControllerRumbleAmplitudes.single(low, high),
                plan.getAmplitude());
    }

    @Test
    public void legacyVibratorUsesBoundedPwmPeriod() {
        SingleVibratorRumblePlan plan = create(
                (short) 0x4000,
                (short) 0x2000,
                false,
                false,
                false,
                false);

        assertEquals(
                SingleVibratorRumblePlan.Action.PWM_CONTINUOUS,
                plan.getAction());
        assertTrue(plan.getPwmOnTimeMs() > 0);
        assertEquals(
                20,
                plan.getPwmOnTimeMs() + plan.getPwmOffTimeMs());
    }

    private static SingleVibratorRumblePlan create(
            short low,
            short high,
            boolean deviceVibrator,
            boolean forceStrong,
            boolean stopPulse,
            boolean amplitudeControl) {
        return SingleVibratorRumblePlan.create(
                low,
                high,
                deviceVibrator,
                forceStrong,
                stopPulse,
                amplitudeControl);
    }
}
