package com.limelight.binding.input;

import android.media.AudioAttributes;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.InputDevice;

import androidx.annotation.RequiresApi;

import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsState;

import org.cgutman.shieldcontrollerextensions.SceManager;

import java.util.Objects;

/**
 * Selects and renders Android vibration output for physical input devices and
 * the handset fallback vibrator.
 */
final class ControllerVibrationRenderer {
    static final class Target {
        private final VibratorManager vibratorManager;
        private final Vibrator vibrator;
        private final boolean quadVibrators;
        private final boolean deviceVibrator;

        private short lowFrequencyMotor;
        private short highFrequencyMotor;
        private short leftTriggerMotor;
        private short rightTriggerMotor;

        private Target(
                VibratorManager vibratorManager,
                Vibrator vibrator,
                boolean quadVibrators,
                boolean deviceVibrator) {
            this.vibratorManager = vibratorManager;
            this.vibrator = vibrator;
            this.quadVibrators = quadVibrators;
            this.deviceVibrator = deviceVibrator;
        }

        boolean hasQuadVibrators() {
            return vibratorManager != null && quadVibrators;
        }

        boolean hasVibratorManager() {
            return vibratorManager != null;
        }

        boolean hasLegacyVibrator() {
            return vibrator != null;
        }
    }

    private static final long CONTINUOUS_DURATION_MS = 60_000;

    private final ControllerSettingsState settingsState;
    private final SceManager sceManager;
    private final Vibrator deviceVibrator;
    private final VibratorManager deviceVibratorManager;

    ControllerVibrationRenderer(
            ControllerSettingsState settingsState,
            SceManager sceManager,
            Vibrator deviceVibrator,
            VibratorManager deviceVibratorManager) {
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
        this.sceManager = Objects.requireNonNull(
                sceManager,
                "sceManager");
        this.deviceVibrator = Objects.requireNonNull(
                deviceVibrator,
                "deviceVibrator");
        this.deviceVibratorManager = deviceVibratorManager;
    }

    Target selectTarget(InputDevice inputDevice, boolean external) {
        Objects.requireNonNull(inputDevice, "inputDevice");
        if (settingsState.get().isDeviceRumbleEnabled()) {
            return singleTarget(deviceVibrator, true);
        }

        Vibrator inputVibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager inputManager =
                    inputDevice.getVibratorManager();
            if (hasControlledVibrators(inputManager, 4)) {
                return managerTarget(inputManager, true);
            }
            if (hasControlledVibrators(inputManager, 2)) {
                return managerTarget(inputManager, false);
            }
            inputVibrator = inputManager.getDefaultVibrator();
        } else {
            inputVibrator = getInputDeviceVibratorBeforeS(inputDevice);
        }
        if (inputVibrator.hasVibrator()) {
            return singleTarget(inputVibrator, false);
        }
        if (!external) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (hasControlledVibrators(deviceVibratorManager, 4)) {
                    return managerTarget(deviceVibratorManager, true);
                }
                if (hasControlledVibrators(deviceVibratorManager, 2)) {
                    return managerTarget(deviceVibratorManager, false);
                }
            }
            if (deviceVibrator.hasVibrator()) {
                return singleTarget(deviceVibrator, true);
            }
        }
        return new Target(null, null, false, false);
    }

    Target emptyTarget() {
        return new Target(null, null, false, false);
    }

    boolean rumble(
            Target target,
            InputDevice inputDevice,
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(inputDevice, "inputDevice");
        target.lowFrequencyMotor = lowFrequencyMotor;
        target.highFrequencyMotor = highFrequencyMotor;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                target.vibratorManager != null) {
            if (target.quadVibrators) {
                renderQuad(target);
            } else {
                renderDual(target);
            }
            return true;
        }
        if (sceManager.rumble(
                inputDevice,
                lowFrequencyMotor,
                highFrequencyMotor)) {
            return true;
        }
        if (target.vibrator != null) {
            renderSingle(
                    target.vibrator,
                    target.deviceVibrator,
                    lowFrequencyMotor,
                    highFrequencyMotor);
            return true;
        }
        return false;
    }

    void rumbleTriggers(
            Target target,
            short leftTriggerMotor,
            short rightTriggerMotor) {
        Objects.requireNonNull(target, "target");
        target.leftTriggerMotor = leftTriggerMotor;
        target.rightTriggerMotor = rightTriggerMotor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                target.hasQuadVibrators()) {
            renderQuad(target);
        }
    }

    void rumbleDevice(
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        renderSingle(
                deviceVibrator,
                true,
                lowFrequencyMotor,
                highFrequencyMotor);
    }

    void cancel(Target target) {
        if (target == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                target.vibratorManager != null) {
            target.vibratorManager.cancel();
        } else if (target.vibrator != null) {
            target.vibrator.cancel();
        }
    }

    void cancelDevice() {
        deviceVibrator.cancel();
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static boolean hasControlledVibrators(
            VibratorManager vibratorManager,
            int expectedCount) {
        if (vibratorManager == null) {
            return false;
        }
        int[] vibratorIds = vibratorManager.getVibratorIds();
        if (vibratorIds.length != expectedCount) {
            return false;
        }
        for (int vibratorId : vibratorIds) {
            if (!vibratorManager.getVibrator(vibratorId)
                    .hasAmplitudeControl()) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private void renderDual(Target target) {
        int[] amplitudes = ControllerRumbleAmplitudes.dual(
                target.lowFrequencyMotor,
                target.highFrequencyMotor,
                settingsState.get().areRumbleMotorsFlipped());
        renderParallel(target.vibratorManager, amplitudes);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private void renderQuad(Target target) {
        int[] amplitudes = ControllerRumbleAmplitudes.quad(
                target.lowFrequencyMotor,
                target.highFrequencyMotor,
                target.leftTriggerMotor,
                target.rightTriggerMotor,
                settingsState.get().areRumbleMotorsFlipped());
        renderParallel(target.vibratorManager, amplitudes);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static void renderParallel(
            VibratorManager vibratorManager,
            int[] amplitudes) {
        if (ControllerRumbleAmplitudes.areAllZero(amplitudes)) {
            vibratorManager.cancel();
            return;
        }

        int[] vibratorIds = vibratorManager.getVibratorIds();
        CombinedVibration.ParallelCombination combination =
                CombinedVibration.startParallel();
        for (int index = 0; index < vibratorIds.length; index++) {
            if (amplitudes[index] != 0) {
                combination.addVibrator(
                        vibratorIds[index],
                        VibrationEffect.createOneShot(
                                CONTINUOUS_DURATION_MS,
                                amplitudes[index]));
            }
        }

        VibrationAttributes.Builder attributes =
                new VibrationAttributes.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            attributes.setUsage(VibrationAttributes.USAGE_MEDIA);
        }
        vibratorManager.vibrate(
                combination.combine(),
                attributes.build());
    }

    private void renderSingle(
            Vibrator vibrator,
            boolean isDeviceVibrator,
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        ControllerSettings settings = settingsState.get();
        boolean amplitudeControlAvailable =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        vibrator.hasAmplitudeControl();
        SingleVibratorRumblePlan plan =
                SingleVibratorRumblePlan.create(
                        lowFrequencyMotor,
                        highFrequencyMotor,
                        isDeviceVibrator,
                        settings.isForceStrongVibrationsEnabled(),
                        settings
                                .isForceStrongVibrationsStopPulseEnabled(),
                        amplitudeControlAvailable);
        switch (plan.getAction()) {
            case CANCEL:
                vibrator.cancel();
                return;
            case CANCEL_WITH_STOP_PULSE:
                vibrator.cancel();
                vibrateStrong(vibrator, 1);
                return;
            case STRONG_CONTINUOUS:
                vibrateStrong(vibrator, CONTINUOUS_DURATION_MS);
                return;
            case AMPLITUDE_CONTINUOUS:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    throw new AssertionError(
                            "Amplitude plan below Android O");
                }
                renderAmplitude(vibrator, plan.getAmplitude());
                return;
            case PWM_CONTINUOUS:
                renderPwm(
                        vibrator,
                        plan.getPwmOnTimeMs(),
                        plan.getPwmOffTimeMs());
                return;
            default:
                throw new AssertionError(plan.getAction());
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void renderAmplitude(
            Vibrator vibrator,
            int amplitude) {
        VibrationEffect effect = VibrationEffect.createOneShot(
                CONTINUOUS_DURATION_MS,
                amplitude);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            VibrationAttributes attributes =
                    new VibrationAttributes.Builder()
                            .setUsage(VibrationAttributes.USAGE_MEDIA)
                            .build();
            vibrator.vibrate(effect, attributes);
        } else {
            vibrateEffectBeforeTiramisu(vibrator, effect);
        }
    }

    private static void renderPwm(
            Vibrator vibrator,
            long onTimeMs,
            long offTimeMs) {
        long[] pattern = {0, onTimeMs, offTimeMs};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            VibrationAttributes attributes =
                    new VibrationAttributes.Builder()
                            .setUsage(VibrationAttributes.USAGE_MEDIA)
                            .build();
            vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 0),
                    attributes);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffectBeforeTiramisu(
                    vibrator,
                    VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrateWaveformBeforeOreo(vibrator, pattern);
        }
    }

    /** InputDevice exposes its single-vibrator path only through this API below S. */
    @SuppressWarnings("deprecation")
    private static Vibrator getInputDeviceVibratorBeforeS(
            InputDevice inputDevice) {
        return inputDevice.getVibrator();
    }

    private static void vibrateStrong(Vibrator vibrator, long durationMs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrateStrongBeforeOreo(vibrator, durationMs);
        }
    }

    /** API 26-32 media/game vibration overload retained for matching stream haptics. */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressWarnings("deprecation")
    private static void vibrateEffectBeforeTiramisu(
            Vibrator vibrator,
            VibrationEffect effect) {
        AudioAttributes attributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build();
        vibrator.vibrate(effect, attributes);
    }

    /** API 23-25 compatibility path; VibrationEffect starts at API 26. */
    @SuppressWarnings("deprecation")
    private static void vibrateStrongBeforeOreo(
            Vibrator vibrator,
            long durationMs) {
        vibrator.vibrate(durationMs);
    }

    /** API 23-25 compatibility path for repeating PWM vibration. */
    @SuppressWarnings("deprecation")
    private static void vibrateWaveformBeforeOreo(
            Vibrator vibrator,
            long[] pattern) {
        AudioAttributes attributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build();
        vibrator.vibrate(pattern, 0, attributes);
    }

    private static Target managerTarget(
            VibratorManager vibratorManager,
            boolean quadVibrators) {
        return new Target(
                vibratorManager,
                null,
                quadVibrators,
                false);
    }

    private static Target singleTarget(
            Vibrator vibrator,
            boolean deviceVibrator) {
        return new Target(
                null,
                vibrator,
                false,
                deviceVibrator);
    }
}
