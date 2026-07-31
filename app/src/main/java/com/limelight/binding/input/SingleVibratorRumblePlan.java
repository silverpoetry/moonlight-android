package com.limelight.binding.input;

/**
 * Immutable rendering plan for a two-motor protocol report on one Android
 * vibrator.
 */
public final class SingleVibratorRumblePlan {
    public enum Action {
        CANCEL,
        CANCEL_WITH_STOP_PULSE,
        STRONG_CONTINUOUS,
        AMPLITUDE_CONTINUOUS,
        PWM_CONTINUOUS
    }

    private static final long PWM_PERIOD_MS = 20;

    private final Action action;
    private final int amplitude;
    private final long pwmOnTimeMs;
    private final long pwmOffTimeMs;

    private SingleVibratorRumblePlan(
            Action action,
            int amplitude,
            long pwmOnTimeMs,
            long pwmOffTimeMs) {
        this.action = action;
        this.amplitude = amplitude;
        this.pwmOnTimeMs = pwmOnTimeMs;
        this.pwmOffTimeMs = pwmOffTimeMs;
    }

    public static SingleVibratorRumblePlan create(
            short lowFrequencyMotor,
            short highFrequencyMotor,
            boolean deviceVibrator,
            boolean forceStrongVibrations,
            boolean forceStrongStopPulse,
            boolean amplitudeControlAvailable) {
        int amplitude = ControllerRumbleAmplitudes.single(
                lowFrequencyMotor,
                highFrequencyMotor);
        if (amplitude == 0) {
            return new SingleVibratorRumblePlan(
                    deviceVibrator && forceStrongStopPulse
                            ? Action.CANCEL_WITH_STOP_PULSE
                            : Action.CANCEL,
                    0,
                    0,
                    0);
        }
        if (deviceVibrator && forceStrongVibrations) {
            return new SingleVibratorRumblePlan(
                    Action.STRONG_CONTINUOUS,
                    amplitude,
                    0,
                    0);
        }
        if (amplitudeControlAvailable) {
            return new SingleVibratorRumblePlan(
                    Action.AMPLITUDE_CONTINUOUS,
                    amplitude,
                    0,
                    0);
        }

        long onTimeMs = (long) ((amplitude / 255.0) *
                PWM_PERIOD_MS);
        return new SingleVibratorRumblePlan(
                Action.PWM_CONTINUOUS,
                amplitude,
                onTimeMs,
                PWM_PERIOD_MS - onTimeMs);
    }

    public Action getAction() {
        return action;
    }

    public int getAmplitude() {
        return amplitude;
    }

    public long getPwmOnTimeMs() {
        return pwmOnTimeMs;
    }

    public long getPwmOffTimeMs() {
        return pwmOffTimeMs;
    }
}
