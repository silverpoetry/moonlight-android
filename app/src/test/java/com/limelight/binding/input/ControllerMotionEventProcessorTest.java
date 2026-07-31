package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.controller.ControllerSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerMotionEventProcessorTest {
    private static final float DELTA = 0.0001f;

    @Test
    public void nativeMotionUsesTransformedDeviceCoordinates() {
        RecordingOutput output = new RecordingOutput();
        ProcessorFixture fixture = new ProcessorFixture(
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                true,
                ControllerSettings.builder().build(),
                output);
        fixture.rotation = ControllerMotionSampleTransformer.ROTATION_90;

        fixture.processor.process(1, 2, 3);

        assertEquals(1, output.motionCount);
        assertEquals(-2, output.motionX, DELTA);
        assertEquals(3, output.motionY, DELTA);
        assertEquals(-1, output.motionZ, DELTA);
    }

    @Test
    public void duplicateSampleIsSuppressedBeforeSettingsLookup() {
        RecordingOutput output = new RecordingOutput();
        ProcessorFixture fixture = new ProcessorFixture(
                MoonBridge.LI_MOTION_TYPE_GYRO,
                false,
                ControllerSettings.builder().build(),
                output);

        fixture.processor.process(1, 2, 3);
        fixture.processor.process(1, 2, 3);

        assertEquals(1, fixture.settingsReadCount);
        assertEquals(1, output.motionCount);
    }

    @Test
    public void forceGyroResetsStickWhileRequiredTriggerIsReleased() {
        RecordingOutput output = new RecordingOutput();
        ControllerGyroStickTranslator translator =
                new ControllerGyroStickTranslator();
        translator.update(
                1,
                1,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                100);
        ProcessorFixture fixture = new ProcessorFixture(
                MoonBridge.LI_MOTION_TYPE_GYRO,
                false,
                forceGyroSettings(true),
                output,
                translator);
        fixture.leftTriggerState = 199;

        fixture.processor.process(1, 2, 3);

        assertEquals(1, output.controllerInputCount);
        assertEquals(0, output.rightStickX);
        assertEquals(0, output.rightStickY);
        assertEquals(0, output.motionCount);
    }

    @Test
    public void forceGyroUsesDisplayRotationForControllerSensor() {
        RecordingOutput output = new RecordingOutput();
        ProcessorFixture fixture = new ProcessorFixture(
                MoonBridge.LI_MOTION_TYPE_GYRO,
                false,
                forceGyroSettings(false),
                output);
        fixture.rotation = ControllerMotionSampleTransformer.ROTATION_90;

        fixture.processor.process(0.5f, 0.25f, 0);

        assertEquals(1, fixture.rotationReadCount);
        assertEquals(1, output.controllerInputCount);
        assertEquals(-4665, output.rightStickX);
        assertEquals(-1637, output.rightStickY);
        assertEquals(0, output.motionCount);
    }

    @Test
    public void forceGyroIgnoresAccelerometerWhenTriggerAllowsInput() {
        RecordingOutput output = new RecordingOutput();
        ProcessorFixture fixture = new ProcessorFixture(
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                true,
                forceGyroSettings(true),
                output);
        fixture.leftTriggerState = 200;

        fixture.processor.process(1, 2, 3);

        assertEquals(0, output.controllerInputCount);
        assertEquals(0, output.motionCount);
    }

    @Test
    public void nativeControllerMotionDoesNotReadDisplayRotation() {
        RecordingOutput output = new RecordingOutput();
        ProcessorFixture fixture = new ProcessorFixture(
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                false,
                ControllerSettings.builder().build(),
                output);

        fixture.processor.process(1, 2, 3);

        assertEquals(0, fixture.rotationReadCount);
        assertEquals(1, output.motionCount);
    }

    private static ControllerSettings forceGyroSettings(
            boolean requiresTrigger) {
        return ControllerSettings.builder()
                .setForceGyro(
                        true,
                        requiresTrigger,
                        false,
                        100)
                .build();
    }

    private static final class ProcessorFixture {
        final ControllerMotionEventProcessor processor;
        int rotation;
        int rotationReadCount;
        int leftTriggerState;
        int settingsReadCount;

        private ProcessorFixture(
                byte motionType,
                boolean needsDeviceOrientationCorrection,
                ControllerSettings settings,
                RecordingOutput output) {
            this(
                    motionType,
                    needsDeviceOrientationCorrection,
                    settings,
                    output,
                    new ControllerGyroStickTranslator());
        }

        private ProcessorFixture(
                byte motionType,
                boolean needsDeviceOrientationCorrection,
                ControllerSettings settings,
                RecordingOutput output,
                ControllerGyroStickTranslator translator) {
            processor = new ControllerMotionEventProcessor(
                    (short) 2,
                    motionType,
                    needsDeviceOrientationCorrection,
                    () -> {
                        settingsReadCount++;
                        return settings;
                    },
                    () -> {
                        rotationReadCount++;
                        return rotation;
                    },
                    controllerNumber -> leftTriggerState,
                    output,
                    translator);
        }
    }

    private static final class RecordingOutput
            implements ControllerMotionEventProcessor.Output {
        int controllerInputCount;
        int motionCount;
        short rightStickX;
        short rightStickY;
        float motionX;
        float motionY;
        float motionZ;

        @Override
        public void sendControllerInput(
                short rightStickX,
                short rightStickY) {
            controllerInputCount++;
            this.rightStickX = rightStickX;
            this.rightStickY = rightStickY;
        }

        @Override
        public void sendMotion(
                short controllerNumber,
                byte motionType,
                float x,
                float y,
                float z) {
            motionCount++;
            motionX = x;
            motionY = y;
            motionZ = z;
        }
    }
}
