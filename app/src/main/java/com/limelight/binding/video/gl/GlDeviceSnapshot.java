package com.limelight.binding.video.gl;

import java.util.Objects;

/**
 * Immutable build-scoped OpenGL device identity used by decoder policy.
 *
 * <p>The build fingerprint and renderer are one atomic fact: a renderer from
 * another system image must never be treated as current. Unavailable values
 * are represented explicitly and expose an empty renderer to preserve the
 * conservative decoder fallback used before a probe succeeds.</p>
 */
public final class GlDeviceSnapshot {
    static final int MAX_BUILD_FINGERPRINT_LENGTH = 4_096;
    static final int MAX_RENDERER_LENGTH = 1_024;

    private static final GlDeviceSnapshot UNAVAILABLE =
            new GlDeviceSnapshot(false, "", "");

    private final boolean available;
    private final String buildFingerprint;
    private final String renderer;

    private GlDeviceSnapshot(
            boolean available,
            String buildFingerprint,
            String renderer) {
        this.available = available;
        this.buildFingerprint = buildFingerprint;
        this.renderer = renderer;
    }

    public static GlDeviceSnapshot unavailable() {
        return UNAVAILABLE;
    }

    public static GlDeviceSnapshot available(
            String buildFingerprint,
            String renderer) {
        if (!isValid(buildFingerprint, MAX_BUILD_FINGERPRINT_LENGTH)) {
            throw new IllegalArgumentException(
                    "buildFingerprint must be non-empty and bounded");
        }
        if (!isValid(renderer, MAX_RENDERER_LENGTH)) {
            throw new IllegalArgumentException(
                    "renderer must be non-empty and bounded");
        }
        return new GlDeviceSnapshot(
                true,
                buildFingerprint,
                renderer);
    }

    /** Converts untrusted platform or persisted strings into a safe value. */
    public static GlDeviceSnapshot fromUntrusted(
            String buildFingerprint,
            String renderer) {
        if (!isValid(buildFingerprint, MAX_BUILD_FINGERPRINT_LENGTH) ||
                !isValid(renderer, MAX_RENDERER_LENGTH)) {
            return unavailable();
        }
        return new GlDeviceSnapshot(
                true,
                buildFingerprint,
                renderer);
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isCurrentFor(String currentBuildFingerprint) {
        return available &&
                buildFingerprint.equals(currentBuildFingerprint);
    }

    public String getBuildFingerprint() {
        return buildFingerprint;
    }

    public String getRenderer() {
        return renderer;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GlDeviceSnapshot)) {
            return false;
        }
        GlDeviceSnapshot snapshot = (GlDeviceSnapshot) other;
        return available == snapshot.available &&
                buildFingerprint.equals(snapshot.buildFingerprint) &&
                renderer.equals(snapshot.renderer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(available, buildFingerprint, renderer);
    }

    private static boolean isValid(String value, int maximumLength) {
        return value != null &&
                !value.isEmpty() &&
                value.length() <= maximumLength;
    }
}
