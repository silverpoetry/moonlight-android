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

    void restoreLearnedStateFrom(
            ControllerButtonMappingState previousState) {
        // Device recreation may discover stricter capabilities than the old
        // snapshot. Migration can disable a fallback that was disproved at
        // runtime, but it must never re-enable one rejected by the new probe.
        backIsStart &= previousState.backIsStart;
        modeIsSelect &= previousState.modeIsSelect;
    }
}
