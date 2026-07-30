package com.limelight.ui.gamemenu;

import com.limelight.binding.input.GameInputDevice;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GameMenuSessionTest {
    @Test
    public void currentOwnerControlsDeviceContext() {
        GameMenuSession<Object> session = new GameMenuSession<>();
        Object owner = new Object();
        FakeInputDevice inputDevice = new FakeInputDevice();

        session.open(owner, inputDevice);

        assertTrue(session.isMouseEmulationAvailable());
        session.toggleMouseEmulation();
        assertTrue(inputDevice.toggled);

        assertTrue(session.close(owner));
        assertFalse(session.isMouseEmulationAvailable());
    }

    @Test
    public void staleOwnerCannotClearNewerDeviceContext() {
        GameMenuSession<Object> session = new GameMenuSession<>();
        Object firstOwner = new Object();
        Object secondOwner = new Object();
        FakeInputDevice secondDevice = new FakeInputDevice();

        session.open(firstOwner, new FakeInputDevice());
        session.open(secondOwner, secondDevice);

        assertFalse(session.close(firstOwner));
        assertTrue(session.isMouseEmulationAvailable());

        session.toggleMouseEmulation();
        assertTrue(secondDevice.toggled);
    }

    private static final class FakeInputDevice
            implements GameInputDevice {
        private boolean toggled;

        @Override
        public void toggleMouseEmulation() {
            toggled = true;
        }
    }
}
