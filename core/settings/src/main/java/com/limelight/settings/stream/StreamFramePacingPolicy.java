package com.limelight.settings.stream;

import java.util.Objects;

/**
 * Resolves display-dependent frame pacing without mutating persisted settings.
 */
public final class StreamFramePacingPolicy {
    private StreamFramePacingPolicy() {
    }

    public static Decision resolve(
            StreamDecoderSettings.FramePacing requestedMode,
            int streamFps,
            float displayRefreshRate) {
        Objects.requireNonNull(requestedMode, "requestedMode");
        if (streamFps <= 0) {
            throw new IllegalArgumentException(
                    "Stream FPS must be positive");
        }

        if (requestedMode !=
                StreamDecoderSettings.FramePacing.CAP_FPS) {
            return new Decision(streamFps, requestedMode);
        }

        int roundedRefreshRate =
                Float.isFinite(displayRefreshRate)
                        ? Math.round(displayRefreshRate)
                        : 0;
        if (streamFps < roundedRefreshRate) {
            return new Decision(streamFps, requestedMode);
        }
        if (streamFps > roundedRefreshRate + 3 ||
                roundedRefreshRate <= 49) {
            return new Decision(
                    streamFps,
                    StreamDecoderSettings.FramePacing.BALANCED);
        }
        return new Decision(
                roundedRefreshRate - 1,
                requestedMode);
    }

    public static final class Decision {
        private final int targetFps;
        private final StreamDecoderSettings.FramePacing
                effectiveMode;

        Decision(
                int targetFps,
                StreamDecoderSettings.FramePacing effectiveMode) {
            this.targetFps = targetFps;
            this.effectiveMode = Objects.requireNonNull(
                    effectiveMode,
                    "effectiveMode");
        }

        public int getTargetFps() {
            return targetFps;
        }

        public StreamDecoderSettings.FramePacing getEffectiveMode() {
            return effectiveMode;
        }
    }
}
