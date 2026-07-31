package com.limelight.ui.performance;

/**
 * Immutable runtime values used to render one performance-overlay update.
 */
public final class PerformanceOverlayRuntimeState {
    public final boolean micActive;
    public final String streamHost;
    public final long sessionStartElapsedMs;
    public final long nowElapsedMs;
    public final boolean usbControllerActive;
    public final String usbControllerTypeDisplayName;
    public final boolean usbServiceConnected;

    public PerformanceOverlayRuntimeState(
            boolean micActive,
            String streamHost,
            long sessionStartElapsedMs,
            long nowElapsedMs,
            boolean usbControllerActive,
            String usbControllerTypeDisplayName,
            boolean usbServiceConnected) {
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
