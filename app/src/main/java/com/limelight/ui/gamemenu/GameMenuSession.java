package com.limelight.ui.gamemenu;

import com.limelight.binding.input.GameInputDevice;

import java.util.Objects;

/**
 * Owns transient input context for one visible game-menu instance.
 *
 * <p>Identity comparison prevents a stale menu dismissal from clearing the
 * device context of a newer menu.</p>
 */
public final class GameMenuSession<OwnerT> {
    private OwnerT owner;
    private GameInputDevice inputDevice;

    public void open(OwnerT owner, GameInputDevice inputDevice) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.inputDevice = inputDevice;
    }

    public boolean close(OwnerT owner) {
        if (this.owner != owner) {
            return false;
        }
        this.owner = null;
        inputDevice = null;
        return true;
    }

    public boolean isMouseEmulationAvailable() {
        return inputDevice != null;
    }

    public void toggleMouseEmulation() {
        if (inputDevice != null) {
            inputDevice.toggleMouseEmulation();
        }
    }
}
