package com.limelight.binding.video;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe recovery request priority plus monitor-guarded quiescence state.
 */
final class CodecRecoveryCoordinator {
    enum RecoveryType {
        NONE(0),
        FLUSH(1),
        RESTART(2),
        RESET(3);

        private final int strength;

        RecoveryType(int strength) {
            this.strength = strength;
        }

        boolean isAtLeast(RecoveryType other) {
            return strength >= other.strength;
        }
    }

    static final int INPUT_THREAD = 0x1;
    static final int RENDER_THREAD = 0x2;
    static final int CHOREOGRAPHER_THREAD = 0x4;

    private static final int ALL_THREADS =
            INPUT_THREAD |
                    RENDER_THREAD |
                    CHOREOGRAPHER_THREAD;

    private final AtomicReference<RecoveryType> recoveryType =
            new AtomicReference<>(RecoveryType.NONE);
    private final AtomicInteger attempts =
            new AtomicInteger();

    // Accessed only while the decoder recovery monitor is held.
    private int quiescedThreads;

    RecoveryType getRecoveryType() {
        return recoveryType.get();
    }

    boolean hasPendingRecovery() {
        return getRecoveryType() != RecoveryType.NONE;
    }

    /**
     * Requests at least the supplied recovery strength.
     *
     * @return true only when the request changed the current state
     */
    boolean request(RecoveryType requestedType) {
        if (requestedType == RecoveryType.NONE) {
            throw new IllegalArgumentException(
                    "NONE is not a recovery request");
        }

        while (true) {
            RecoveryType current = recoveryType.get();
            if (current.isAtLeast(requestedType)) {
                return false;
            }
            if (recoveryType.compareAndSet(
                    current,
                    requestedType)) {
                return true;
            }
        }
    }

    void completeRecovery() {
        recoveryType.set(RecoveryType.NONE);
    }

    void markThreadQuiesced(
            int threadFlag,
            boolean hasChoreographerThread) {
        if (!hasChoreographerThread) {
            quiescedThreads |= CHOREOGRAPHER_THREAD;
        }
        quiescedThreads |= threadFlag;
    }

    boolean areAllThreadsQuiesced() {
        return quiescedThreads == ALL_THREADS;
    }

    int getQuiescedThreads() {
        return quiescedThreads;
    }

    void clearQuiescedThreads() {
        quiescedThreads = 0;
    }

    boolean hasAttemptsRemaining(int maximumAttempts) {
        return attempts.get() < maximumAttempts;
    }

    int beginRecoveryAttempt() {
        return attempts.incrementAndGet();
    }

    void resetAttempts() {
        attempts.set(0);
    }
}
