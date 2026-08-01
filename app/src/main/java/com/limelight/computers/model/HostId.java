package com.limelight.computers.model;

import java.util.Locale;
import java.util.Objects;

/** Stable, case-insensitive host identity reported by the streaming server. */
public final class HostId {
    public static final int MAXIMUM_LENGTH = 256;

    private final String value;

    private HostId(String value) {
        this.value = value;
    }

    public static HostId of(String value) {
        String normalized = Objects.requireNonNull(value, "value")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() ||
                normalized.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Invalid host ID");
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (Character.isWhitespace(normalized.charAt(index))) {
                throw new IllegalArgumentException(
                        "Host ID cannot contain whitespace");
            }
        }
        return new HostId(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HostId &&
                value.equals(((HostId) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
