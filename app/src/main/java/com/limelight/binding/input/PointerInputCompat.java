package com.limelight.binding.input;

import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

/**
 * Centralizes pointer-input API values and calls that are unavailable on the
 * project's full minSdk range.
 */
public final class PointerInputCompat {
    // These are inlined framework protocol values. Comparing or masking them
    // is safe on older Android versions where the corresponding input type
    // cannot be produced by the framework.
    public static final int SOURCE_MOUSE_RELATIVE = 0x0002_0004;
    public static final int BUTTON_STYLUS_PRIMARY = 1 << 5;
    public static final int BUTTON_STYLUS_SECONDARY = 1 << 6;

    private PointerInputCompat() {
    }

    public static boolean isMouseSource(int source) {
        return source == InputDevice.SOURCE_MOUSE ||
                source == SOURCE_MOUSE_RELATIVE;
    }

    public static int getActionButton(MotionEvent event) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return 0;
        }
        return getActionButtonApi23(event);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static int getActionButtonApi23(MotionEvent event) {
        return event.getActionButton();
    }
}
