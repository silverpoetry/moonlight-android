package com.limelight.ui;

/**
 * Pure policy for deciding whether stream content may occupy the complete
 * physical display.
 *
 * <p>Android display discovery remains in the platform adapter. This class
 * owns the product decision so rotation handling and native-resolution
 * behavior can be verified without an Activity or Display instance.</p>
 */
public final class StreamWindowPolicy {
    private StreamWindowPolicy() {
    }

    public static boolean shouldUseEntireDisplay(
            boolean stretchVideo,
            boolean displayCutoutEnabled,
            boolean nativeResolution,
            boolean matchesPhysicalDisplayMode) {
        return stretchVideo ||
                displayCutoutEnabled ||
                nativeResolution && matchesPhysicalDisplayMode;
    }

    public static boolean matchesPhysicalResolution(
            int streamWidth,
            int streamHeight,
            int physicalWidth,
            int physicalHeight) {
        if (streamWidth <= 0 || streamHeight <= 0 ||
                physicalWidth <= 0 || physicalHeight <= 0) {
            return false;
        }

        return streamWidth == physicalWidth &&
                streamHeight == physicalHeight ||
                streamWidth == physicalHeight &&
                        streamHeight == physicalWidth;
    }
}
