package com.limelight.ui.stream;

import java.util.Objects;

/** Coordinates stream overlays and sensors across PiP visibility changes. */
public final class StreamOverlayVisibilityController {
    public interface Host {
        void hideVirtualControls();

        void hidePerformanceOverlay();

        void restorePerformanceOverlay();

        void setConnectionWarningVisible(boolean visible);

        void disableControllerSensors();

        void enableControllerSensors();

        void notifyPictureInPictureEntered();

        void notifyPictureInPictureExited();
    }

    private final Host host;
    private boolean pictureInPicture;
    private boolean connectionWarningVisible;
    private boolean destroyed;

    public StreamOverlayVisibilityController(Host host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public void setConnectionWarningVisible(boolean visible) {
        if (destroyed) {
            return;
        }
        connectionWarningVisible = visible;
        if (!pictureInPicture) {
            host.setConnectionWarningVisible(visible);
        }
    }

    public void onPictureInPictureModeChanged(boolean enabled) {
        if (destroyed || pictureInPicture == enabled) {
            return;
        }
        pictureInPicture = enabled;
        if (enabled) {
            host.hideVirtualControls();
            host.hidePerformanceOverlay();
            host.setConnectionWarningVisible(false);
            host.disableControllerSensors();
            host.notifyPictureInPictureEntered();
        }
        else {
            host.restorePerformanceOverlay();
            host.setConnectionWarningVisible(
                    connectionWarningVisible);
            host.enableControllerSensors();
            host.notifyPictureInPictureExited();
        }
    }

    public void destroy() {
        destroyed = true;
    }
}
