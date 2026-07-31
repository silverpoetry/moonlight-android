package com.limelight.ui.stream;

import android.view.Surface;

import androidx.annotation.MainThread;

import com.limelight.nvstream.StreamSessionController;

import java.util.Objects;

/**
 * Coordinates the render Surface with media-resource and session lifecycles.
 */
public final class StreamRenderSessionHost
        implements StreamRenderSurfaceController.Host {
    interface SessionPort {
        boolean canStart();

        boolean start(StreamMediaResourceOwner.StartResources resources);

        boolean hasStartBeenRequested();

        boolean needsStop();
    }

    interface MediaPort {
        StreamMediaResourceOwner.StartResources prepareStart(
                Surface renderTarget);

        void releaseStartResources();

        void prepareVideoForStop();
    }

    interface UiEffectsPort {
        void onConnecting();

        void onEnded();
    }

    public interface BooleanValue {
        boolean get();
    }

    private final SessionPort session;
    private final MediaPort media;
    private final UiEffectsPort uiEffects;
    private final BooleanValue dependenciesReady;
    private final Runnable stopSession;

    public StreamRenderSessionHost(
            StreamSessionController sessionController,
            StreamMediaResourceOwner mediaResourceOwner,
            StreamSessionUiEffects sessionUiEffects,
            BooleanValue dependenciesReady,
            Runnable stopSession) {
        StreamSessionController sessionOwner = Objects.requireNonNull(
                sessionController,
                "sessionController");
        StreamMediaResourceOwner mediaOwner = Objects.requireNonNull(
                mediaResourceOwner,
                "mediaResourceOwner");
        StreamSessionUiEffects effectsOwner = Objects.requireNonNull(
                sessionUiEffects,
                "sessionUiEffects");
        this.session = new SessionPort() {
            @Override
            public boolean canStart() {
                return sessionOwner.canStart();
            }

            @Override
            public boolean start(
                    StreamMediaResourceOwner.StartResources resources) {
                return sessionOwner.start(
                        resources.getAudioRenderer(),
                        resources.getVideoRenderer());
            }

            @Override
            public boolean hasStartBeenRequested() {
                return sessionOwner.hasStartBeenRequested();
            }

            @Override
            public boolean needsStop() {
                return sessionOwner.getState().needsStop();
            }
        };
        this.media = new MediaPort() {
            @Override
            public StreamMediaResourceOwner.StartResources prepareStart(
                    Surface renderTarget) {
                return mediaOwner.prepareStart(renderTarget);
            }

            @Override
            public void releaseStartResources() {
                mediaOwner.releaseStartResources();
            }

            @Override
            public void prepareVideoForStop() {
                mediaOwner.prepareVideoForStop();
            }
        };
        this.uiEffects = new UiEffectsPort() {
            @Override
            public void onConnecting() {
                effectsOwner.onConnecting();
            }

            @Override
            public void onEnded() {
                effectsOwner.onEnded();
            }
        };
        this.dependenciesReady = Objects.requireNonNull(
                dependenciesReady,
                "dependenciesReady");
        this.stopSession = Objects.requireNonNull(
                stopSession,
                "stopSession");
    }

    StreamRenderSessionHost(
            SessionPort session,
            MediaPort media,
            UiEffectsPort uiEffects,
            BooleanValue dependenciesReady,
            Runnable stopSession) {
        this.session = Objects.requireNonNull(session, "session");
        this.media = Objects.requireNonNull(media, "media");
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
        return dependenciesReady.get() && session.canStart();
    }

    @MainThread
    @Override
    public void startSession(Surface renderTarget) {
        if (!canStartSession()) {
            return;
        }
        StreamMediaResourceOwner.StartResources resources =
                media.prepareStart(renderTarget);
        uiEffects.onConnecting();
        try {
            if (session.start(resources)) {
                return;
            }
        } catch (RuntimeException | Error error) {
            releaseRejectedStart();
            throw error;
        }
        releaseRejectedStart();
    }

    @Override
    public boolean hasSessionStarted() {
        return session.hasStartBeenRequested();
    }

    @Override
    public boolean sessionNeedsStop() {
        return session.needsStop();
    }

    @MainThread
    @Override
    public void prepareVideoForStop() {
        media.prepareVideoForStop();
    }

    @MainThread
    @Override
    public void stopSession() {
        stopSession.run();
    }

    private void releaseRejectedStart() {
        media.releaseStartResources();
        uiEffects.onEnded();
    }
}
