package com.limelight.binding.input.virtual_controller.keyboard;

/**
 * Interaction mode for a user-editable virtual-control overlay.
 *
 * <p>This model is shared with stream-menu UI without exposing the concrete
 * Android overlay implementation.</p>
 */
public enum VirtualControlEditMode {
    ACTIVE,
    MOVE_BUTTONS,
    RESIZE_BUTTONS,
    DISABLE_ENABLE_BUTTONS,
    NONE;

    public boolean isEditing() {
        return this == MOVE_BUTTONS ||
                this == RESIZE_BUTTONS ||
                this == DISABLE_ENABLE_BUTTONS;
    }
}
