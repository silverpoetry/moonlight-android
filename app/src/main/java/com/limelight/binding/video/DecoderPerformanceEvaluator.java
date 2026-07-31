package com.limelight.binding.video;

import java.util.List;
import java.util.Objects;

/**
 * Applies Android's ordered decoder-performance evidence without depending on
 * framework codec classes.
 */
public final class DecoderPerformanceEvaluator {
    public interface PerformancePoint {
        boolean coversTarget();
    }

    public interface Capabilities {
        /**
         * Returns {@code null} when performance-point data is unavailable.
         */
        List<PerformancePoint> getSupportedPerformancePoints();

        /**
         * Returns {@code null} when achievable-frame-rate data is unavailable.
         */
        Double getAchievableFrameRateUpper(int width, int height);

        boolean isSizeAndRateSupported(
                int width,
                int height,
                double framesPerSecond);
    }

    private static final int API_M = 23;
    private static final int API_Q = 29;

    private DecoderPerformanceEvaluator() {
    }

    public static boolean canMeetTarget(
            int apiLevel,
            int width,
            int height,
            double framesPerSecond,
            Capabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");

        if (apiLevel >= API_Q) {
            List<PerformancePoint> performancePoints =
                    capabilities.getSupportedPerformancePoints();
            if (performancePoints != null) {
                for (PerformancePoint performancePoint :
                        performancePoints) {
                    if (performancePoint.coversTarget()) {
                        return true;
                    }
                }
                return false;
            }
        }

        if (apiLevel >= API_M) {
            try {
                Double upperFrameRate = capabilities
                        .getAchievableFrameRateUpper(width, height);
                if (upperFrameRate != null) {
                    return framesPerSecond <= upperFrameRate;
                }
            } catch (IllegalArgumentException unsupportedSize) {
                return false;
            }
        }

        return capabilities.isSizeAndRateSupported(
                width,
                height,
                framesPerSecond);
    }
}
