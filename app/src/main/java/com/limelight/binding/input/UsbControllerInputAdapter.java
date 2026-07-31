package com.limelight.binding.input;

import java.util.Objects;

/** Adapts normalized USB-driver reports to controller protocol state/output. */
final class UsbControllerInputAdapter {
    interface Target {
        ControllerInputState getControllerInputState();

        float getLeftStickDeadzoneRadius();

        float getRightStickDeadzoneRadius();

        float getTriggerDeadzone();

        byte ensureAssignedControllerNumber();

        void sendControllerInput();
    }

    interface Output {
        void sendMotion(
                byte controllerNumber,
                byte motionType,
                float x,
                float y,
                float z);

        void sendTouch(
                byte controllerNumber,
                byte eventType,
                int pointerId,
                float x,
                float y,
                float pressure);
    }

    private final Output output;

    UsbControllerInputAdapter(Output output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    void handleState(
            Target target,
            int buttonFlags,
            float leftStickX,
            float leftStickY,
            float rightStickX,
            float rightStickY,
            float leftTrigger,
            float rightTrigger) {
        Objects.requireNonNull(target, "target");
        ControllerInputState state =
                target.getControllerInputState();
        state.updateLeftStick(
                clampStickRange(leftStickX),
                clampStickRange(leftStickY),
                target.getLeftStickDeadzoneRadius());
        state.updateRightStick(
                clampStickRange(rightStickX),
                clampStickRange(rightStickY),
                target.getRightStickDeadzoneRadius());
        state.setInputMap(buttonFlags);
        state.setTriggers(
                toProtocolTrigger(
                        leftTrigger,
                        target.getTriggerDeadzone()),
                toProtocolTrigger(
                        rightTrigger,
                        target.getTriggerDeadzone()));
        target.sendControllerInput();
    }

    void handleMotion(
            Target target,
            byte motionType,
            float x,
            float y,
            float z) {
        Objects.requireNonNull(target, "target");
        output.sendMotion(
                target.ensureAssignedControllerNumber(),
                motionType,
                x,
                y,
                z);
    }

    void handleTouch(
            Target target,
            byte eventType,
            int pointerId,
            float x,
            float y,
            float pressure) {
        Objects.requireNonNull(target, "target");
        output.sendTouch(
                target.ensureAssignedControllerNumber(),
                eventType,
                pointerId,
                clampUnitRange(x),
                clampUnitRange(y),
                clampUnitRange(pressure));
    }

    private static byte toProtocolTrigger(
            float value,
            float deadzone) {
        float normalized = clampUnitRange(value);
        if (normalized <= deadzone) {
            normalized = 0;
        }
        return (byte) (normalized * 0xff);
    }

    private static float clampUnitRange(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }

    private static float clampStickRange(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0;
        }
        return Math.max(-1, Math.min(1, value));
    }
}
