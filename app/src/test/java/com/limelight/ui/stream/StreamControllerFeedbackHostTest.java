package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class StreamControllerFeedbackHostTest {
    @Test
    public void rumbleRoutesDeviceLinkAndObserverInOrder() {
        RecordingDeviceFeedback device =
                new RecordingDeviceFeedback();
        List<String> observations = new ArrayList<>();
        StreamControllerFeedbackHost host =
                new StreamControllerFeedbackHost(
                        device,
                        () -> true,
                        (controller, low, high) -> observations.add(
                                controller + ":" + low + ":" + high));

        host.onRumble((short) 1, (short) 2, (short) 3);

        assertEquals(
                List.of("rumble:1:2:3", "triggers:1:2:3"),
                device.events);
        assertEquals(List.of("1:2:3"), observations);
    }

    @Test
    public void disabledLinkDoesNotSynthesizeTriggerRumble() {
        RecordingDeviceFeedback device =
                new RecordingDeviceFeedback();
        StreamControllerFeedbackHost host =
                new StreamControllerFeedbackHost(
                        device,
                        () -> false,
                        (controller, low, high) -> { });

        host.onRumble((short) 1, (short) 2, (short) 3);

        assertEquals(List.of("rumble:1:2:3"), device.events);
    }

    @Test
    public void explicitFeedbackCommandsReachTheirDeviceEndpoints() {
        RecordingDeviceFeedback device =
                new RecordingDeviceFeedback();
        StreamControllerFeedbackHost host =
                new StreamControllerFeedbackHost(
                        device,
                        () -> false,
                        (controller, low, high) -> { });

        host.onRumbleTriggers(
                (short) 4,
                (short) 5,
                (short) 6);
        host.onMotionEventState(
                (short) 7,
                (byte) 8,
                (short) 9);
        host.onControllerLed(
                (short) 10,
                (byte) 11,
                (byte) 12,
                (byte) 13);

        assertEquals(
                List.of(
                        "triggers:4:5:6",
                        "motion:7:8:9",
                        "led:10:11:12:13"),
                device.events);
    }

    private static final class RecordingDeviceFeedback
            implements StreamControllerFeedbackHost.DeviceFeedback {
        private final List<String> events = new ArrayList<>();

        @Override
        public void rumble(
                short controllerNumber,
                short lowFrequencyMotor,
                short highFrequencyMotor) {
            events.add(
                    "rumble:" + controllerNumber + ":" +
                            lowFrequencyMotor + ":" +
                            highFrequencyMotor);
        }

        @Override
        public void rumbleTriggers(
                short controllerNumber,
                short leftTrigger,
                short rightTrigger) {
            events.add(
                    "triggers:" + controllerNumber + ":" +
                            leftTrigger + ":" + rightTrigger);
        }

        @Override
        public void setMotionEventState(
                short controllerNumber,
                byte motionType,
                short reportRateHz) {
            events.add(
                    "motion:" + controllerNumber + ":" +
                            motionType + ":" + reportRateHz);
        }

        @Override
        public void setControllerLed(
                short controllerNumber,
                byte red,
                byte green,
                byte blue) {
            events.add(
                    "led:" + controllerNumber + ":" + red + ":" +
                            green + ":" + blue);
        }
    }
}
