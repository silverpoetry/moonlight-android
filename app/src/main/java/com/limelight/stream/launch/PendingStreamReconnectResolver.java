package com.limelight.stream.launch;

/** Pure policy for deciding whether a pending stream can be resumed now. */
public final class PendingStreamReconnectResolver {
    public enum Outcome {
        READY,
        NO_PENDING,
        HOST_MISMATCH,
        HOST_UNAVAILABLE,
        INVALID_APP
    }

    public static final class Resolution {
        private final Outcome outcome;
        private final int appId;

        private Resolution(Outcome outcome, int appId) {
            this.outcome = outcome;
            this.appId = appId;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public int getAppId() {
            if (outcome != Outcome.READY) {
                throw new IllegalStateException(
                        "Only a ready resolution has an app ID");
            }
            return appId;
        }
    }

    public Resolution resolve(
            PendingStreamReconnect pending,
            String currentHostId,
            boolean hostAvailable,
            int runningAppId) {
        if (pending == null) {
            return outcome(Outcome.NO_PENDING);
        }
        if (currentHostId != null &&
                !currentHostId.equalsIgnoreCase(
                        pending.getHostId())) {
            return outcome(Outcome.HOST_MISMATCH);
        }
        if (!hostAvailable) {
            return outcome(Outcome.HOST_UNAVAILABLE);
        }
        int appId = pending.getAppId() == 0
                ? runningAppId
                : pending.getAppId();
        if (appId <= 0) {
            return outcome(Outcome.INVALID_APP);
        }
        return new Resolution(Outcome.READY, appId);
    }

    private static Resolution outcome(Outcome outcome) {
        return new Resolution(outcome, 0);
    }
}
