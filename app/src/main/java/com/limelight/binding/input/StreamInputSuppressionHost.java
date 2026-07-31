package com.limelight.binding.input;

import java.util.Objects;

/** Adapts live UI suppression state to the input orchestrator. */
public final class StreamInputSuppressionHost
        implements StreamInputController.Host {
    public interface BooleanValue {
        boolean get();
    }

    private final BooleanValue suppressionState;

    public StreamInputSuppressionHost(BooleanValue suppressionState) {
        this.suppressionState = Objects.requireNonNull(
                suppressionState,
                "suppressionState");
    }

    @Override
    public boolean shouldSuppressTouchscreenInput() {
        return suppressionState.get();
    }
}
