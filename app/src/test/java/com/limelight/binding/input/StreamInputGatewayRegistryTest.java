package com.limelight.binding.input;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public final class StreamInputGatewayRegistryTest {
    @Test
    public void registrationExposesGatewayUntilUnregistered() {
        StreamInputGatewayRegistry registry =
                new StreamInputGatewayRegistry();
        FakeGateway gateway = new FakeGateway();

        StreamInputGatewayRegistry.Registration registration =
                registry.register(gateway);

        assertSame(gateway, registry.getActiveGateway());

        registration.unregister();

        assertNull(registry.getActiveGateway());
    }

    @Test
    public void staleRegistrationCannotClearNewerGateway() {
        StreamInputGatewayRegistry registry =
                new StreamInputGatewayRegistry();
        FakeGateway firstGateway = new FakeGateway();
        FakeGateway secondGateway = new FakeGateway();

        StreamInputGatewayRegistry.Registration firstRegistration =
                registry.register(firstGateway);
        StreamInputGatewayRegistry.Registration secondRegistration =
                registry.register(secondGateway);

        firstRegistration.unregister();

        assertSame(secondGateway, registry.getActiveGateway());

        secondRegistration.unregister();

        assertNull(registry.getActiveGateway());
    }

    private static final class FakeGateway implements StreamInputGateway {
        @Override
        public boolean isInputReady() {
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            return true;
        }

        @Override
        public void sendRelativeMouseMove(int deltaX, int deltaY) {
        }

        @Override
        public void sendMouseButton(int buttonId, boolean down) {
        }

        @Override
        public void sendHighResolutionScroll(boolean up) {
        }

        @Override
        public void sendImeText(String text) {
        }

        @Override
        public void sendImeBackspace(int count) {
        }

        @Override
        public void sendImeForwardDelete(int count) {
        }
    }
}
