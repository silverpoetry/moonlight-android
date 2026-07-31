package com.limelight.binding.input;

import java.util.Objects;

/**
 * Aggregates every controller source assigned to one protocol slot into one
 * host-visible report.
 *
 * <p>The aggregator is stateless. All report fields remain method-local so
 * callers from different input execution domains cannot overwrite shared
 * scratch state. Traversal is allocation-free and preserves source order for
 * equal-magnitude analog values.</p>
 */
final class ControllerInputReportAggregator {
    interface Source {
        boolean isAssigned();

        short getControllerNumber();

        boolean isMouseEmulationActive();

        boolean isIncludedAcrossMouseModes();

        ControllerInputState getControllerInputState();
    }

    interface Sources {
        int size();

        Source sourceAt(int index);
    }

    interface Output {
        void send(
                int inputMap,
                byte leftTrigger,
                byte rightTrigger,
                short leftStickX,
                short leftStickY,
                short rightStickX,
                short rightStickY);
    }

    private ControllerInputReportAggregator() {
    }

    static void aggregateAndSend(
            Sources sources,
            short controllerNumber,
            boolean mouseEmulationActive,
            Output output) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(output, "output");

        int inputMap = 0;
        byte leftTrigger = 0;
        byte rightTrigger = 0;
        short leftStickX = 0;
        short leftStickY = 0;
        short rightStickX = 0;
        short rightStickY = 0;

        int sourceCount = sources.size();
        for (int index = 0; index < sourceCount; index++) {
            Source source = sources.sourceAt(index);
            if (!source.isAssigned() ||
                    source.getControllerNumber() != controllerNumber ||
                    (!source.isIncludedAcrossMouseModes() &&
                            source.isMouseEmulationActive() !=
                                    mouseEmulationActive)) {
                continue;
            }

            ControllerInputState state =
                    source.getControllerInputState();
            inputMap |= state.getInputMap();
            leftTrigger =
                    ControllerAnalogInputCombiner.combineTrigger(
                            leftTrigger,
                            state.getLeftTrigger());
            rightTrigger =
                    ControllerAnalogInputCombiner.combineTrigger(
                            rightTrigger,
                            state.getRightTrigger());
            leftStickX =
                    ControllerAnalogInputCombiner.combineAxis(
                            leftStickX,
                            state.getLeftStickX());
            leftStickY =
                    ControllerAnalogInputCombiner.combineAxis(
                            leftStickY,
                            state.getLeftStickY());
            rightStickX =
                    ControllerAnalogInputCombiner.combineAxis(
                            rightStickX,
                            state.getRightStickX());
            rightStickY =
                    ControllerAnalogInputCombiner.combineAxis(
                            rightStickY,
                            state.getRightStickY());
        }

        output.send(
                inputMap,
                leftTrigger,
                rightTrigger,
                leftStickX,
                leftStickY,
                rightStickX,
                rightStickY);
    }
}
