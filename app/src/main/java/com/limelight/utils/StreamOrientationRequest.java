package com.limelight.utils;

/**
 * Immutable per-session inputs for resolving stream-window orientation.
 *
 * <p>The request deliberately contains no Android preference or UI objects.
 * Runtime visibility belongs to the overlay owner, while persisted launch
 * policy is projected into this value by the composition root.</p>
 */
public final class StreamOrientationRequest {
    private final boolean onScreenControllerVisible;
    private final boolean nativeResolution;
    private final int streamWidth;
    private final int streamHeight;
    private final boolean portraitRequested;
    private final boolean automaticOrientationEnabled;

    public StreamOrientationRequest(
            boolean onScreenControllerVisible,
            boolean nativeResolution,
            int streamWidth,
            int streamHeight,
            boolean portraitRequested,
            boolean automaticOrientationEnabled) {
        if (streamWidth <= 0 || streamHeight <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions must be positive");
        }
        this.onScreenControllerVisible =
                onScreenControllerVisible;
        this.nativeResolution = nativeResolution;
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        this.portraitRequested = portraitRequested;
        this.automaticOrientationEnabled =
                automaticOrientationEnabled;
    }

    public boolean isOnScreenControllerVisible() {
        return onScreenControllerVisible;
    }

    public boolean isNativeResolution() {
        return nativeResolution;
    }

    public int getStreamWidth() {
        return streamWidth;
    }

    public int getStreamHeight() {
        return streamHeight;
    }

    public boolean isPortraitRequested() {
        return portraitRequested;
    }

    public boolean isAutomaticOrientationEnabled() {
        return automaticOrientationEnabled;
    }
}
