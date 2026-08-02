package com.limelight.ui;

import com.limelight.virtualcontrols.action.VirtualControlAction;

/**
 * Commands that affect only the local streaming UI.
 */
public interface StreamUiActions {
    void performStreamUiAction(VirtualControlAction action);
}
