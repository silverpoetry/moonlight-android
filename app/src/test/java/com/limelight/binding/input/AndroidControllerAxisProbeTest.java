package com.limelight.binding.input;

import android.view.MotionEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class AndroidControllerAxisProbeTest {
    @Test
    public void profileAxesMapToAndroidConstants() {
        assertAxis(ControllerAxisProfile.Axis.X, MotionEvent.AXIS_X);
        assertAxis(ControllerAxisProfile.Axis.Y, MotionEvent.AXIS_Y);
        assertAxis(ControllerAxisProfile.Axis.Z, MotionEvent.AXIS_Z);
        assertAxis(ControllerAxisProfile.Axis.RZ, MotionEvent.AXIS_RZ);
        assertAxis(ControllerAxisProfile.Axis.RX, MotionEvent.AXIS_RX);
        assertAxis(ControllerAxisProfile.Axis.RY, MotionEvent.AXIS_RY);
        assertAxis(
                ControllerAxisProfile.Axis.LEFT_TRIGGER,
                MotionEvent.AXIS_LTRIGGER);
        assertAxis(
                ControllerAxisProfile.Axis.RIGHT_TRIGGER,
                MotionEvent.AXIS_RTRIGGER);
        assertAxis(
                ControllerAxisProfile.Axis.BRAKE,
                MotionEvent.AXIS_BRAKE);
        assertAxis(
                ControllerAxisProfile.Axis.GAS,
                MotionEvent.AXIS_GAS);
        assertAxis(
                ControllerAxisProfile.Axis.THROTTLE,
                MotionEvent.AXIS_THROTTLE);
        assertAxis(
                ControllerAxisProfile.Axis.HAT_X,
                MotionEvent.AXIS_HAT_X);
        assertAxis(
                ControllerAxisProfile.Axis.HAT_Y,
                MotionEvent.AXIS_HAT_Y);
        assertAxis(ControllerAxisProfile.Axis.NONE, -1);
    }

    private static void assertAxis(
            ControllerAxisProfile.Axis axis,
            int expected) {
        assertEquals(
                expected,
                AndroidControllerAxisProbe.toAndroidAxis(axis));
    }
}
