package com.limelight.binding.input.virtual_controller.keyboard;

/**
 * Lifecycle and editing port implemented by a virtual gamepad or key overlay.
 */
public interface EditableVirtualControlOverlay {
    void show();

    void hide();

    void toggleVisibility();

    boolean isVisible();

    void refreshLayout();

    void switchMode(VirtualControlEditMode mode);

    VirtualControlEditMode getControllerMode();

    void destroy();
}
