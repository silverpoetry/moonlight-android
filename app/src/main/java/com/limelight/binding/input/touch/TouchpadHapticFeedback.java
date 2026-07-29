package com.limelight.binding.input.touch;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

// Keeps remote mouse-button haptics consistent across all touchscreen input modes.
final class TouchpadHapticFeedback {
    private final View targetView;

    TouchpadHapticFeedback(View targetView) {
        this.targetView = targetView;
    }

    void performButtonPress() {
        targetView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    void performButtonRelease() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
                targetView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)) {
            return;
        }

        // Older Android versions and some vendor implementations don't expose a
        // dedicated release effect. Keep release feedback observable there too.
        targetView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }
}
