package com.limelight.binding.input.virtual_controller.keyboard;

/** Lifecycle port for the in-stream full-keyboard overlay. */
public interface FullKeyboardOverlay {
    void show();

    void hide();

    void toggleVisibility();

    boolean isVisible();

    void refreshLayout();

    void destroy();
}
