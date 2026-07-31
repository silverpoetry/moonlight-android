package com.limelight.binding.input;

/** Per-controller policy state for mouse-mode or game-menu activation. */
final class ControllerMouseModeActivationState {
    private static final long HOLD_THRESHOLD_MS = 750;

    private long lastPlayButtonDownTime;

    void observeButtonDown(
            ControllerDigitalButtonMapping.Target target,
            long eventTime,
            int repeatCount) {
        if (target == ControllerDigitalButtonMapping.Target.PLAY &&
                repeatCount == 0) {
            lastPlayButtonDownTime = eventTime;
        }
    }

    boolean shouldActivateOnRelease(
            ControllerDigitalButtonMapping.Target target,
            boolean enabled,
            int configuredButton,
            int currentInputMap,
            long eventTime) {
        if (!enabled) {
            return false;
        }

        switch (target) {
            case SPECIAL:
                return configuredButton == 1 &&
                        (currentInputMap &
                                target.getInputMask()) != 0;
            case PLAY:
                return configuredButton == 0 &&
                        isHeldAndPastThreshold(
                                currentInputMap,
                                target,
                                eventTime);
            case BACK:
                return configuredButton == 2 &&
                        isHeldAndPastThreshold(
                                currentInputMap,
                                target,
                                eventTime);
            default:
                return false;
        }
    }

    private boolean isHeldAndPastThreshold(
            int currentInputMap,
            ControllerDigitalButtonMapping.Target target,
            long eventTime) {
        return (currentInputMap & target.getInputMask()) != 0 &&
                eventTime - lastPlayButtonDownTime >
                        HOLD_THRESHOLD_MS;
    }

    void restoreFrom(
            ControllerMouseModeActivationState previousState) {
        lastPlayButtonDownTime =
                previousState.lastPlayButtonDownTime;
    }
}
