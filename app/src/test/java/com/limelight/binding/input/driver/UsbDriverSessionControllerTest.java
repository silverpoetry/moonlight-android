package com.limelight.binding.input.driver;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class UsbDriverSessionControllerTest {
    @Test
    public void connectDestroyRevokesCallbacksBeforeUnbind() {
        Events events = new Events();
        RecordingBinding binding = new RecordingBinding(events);
        UsbDriverSessionController controller =
                new UsbDriverSessionController(binding);

        assertTrue(controller.bind());
        controller.onConnected(new RecordingEndpoint(events, "first"));
        assertTrue(controller.isConnected());

        controller.destroy();
        controller.destroy();

        assertFalse(controller.isConnected());
        events.assertValues(
                "binding.bind",
                "first.activate",
                "first.deactivate",
                "binding.unbind");
    }

    @Test
    public void acceptedBindingIsUnboundWhenDestroyIsReentrant() {
        Events events = new Events();
        ReentrantBinding binding = new ReentrantBinding(events);
        UsbDriverSessionController controller =
                new UsbDriverSessionController(binding);
        binding.controller = controller;

        assertFalse(controller.bind());

        events.assertValues(
                "binding.bind",
                "binding.unbind");
    }

    @Test
    public void rejectedBindingDoesNotUnbindOrAcceptLateEndpoint() {
        Events events = new Events();
        RecordingBinding binding = new RecordingBinding(events);
        binding.accept = false;
        UsbDriverSessionController controller =
                new UsbDriverSessionController(binding);

        assertFalse(controller.bind());
        controller.onConnected(new RecordingEndpoint(events, "late"));
        controller.destroy();

        events.assertValues(
                "binding.bind",
                "late.deactivate");
    }

    @Test
    public void reconnectReplacesEndpointWithoutRebinding() {
        Events events = new Events();
        RecordingBinding binding = new RecordingBinding(events);
        UsbDriverSessionController controller =
                new UsbDriverSessionController(binding);

        controller.bind();
        controller.onConnected(new RecordingEndpoint(events, "first"));
        controller.onDisconnected();
        assertFalse(controller.isConnected());
        controller.onConnected(new RecordingEndpoint(events, "second"));
        assertTrue(controller.isConnected());
        controller.destroy();

        events.assertValues(
                "binding.bind",
                "first.activate",
                "second.activate",
                "second.deactivate",
                "binding.unbind");
    }

    @Test
    public void replacementRevokesOldEndpointBeforeActivatingNewOne() {
        Events events = new Events();
        RecordingBinding binding = new RecordingBinding(events);
        UsbDriverSessionController controller =
                new UsbDriverSessionController(binding);

        controller.bind();
        controller.onConnected(new RecordingEndpoint(events, "first"));
        controller.onConnected(new RecordingEndpoint(events, "second"));
        controller.destroy();

        events.assertValues(
                "binding.bind",
                "first.activate",
                "first.deactivate",
                "second.activate",
                "second.deactivate",
                "binding.unbind");
    }

    @Test
    public void failedActivationRevokesPartialEndpointLease() {
        Events events = new Events();
        RecordingBinding binding = new RecordingBinding(events);
        UsbDriverSessionController controller =
                new UsbDriverSessionController(binding);
        controller.bind();

        try {
            controller.onConnected(
                    new UsbDriverSessionController.Endpoint() {
                        @Override
                        public void activate() {
                            events.values.add("endpoint.activate");
                            throw new IllegalStateException("failed");
                        }

                        @Override
                        public void deactivate() {
                            events.values.add("endpoint.deactivate");
                        }
                    });
            org.junit.Assert.fail("Expected activation failure");
        } catch (IllegalStateException expected) {
            // The activation failure is propagated after callback cleanup.
        }
        assertFalse(controller.isConnected());
        controller.destroy();

        events.assertValues(
                "binding.bind",
                "endpoint.activate",
                "endpoint.deactivate",
                "binding.unbind");
    }

    private static class RecordingBinding
            implements UsbDriverSessionController.ServiceBinding {
        final Events events;
        boolean accept = true;

        RecordingBinding(Events events) {
            this.events = events;
        }

        @Override
        public boolean bind() {
            events.values.add("binding.bind");
            return accept;
        }

        @Override
        public void unbind() {
            events.values.add("binding.unbind");
        }
    }

    private static final class ReentrantBinding
            extends RecordingBinding {
        UsbDriverSessionController controller;

        private ReentrantBinding(Events events) {
            super(events);
        }

        @Override
        public boolean bind() {
            super.bind();
            controller.destroy();
            return true;
        }
    }

    private static final class RecordingEndpoint
            implements UsbDriverSessionController.Endpoint {
        private final Events events;
        private final String name;

        private RecordingEndpoint(Events events, String name) {
            this.events = events;
            this.name = name;
        }

        @Override
        public void activate() {
            events.values.add(name + ".activate");
        }

        @Override
        public void deactivate() {
            events.values.add(name + ".deactivate");
        }
    }

    private static final class Events {
        private final List<String> values = new ArrayList<>();

        private void assertValues(String... expected) {
            org.junit.Assert.assertEquals(
                    Arrays.asList(expected),
                    values);
        }
    }
}
