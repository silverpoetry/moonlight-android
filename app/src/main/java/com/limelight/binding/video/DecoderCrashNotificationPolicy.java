package com.limelight.binding.video;

import java.util.Objects;

/** Pure policy for selecting the decoder-crash notification, if any. */
public final class DecoderCrashNotificationPolicy {
    public enum Action {
        NONE,
        WARN,
        RESET_SETTINGS
    }

    private DecoderCrashNotificationPolicy() {
    }

    public static Action evaluate(DecoderCrashState state) {
        Objects.requireNonNull(state, "state");
        int crashCount = state.getCrashCount();
        if (crashCount == 0 ||
                crashCount == state.getAcknowledgedCrashCount()) {
            return Action.NONE;
        }
        return crashCount % 3 == 0
                ? Action.RESET_SETTINGS
                : Action.WARN;
    }
}
