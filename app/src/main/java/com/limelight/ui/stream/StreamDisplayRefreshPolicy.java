package com.limelight.ui.stream;

import com.limelight.settings.stream.StreamDecoderSettings;

import java.util.Locale;
import java.util.Objects;

/** Platform-independent refresh-rate policy for stream display setup. */
public final class StreamDisplayRefreshPolicy {
    private StreamDisplayRefreshPolicy() {
    }

    public static boolean mayReduceRefreshRate(
            StreamDecoderSettings.FramePacing framePacing,
            boolean balancedReductionEnabled) {
        Objects.requireNonNull(framePacing, "framePacing");
        return framePacing ==
                StreamDecoderSettings.FramePacing.CAP_FPS ||
                framePacing ==
                        StreamDecoderSettings.FramePacing
                                .MAXIMUM_SMOOTHNESS ||
                framePacing ==
                        StreamDecoderSettings.FramePacing.BALANCED &&
                        balancedReductionEnabled;
    }

    public static boolean shouldLetSystemManageRefreshRate(
            boolean television,
            String manufacturer,
            String brand) {
        if (television) {
            return false;
        }
        String deviceFamily =
                (String.valueOf(manufacturer) + " " +
                        String.valueOf(brand))
                        .toLowerCase(Locale.ROOT);
        return deviceFamily.contains("xiaomi") ||
                deviceFamily.contains("redmi") ||
                deviceFamily.contains("poco");
    }
}
