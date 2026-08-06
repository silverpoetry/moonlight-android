package com.limelight.ui.stream;

import android.view.Surface;

import androidx.annotation.MainThread;

import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.stream.launch.android.AndroidPreparedStreamSession;

import java.util.Objects;

/** Attaches a prepared decoder session to the stream Activity's Surface. */
public final class AndroidPreparedStreamRenderHost
        implements StreamRenderSurfaceController.Host {
    public interface BooleanValue {
        boolean get();
    }

    private final AndroidPreparedStreamSession session;
    private final NvConnectionListener streamObserver;
    private final PerfOverlayListener performanceObserver;
    private final StreamSessionUiEffects uiEffects;
    private final BooleanValue dependenciesReady;
    private final Runnable stopSession;
    private boolean attached;

    public AndroidPreparedStreamRenderHost(
            AndroidPreparedStreamSession session,
            NvConnectionListener streamObserver,
            PerfOverlayListener performanceObserver,
            StreamSessionUiEffects uiEffects,
            BooleanValue dependenciesReady,
            Runnable stopSession) {
        this.session = Objects.requireNonNull(session, "session");
        this.streamObserver = Objects.requireNonNull(
                streamObserver,
                "streamObserver");
        this.performanceObserver = Objects.requireNonNull(
                performanceObserver,
                "performanceObserver");
        this.uiEffects = Objects.requireNonNull(uiEffects, "uiEffects");
        this.dependenciesReady = Objects.requireNonNull(
                dependenciesReady,
                "dependenciesReady");
        this.stopSession = Objects.requireNonNull(
                stopSession,
                "stopSession");
    }

    @Override
    public boolean canStartSession() {
        return !attached &&
                dependenciesReady.get() &&
                session.isReadyForAttach();
    }

    @Override
    @MainThread
    public void startSession(Surface renderTarget) {
        if (!canStartSession()) {
            return;
        }
        uiEffects.onConnecting();
        try {
            session.attach(
                    renderTarget,
                    streamObserver,
                    performanceObserver);
            attached = true;
        }
        catch (RuntimeException | Error failure) {
            uiEffects.onEnded();
            throw failure;
        }
    }

    @Override
    public boolean hasSessionStarted() {
        return true;
    }

    @Override
    public boolean sessionNeedsStop() {
        return session.getSessionController()
                .getState()
                .needsStop();
    }

    @Override
    @MainThread
    public void prepareVideoForStop() {
        session.getMediaResourceOwner().prepareVideoForStop();
    }

    @Override
    @MainThread
    public void stopSession() {
        stopSession.run();
    }
}
