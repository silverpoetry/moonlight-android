package com.limelight.computers;

import java.util.Objects;

/**
 * Lifecycle and generation owner for one UI client's host-polling
 * subscription. It supports synchronous initial callbacks while a start is
 * still in progress and rejects completion from an obsolete binding.
 */
public final class HostPollingClientLifecycle {
    public static final class StartToken {
        private final long generation;

        private StartToken(long generation) {
            this.generation = generation;
        }
    }

    private long nextGeneration;
    private StartToken currentToken;
    private Runnable closeAction;
    private boolean active;
    private boolean startInProgress;
    private boolean destroyed;

    public synchronized void activate() {
        if (!destroyed) {
            active = true;
        }
    }

    public synchronized StartToken beginStart() {
        if (!active || destroyed ||
                startInProgress || closeAction != null) {
            return null;
        }
        if (nextGeneration == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Host polling client generation overflow");
        }
        currentToken = new StartToken(++nextGeneration);
        startInProgress = true;
        return currentToken;
    }

    public synchronized boolean owns(StartToken token) {
        return active && !destroyed && token != null &&
                currentToken != null &&
                currentToken.generation == token.generation;
    }

    public boolean completeStart(
            StartToken token,
            Runnable closeAction) {
        Objects.requireNonNull(closeAction, "closeAction");
        synchronized (this) {
            if (owns(token) && startInProgress) {
                this.closeAction = closeAction;
                startInProgress = false;
                return true;
            }
        }

        // The resource was created after its owner stopped or was replaced.
        // Rejecting and closing in one operation prevents callers from
        // accidentally leaking a late subscription.
        closeAction.run();
        return false;
    }

    public synchronized void failStart(StartToken token) {
        if (currentToken == token && startInProgress) {
            invalidateCurrentStart();
        }
    }

    /** Stops the current subscription and requires activate() before retry. */
    public synchronized Runnable deactivate() {
        active = false;
        return invalidateCurrentStart();
    }

    /** Invalidates a dead service binding while preserving foreground intent. */
    public synchronized Runnable onConnectionLost() {
        return invalidateCurrentStart();
    }

    public synchronized Runnable destroy() {
        destroyed = true;
        active = false;
        return invalidateCurrentStart();
    }

    private Runnable invalidateCurrentStart() {
        Runnable detachedCloseAction = closeAction;
        closeAction = null;
        currentToken = null;
        startInProgress = false;
        return detachedCloseAction;
    }
}
