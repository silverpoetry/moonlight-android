package com.limelight.binding.input.capture;

/** Pure session state for input grabbing and the user's local-cursor choice. */
public final class StreamInputCaptureState {
    private boolean inputGrabbed = true;
    private boolean localCursorVisible;

    public boolean isInputGrabbed() {
        return inputGrabbed;
    }

    public boolean isLocalCursorVisible() {
        return localCursorVisible;
    }

    public void setInputGrabbed(boolean inputGrabbed) {
        this.inputGrabbed = inputGrabbed;
    }

    public void toggleLocalCursorVisibility() {
        inputGrabbed = true;
        localCursorVisible = !localCursorVisible;
    }
}
