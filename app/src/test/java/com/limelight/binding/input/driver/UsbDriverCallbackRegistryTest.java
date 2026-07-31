package com.limelight.binding.input.driver;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class UsbDriverCallbackRegistryTest {
    @Test
    public void newerLeaseAtomicallyReplacesOlderCallbacks() {
        UsbDriverCallbackRegistry registry =
                new UsbDriverCallbackRegistry();
        StubInputListener firstInput = new StubInputListener();
        StubStateListener firstState = new StubStateListener();
        StubInputListener secondInput = new StubInputListener();
        StubStateListener secondState = new StubStateListener();

        long firstLease = registry.acquire(firstInput, firstState);
        long secondLease = registry.acquire(secondInput, secondState);

        assertTrue(secondLease > firstLease);
        assertSame(secondInput, registry.get().inputListener);
        assertSame(secondState, registry.get().stateListener);
    }

    @Test
    public void staleReleaseCannotClearNewerLease() {
        UsbDriverCallbackRegistry registry =
                new UsbDriverCallbackRegistry();
        long staleLease = registry.acquire(
                new StubInputListener(),
                new StubStateListener());
        StubInputListener currentInput = new StubInputListener();
        StubStateListener currentState = new StubStateListener();
        long currentLease = registry.acquire(
                currentInput,
                currentState);

        registry.release(staleLease);

        assertSame(currentInput, registry.get().inputListener);
        assertSame(currentState, registry.get().stateListener);

        registry.release(currentLease);
        assertNull(registry.get());
    }

    @Test
    public void clearInvalidatesAnyActiveLease() {
        UsbDriverCallbackRegistry registry =
                new UsbDriverCallbackRegistry();
        registry.acquire(
                new StubInputListener(),
                new StubStateListener());

        registry.clear();

        assertNull(registry.get());
    }

    private static final class StubStateListener
            implements UsbDriverService.UsbDriverStateListener {
        @Override
        public void onUsbPermissionPromptStarting() {
        }

        @Override
        public void onUsbPermissionPromptCompleted() {
        }
    }

    private static final class StubInputListener
            implements UsbDriverListener {
        @Override
        public void reportControllerState(
                int controllerId,
                int buttonFlags,
                float leftStickX,
                float leftStickY,
                float rightStickX,
                float rightStickY,
                float leftTrigger,
                float rightTrigger) {
        }

        @Override
        public void reportControllerMotion(
                int controllerId,
                byte motionType,
                float motionX,
                float motionY,
                float motionZ) {
        }

        @Override
        public void reportControllerTouchpadEvent(
                int controllerId,
                byte eventType,
                int pointerId,
                float x,
                float y,
                float pressure) {
        }

        @Override
        public void deviceRemoved(AbstractController controller) {
        }

        @Override
        public void deviceAdded(AbstractController controller) {
        }
    }
}
