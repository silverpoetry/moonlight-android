package com.limelight.binding.input;

import android.view.MotionEvent;

/**
 * Narrow motion-event boundary exposed by the gamepad subsystem.
 */
public interface GamepadMotionInputHandler {
    boolean handleMotionEvent(MotionEvent event);

    boolean tryHandleTouchpadEvent(MotionEvent event);
}
