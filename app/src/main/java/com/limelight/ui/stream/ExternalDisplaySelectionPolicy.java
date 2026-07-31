package com.limelight.ui.stream;

/** Selects the first non-default Android display for external presentation. */
public final class ExternalDisplaySelectionPolicy {
    public static final int NO_DISPLAY = -1;

    private ExternalDisplaySelectionPolicy() {
    }

    public static int select(
            int defaultDisplayId,
            int[] availableDisplayIds) {
        if (availableDisplayIds == null) {
            return NO_DISPLAY;
        }
        for (int displayId : availableDisplayIds) {
            if (displayId != defaultDisplayId) {
                return displayId;
            }
        }
        return NO_DISPLAY;
    }
}
