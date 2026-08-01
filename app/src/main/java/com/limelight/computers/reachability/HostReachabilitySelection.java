package com.limelight.computers.reachability;

import java.util.Objects;

/**
 * Pure winner-selection state for concurrent endpoint probes.
 *
 * <p>The first reachable endpoint starts one bounded grace window. A result is
 * published earlier only when every higher-priority probe has completed, or
 * when the highest-priority endpoint succeeds.</p>
 */
final class HostReachabilitySelection<T> {
    static final long WAIT_INDEFINITELY = -1L;

    enum Status {
        WAITING,
        REACHABLE,
        UNREACHABLE
    }

    static final class Decision<T> {
        private final Status status;
        private final int winnerIndex;
        private final T value;
        private final long waitMillis;

        private Decision(
                Status status,
                int winnerIndex,
                T value,
                long waitMillis) {
            this.status = status;
            this.winnerIndex = winnerIndex;
            this.value = value;
            this.waitMillis = waitMillis;
        }

        Status getStatus() {
            return status;
        }

        int getWinnerIndex() {
            return winnerIndex;
        }

        T getValue() {
            return value;
        }

        long getWaitMillis() {
            return waitMillis;
        }
    }

    private final boolean[] completed;
    private final long upgradeGraceMillis;

    private int completedCount;
    private int winnerIndex = -1;
    private T winnerValue;
    private long upgradeDeadlineMillis = Long.MAX_VALUE;

    HostReachabilitySelection(
            int candidateCount,
            long upgradeGraceMillis) {
        if (candidateCount < 0) {
            throw new IllegalArgumentException(
                    "Candidate count cannot be negative");
        }
        if (upgradeGraceMillis < 0) {
            throw new IllegalArgumentException(
                    "Upgrade grace cannot be negative");
        }
        completed = new boolean[candidateCount];
        this.upgradeGraceMillis = upgradeGraceMillis;
    }

    void recordCompletion(int index, T value, long nowMillis) {
        if (index < 0 || index >= completed.length) {
            throw new IndexOutOfBoundsException(
                    "Candidate index is out of range");
        }
        if (completed[index]) {
            throw new IllegalStateException(
                    "Candidate completion was already recorded");
        }
        completed[index] = true;
        completedCount++;

        if (value == null ||
                (winnerIndex >= 0 && index >= winnerIndex)) {
            return;
        }
        winnerIndex = index;
        winnerValue = Objects.requireNonNull(value, "value");
        if (upgradeDeadlineMillis == Long.MAX_VALUE) {
            upgradeDeadlineMillis = saturatingAdd(
                    nowMillis,
                    upgradeGraceMillis);
        }
    }

    Decision<T> decide(long nowMillis) {
        if (winnerIndex >= 0) {
            if (winnerIndex == 0 ||
                    allHigherPriorityCandidatesCompleted() ||
                    nowMillis >= upgradeDeadlineMillis) {
                return new Decision<>(
                        Status.REACHABLE,
                        winnerIndex,
                        winnerValue,
                        0L);
            }
            return new Decision<>(
                    Status.WAITING,
                    -1,
                    null,
                    Math.max(
                            0L,
                            upgradeDeadlineMillis - nowMillis));
        }
        if (completedCount == completed.length) {
            return new Decision<>(
                    Status.UNREACHABLE,
                    -1,
                    null,
                    0L);
        }
        return new Decision<>(
                Status.WAITING,
                -1,
                null,
                WAIT_INDEFINITELY);
    }

    private boolean allHigherPriorityCandidatesCompleted() {
        for (int index = 0; index < winnerIndex; index++) {
            if (!completed[index]) {
                return false;
            }
        }
        return true;
    }

    private static long saturatingAdd(long value, long increment) {
        if (increment > Long.MAX_VALUE - value) {
            return Long.MAX_VALUE;
        }
        return value + increment;
    }
}
