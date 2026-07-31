package com.limelight.ui.stream;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class StreamDisplayModeSelectorTest {
    @Test
    public void maximumSmoothnessSelectsHighestGoodRefreshRate() {
        StreamDisplayModeSelector.Mode selected = select(
                1920,
                1080,
                60,
                false,
                false,
                mode(1, 3840, 2160, 90),
                mode(2, 3840, 2160, 60),
                mode(3, 3840, 2160, 120));

        assertEquals(3, selected.id);
    }

    @Test
    public void refreshReductionSelectsLowestEqualRefreshRate() {
        StreamDisplayModeSelector.Mode selected = select(
                3840,
                2160,
                60,
                false,
                true,
                mode(1, 3840, 2160, 120),
                mode(2, 3840, 2160, 60),
                mode(3, 3840, 2160, 90));

        assertEquals(2, selected.id);
    }

    @Test
    public void standardLowFpsStreamKeepsCurrentResolution() {
        StreamDisplayModeSelector.Mode selected = select(
                1920,
                1080,
                60,
                false,
                false,
                mode(1, 3840, 2160, 60),
                mode(2, 1920, 1080, 120));

        assertEquals(1, selected.id);
    }

    @Test
    public void highFpsStreamCanReduceResolutionWhenItStillFits() {
        StreamDisplayModeSelector.Mode selected = select(
                1920,
                1080,
                120,
                false,
                false,
                mode(1, 3840, 2160, 60),
                mode(2, 1920, 1080, 120));

        assertEquals(2, selected.id);
    }

    @Test
    public void streamAtOrBelow4kRejectsUnsafeOutputWidth() {
        StreamDisplayModeSelector.Mode selected = select(
                3840,
                2160,
                120,
                true,
                false,
                mode(1, 3840, 2160, 120),
                mode(2, 5120, 2880, 144));

        assertEquals(1, selected.id);
    }

    @Test
    public void invalidStreamContractIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StreamDisplayModeSelector.select(
                        1920,
                        1080,
                        0,
                        false,
                        false,
                        mode(1, 1920, 1080, 60),
                        Arrays.asList(
                                mode(1, 1920, 1080, 60))));
    }

    private static StreamDisplayModeSelector.Mode select(
            int streamWidth,
            int streamHeight,
            int streamFps,
            boolean nativeResolutionStream,
            boolean reduceRefreshRate,
            StreamDisplayModeSelector.Mode currentMode,
            StreamDisplayModeSelector.Mode... supportedModes) {
        return StreamDisplayModeSelector.select(
                streamWidth,
                streamHeight,
                streamFps,
                nativeResolutionStream,
                reduceRefreshRate,
                currentMode,
                Arrays.asList(supportedModes));
    }

    private static StreamDisplayModeSelector.Mode mode(
            int id,
            int width,
            int height,
            float refreshRate) {
        return new StreamDisplayModeSelector.Mode(
                id,
                width,
                height,
                refreshRate);
    }
}
