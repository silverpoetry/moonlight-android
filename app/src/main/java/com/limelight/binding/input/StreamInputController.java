package com.limelight.binding.input;

import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.binding.input.pointer.ExternalPointerInputController;
import com.limelight.binding.input.touch.TouchInputController;
import com.limelight.binding.input.touch.TouchInputMode;

import java.util.Objects;

/**
 * Single motion-input entry point for a running stream.
 *
 * <p>This class owns source classification and dispatch precedence. The
 * Activity supplies only UI policy and forwards platform callbacks.</p>
 */
public final class StreamInputController {
    public interface Host {
        boolean shouldSuppressTouchscreenInput();
    }

    private final GamepadMotionInputHandler gamepadInputHandler;
    private final ExternalPointerInputController
            externalPointerInputController;
    private final TouchInputController touchInputController;
    private final Host host;

    public StreamInputController(
            GamepadMotionInputHandler gamepadInputHandler,
            ExternalPointerInputController
                    externalPointerInputController,
            TouchInputController touchInputController,
            Host host) {
        this.gamepadInputHandler = Objects.requireNonNull(
                gamepadInputHandler,
                "gamepadInputHandler");
        this.externalPointerInputController = Objects.requireNonNull(
                externalPointerInputController,
                "externalPointerInputController");
        this.touchInputController = Objects.requireNonNull(
                touchInputController,
                "touchInputController");
        this.host = Objects.requireNonNull(host, "host");
    }

    public void start() {
        touchInputController.start();
    }

    public void stop() {
        touchInputController.stop();
    }

    public void destroy() {
        touchInputController.destroy();
    }

    public void cancelActiveInput() {
        touchInputController.cancelActiveInput();
    }

    public void setTouchMode(TouchInputMode mode) {
        touchInputController.setMode(mode);
    }

    public void setTouchInputSuspended(boolean suspended) {
        touchInputController.setInputSuspended(suspended);
    }

    public boolean handleMotionEvent(
            View eventView,
            MotionEvent event) {
        Objects.requireNonNull(event, "event");
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            return gamepadInputHandler.handleMotionEvent(event);
        }

        InputDevice device = event.getDevice();
        int deviceSources = device != null ? device.getSources() : 0;
        if ((deviceSources & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 &&
                gamepadInputHandler.tryHandleTouchpadEvent(event)) {
            return true;
        }

        if (!ExternalPointerInputController
                .isPointerClassEvent(event)) {
            return false;
        }

        if (externalPointerInputController.canHandle(event)) {
            return externalPointerInputController
                    .handleMotionEvent(eventView, event);
        }

        if (host.shouldSuppressTouchscreenInput()) {
            return true;
        }
        return touchInputController.handleMotionEvent(eventView, event);
    }
}
