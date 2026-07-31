package com.limelight.binding.input.virtual_controller.keyboard;

import java.util.Objects;

/**
 * Sole lifecycle and visibility owner for stream virtual-control overlays.
 *
 * <p>Overlays are created lazily, refreshed only after creation, hidden as a
 * group for lifecycle transitions, and permanently detached on destruction.</p>
 */
public final class StreamVirtualControlsController {
    public interface Factory {
        EditableVirtualControlOverlay createVirtualGamepad();

        EditableVirtualControlOverlay createVirtualKeys();

        FullKeyboardOverlay createFullKeyboard();
    }

    private final Factory factory;

    private EditableVirtualControlOverlay virtualGamepad;
    private EditableVirtualControlOverlay virtualKeys;
    private FullKeyboardOverlay fullKeyboard;
    private boolean destroyed;

    public StreamVirtualControlsController(Factory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public void showVirtualGamepad() {
        EditableVirtualControlOverlay overlay = ensureVirtualGamepad();
        if (overlay != null) {
            overlay.show();
        }
    }

    public void showVirtualKeys() {
        EditableVirtualControlOverlay overlay = ensureVirtualKeys();
        if (overlay != null) {
            overlay.show();
        }
    }

    public void toggleVirtualGamepad() {
        if (virtualGamepad == null) {
            showVirtualGamepad();
            return;
        }
        EditableVirtualControlOverlay overlay = ensureVirtualGamepad();
        if (overlay != null) {
            overlay.toggleVisibility();
        }
    }

    public void toggleVirtualKeys() {
        if (virtualKeys == null) {
            showVirtualKeys();
            return;
        }
        EditableVirtualControlOverlay overlay = ensureVirtualKeys();
        if (overlay != null) {
            overlay.toggleVisibility();
        }
    }

    public void toggleFullKeyboard() {
        if (fullKeyboard == null) {
            FullKeyboardOverlay overlay = ensureFullKeyboard();
            if (overlay != null) {
                overlay.refreshLayout();
                overlay.show();
            }
            return;
        }
        FullKeyboardOverlay overlay = ensureFullKeyboard();
        if (overlay != null) {
            overlay.toggleVisibility();
        }
    }

    public boolean isVirtualGamepadVisible() {
        return virtualGamepad != null && virtualGamepad.isVisible();
    }

    public boolean isVirtualKeysVisible() {
        return virtualKeys != null && virtualKeys.isVisible();
    }

    public boolean isVirtualGamepadCreated() {
        return virtualGamepad != null;
    }

    public boolean isEditingLayout() {
        return isEditing(virtualGamepad) || isEditing(virtualKeys);
    }

    public boolean setVirtualGamepadMode(VirtualControlEditMode mode) {
        if (!isVirtualGamepadVisible()) {
            return false;
        }
        virtualGamepad.switchMode(Objects.requireNonNull(mode, "mode"));
        return true;
    }

    public boolean setVirtualKeysMode(VirtualControlEditMode mode) {
        if (!isVirtualKeysVisible()) {
            return false;
        }
        virtualKeys.switchMode(Objects.requireNonNull(mode, "mode"));
        return true;
    }

    public VirtualControlEditMode getVirtualGamepadMode() {
        return virtualGamepad == null
                ? VirtualControlEditMode.NONE
                : virtualGamepad.getControllerMode();
    }

    public VirtualControlEditMode getVirtualKeysMode() {
        return virtualKeys == null
                ? VirtualControlEditMode.NONE
                : virtualKeys.getControllerMode();
    }

    public void refreshCreatedLayouts() {
        if (destroyed) {
            return;
        }
        if (virtualGamepad != null) {
            virtualGamepad.refreshLayout();
        }
        if (virtualKeys != null) {
            virtualKeys.refreshLayout();
        }
        if (fullKeyboard != null) {
            fullKeyboard.refreshLayout();
        }
    }

    public void hideAll() {
        if (virtualGamepad != null) {
            virtualGamepad.hide();
        }
        if (virtualKeys != null) {
            virtualKeys.hide();
        }
        if (fullKeyboard != null) {
            fullKeyboard.hide();
        }
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (virtualGamepad != null) {
            virtualGamepad.destroy();
            virtualGamepad = null;
        }
        if (virtualKeys != null) {
            virtualKeys.destroy();
            virtualKeys = null;
        }
        if (fullKeyboard != null) {
            fullKeyboard.destroy();
            fullKeyboard = null;
        }
    }

    private EditableVirtualControlOverlay ensureVirtualGamepad() {
        if (destroyed) {
            return null;
        }
        if (virtualGamepad == null) {
            virtualGamepad = Objects.requireNonNull(
                    factory.createVirtualGamepad(),
                    "virtual gamepad");
        }
        return virtualGamepad;
    }

    private EditableVirtualControlOverlay ensureVirtualKeys() {
        if (destroyed) {
            return null;
        }
        if (virtualKeys == null) {
            virtualKeys = Objects.requireNonNull(
                    factory.createVirtualKeys(),
                    "virtual keys");
        }
        return virtualKeys;
    }

    private FullKeyboardOverlay ensureFullKeyboard() {
        if (destroyed) {
            return null;
        }
        if (fullKeyboard == null) {
            fullKeyboard = Objects.requireNonNull(
                    factory.createFullKeyboard(),
                    "full keyboard");
        }
        return fullKeyboard;
    }

    private static boolean isEditing(
            EditableVirtualControlOverlay overlay) {
        return overlay != null &&
                overlay.isVisible() &&
                overlay.getControllerMode().isEditing();
    }
}
