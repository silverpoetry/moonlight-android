package com.limelight.binding.input.touch;

/**
 * Estimates physical finger velocity and applies an adaptive touchpad acceleration profile.
 *
 * <p>The profile follows the shape and constants of libinput's mature adaptive touchpad filter:
 * low-speed motion is decelerated for precision, ordinary motion remains on a plateau, and fast
 * motion is accelerated up to a bounded maximum. Windows Precision Touchpad uses the same
 * velocity-dependent behavior class, but its exact cursor curve is not public.</p>
 *
 * <p>Velocity is calculated from a short history of physical contact positions rather than a
 * single sample. History is discarded across pauses and direction reversals so a fast movement
 * cannot leak acceleration into a subsequent precision movement.</p>
 *
 * @see <a href="https://wayland.freedesktop.org/libinput/doc/latest/pointer-acceleration.html">
 * libinput pointer acceleration</a>
 */
final class TouchpadPointerAcceleration {
    private static final int MAX_SAMPLES = 16;
    private static final long MOTION_TIMEOUT_MS = 1000;
    private static final long DEFAULT_FIRST_SAMPLE_INTERVAL_MS = 7;
    private static final double MAX_VELOCITY_DIFFERENCE_MM_PER_SECOND = 64.0;

    // Android touch coordinates are quantized to logical display pixels before they reach the
    // legacy touch-context API. On a high-refresh phone, one pixel per frame can already exceed
    // libinput's 7 mm/s precision range. Keep the precision ramp wide enough to remain reachable
    // after that quantization while leaving ordinary movement at the existing 1.0 plateau.
    private static final double PRECISION_SPEED_THRESHOLD_MM_PER_SECOND = 40.0;
    private static final double MINIMUM_PRECISION_FACTOR = 1.0 / 3.0;
    private static final double ACCELERATION_THRESHOLD_MM_PER_SECOND = 130.0;
    private static final double MAX_ACCELERATION_SPEED_MM_PER_SECOND =
            ACCELERATION_THRESHOLD_MM_PER_SECOND * 4.0;
    private static final double BASELINE_FACTOR = 0.9;

    private final long[] sampleTimes = new long[MAX_SAMPLES];
    private final double[] sampleX = new double[MAX_SAMPLES];
    private final double[] sampleY = new double[MAX_SAMPLES];

    private int sampleCount;
    private double positionX;
    private double positionY;
    private double previousDeltaX;
    private double previousDeltaY;
    private double lastVelocity;
    private long lastEventTime;
    private boolean hasPreviousDelta;
    private boolean hasLastVelocity;

    void restart(long eventTime) {
        sampleCount = 0;
        positionX = 0.0;
        positionY = 0.0;
        previousDeltaX = 0.0;
        previousDeltaY = 0.0;
        lastVelocity = 0.0;
        lastEventTime = eventTime;
        hasPreviousDelta = false;
        hasLastVelocity = false;
        appendSample(eventTime, positionX, positionY);
    }

    double calculateAcceleration(double deltaXMillimeters, double deltaYMillimeters,
                                 long eventTime) {
        if (sampleCount == 0) {
            restart(eventTime - DEFAULT_FIRST_SAMPLE_INTERVAL_MS);
        }

        long elapsedTime = eventTime - lastEventTime;
        boolean historyRestarted = false;
        if (elapsedTime < 0 || elapsedTime > MOTION_TIMEOUT_MS) {
            restart(eventTime - DEFAULT_FIRST_SAMPLE_INTERVAL_MS);
            historyRestarted = true;
        }
        else if (hasPreviousDelta &&
                isDirectionReversal(previousDeltaX, previousDeltaY,
                        deltaXMillimeters, deltaYMillimeters)) {
            restartFromCurrentPosition(eventTime, elapsedTime);
            historyRestarted = true;
        }

        positionX += deltaXMillimeters;
        positionY += deltaYMillimeters;
        appendSample(eventTime, positionX, positionY);

        double velocity = estimateVelocity();
        double factor;
        if (!hasLastVelocity || historyRestarted) {
            factor = profile(velocity);
        }
        else {
            // Simpson's rule smooths gain transitions between consecutive velocity estimates.
            factor = (profile(lastVelocity) + 4.0 * profile(
                    (lastVelocity + velocity) * 0.5) + profile(velocity)) / 6.0;
        }

        previousDeltaX = deltaXMillimeters;
        previousDeltaY = deltaYMillimeters;
        lastVelocity = velocity;
        lastEventTime = eventTime;
        hasPreviousDelta = true;
        hasLastVelocity = true;
        return factor;
    }

    private void restartFromCurrentPosition(long eventTime, long elapsedTime) {
        long baselineInterval = elapsedTime > 0
                ? Math.min(elapsedTime, DEFAULT_FIRST_SAMPLE_INTERVAL_MS)
                : DEFAULT_FIRST_SAMPLE_INTERVAL_MS;

        sampleCount = 0;
        previousDeltaX = 0.0;
        previousDeltaY = 0.0;
        lastVelocity = 0.0;
        lastEventTime = eventTime - baselineInterval;
        hasPreviousDelta = false;
        hasLastVelocity = false;
        appendSample(lastEventTime, positionX, positionY);
    }

    private void appendSample(long eventTime, double x, double y) {
        if (sampleCount != 0 && sampleTimes[sampleCount - 1] == eventTime) {
            sampleX[sampleCount - 1] = x;
            sampleY[sampleCount - 1] = y;
            return;
        }

        if (sampleCount == MAX_SAMPLES) {
            System.arraycopy(sampleTimes, 1, sampleTimes, 0, MAX_SAMPLES - 1);
            System.arraycopy(sampleX, 1, sampleX, 0, MAX_SAMPLES - 1);
            System.arraycopy(sampleY, 1, sampleY, 0, MAX_SAMPLES - 1);
            sampleCount--;
        }

        sampleTimes[sampleCount] = eventTime;
        sampleX[sampleCount] = x;
        sampleY[sampleCount] = y;
        sampleCount++;
    }

    private double estimateVelocity() {
        if (sampleCount < 2) {
            return 0.0;
        }

        int newest = sampleCount - 1;
        int oldestAccepted = newest - 1;
        double initialVelocity = velocityBetween(oldestAccepted, newest);
        double currentDeltaX = sampleX[newest] - sampleX[newest - 1];
        double currentDeltaY = sampleY[newest] - sampleY[newest - 1];

        for (int candidate = newest - 2; candidate >= 0; candidate--) {
            long timeDelta = sampleTimes[newest] - sampleTimes[candidate];
            if (timeDelta <= 0 || timeDelta > MOTION_TIMEOUT_MS) {
                break;
            }

            double segmentX = sampleX[candidate + 1] - sampleX[candidate];
            double segmentY = sampleY[candidate + 1] - sampleY[candidate];
            if (isDirectionReversal(currentDeltaX, currentDeltaY, segmentX, segmentY)) {
                break;
            }

            double candidateVelocity = velocityBetween(candidate, newest);
            // Always include the first two intervals to damp noisy initial touch samples.
            if (candidate < newest - 2 &&
                    Math.abs(candidateVelocity - initialVelocity) >
                            MAX_VELOCITY_DIFFERENCE_MM_PER_SECOND) {
                break;
            }

            oldestAccepted = candidate;
        }

        return velocityBetween(oldestAccepted, newest);
    }

    private double velocityBetween(int olderSample, int newerSample) {
        long timeDelta = sampleTimes[newerSample] - sampleTimes[olderSample];
        if (timeDelta <= 0) {
            return hasLastVelocity ? lastVelocity : 0.0;
        }

        return Math.hypot(sampleX[newerSample] - sampleX[olderSample],
                sampleY[newerSample] - sampleY[olderSample]) * 1000.0 / timeDelta;
    }

    private static boolean isDirectionReversal(double firstX, double firstY,
                                               double secondX, double secondY) {
        return firstX * secondX + firstY * secondY <= 0.0;
    }

    private static double profile(double speedMillimetersPerSecond) {
        if (speedMillimetersPerSecond < PRECISION_SPEED_THRESHOLD_MM_PER_SECOND) {
            double normalizedSpeed =
                    speedMillimetersPerSecond / PRECISION_SPEED_THRESHOLD_MM_PER_SECOND;
            double smoothSpeed = normalizedSpeed * normalizedSpeed *
                    (3.0 - 2.0 * normalizedSpeed);
            return MINIMUM_PRECISION_FACTOR +
                    (1.0 - MINIMUM_PRECISION_FACTOR) * smoothSpeed;
        }

        double factor = BASELINE_FACTOR;
        if (speedMillimetersPerSecond < ACCELERATION_THRESHOLD_MM_PER_SECOND) {
            return 1.0;
        }

        double boundedSpeed = Math.min(speedMillimetersPerSecond,
                MAX_ACCELERATION_SPEED_MM_PER_SECOND);
        factor = 0.0025 *
                (boundedSpeed / ACCELERATION_THRESHOLD_MM_PER_SECOND) *
                (boundedSpeed - ACCELERATION_THRESHOLD_MM_PER_SECOND) +
                BASELINE_FACTOR;

        // Normalize the ordinary-speed plateau to the app's existing sensitivity setting.
        return factor / BASELINE_FACTOR;
    }
}
