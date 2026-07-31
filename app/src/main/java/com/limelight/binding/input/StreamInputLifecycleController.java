package com.limelight.binding.input;

import java.util.Objects;

/**
 * Owns Activity lifecycle transitions and ordered teardown for stream input resources.
 */
public final class StreamInputLifecycleController {
    public interface MotionRouting {
        void start();

        void stop();

        void destroy();
    }

    public interface ControllerDevices {
        void stop();

        void destroy();
    }

    public interface KeyboardRegistration {
        void unregister();
    }

    private final MotionRouting motionRouting;
    private final ControllerDevices controllerDevices;
    private final KeyboardRegistration keyboardRegistration;

    private boolean resumed;
    private boolean routingDestroyed;
    private boolean controllerDevicesStopped;
    private boolean destroyed;

    public StreamInputLifecycleController(
            MotionRouting motionRouting,
            ControllerDevices controllerDevices,
            KeyboardRegistration keyboardRegistration) {
        this.motionRouting = Objects.requireNonNull(
                motionRouting,
                "motionRouting");
        this.controllerDevices = Objects.requireNonNull(
                controllerDevices,
                "controllerDevices");
        this.keyboardRegistration = Objects.requireNonNull(
                keyboardRegistration,
                "keyboardRegistration");
    }

    public void resume() {
        if (destroyed || routingDestroyed ||
                controllerDevicesStopped || resumed) {
            return;
        }
        resumed = true;
        motionRouting.start();
    }

    public void pause(boolean finishing) {
        if (destroyed) {
            return;
        }
        if (resumed) {
            resumed = false;
            motionRouting.stop();
        }
        if (finishing) {
            stopControllerDevices();
        }
    }

    /**
     * Detaches motion callbacks before transport and media resources begin teardown.
     */
    public void detachRouting() {
        if (destroyed || routingDestroyed) {
            return;
        }
        if (resumed) {
            resumed = false;
            motionRouting.stop();
        }
        routingDestroyed = true;
        motionRouting.destroy();
    }

    /**
     * Releases controller devices and the keyboard-listener lease after media teardown.
     */
    public void destroy() {
        if (destroyed) {
            return;
        }
        detachRouting();
        stopControllerDevices();
        controllerDevices.destroy();
        keyboardRegistration.unregister();
        destroyed = true;
    }

    private void stopControllerDevices() {
        if (controllerDevicesStopped) {
            return;
        }
        controllerDevicesStopped = true;
        controllerDevices.stop();
    }
}
