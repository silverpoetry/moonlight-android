package com.limelight.virtualcontrols.action;

/**
 * A local client action that can be assigned to an editable virtual control.
 *
 * <p>The persistent ID is part of the saved-layout format. It must remain
 * stable when enum constants or presentation text are renamed.</p>
 */
public enum VirtualControlAction {
    TOGGLE_SOFT_KEYBOARD("toggle_soft_keyboard"),
    TOGGLE_VIRTUAL_KEYS("toggle_virtual_keys"),
    TOGGLE_FULL_KEYBOARD("toggle_full_keyboard"),
    TOGGLE_VIRTUAL_GAMEPAD("toggle_virtual_gamepad"),
    TOGGLE_FLOATING_BUTTON("toggle_floating_button"),
    TOGGLE_PERFORMANCE_OVERLAY("toggle_performance_overlay"),
    OPEN_STREAM_MENU("open_stream_menu");

    private final String persistentId;

    VirtualControlAction(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public static VirtualControlAction fromPersistentId(String value) {
        if (value == null) {
            return null;
        }
        for (VirtualControlAction action : values()) {
            if (action.persistentId.equals(value)) {
                return action;
            }
        }
        return null;
    }
}
