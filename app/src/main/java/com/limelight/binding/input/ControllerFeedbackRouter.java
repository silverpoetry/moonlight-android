package com.limelight.binding.input;

import java.util.Objects;

/** Routes host controller feedback to every target assigned to a slot. */
final class ControllerFeedbackRouter {
    enum RumbleDelivery {
        DELIVERED,
        SUPPRESSED,
        UNAVAILABLE
    }

    enum RumbleRouteResult {
        NO_MATCH,
        MATCHED_UNAVAILABLE,
        HANDLED
    }

    interface Target {
        short getControllerNumber();

        RumbleDelivery deliverRumble(
                short lowFrequencyMotor,
                short highFrequencyMotor);

        void deliverTriggerRumble(
                short leftTriggerMotor,
                short rightTriggerMotor);

        void setLedColor(byte red, byte green, byte blue);

        boolean deliverStandardAudioHaptics(
                short lowFrequencyMotor,
                short highFrequencyMotor);

        boolean submitAdvancedAudioHapticsFrame(
                byte[] frame,
                float intensityGain);

        void setAdvancedAudioHapticsEnabled(boolean enabled);
    }

    interface Targets {
        int size();

        Target targetAt(int index);
    }

    private ControllerFeedbackRouter() {
    }

    static RumbleRouteResult routeRumble(
            Targets targets,
            short controllerNumber,
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        Objects.requireNonNull(targets, "targets");
        RumbleRouteResult result =
                RumbleRouteResult.NO_MATCH;
        int targetCount = targets.size();
        for (int index = 0; index < targetCount; index++) {
            Target target = targets.targetAt(index);
            if (target.getControllerNumber() != controllerNumber) {
                continue;
            }

            if (result == RumbleRouteResult.NO_MATCH) {
                result = RumbleRouteResult.MATCHED_UNAVAILABLE;
            }
            RumbleDelivery delivery = target.deliverRumble(
                    lowFrequencyMotor,
                    highFrequencyMotor);
            if (delivery == RumbleDelivery.DELIVERED ||
                    delivery == RumbleDelivery.SUPPRESSED) {
                result = RumbleRouteResult.HANDLED;
            }
        }
        return result;
    }

    static void routeTriggerRumble(
            Targets targets,
            short controllerNumber,
            short leftTriggerMotor,
            short rightTriggerMotor) {
        Objects.requireNonNull(targets, "targets");
        int targetCount = targets.size();
        for (int index = 0; index < targetCount; index++) {
            Target target = targets.targetAt(index);
            if (target.getControllerNumber() == controllerNumber) {
                target.deliverTriggerRumble(
                        leftTriggerMotor,
                        rightTriggerMotor);
            }
        }
    }

    static void routeLedColor(
            Targets targets,
            short controllerNumber,
            byte red,
            byte green,
            byte blue) {
        Objects.requireNonNull(targets, "targets");
        int targetCount = targets.size();
        for (int index = 0; index < targetCount; index++) {
            Target target = targets.targetAt(index);
            if (target.getControllerNumber() == controllerNumber) {
                target.setLedColor(red, green, blue);
            }
        }
    }

    static boolean routeStandardAudioHaptics(
            Targets targets,
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        Objects.requireNonNull(targets, "targets");
        boolean delivered = false;
        int targetCount = targets.size();
        for (int index = 0; index < targetCount; index++) {
            delivered |= targets.targetAt(index)
                    .deliverStandardAudioHaptics(
                            lowFrequencyMotor,
                            highFrequencyMotor);
        }
        return delivered;
    }

    static boolean routeAdvancedAudioHapticsFrame(
            Targets targets,
            byte[] frame,
            float intensityGain) {
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(frame, "frame");
        boolean delivered = false;
        int targetCount = targets.size();
        for (int index = 0; index < targetCount; index++) {
            delivered |= targets.targetAt(index)
                    .submitAdvancedAudioHapticsFrame(
                            frame,
                            intensityGain);
        }
        return delivered;
    }

    static void setAdvancedAudioHapticsEnabled(
            Targets targets,
            boolean enabled) {
        Objects.requireNonNull(targets, "targets");
        int targetCount = targets.size();
        for (int index = 0; index < targetCount; index++) {
            targets.targetAt(index)
                    .setAdvancedAudioHapticsEnabled(enabled);
        }
    }
}
