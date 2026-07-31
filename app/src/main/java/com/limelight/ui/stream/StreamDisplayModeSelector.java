package com.limelight.ui.stream;

import java.util.Objects;

/**
 * Selects the physical display mode for one stream without Android or window
 * side effects.
 */
public final class StreamDisplayModeSelector {
    private static final int MAX_SAFE_WIDTH = 4096;
    private static final int REFRESH_RATE_TOLERANCE_HZ = 3;

    private StreamDisplayModeSelector() {
    }

    public static Mode select(
            int streamWidth,
            int streamHeight,
            int streamFps,
            boolean nativeResolutionStream,
            boolean reduceRefreshRate,
            Mode currentMode,
            Iterable<Mode> supportedModes) {
        if (streamWidth <= 0 ||
                streamHeight <= 0 ||
                streamFps <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions and FPS must be positive");
        }
        Objects.requireNonNull(currentMode, "currentMode");
        Objects.requireNonNull(supportedModes, "supportedModes");

        Mode bestMode = currentMode;
        boolean refreshRateIsGood =
                isGoodRefreshRateMatch(
                        bestMode.refreshRate,
                        streamFps);
        boolean refreshRateIsEqual =
                isEqualRefreshRateMatch(
                        bestMode.refreshRate,
                        streamFps);

        for (Mode candidate :
                supportedModes) {
            Objects.requireNonNull(candidate, "supportedMode");
            boolean refreshRateReduced =
                    candidate.refreshRate <
                            bestMode.refreshRate;
            boolean resolutionReduced =
                    candidate.width < bestMode.width ||
                            candidate.height < bestMode.height;
            boolean resolutionFitsStream =
                    candidate.width >= streamWidth &&
                            candidate.height >= streamHeight;

            if (candidate.width > MAX_SAFE_WIDTH &&
                    streamWidth <= MAX_SAFE_WIDTH) {
                continue;
            }

            if (streamWidth < 3840 &&
                    streamFps <= 60 &&
                    !nativeResolutionStream &&
                    (candidate.width != currentMode.width ||
                            candidate.height != currentMode.height)) {
                continue;
            }

            if (resolutionReduced &&
                    !(streamFps > 60 &&
                            resolutionFitsStream)) {
                continue;
            }

            boolean candidateIsEqual =
                    isEqualRefreshRateMatch(
                            candidate.refreshRate,
                            streamFps);
            boolean candidateIsGood =
                    isGoodRefreshRateMatch(
                            candidate.refreshRate,
                            streamFps);
            if (reduceRefreshRate &&
                    refreshRateIsEqual &&
                    !candidateIsEqual) {
                continue;
            }
            if (refreshRateIsGood) {
                if (!candidateIsGood) {
                    continue;
                }
                if (reduceRefreshRate) {
                    if (candidate.refreshRate >
                            bestMode.refreshRate) {
                        continue;
                    }
                }
                else if (refreshRateReduced) {
                    continue;
                }
            }
            else if (!candidateIsGood &&
                    refreshRateReduced) {
                continue;
            }

            bestMode = candidate;
            refreshRateIsGood = candidateIsGood;
            refreshRateIsEqual = candidateIsEqual;
        }

        return bestMode;
    }

    static boolean isEqualRefreshRateMatch(
            float refreshRate,
            int streamFps) {
        return refreshRate >= streamFps &&
                refreshRate <=
                        streamFps +
                                REFRESH_RATE_TOLERANCE_HZ;
    }

    static boolean isGoodRefreshRateMatch(
            float refreshRate,
            int streamFps) {
        return refreshRate >= streamFps &&
                Math.round(refreshRate) % streamFps <=
                        REFRESH_RATE_TOLERANCE_HZ;
    }

    public static final class Mode {
        public final int id;
        public final int width;
        public final int height;
        public final float refreshRate;

        public Mode(
                int id,
                int width,
                int height,
                float refreshRate) {
            if (width <= 0 ||
                    height <= 0 ||
                    !Float.isFinite(refreshRate) ||
                    refreshRate <= 0) {
                throw new IllegalArgumentException(
                        "Display mode geometry and refresh rate " +
                                "must be positive");
            }
            this.id = id;
            this.width = width;
            this.height = height;
            this.refreshRate = refreshRate;
        }
    }
}
