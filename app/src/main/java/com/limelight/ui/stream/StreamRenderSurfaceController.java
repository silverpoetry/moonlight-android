package com.limelight.ui.stream;

import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;

import java.util.Objects;

/**
 * Activity-scoped owner for the decoder render Surface lifecycle.
 */
public final class StreamRenderSurfaceController
        implements SurfaceHolder.Callback {
    public interface Host {
        boolean canStartSession();

        void startSession(Surface renderTarget);

        boolean hasSessionStarted();

        boolean sessionNeedsStop();

        void prepareVideoForStop();

        void stopSession();
    }

    private final SurfaceHolder surfaceHolder;
    private final int streamWidth;
    private final int streamHeight;
    private final int streamFps;
    private final float desiredRefreshRate;
    private final boolean reduceRefreshRate;
    private final boolean systemManagedRefreshRate;
    private final StreamRenderSurfaceState state =
            new StreamRenderSurfaceState();

    private Host host;
    private boolean bound;

    public StreamRenderSurfaceController(
            SurfaceHolder surfaceHolder,
            int streamWidth,
            int streamHeight,
            int streamFps,
            float desiredRefreshRate,
            boolean reduceRefreshRate,
            boolean systemManagedRefreshRate,
            Host host) {
        if (streamWidth <= 0 ||
                streamHeight <= 0 ||
                streamFps <= 0 ||
                !Float.isFinite(desiredRefreshRate) ||
                desiredRefreshRate <= 0) {
            throw new IllegalArgumentException(
                    "Stream geometry and refresh rate must be positive");
        }
        this.surfaceHolder = Objects.requireNonNull(
                surfaceHolder,
                "surfaceHolder");
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        this.streamFps = streamFps;
        this.desiredRefreshRate = desiredRefreshRate;
        this.reduceRefreshRate = reduceRefreshRate;
        this.systemManagedRefreshRate =
                systemManagedRefreshRate;
        this.host = Objects.requireNonNull(host, "host");
    }

    @MainThread
    public void bind() {
        checkActive();
        if (bound) {
            return;
        }
        bound = true;
        surfaceHolder.addCallback(this);
    }

    @MainThread
    public void startIfReady() {
        Host activeHost = host;
        if (activeHost == null ||
                !state.canStart(
                        activeHost.canStartSession())) {
            return;
        }
        Surface surface = surfaceHolder.getSurface();
        if (surface != null && surface.isValid()) {
            activeHost.startSession(surface);
        }
    }

    @MainThread
    public void destroy() {
        if (host == null) {
            return;
        }
        if (bound) {
            surfaceHolder.removeCallback(this);
            bound = false;
        }
        state.reset();
        host = null;
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height) {
        if (!isOwnedHolder(holder) || host == null) {
            return;
        }
        LimeLog.info(
                "Stream render surface changed: " +
                        width + "x" + height +
                        ", source=" +
                        streamWidth + "x" +
                        streamHeight);
        Surface surface = holder.getSurface();
        state.onChanged(
                surface != null && surface.isValid());
        startIfReady();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!isOwnedHolder(holder) || host == null) {
            return;
        }
        state.onCreated();
        configureSurfaceFrameRate(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Host activeHost = host;
        if (!isOwnedHolder(holder) || activeHost == null) {
            return;
        }
        state.onDestroyed();
        if (!activeHost.hasSessionStarted()) {
            return;
        }
        activeHost.prepareVideoForStop();
        if (activeHost.sessionNeedsStop()) {
            activeHost.stopSession();
        }
    }

    private void configureSurfaceFrameRate(Surface surface) {
        if (systemManagedRefreshRate) {
            LimeLog.info(
                    "Skipping Surface.setFrameRate() and " +
                            "leaving refresh rate to the system");
            return;
        }
        if (surface == null) {
            return;
        }
        float desiredFrameRate =
                reduceRefreshRate ||
                        desiredRefreshRate < streamFps
                        ? streamFps
                        : desiredRefreshRate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            surface.setFrameRate(
                    desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS);
        }
        else if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R) {
            surface.setFrameRate(
                    desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
        }
    }

    private boolean isOwnedHolder(SurfaceHolder holder) {
        return holder == surfaceHolder;
    }

    private void checkActive() {
        if (host == null) {
            throw new IllegalStateException(
                    "Render surface controller is destroyed");
        }
    }
}
