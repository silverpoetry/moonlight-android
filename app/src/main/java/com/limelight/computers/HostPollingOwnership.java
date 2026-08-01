package com.limelight.computers;

import java.util.Objects;

/** Generation-gated ownership for the single visible host-list subscriber. */
final class HostPollingOwnership<T> {
    static final class Token {
        private final long generation;

        private Token(long generation) {
            this.generation = generation;
        }
    }

    private long nextGeneration;
    private Token activeToken;
    private T listener;

    synchronized Token replace(T listener) {
        if (nextGeneration == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Host polling generation overflow");
        }
        Token token = new Token(++nextGeneration);
        activeToken = token;
        this.listener = Objects.requireNonNull(listener, "listener");
        return token;
    }

    synchronized boolean owns(Token token) {
        return token != null &&
                activeToken != null &&
                activeToken.generation == token.generation;
    }

    synchronized T getListener() {
        return listener;
    }

    synchronized T getListener(Token token) {
        return owns(token) ? listener : null;
    }

    synchronized boolean release(Token token) {
        if (!owns(token)) {
            return false;
        }
        activeToken = null;
        listener = null;
        return true;
    }

    synchronized boolean clear() {
        if (activeToken == null) {
            return false;
        }
        activeToken = null;
        listener = null;
        return true;
    }
}
