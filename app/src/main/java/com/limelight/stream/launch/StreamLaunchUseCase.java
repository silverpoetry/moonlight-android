package com.limelight.stream.launch;

import java.util.Objects;

/** Owns launch admission and post-admission recent-session persistence. */
public final class StreamLaunchUseCase {
    public enum Outcome {
        STARTED,
        ALREADY_STARTING,
        OWNER_UNAVAILABLE,
        LAUNCH_FAILED
    }

    public interface Gateway {
        void launch(StreamLaunchRequest request);
    }

    public static final class Result {
        private final Outcome outcome;
        private final RuntimeException launchFailure;
        private final RuntimeException persistenceFailure;

        private Result(
                Outcome outcome,
                RuntimeException launchFailure,
                RuntimeException persistenceFailure) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.launchFailure = launchFailure;
            this.persistenceFailure = persistenceFailure;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public RuntimeException getLaunchFailure() {
            return launchFailure;
        }

        public RuntimeException getPersistenceFailure() {
            return persistenceFailure;
        }
    }

    private final RecentStreamSessionRepository recentSessions;
    private final StreamLaunchController controller;

    public StreamLaunchUseCase(
            RecentStreamSessionRepository recentSessions) {
        this(
                recentSessions,
                new StreamLaunchController());
    }

    StreamLaunchUseCase(
            RecentStreamSessionRepository recentSessions,
            StreamLaunchController controller) {
        this.recentSessions = Objects.requireNonNull(
                recentSessions,
                "recentSessions");
        this.controller = Objects.requireNonNull(
                controller,
                "controller");
    }

    public Result launch(
            StreamLaunchRequest request,
            Gateway gateway) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(gateway, "gateway");

        final StreamLaunchController.Status status;
        try {
            status = controller.launch(
                    () -> gateway.launch(request));
        }
        catch (RuntimeException failure) {
            return new Result(
                    Outcome.LAUNCH_FAILED,
                    failure,
                    null);
        }

        switch (status) {
            case ALREADY_STARTING:
                return result(Outcome.ALREADY_STARTING);
            case OWNER_DESTROYED:
                return result(Outcome.OWNER_UNAVAILABLE);
            case STARTED:
                return rememberAfterLaunch(request);
            default:
                throw new AssertionError(
                        "Unhandled launch status: " + status);
        }
    }

    public RecentStreamSession findRecentSession(String hostId) {
        return recentSessions.find(hostId);
    }

    public void onOwnerPaused() {
        controller.onOwnerPaused();
    }

    public void onOwnerResumed() {
        controller.onOwnerResumed();
    }

    public void onOwnerDestroyed() {
        controller.onOwnerDestroyed();
    }

    public void onLaunchFailed() {
        controller.onLaunchFailed();
    }

    private Result rememberAfterLaunch(StreamLaunchRequest request) {
        if (request.getHostId() == null ||
                request.getHostId().trim().isEmpty()) {
            return result(Outcome.STARTED);
        }
        try {
            recentSessions.save(
                    request.getHostId(),
                    new RecentStreamSession(
                            request.getAppName(),
                            request.getAppId(),
                            request.supportsHdr()));
            return result(Outcome.STARTED);
        }
        catch (RuntimeException persistenceFailure) {
            // The platform launch already succeeded. Report the auxiliary
            // failure without rolling back admission or misreporting launch.
            return new Result(
                    Outcome.STARTED,
                    null,
                    persistenceFailure);
        }
    }

    private static Result result(Outcome outcome) {
        return new Result(outcome, null, null);
    }
}
