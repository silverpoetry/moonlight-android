package com.limelight.settings.stream;

/**
 * Pure policy for the default stream bitrate.
 */
public final class StreamBitratePolicy {
    private static final long[] PIXEL_BREAKPOINTS = {
            640L * 360L,
            854L * 480L,
            1280L * 720L,
            1920L * 1080L,
            2560L * 1440L,
            3840L * 2160L
    };
    private static final int[] BITRATE_FACTORS = {
            1,
            2,
            5,
            10,
            20,
            40
    };

    private StreamBitratePolicy() {
    }

    public static int calculateDefaultBitrateKbps(
            int width,
            int height,
            int fps) {
        if (width <= 0 || height <= 0 || fps <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions and FPS must be positive");
        }

        double frameRateFactor =
                (fps <= 60
                        ? fps
                        : Math.sqrt(fps / 60d) * 60d) / 30d;
        long pixels = (long) width * height;
        double resolutionFactor =
                interpolateResolutionFactor(pixels);
        long bitrate = Math.round(
                resolutionFactor * frameRateFactor) * 1000L;
        return (int) Math.max(
                1,
                Math.min(
                        StreamVideoSettingKeys.MAX_BITRATE_KBPS,
                        bitrate));
    }

    private static double interpolateResolutionFactor(long pixels) {
        for (int index = 0;
             index < PIXEL_BREAKPOINTS.length;
             index++) {
            long upperPixels = PIXEL_BREAKPOINTS[index];
            if (pixels == upperPixels) {
                return BITRATE_FACTORS[index];
            }
            if (pixels < upperPixels) {
                if (index == 0) {
                    return BITRATE_FACTORS[0];
                }
                long lowerPixels =
                        PIXEL_BREAKPOINTS[index - 1];
                int lowerFactor =
                        BITRATE_FACTORS[index - 1];
                int upperFactor = BITRATE_FACTORS[index];
                double position =
                        (double) (pixels - lowerPixels) /
                                (upperPixels - lowerPixels);
                return lowerFactor +
                        position *
                                (upperFactor - lowerFactor);
            }
        }
        return BITRATE_FACTORS[
                BITRATE_FACTORS.length - 1];
    }
}
