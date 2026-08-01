package com.limelight.binding.input;

import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.binding.input.pointer.ExternalPointerInputController;
import com.limelight.binding.input.touch.TouchInputController;
import com.limelight.binding.input.touch.TouchInputMode;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

import java.util.Objects;

/**
 * Single motion-input entry point for a running stream.
 *
 * <p>This class owns source classification and dispatch precedence. The
 * Activity supplies only UI policy and forwards platform callbacks.</p>
 */
public final class StreamInputController
        implements StreamInputLifecycleController.MotionRouting {
    public interface Host {
        boolean shouldSuppressTouchscreenInput();
    }

    private final GamepadInputHandler gamepadInputHandler;
    private final ExternalPointerInputController
            externalPointerInputController;
    private final TouchInputController touchInputController;
    private final InputSettingsState settingsState;
    private final Host host;

    public StreamInputController(
            GamepadInputHandler gamepadInputHandler,
            ExternalPointerInputController
                    externalPointerInputController,
            TouchInputController touchInputController,
            InputSettingsState settingsState,
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
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
        this.host = Objects.requireNonNull(host, "host");
    }

    @Override
    public void start() {
        touchInputController.start();
    }

    @Override
    public void stop() {
        touchInputController.stop();
    }

    @Override
    public void destroy() {
        touchInputController.destroy();
    }

    public void cancelActiveInput() {
        touchInputController.cancelActiveInput();
    }

    public void setTouchMode(TouchInputMode mode) {
        Objects.requireNonNull(mode, "mode");
        touchInputController.setMode(mode);
        replaceLiveSettings(
                settingsState.get()
                        .toBuilder()
                        .setTouchModePreferenceValue(
                                mode.getPreferenceValue())
                        .build());
    }

    public InputSettings getSettings() {
        return settingsState.get();
    }

    public void replaceLiveSettings(InputSettings settings) {
        InputSettings previous = settingsState.get();
        InputSettings current = Objects.requireNonNull(
                settings,
                "settings");
        settingsState.replace(current);
        touchInputController.onInputSettingsChanged(
                previous,
                current);
    }

    public void onInputSettingsChanged(
            InputSettings previous,
            InputSettings current) {
        touchInputController.onInputSettingsChanged(
                Objects.requireNonNull(previous, "previous"),
                Objects.requireNonNull(current, "current"));
    }

    public void setAbsoluteMouseMode(boolean enabled) {
        replaceLiveSettings(
                settingsState.get()
                        .toBuilder()
                        .setAbsoluteMouseMode(enabled)
                        .build());
    }

    public void setDirectTouchSensitivityEnabled(
            boolean enabled) {
        replaceLiveSettings(
                settingsState.get()
                        .toBuilder()
                        .setDirectTouchSensitivityEnabled(enabled)
                        .build());
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
