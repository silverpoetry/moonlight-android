package com.limelight.virtualcontrols.layout;

import java.util.Objects;

/**
 * Explicit result of loading a layout document without relying on
 * API-24-only {@code java.util.Optional}.
 */
public final class VirtualControlLayoutReadResult {
    private static final VirtualControlLayoutReadResult MISSING =
            new VirtualControlLayoutReadResult(null);

    private final VirtualControlLayoutDocument document;

    private VirtualControlLayoutReadResult(
            VirtualControlLayoutDocument document) {
        this.document = document;
    }

    public static VirtualControlLayoutReadResult missing() {
        return MISSING;
    }

    public static VirtualControlLayoutReadResult found(
            VirtualControlLayoutDocument document) {
        return new VirtualControlLayoutReadResult(
                Objects.requireNonNull(document, "document"));
    }

    public boolean isFound() {
        return document != null;
    }

    public VirtualControlLayoutDocument getDocument() {
        if (document == null) {
            throw new IllegalStateException(
                    "Missing layout has no document");
        }
        return document;
    }
}
