package com.limelight.ui.gamemenu;

import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.virtualcontrols.action.VirtualControlAction;

import java.util.List;

/** Migrates the former synthetic-key representation of local actions. */
public final class LegacyVirtualControlActionMigration {
    private static final int KEYBOARD_BUTTON_TYPE = 4;

    private LegacyVirtualControlActionMigration() {
    }

    public static boolean migrate(List<GameMenuQuickBean> controls) {
        boolean changed = false;
        for (GameMenuQuickBean control : controls) {
            VirtualControlAction action = legacyAction(control);
            if (action == null) {
                continue;
            }
            control.setLocalAction(action);
            control.setDesc(control.getName());
            changed = true;
        }
        return changed;
    }

    private static VirtualControlAction legacyAction(
            GameMenuQuickBean control) {
        if (control == null ||
                control.getBtnType() != KEYBOARD_BUTTON_TYPE ||
                control.isGamePad() ||
                control.getLocalActionId() != null) {
            return null;
        }

        String codes = control.getCodes();
        String description = control.getDesc();
        if (codes == null || description == null) {
            return null;
        }

        switch (codes) {
            case "29,52,37,52,7":
                return "AXIX0".equals(description)
                        ? VirtualControlAction.TOGGLE_SOFT_KEYBOARD
                        : null;
            case "29,52,37,52,8":
                return "AXIX1".equals(description)
                        ? VirtualControlAction.TOGGLE_VIRTUAL_KEYS
                        : null;
            case "29,52,37,52,9":
                return "AXIX2".equals(description)
                        ? VirtualControlAction.TOGGLE_FULL_KEYBOARD
                        : null;
            case "29,52,37,52,10":
                return "AXIX3".equals(description)
                        ? VirtualControlAction.TOGGLE_VIRTUAL_GAMEPAD
                        : null;
            case "29,52,37,52,11":
                return "AXIX4".equals(description)
                        ? VirtualControlAction.TOGGLE_FLOATING_BUTTON
                        : null;
            case "29,52,37,52,12":
                return "AXIX5".equals(description)
                        ? VirtualControlAction.TOGGLE_PERFORMANCE_OVERLAY
                        : null;
            case "29,52,37,52,13":
                return "AXIX6".equals(description)
                        ? VirtualControlAction.OPEN_STREAM_MENU
                        : null;
            default:
                return null;
        }
    }
}
