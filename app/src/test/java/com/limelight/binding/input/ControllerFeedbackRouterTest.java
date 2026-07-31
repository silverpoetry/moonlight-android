package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ControllerFeedbackRouterTest {
    @Test
    public void noMatchingSlotIsDistinctFromUnavailableHardware() {
        FakeTarget other = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);

        assertEquals(
                ControllerFeedbackRouter.RumbleRouteResult.NO_MATCH,
                routeRumble((short) 1, other));
        assertTrue(other.events.isEmpty());
    }

    @Test
    public void matchingTargetWithoutRumbleReportsUnavailable() {
        FakeTarget target = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.UNAVAILABLE);

        assertEquals(
                ControllerFeedbackRouter.RumbleRouteResult
                        .MATCHED_UNAVAILABLE,
                routeRumble((short) 1, target));
        assertEquals(
                Arrays.asList("rumble:10:20"),
                target.events);
    }

    @Test
    public void deliberateSuppressionCountsAsHandled() {
        FakeTarget target = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.SUPPRESSED);

        assertEquals(
                ControllerFeedbackRouter.RumbleRouteResult.HANDLED,
                routeRumble((short) 1, target));
    }

    @Test
    public void everyMatchingTargetReceivesRumbleAndAnyHandlerWins() {
        FakeTarget unavailable = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.UNAVAILABLE);
        FakeTarget delivered = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        FakeTarget other = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);

        assertEquals(
                ControllerFeedbackRouter.RumbleRouteResult.HANDLED,
                routeRumble(
                        (short) 1,
                        unavailable,
                        delivered,
                        other));
        assertEquals(1, unavailable.events.size());
        assertEquals(1, delivered.events.size());
        assertTrue(other.events.isEmpty());
    }

    @Test
    public void triggerRumbleFansOutOnlyWithinSlot() {
        FakeTarget first = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        FakeTarget second = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        FakeTarget other = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);

        ControllerFeedbackRouter.routeTriggerRumble(
                targets(first, second, other),
                (short) 1,
                (short) 30,
                (short) 40);

        assertEquals(
                Arrays.asList("trigger:30:40"),
                first.events);
        assertEquals(
                Arrays.asList("trigger:30:40"),
                second.events);
        assertTrue(other.events.isEmpty());
    }

    @Test
    public void ledColorFansOutOnlyWithinSlot() {
        FakeTarget first = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        FakeTarget second = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);

        ControllerFeedbackRouter.routeLedColor(
                targets(first, second),
                (short) 1,
                (byte) 4,
                (byte) 5,
                (byte) 6);

        assertEquals(
                Arrays.asList("led:4:5:6"),
                first.events);
        assertTrue(second.events.isEmpty());
    }

    @Test
    public void standardAudioHapticsFansOutAndAggregatesDelivery() {
        FakeTarget unavailable = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        unavailable.standardAudioDelivery = false;
        FakeTarget delivered = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        delivered.standardAudioDelivery = true;

        assertTrue(ControllerFeedbackRouter.routeStandardAudioHaptics(
                targets(unavailable, delivered),
                (short) 70,
                (short) 80));
        assertEquals(
                Arrays.asList("audio:70:80"),
                unavailable.events);
        assertEquals(
                Arrays.asList("audio:70:80"),
                delivered.events);
    }

    @Test
    public void advancedAudioFrameFansOutAndAggregatesDelivery() {
        FakeTarget ignored = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        FakeTarget delivered = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        delivered.advancedAudioDelivery = true;
        byte[] frame = {1, 2, 3};

        assertTrue(ControllerFeedbackRouter
                .routeAdvancedAudioHapticsFrame(
                        targets(ignored, delivered),
                        frame,
                        1.5f));
        assertEquals(
                Arrays.asList("advanced:3:1.5"),
                ignored.events);
        assertEquals(
                Arrays.asList("advanced:3:1.5"),
                delivered.events);
    }

    @Test
    public void advancedAudioEnablementReachesEveryTarget() {
        FakeTarget first = target(
                1,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);
        FakeTarget second = target(
                2,
                ControllerFeedbackRouter.RumbleDelivery.DELIVERED);

        ControllerFeedbackRouter.setAdvancedAudioHapticsEnabled(
                targets(first, second),
                true);

        assertEquals(
                Arrays.asList("advanced-enabled:true"),
                first.events);
        assertEquals(
                Arrays.asList("advanced-enabled:true"),
                second.events);
    }

    private static ControllerFeedbackRouter.RumbleRouteResult
            routeRumble(
                    short controllerNumber,
                    FakeTarget... targets) {
        return ControllerFeedbackRouter.routeRumble(
                targets(targets),
                controllerNumber,
                (short) 10,
                (short) 20);
    }

    private static ControllerFeedbackRouter.Targets targets(
            FakeTarget... targets) {
        return new ControllerFeedbackRouter.Targets() {
            @Override
            public int size() {
                return targets.length;
            }

            @Override
            public ControllerFeedbackRouter.Target targetAt(
                    int index) {
                return targets[index];
            }
        };
    }

    private static FakeTarget target(
            int controllerNumber,
            ControllerFeedbackRouter.RumbleDelivery delivery) {
        return new FakeTarget(
                (short) controllerNumber,
                delivery);
    }

    private static final class FakeTarget
            implements ControllerFeedbackRouter.Target {
        private final short controllerNumber;
        private final ControllerFeedbackRouter.RumbleDelivery delivery;
        private final List<String> events = new ArrayList<>();
        boolean standardAudioDelivery;
        boolean advancedAudioDelivery;

        FakeTarget(
                short controllerNumber,
                ControllerFeedbackRouter.RumbleDelivery delivery) {
            this.controllerNumber = controllerNumber;
            this.delivery = delivery;
        }

        @Override
        public short getControllerNumber() {
            return controllerNumber;
        }

        @Override
        public ControllerFeedbackRouter.RumbleDelivery deliverRumble(
                short lowFrequencyMotor,
                short highFrequencyMotor) {
            events.add(
                    "rumble:" + lowFrequencyMotor + ":" +
                            highFrequencyMotor);
            return delivery;
        }

        @Override
        public void deliverTriggerRumble(
                short leftTriggerMotor,
                short rightTriggerMotor) {
            events.add(
                    "trigger:" + leftTriggerMotor + ":" +
                            rightTriggerMotor);
        }

        @Override
        public void setLedColor(
                byte red,
                byte green,
                byte blue) {
            events.add(
                    "led:" + red + ":" + green + ":" + blue);
        }

        @Override
        public boolean deliverStandardAudioHaptics(
                short lowFrequencyMotor,
                short highFrequencyMotor) {
            events.add(
                    "audio:" + lowFrequencyMotor + ":" +
                            highFrequencyMotor);
            return standardAudioDelivery;
        }

        @Override
        public boolean submitAdvancedAudioHapticsFrame(
                byte[] frame,
                float intensityGain) {
            events.add(
                    "advanced:" + frame.length + ":" +
                            intensityGain);
            return advancedAudioDelivery;
        }

        @Override
        public void setAdvancedAudioHapticsEnabled(
                boolean enabled) {
            events.add("advanced-enabled:" + enabled);
        }
    }
}
