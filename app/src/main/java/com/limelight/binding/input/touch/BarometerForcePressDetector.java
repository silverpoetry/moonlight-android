package com.limelight.binding.input.touch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Fuses a live touchscreen contact with a positive barometer transient.
 *
 * <p>The pressure baseline is learned only while no pointer is touching the
 * screen and is frozen for the complete touch session, so a real pressure
 * spike cannot be absorbed into the baseline. The user-configured threshold is
 * fixed. A minimum stable contact-set duration filters pressure transients
 * caused by fingers landing on the screen. A detected force press remains
 * latched even after pressure falls and is released when its owning contact
 * set changes or the session is cancelled.</p>
 */
final class BarometerForcePressDetector {
    private static final int IDLE_WINDOW_SIZE = 64;
    private static final int MIN_BASELINE_SAMPLES = 8;
    private static final float DEFAULT_THRESHOLD_HPA = 0.18f;

    private final float[] idleSamples = new float[IDLE_WINDOW_SIZE];
    private final Set<Integer> activePointers = new HashSet<>();

    private int idleSampleCount;
    private int idleWriteIndex;
    private int primaryPointerId = -1;
    private int forcePointerId = -1;
    private int forcePointerCount;
    private float thresholdHpa = DEFAULT_THRESHOLD_HPA;
    private long minimumTouchDurationMs;
    private long contactSetStartTimeMs;
    private float currentPressureHpa = Float.NaN;
    private float idleBaselineHpa = Float.NaN;
    private float sessionBaselineHpa = Float.NaN;
    private float currentDeltaHpa;
    private boolean forcePressed;
    private boolean forceTriggeredOnLastSample;
    private boolean forceBlockedForTouchSession;

    void onPointerDown(int pointerId, long eventTimeMs) {
        if (activePointers.isEmpty()) {
            primaryPointerId = pointerId;
            forcePointerId = -1;
            forcePressed = false;
            forceTriggeredOnLastSample = false;
            forceBlockedForTouchSession = false;
            sessionBaselineHpa = hasStableBaseline()
                    ? idleBaselineHpa
                    : currentPressureHpa;
            currentDeltaHpa = 0.0f;
        }
        activePointers.add(pointerId);
        contactSetStartTimeMs = eventTimeMs;
    }

    void onPointerUp(int pointerId, long eventTimeMs) {
        activePointers.remove(pointerId);

        if (forcePressed &&
                (pointerId == forcePointerId ||
                        activePointers.size() < forcePointerCount)) {
            clearForcePress();
        }
        if (activePointers.isEmpty()) {
            clearTouchSession();
        }
        else if (pointerId == primaryPointerId) {
            primaryPointerId = activePointers.iterator().next();
        }
        if (!activePointers.isEmpty()) {
            contactSetStartTimeMs = eventTimeMs;
        }
    }

    void onPressureSample(float pressureHpa, long sampleTimeMs) {
        if (!isFinite(pressureHpa) || pressureHpa <= 0.0f) {
            return;
        }

        currentPressureHpa = pressureHpa;
        forceTriggeredOnLastSample = false;
        if (activePointers.isEmpty()) {
            addIdleSample(pressureHpa);
            currentDeltaHpa = isFinite(idleBaselineHpa)
                    ? pressureHpa - idleBaselineHpa
                    : 0.0f;
            return;
        }

        if (!isFinite(sessionBaselineHpa)) {
            sessionBaselineHpa = pressureHpa;
        }
        currentDeltaHpa = pressureHpa - sessionBaselineHpa;

        if (!forcePressed &&
                !forceBlockedForTouchSession &&
                activePointers.size() >= 1 &&
                activePointers.size() <= 2 &&
                sampleTimeMs - contactSetStartTimeMs >=
                        minimumTouchDurationMs &&
                currentDeltaHpa >= getThresholdHpa()) {
            forcePressed = true;
            forceTriggeredOnLastSample = true;
            forcePointerId = primaryPointerId;
            forcePointerCount = activePointers.size();
        }
    }

    void setThresholdHpa(float thresholdHpa) {
        if (!isFinite(thresholdHpa) || thresholdHpa <= 0.0f) {
            throw new IllegalArgumentException("thresholdHpa must be positive");
        }
        this.thresholdHpa = thresholdHpa;
    }

    void setMinimumTouchDurationMs(long minimumTouchDurationMs) {
        if (minimumTouchDurationMs < 0) {
            throw new IllegalArgumentException(
                    "minimumTouchDurationMs must not be negative");
        }
        this.minimumTouchDurationMs = minimumTouchDurationMs;
    }

    void blockCurrentTouchSession() {
        clearForcePress();
        forceBlockedForTouchSession = true;
    }

    void cancelTouchSession() {
        activePointers.clear();
        clearTouchSession();
    }

    private void addIdleSample(float pressureHpa) {
        idleSamples[idleWriteIndex] = pressureHpa;
        idleWriteIndex = (idleWriteIndex + 1) % IDLE_WINDOW_SIZE;
        idleSampleCount = Math.min(idleSampleCount + 1, IDLE_WINDOW_SIZE);

        float[] sorted = Arrays.copyOf(idleSamples, idleSampleCount);
        Arrays.sort(sorted);
        idleBaselineHpa = median(sorted);

    }

    private static float median(float[] sortedValues) {
        int middle = sortedValues.length / 2;
        if ((sortedValues.length & 1) == 0) {
            return (sortedValues[middle - 1] + sortedValues[middle]) * 0.5f;
        }
        return sortedValues[middle];
    }

    private void clearForcePress() {
        forcePressed = false;
        forceTriggeredOnLastSample = false;
        forcePointerId = -1;
        forcePointerCount = 0;
    }

    private void clearTouchSession() {
        clearForcePress();
        primaryPointerId = -1;
        sessionBaselineHpa = Float.NaN;
        currentDeltaHpa = 0.0f;
        contactSetStartTimeMs = 0;
        forceBlockedForTouchSession = false;
    }

    boolean hasStableBaseline() {
        return idleSampleCount >= MIN_BASELINE_SAMPLES &&
                isFinite(idleBaselineHpa);
    }

    float getThresholdHpa() {
        return thresholdHpa;
    }

    long getMinimumTouchDurationMs() {
        return minimumTouchDurationMs;
    }

    int getActivePointerCount() {
        return activePointers.size();
    }

    int getForcePointerId() {
        return forcePointerId;
    }

    float getCurrentPressureHpa() {
        return currentPressureHpa;
    }

    float getSessionBaselineHpa() {
        return sessionBaselineHpa;
    }

    float getCurrentDeltaHpa() {
        return currentDeltaHpa;
    }

    boolean isForcePressed() {
        return forcePressed;
    }

    boolean wasForceTriggeredOnLastSample() {
        return forceTriggeredOnLastSample;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
