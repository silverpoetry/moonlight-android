package com.limelight.binding.input;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Narrow input-event boundary exposed by the gamepad subsystem.
 */
public interface GamepadInputHandler {
    boolean isGameControllerDevice(InputDevice device);

    boolean handleButtonDown(KeyEvent event);

    boolean handleButtonUp(KeyEvent event);

    boolean handleMotionEvent(MotionEvent event);

    boolean tryHandleTouchpadEvent(MotionEvent event);
}
