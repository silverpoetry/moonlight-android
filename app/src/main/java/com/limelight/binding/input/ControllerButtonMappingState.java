package com.limelight.binding.input;

/** Mutable key-layout fallback state owned by one controller session. */
final class ControllerButtonMappingState {
    private boolean backIsStart;
    private boolean modeIsSelect;

    ControllerButtonMappingState(
            boolean backIsStart,
            boolean modeIsSelect) {
        this.backIsStart = backIsStart;
        this.modeIsSelect = modeIsSelect;
    }

    void observeStartButton() {
        backIsStart = false;
    }

    void observeSelectButton() {
        modeIsSelect = false;
    }

    boolean shouldMapBackToStart() {
        return backIsStart;
    }

    boolean shouldMapModeToSelect() {
        return modeIsSelect;
    }
}
