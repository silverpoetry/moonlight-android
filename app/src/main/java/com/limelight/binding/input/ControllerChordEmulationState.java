package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

/** Allocation-free chord emulation state owned by one controller session. */
final class ControllerChordEmulationState {
    private static final long BUMPER_UP_GRACE_PERIOD_MS = 100;

    private static final int EMULATING_SPECIAL = 0x1;
    private static final int EMULATING_SELECT = 0x2;
    private static final int EMULATING_TOUCHPAD = 0x4;

    private boolean hasModeButton;
    private boolean hasSelectButton;
    private boolean clickpadEmulationRequired;
    private boolean exitPending;
    private int emulatingButtonFlags;
    private long lastLeftBumperUpTime;
    private long lastRightBumperUpTime;

    ControllerChordEmulationState(
            boolean hasModeButton,
            boolean hasSelectButton) {
        this.hasModeButton = hasModeButton;
        this.hasSelectButton = hasSelectButton;
    }

    void observeModeButton() {
        hasModeButton = true;
    }

    void observeSelectButton() {
        hasSelectButton = true;
    }

    void recordLeftBumperUp(long eventTime) {
        lastLeftBumperUpTime = eventTime;
    }

    void recordRightBumperUp(long eventTime) {
        lastRightBumperUpTime = eventTime;
    }

    void setClickpadEmulationRequired(boolean required) {
        clickpadEmulationRequired = required;
    }

    boolean isClickpadEmulationRequired() {
        return clickpadEmulationRequired;
    }

    int applyButtonDown(int inputMap, long eventTime) {
        if (inputMap ==
                (ControllerPacket.BACK_FLAG |
                        ControllerPacket.PLAY_FLAG |
                        ControllerPacket.LB_FLAG |
                        ControllerPacket.RB_FLAG)) {
            exitPending = true;
        }

        if (!hasSelectButton) {
            if (inputMap ==
                    (ControllerPacket.PLAY_FLAG |
                            ControllerPacket.LB_FLAG) ||
                    (inputMap == ControllerPacket.PLAY_FLAG &&
                            eventTime - lastLeftBumperUpTime <=
                                    BUMPER_UP_GRACE_PERIOD_MS)) {
                inputMap &= ~(ControllerPacket.PLAY_FLAG |
                        ControllerPacket.LB_FLAG);
                inputMap |= ControllerPacket.BACK_FLAG;
                emulatingButtonFlags |= EMULATING_SELECT;
            }
        }
        else if (clickpadEmulationRequired) {
            if (inputMap ==
                    (ControllerPacket.BACK_FLAG |
                            ControllerPacket.LB_FLAG) ||
                    (inputMap == ControllerPacket.BACK_FLAG &&
                            eventTime - lastLeftBumperUpTime <=
                                    BUMPER_UP_GRACE_PERIOD_MS)) {
                inputMap &= ~(ControllerPacket.BACK_FLAG |
                        ControllerPacket.LB_FLAG);
                inputMap |= ControllerPacket.TOUCHPAD_FLAG;
                emulatingButtonFlags |= EMULATING_TOUCHPAD;
            }
        }

        if (!hasModeButton) {
            if (hasSelectButton) {
                if (inputMap ==
                        (ControllerPacket.PLAY_FLAG |
                                ControllerPacket.BACK_FLAG)) {
                    inputMap &= ~(ControllerPacket.PLAY_FLAG |
                            ControllerPacket.BACK_FLAG);
                    inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;
                    emulatingButtonFlags |= EMULATING_SPECIAL;
                }
            }
            else if (inputMap ==
                    (ControllerPacket.PLAY_FLAG |
                            ControllerPacket.RB_FLAG) ||
                    (inputMap == ControllerPacket.PLAY_FLAG &&
                            eventTime - lastRightBumperUpTime <=
                                    BUMPER_UP_GRACE_PERIOD_MS)) {
                inputMap &= ~(ControllerPacket.PLAY_FLAG |
                        ControllerPacket.RB_FLAG);
                inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;
                emulatingButtonFlags |= EMULATING_SPECIAL;
            }
        }

        return inputMap;
    }

    int applyButtonUp(int inputMap) {
        if ((emulatingButtonFlags & EMULATING_SELECT) != 0 &&
                ((inputMap & ControllerPacket.PLAY_FLAG) == 0 ||
                        (inputMap & ControllerPacket.LB_FLAG) == 0)) {
            inputMap &= ~ControllerPacket.BACK_FLAG;
            emulatingButtonFlags &= ~EMULATING_SELECT;
        }

        if ((emulatingButtonFlags & EMULATING_SPECIAL) != 0 &&
                ((inputMap & ControllerPacket.PLAY_FLAG) == 0 ||
                        ((inputMap & ControllerPacket.BACK_FLAG) == 0 &&
                                (inputMap & ControllerPacket.RB_FLAG) == 0))) {
            inputMap &= ~ControllerPacket.SPECIAL_BUTTON_FLAG;
            emulatingButtonFlags &= ~EMULATING_SPECIAL;
        }

        if ((emulatingButtonFlags & EMULATING_TOUCHPAD) != 0 &&
                ((inputMap & ControllerPacket.BACK_FLAG) == 0 ||
                        (inputMap & ControllerPacket.LB_FLAG) == 0)) {
            inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
            emulatingButtonFlags &= ~EMULATING_TOUCHPAD;
        }

        return inputMap;
    }

    boolean shouldFinishAfterButtonUp(int inputMap) {
        return exitPending && inputMap == 0;
    }

    void restoreFrom(
            ControllerChordEmulationState previousState) {
        hasModeButton |= previousState.hasModeButton;
        hasSelectButton |= previousState.hasSelectButton;
        clickpadEmulationRequired =
                previousState.clickpadEmulationRequired;
        exitPending = previousState.exitPending;
        emulatingButtonFlags =
                previousState.emulatingButtonFlags;
        lastLeftBumperUpTime =
                previousState.lastLeftBumperUpTime;
        lastRightBumperUpTime =
                previousState.lastRightBumperUpTime;
    }
}
