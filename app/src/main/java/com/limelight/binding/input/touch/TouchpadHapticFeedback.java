package com.limelight.binding.input.touch;

import android.view.HapticFeedbackConstants;
import android.view.View;

// Uses Android's standard long-press haptic, matching the feedback produced by
// normal long-click UI elements such as the host card long-press action.
final class TouchpadHapticFeedback {
    private final View targetView;

    TouchpadHapticFeedback(View targetView) {
        this.targetView = targetView;
    }

    void performPhysicalClick() {
        targetView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }
}
