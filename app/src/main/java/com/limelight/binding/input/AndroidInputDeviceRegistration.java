package com.limelight.binding.input;

import android.hardware.input.InputManager;

import java.util.Objects;

/** Owns one InputManager listener registration. */
public final class AndroidInputDeviceRegistration
        implements StreamInputLifecycleController.KeyboardRegistration {
    private final InputManager inputManager;
    private final InputManager.InputDeviceListener listener;
    private boolean registered = true;

    public static AndroidInputDeviceRegistration register(
            InputManager inputManager,
            InputManager.InputDeviceListener listener) {
        InputManager manager = Objects.requireNonNull(
                inputManager,
                "inputManager");
        InputManager.InputDeviceListener inputListener =
                Objects.requireNonNull(listener, "listener");
        manager.registerInputDeviceListener(inputListener, null);
        return new AndroidInputDeviceRegistration(
                manager,
                inputListener);
    }

    private AndroidInputDeviceRegistration(
            InputManager inputManager,
            InputManager.InputDeviceListener listener) {
        this.inputManager = inputManager;
        this.listener = listener;
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }
        registered = false;
        inputManager.unregisterInputDeviceListener(listener);
    }
}
