package com.limelight.ui.performance;

/**
 * Immutable runtime values used to render one performance-overlay update.
 */
public final class PerformanceOverlayRuntimeState {
    public final boolean fsrEnabled;
    public final String fsrTargetDisplayName;
    public final String fsrSharpnessDisplayName;
    public final boolean fsrHdrOutput;
    public final boolean micActive;
    public final String streamHost;
    public final long sessionStartElapsedMs;
    public final long nowElapsedMs;
    public final boolean usbControllerActive;
    public final String usbControllerTypeDisplayName;
    public final boolean usbServiceConnected;

    public PerformanceOverlayRuntimeState(
            boolean fsrEnabled,
            String fsrTargetDisplayName,
            String fsrSharpnessDisplayName,
            boolean fsrHdrOutput,
            boolean micActive,
            String streamHost,
            long sessionStartElapsedMs,
            long nowElapsedMs,
            boolean usbControllerActive,
            String usbControllerTypeDisplayName,
            boolean usbServiceConnected) {
        this.fsrEnabled = fsrEnabled;
        this.fsrTargetDisplayName = fsrTargetDisplayName;
        this.fsrSharpnessDisplayName = fsrSharpnessDisplayName;
        this.fsrHdrOutput = fsrHdrOutput;
        this.micActive = micActive;
        this.streamHost = streamHost;
        this.sessionStartElapsedMs = sessionStartElapsedMs;
        this.nowElapsedMs = nowElapsedMs;
        this.usbControllerActive = usbControllerActive;
        this.usbControllerTypeDisplayName =
                usbControllerTypeDisplayName;
        this.usbServiceConnected = usbServiceConnected;
    }
}
