package com.limelight.ui;

/**
 * Commands that affect only the local streaming UI.
 */
public interface StreamUiActions {
    enum Action {
        TOGGLE_SOFT_KEYBOARD,
        TOGGLE_VIRTUAL_KEYS,
        TOGGLE_FULL_KEYBOARD,
        TOGGLE_VIRTUAL_GAMEPAD,
        TOGGLE_FLOATING_BUTTON,
        TOGGLE_PERFORMANCE_OVERLAY,
        OPEN_STREAM_MENU
    }

    void performStreamUiAction(Action action);
}
