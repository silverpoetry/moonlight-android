package com.limelight.stream.launch.android;

import android.app.Activity;
import android.content.Intent;

import com.limelight.LimeLog;
import com.limelight.MoonlightApplication;
import com.limelight.R;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.RecentStreamSession;
import com.limelight.stream.launch.RecentStreamSessionRepository;
import com.limelight.stream.launch.StreamLaunchRequest;
import com.limelight.stream.launch.StreamLaunchUseCase;

import java.security.cert.CertificateEncodingException;
import java.util.Objects;

import com.limelight.ui.stream.AndroidStreamConnectionMessages;
import com.limelight.ui.stream.StreamConnectionMessages;
import com.limelight.utils.Dialog;

/** Activity-scoped application boundary for admitting a stream launch. */
public final class AndroidStreamLauncher {
    public enum Outcome {
        STARTED,
        ALREADY_STARTING,
        HOST_UNAVAILABLE,
        INVALID_REQUEST,
        LAUNCH_FAILED,
        OWNER_UNAVAILABLE
    }

    public static final class Result {
        private final Outcome outcome;
        private final Exception failure;

        private Result(Outcome outcome, Exception failure) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.failure = failure;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public Exception getFailure() {
            return failure;
        }

        public boolean isStarted() {
            return outcome == Outcome.STARTED;
        }
    }

    private final Activity activity;
    private final StreamLaunchUseCase useCase;
    private final AndroidStreamLaunchProgress progress;
    private final AndroidStreamSessionCoordinator coordinator;
    private String activeSessionToken;

    public AndroidStreamLauncher(Activity activity) {
        this(
                activity,
                new SharedPreferencesRecentStreamSessionRepository(
                        activity));
    }

    AndroidStreamLauncher(
            Activity activity,
            RecentStreamSessionRepository recentSessions) {
        this.activity = Objects.requireNonNull(activity, "activity");
        progress = ((MoonlightApplication) activity.getApplication())
                .getStreamLaunchProgress();
        coordinator = ((MoonlightApplication) activity.getApplication())
                .getStreamSessionCoordinator();
        useCase = new StreamLaunchUseCase(
                Objects.requireNonNull(
                        recentSessions,
                        "recentSessions"));
    }

    public Result launch(
            HostRuntimeSnapshot host,
            NvApp app,
            String uniqueId) {
        if (host == null ||
                host.getConnectionState().getActiveEndpoint() == null) {
            return result(Outcome.HOST_UNAVAILABLE);
        }
        if (app == null) {
            return result(Outcome.INVALID_REQUEST);
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            return result(Outcome.OWNER_UNAVAILABLE);
        }

        final StreamLaunchRequest request;
        try {
            request = AndroidStreamLaunchRequestFactory.create(
                    host,
                    app,
                    uniqueId);
        }
        catch (CertificateEncodingException |
                IllegalArgumentException failure) {
            return new Result(Outcome.INVALID_REQUEST, failure);
        }

        StreamLaunchUseCase.Result result = useCase.launch(
                request,
                acceptedRequest -> {
                    AndroidStreamLaunchProgress.Session session =
                            progress.begin(activity);
                    StreamConnectionMessages messages =
                            AndroidStreamConnectionMessages.create(
                                    activity);
                    AndroidStreamSessionCoordinator.BeginResult begin =
                            coordinator.begin(
                                    activity,
                                    acceptedRequest,
                                    new AndroidStreamSessionCoordinator
                                            .Listener() {
                                        @Override
                                        public void onProgress(
                                                String message) {
                                            session.updateMessage(
                                                    messages.stageStarting(
                                                            message));
                                        }

                                        @Override
                                        public void onReady(
                                                String sessionToken) {
                                            if (!sessionToken.equals(
                                                    activeSessionToken)) {
                                                coordinator.cancel(
                                                        sessionToken);
                                                return;
                                            }
                                            if (activity.isFinishing() ||
                                                    activity.isDestroyed()) {
                                                coordinator.cancel(
                                                        sessionToken);
                                                progress.finish(session);
                                                activeSessionToken = null;
                                                useCase.onLaunchFailed();
                                                return;
                                            }
                                            try {
                                                activity.startActivity(
                                                        AndroidStreamLaunchIntentFactory
                                                                .create(
                                                                        activity,
                                                                        acceptedRequest,
                                                                        sessionToken));
                                                activeSessionToken = null;
                                            }
                                            catch (RuntimeException |
                                                    Error failure) {
                                                coordinator.cancel(
                                                        sessionToken);
                                                progress.finish(session);
                                                activeSessionToken = null;
                                                useCase.onLaunchFailed();
                                                LimeLog.warning(
                                                        "Unable to open stream Activity",
                                                        failure);
                                                if (!activity.isFinishing() &&
                                                        !activity.isDestroyed()) {
                                                    Dialog.displayDialog(
                                                            activity,
                                                            activity.getString(
                                                                    R.string
                                                                            .conn_error_title),
                                                            activity.getString(
                                                                    R.string
                                                                            .conn_error_msg),
                                                            false);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(
                                                AndroidPreparedStreamSession
                                                        .Failure failure) {
                                            if (activeSessionToken != null) {
                                                activeSessionToken = null;
                                            }
                                            progress.finish(session);
                                            useCase.onLaunchFailed();
                                            AndroidStreamPreparationFailurePresenter
                                                    .present(
                                                            activity,
                                                            failure);
                                        }
                                    });
                    if (begin.getOutcome() !=
                            AndroidStreamSessionCoordinator.BeginOutcome
                                    .STARTED) {
                        progress.finish(session);
                        RuntimeException failure = begin.getFailure();
                        if (failure != null) {
                            throw failure;
                        }
                        throw new IllegalStateException(
                                "Another stream session is active");
                    }
                    activeSessionToken = begin.getSessionToken();
                    session.bindSessionToken(activeSessionToken);
                });
        if (result.getPersistenceFailure() != null) {
            LimeLog.warning(
                    "Unable to remember recent stream: " +
                            result.getPersistenceFailure()
                                    .getClass()
                                    .getSimpleName());
        }
        Result mapped = map(result);
        LimeLog.info(
                "Stream launch admission: appId=" + app.getAppId() +
                        ", outcome=" + mapped.getOutcome());
        return mapped;
    }

    public RecentStreamSession findRecentSession(String hostId) {
        return useCase.findRecentSession(hostId);
    }

    public void onOwnerResumed() {
        useCase.onOwnerResumed();
    }

    public void onOwnerPaused() {
        useCase.onOwnerPaused();
    }

    public void onOwnerDestroyed() {
        if (activeSessionToken != null) {
            coordinator.cancel(activeSessionToken);
            activeSessionToken = null;
        }
        progress.finishFor(activity);
        useCase.onOwnerDestroyed();
    }

    private static Result map(StreamLaunchUseCase.Result result) {
        switch (result.getOutcome()) {
            case STARTED:
                return result(Outcome.STARTED);
            case ALREADY_STARTING:
                return result(Outcome.ALREADY_STARTING);
            case OWNER_UNAVAILABLE:
                return result(Outcome.OWNER_UNAVAILABLE);
            case LAUNCH_FAILED:
                return new Result(
                        Outcome.LAUNCH_FAILED,
                        result.getLaunchFailure());
            default:
                throw new AssertionError(
                        "Unhandled launch result: " +
                                result.getOutcome());
        }
    }

    private static Result result(Outcome outcome) {
        return new Result(outcome, null);
    }
}
