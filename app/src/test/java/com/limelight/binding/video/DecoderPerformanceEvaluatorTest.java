package com.limelight.binding.video;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DecoderPerformanceEvaluatorTest {
    private static final int WIDTH = 3840;
    private static final int HEIGHT = 2160;
    private static final double FPS = 120;

    @Test
    public void coveringPerformancePointIsAuthoritative() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.performancePoints = List.of(
                () -> false,
                () -> true);

        assertTrue(evaluate(29, capabilities));
        assertFalse(capabilities.achievableRatesQueried);
        assertFalse(capabilities.sizeAndRateQueried);
    }

    @Test
    public void nonCoveringPerformancePointsRejectWithoutFallback() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.performancePoints = Collections.singletonList(
                () -> false);
        capabilities.achievableUpper = 240.0;
        capabilities.sizeAndRateSupported = true;

        assertFalse(evaluate(29, capabilities));
        assertFalse(capabilities.achievableRatesQueried);
        assertFalse(capabilities.sizeAndRateQueried);
    }

    @Test
    public void missingPerformancePointsFallBackToAchievableRate() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.performancePoints = null;
        capabilities.achievableUpper = FPS;

        assertTrue(evaluate(29, capabilities));
        assertTrue(capabilities.achievableRatesQueried);
        assertFalse(capabilities.sizeAndRateQueried);
    }

    @Test
    public void insufficientAchievableRateRejectsTarget() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.achievableUpper = FPS - 1;

        assertFalse(evaluate(23, capabilities));
        assertFalse(capabilities.sizeAndRateQueried);
    }

    @Test
    public void unsupportedSizeRejectsWithoutOptimisticFallback() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.throwUnsupportedSize = true;
        capabilities.sizeAndRateSupported = true;

        assertFalse(evaluate(23, capabilities));
        assertFalse(capabilities.sizeAndRateQueried);
    }

    @Test
    public void missingAchievableRateUsesSizeAndRateCapability() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.achievableUpper = null;
        capabilities.sizeAndRateSupported = true;

        assertTrue(evaluate(23, capabilities));
        assertTrue(capabilities.sizeAndRateQueried);
    }

    @Test
    public void preMarshmallowUsesSizeAndRateCapabilityDirectly() {
        FakeCapabilities capabilities = new FakeCapabilities();
        capabilities.sizeAndRateSupported = true;

        assertTrue(evaluate(22, capabilities));
        assertFalse(capabilities.achievableRatesQueried);
        assertTrue(capabilities.sizeAndRateQueried);
    }

    private static boolean evaluate(
            int apiLevel,
            FakeCapabilities capabilities) {
        return DecoderPerformanceEvaluator.canMeetTarget(
                apiLevel,
                WIDTH,
                HEIGHT,
                FPS,
                capabilities);
    }

    private static final class FakeCapabilities
            implements DecoderPerformanceEvaluator.Capabilities {
        List<DecoderPerformanceEvaluator.PerformancePoint>
                performancePoints = new ArrayList<>();
        Double achievableUpper;
        boolean sizeAndRateSupported;
        boolean throwUnsupportedSize;
        boolean achievableRatesQueried;
        boolean sizeAndRateQueried;

        @Override
        public List<DecoderPerformanceEvaluator.PerformancePoint>
                getSupportedPerformancePoints() {
            return performancePoints;
        }

        @Override
        public Double getAchievableFrameRateUpper(
                int width,
                int height) {
            achievableRatesQueried = true;
            if (throwUnsupportedSize) {
                throw new IllegalArgumentException("unsupported size");
            }
            return achievableUpper;
        }

        @Override
        public boolean isSizeAndRateSupported(
                int width,
                int height,
                double framesPerSecond) {
            sizeAndRateQueried = true;
            return sizeAndRateSupported;
        }
    }
}
