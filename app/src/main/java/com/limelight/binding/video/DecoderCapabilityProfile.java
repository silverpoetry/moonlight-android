package com.limelight.binding.video;

/**
 * Immutable decoder capabilities published before stream setup begins.
 */
final class DecoderCapabilityProfile {
    private final boolean directSubmit;
    private final boolean avcReferenceFrameInvalidation;
    private final boolean hevcReferenceFrameInvalidation;
    private final boolean av1ReferenceFrameInvalidation;
    private final byte optimalSlicesPerFrame;

    private DecoderCapabilityProfile(
            boolean directSubmit,
            boolean avcReferenceFrameInvalidation,
            boolean hevcReferenceFrameInvalidation,
            boolean av1ReferenceFrameInvalidation,
            byte optimalSlicesPerFrame) {
        this.directSubmit = directSubmit;
        this.avcReferenceFrameInvalidation =
                avcReferenceFrameInvalidation;
        this.hevcReferenceFrameInvalidation =
                hevcReferenceFrameInvalidation;
        this.av1ReferenceFrameInvalidation =
                av1ReferenceFrameInvalidation;
        this.optimalSlicesPerFrame =
                optimalSlicesPerFrame;
    }

    static DecoderCapabilityProfile create(
            boolean directSubmit,
            boolean avcReferenceFrameInvalidation,
            boolean hevcReferenceFrameInvalidation,
            boolean av1ReferenceFrameInvalidation,
            int avcOptimalSlicesPerFrame,
            int hevcOptimalSlicesPerFrame,
            int consecutiveCrashCount) {
        boolean disableLegacyReferenceFrameInvalidation =
                consecutiveCrashCount % 2 == 1;
        return new DecoderCapabilityProfile(
                directSubmit,
                avcReferenceFrameInvalidation &&
                        !disableLegacyReferenceFrameInvalidation,
                hevcReferenceFrameInvalidation &&
                        !disableLegacyReferenceFrameInvalidation,
                av1ReferenceFrameInvalidation,
                (byte) Math.max(
                        avcOptimalSlicesPerFrame,
                        hevcOptimalSlicesPerFrame));
    }

    boolean isDirectSubmitEnabled() {
        return directSubmit;
    }

    boolean isAvcReferenceFrameInvalidationEnabled() {
        return avcReferenceFrameInvalidation;
    }

    boolean isHevcReferenceFrameInvalidationEnabled() {
        return hevcReferenceFrameInvalidation;
    }

    boolean isAv1ReferenceFrameInvalidationEnabled() {
        return av1ReferenceFrameInvalidation;
    }

    byte getOptimalSlicesPerFrame() {
        return optimalSlicesPerFrame;
    }
}
