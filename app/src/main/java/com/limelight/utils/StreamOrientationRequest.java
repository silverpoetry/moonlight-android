package com.limelight.utils;

/**
 * Immutable per-session inputs for resolving stream-window orientation.
 *
 * <p>The request deliberately contains no Android preference or UI objects.
 * Runtime visibility belongs to the overlay owner, while persisted launch
 * policy is projected into this value by the composition root.</p>
 */
public final class StreamOrientationRequest {
    public enum ManualOrientation {
        NONE,
        LANDSCAPE,
        PORTRAIT
    }

    private final boolean onScreenControllerVisible;
    private final boolean nativeResolution;
    private final int streamWidth;
    private final int streamHeight;
    private final boolean portraitRequested;
    private final boolean automaticOrientationEnabled;
    private final ManualOrientation manualOrientation;

    public StreamOrientationRequest(
            boolean onScreenControllerVisible,
            boolean nativeResolution,
            int streamWidth,
            int streamHeight,
            boolean portraitRequested,
            boolean automaticOrientationEnabled) {
        this(
                onScreenControllerVisible,
                nativeResolution,
                streamWidth,
                streamHeight,
                portraitRequested,
                automaticOrientationEnabled,
                ManualOrientation.NONE);
    }

    public StreamOrientationRequest(
            boolean onScreenControllerVisible,
            boolean nativeResolution,
            int streamWidth,
            int streamHeight,
            boolean portraitRequested,
            boolean automaticOrientationEnabled,
            ManualOrientation manualOrientation) {
        if (streamWidth <= 0 || streamHeight <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions must be positive");
        }
        if (manualOrientation == null) {
            throw new IllegalArgumentException(
                    "Manual orientation must not be null");
        }
        this.onScreenControllerVisible =
                onScreenControllerVisible;
        this.nativeResolution = nativeResolution;
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        this.portraitRequested = portraitRequested;
        this.automaticOrientationEnabled =
                automaticOrientationEnabled;
        this.manualOrientation = manualOrientation;
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

    public ManualOrientation getManualOrientation() {
        return manualOrientation;
    }
}
