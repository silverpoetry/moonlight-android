package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class StreamInputLifecycleControllerTest {
    @Test
    public void resumePauseCyclesOnlyTransitionOnce() {
        Events events = new Events();
        StreamInputLifecycleController controller =
                create(events);

        controller.resume();
        controller.resume();
        controller.pause(false);
        controller.pause(false);
        controller.resume();

        assertEquals(
                Arrays.asList(
                        "routing.start",
                        "routing.stop",
                        "routing.start"),
                events.values);
    }

    @Test
    public void finishingPauseStopsControllerDevicesAndRejectsResume() {
        Events events = new Events();
        StreamInputLifecycleController controller =
                create(events);
        controller.resume();

        controller.pause(true);
        controller.resume();

        assertEquals(
                Arrays.asList(
                        "routing.start",
                        "routing.stop",
                        "devices.stop"),
                events.values);
    }

    @Test
    public void stagedDestroyPreservesRoutingMediaDeviceOrder() {
        Events events = new Events();
        StreamInputLifecycleController controller =
                create(events);
        controller.resume();

        controller.detachRouting();
        events.values.add("media.destroy");
        controller.destroy();

        assertEquals(
                Arrays.asList(
                        "routing.start",
                        "routing.stop",
                        "routing.destroy",
                        "media.destroy",
                        "devices.stop",
                        "devices.destroy",
                        "keyboard.unregister"),
                events.values);
    }

    @Test
    public void destroyIsIdempotentAndHandlesPartialStartup() {
        Events events = new Events();
        StreamInputLifecycleController controller =
                create(events);

        controller.destroy();
        controller.destroy();
        controller.pause(true);
        controller.resume();
        controller.detachRouting();

        assertEquals(
                Arrays.asList(
                        "routing.destroy",
                        "devices.stop",
                        "devices.destroy",
                        "keyboard.unregister"),
                events.values);
    }

    private static StreamInputLifecycleController create(
            Events events) {
        return new StreamInputLifecycleController(
                new StreamInputLifecycleController.MotionRouting() {
                    @Override
                    public void start() {
                        events.values.add("routing.start");
                    }

                    @Override
                    public void stop() {
                        events.values.add("routing.stop");
                    }

                    @Override
                    public void destroy() {
                        events.values.add("routing.destroy");
                    }
                },
                new StreamInputLifecycleController.ControllerDevices() {
                    @Override
                    public void stop() {
                        events.values.add("devices.stop");
                    }

                    @Override
                    public void destroy() {
                        events.values.add("devices.destroy");
                    }
                },
                new StreamInputLifecycleController.KeyboardRegistration() {
                    @Override
                    public void unregister() {
                        events.values.add("keyboard.unregister");
                    }
                });
    }

    private static final class Events {
        private final List<String> values = new ArrayList<>();
    }
}
