package com.limelight.binding.video;

/**
 * Decodes the packed ENet control-stream RTT estimate returned by common-c.
 *
 * <p>This value is a smoothed round-trip time. It is neither a one-way delay
 * nor a video capture-to-display latency, so it must not be divided by two or
 * combined with decoder timing.</p>
 */
final class ControlStreamRtt {
    private static final long UNAVAILABLE_PACKED_VALUE = -1L;

    private final boolean available;
    private final int roundTripTimeMs;
    private final int varianceMs;

    private ControlStreamRtt(
            boolean available,
            int roundTripTimeMs,
            int varianceMs) {
        this.available = available;
        this.roundTripTimeMs = roundTripTimeMs;
        this.varianceMs = varianceMs;
    }

    static ControlStreamRtt fromPacked(long packedValue) {
        if (packedValue == UNAVAILABLE_PACKED_VALUE) {
            return new ControlStreamRtt(false, 0, 0);
        }
        return new ControlStreamRtt(
                true,
                (int) (packedValue >>> 32),
                (int) (packedValue & 0xffffffffL));
    }

    boolean isAvailable() {
        return available;
    }

    int getRoundTripTimeMs() {
        return roundTripTimeMs;
    }

    int getVarianceMs() {
        return varianceMs;
    }
}
