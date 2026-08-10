package com.limelight.stream.launch.android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;

import com.limelight.stream.launch.StreamLaunchRequest;

import java.util.Objects;

/**
 * Process composition root for one stream preparation and Activity handoff.
 */
public final class AndroidStreamSessionCoordinator {
    private static final long HANDOFF_TIMEOUT_MS = 10_000L;
    public enum BeginOutcome {
        STARTED,
        ALREADY_ACTIVE,
        FAILED
    }

    public interface Listener {
        void onProgress(String message);

        void onReady(
                String sessionToken,
                StreamInitialOrientation initialOrientation);

        void onFailure(AndroidPreparedStreamSession.Failure failure);
    }

    public static final class BeginResult {
        private final BeginOutcome outcome;
        private final RuntimeException failure;
        private final String sessionToken;

        private BeginResult(
                BeginOutcome outcome,
                RuntimeException failure,
                String sessionToken) {
            this.outcome = outcome;
            this.failure = failure;
            this.sessionToken = sessionToken;
        }

        public BeginOutcome getOutcome() {
            return outcome;
        }

        public RuntimeException getFailure() {
            return failure;
        }

        public String getSessionToken() {
            return sessionToken;
        }
    }

    private AndroidPreparedStreamSession activeSession;
    private boolean claimed;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingHandoffTimeout;

    @MainThread
    public BeginResult begin(
            Activity activity,
            StreamLaunchRequest request,
            Listener listener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");
        if (activeSession != null) {
            return new BeginResult(
                    BeginOutcome.ALREADY_ACTIVE,
                    null,
                    activeSession.getToken());
        }

        AndroidPreparedStreamSession session = null;
        try {
            session = AndroidPreparedStreamSessionFactory.create(
                    activity,
                    request,
                    new CoordinatingListener(listener));
            activeSession = session;
            claimed = false;
            session.start();
            return new BeginResult(
                    BeginOutcome.STARTED,
                    null,
                    session.getToken());
        }
        catch (RuntimeException failure) {
            if (session != null) {
                session.close();
            }
            activeSession = null;
            claimed = false;
            return new BeginResult(
                    BeginOutcome.FAILED,
                    failure,
                    null);
        }
    }

    @MainThread
    public AndroidPreparedStreamSession claim(String token) {
        AndroidPreparedStreamSession session = activeSession;
        if (session == null ||
                claimed ||
                token == null ||
                !token.equals(session.getToken()) ||
                !session.isReadyForAttach()) {
            return null;
        }
        claimed = true;
        cancelHandoffTimeout();
        return session;
    }

    @MainThread
    public void cancel(String token) {
        AndroidPreparedStreamSession session = activeSession;
        if (session == null ||
                token == null ||
                !token.equals(session.getToken())) {
            return;
        }
        clearAndClose(session);
    }

    @MainThread
    public void release(AndroidPreparedStreamSession session) {
        if (session == null || activeSession != session) {
            return;
        }
        clearAndClose(session);
    }

    private void clearAndClose(AndroidPreparedStreamSession session) {
        cancelHandoffTimeout();
        activeSession = null;
        claimed = false;
        session.close();
    }

    private void scheduleHandoffTimeout(
            AndroidPreparedStreamSession session,
            Listener listener) {
        cancelHandoffTimeout();
        pendingHandoffTimeout = () -> {
            pendingHandoffTimeout = null;
            if (activeSession == session && !claimed) {
                clearAndClose(session);
                listener.onFailure(
                        AndroidPreparedStreamSession.Failure
                                .handoffTimedOut());
            }
        };
        mainHandler.postDelayed(
                pendingHandoffTimeout,
                HANDOFF_TIMEOUT_MS);
    }

    private void cancelHandoffTimeout() {
        if (pendingHandoffTimeout == null) {
            return;
        }
        mainHandler.removeCallbacks(pendingHandoffTimeout);
        pendingHandoffTimeout = null;
    }

    private final class CoordinatingListener
            implements AndroidPreparedStreamSession.Listener {
        private final Listener externalListener;

        private CoordinatingListener(Listener externalListener) {
            this.externalListener = externalListener;
        }

        @Override
        public void onProgress(String message) {
            externalListener.onProgress(message);
        }

        @Override
        public void onReady(AndroidPreparedStreamSession session) {
            if (activeSession == session && !claimed) {
                externalListener.onReady(
                        session.getToken(),
                        StreamInitialOrientation.from(
                                session.getVideoSettings()));
                if (activeSession == session && !claimed) {
                    scheduleHandoffTimeout(
                            session,
                            externalListener);
                }
            }
        }

        @Override
        public void onFailure(
                AndroidPreparedStreamSession session,
                AndroidPreparedStreamSession.Failure failure) {
            if (activeSession != session) {
                return;
            }
            clearAndClose(session);
            externalListener.onFailure(failure);
        }
    }
}
