package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.controller.ControllerSettings;

import java.util.Objects;

/**
 * Processes one controller motion stream into either native motion reports or
 * the customized force-gyro right-stick path.
 */
final class ControllerMotionEventProcessor {
    interface SettingsSource {
        ControllerSettings get();
    }

    interface RotationSource {
        int getRotation();
    }

    interface TriggerStateSource {
        int getLeftTriggerState(short controllerNumber);
    }

    interface Output {
        void sendControllerInput(short rightStickX, short rightStickY);

        void sendMotion(
                short controllerNumber,
                byte motionType,
                float x,
                float y,
                float z);
    }

    private static final int FORCE_GYRO_TRIGGER_THRESHOLD = 200;

    private final short controllerNumber;
    private final byte motionType;
    private final boolean needsDeviceOrientationCorrection;
    private final SettingsSource settingsSource;
    private final RotationSource rotationSource;
    private final TriggerStateSource triggerStateSource;
    private final Output output;
    private final ControllerGyroStickTranslator gyroStickTranslator;
    private final ControllerMotionSampleTransformer sampleTransformer =
            new ControllerMotionSampleTransformer();

    ControllerMotionEventProcessor(
            short controllerNumber,
            byte motionType,
            boolean needsDeviceOrientationCorrection,
            SettingsSource settingsSource,
            RotationSource rotationSource,
            TriggerStateSource triggerStateSource,
            Output output,
            ControllerGyroStickTranslator gyroStickTranslator) {
        this.controllerNumber = controllerNumber;
        this.motionType = motionType;
        this.needsDeviceOrientationCorrection =
                needsDeviceOrientationCorrection;
        this.settingsSource = Objects.requireNonNull(
                settingsSource,
                "settingsSource");
        this.rotationSource = Objects.requireNonNull(
                rotationSource,
                "rotationSource");
        this.triggerStateSource = Objects.requireNonNull(
                triggerStateSource,
                "triggerStateSource");
        this.output = Objects.requireNonNull(output, "output");
        this.gyroStickTranslator = Objects.requireNonNull(
                gyroStickTranslator,
                "gyroStickTranslator");
    }

    void process(float rawX, float rawY, float rawZ) {
        int deviceRotation = needsDeviceOrientationCorrection
                ? rotationSource.getRotation()
                : ControllerMotionSampleTransformer.ROTATION_0;
        boolean gyroscope =
                motionType == MoonBridge.LI_MOTION_TYPE_GYRO;
        if (!sampleTransformer.update(
                rawX,
                rawY,
                rawZ,
                deviceRotation,
                needsDeviceOrientationCorrection,
                gyroscope)) {
            return;
        }

        ControllerSettings settings = settingsSource.get();
        if (settings.isForceGyroEnabled()) {
            processForceGyro(
                    settings,
                    gyroscope,
                    deviceRotation);
            return;
        }

        output.sendMotion(
                controllerNumber,
                motionType,
                sampleTransformer.getTransformedX(),
                sampleTransformer.getTransformedY(),
                sampleTransformer.getTransformedZ());
    }

    private void processForceGyro(
            ControllerSettings settings,
            boolean gyroscope,
            int deviceRotation) {
        if (settings.isForceGyroLeftTriggerRequired() &&
                triggerStateSource.getLeftTriggerState(
                        controllerNumber) <
                        FORCE_GYRO_TRIGGER_THRESHOLD) {
            gyroStickTranslator.reset();
            sendTranslatedControllerInput();
            return;
        }

        if (!gyroscope) {
            return;
        }

        if (!needsDeviceOrientationCorrection) {
            deviceRotation = rotationSource.getRotation();
        }
        gyroStickTranslator.update(
                sampleTransformer.getRawX(),
                sampleTransformer.getRawY(),
                deviceRotation,
                settings.areForceGyroAxesSwapped(),
                settings.getForceGyroSensitivityPercent());
        sendTranslatedControllerInput();
    }

    private void sendTranslatedControllerInput() {
        output.sendControllerInput(
                gyroStickTranslator.getRightStickX(),
                gyroStickTranslator.getRightStickY());
    }
}
