package com.limelight.binding.video;

import androidx.annotation.AnyThread;

import java.util.Objects;

/** Rebindable performance sink for a decoder created before its UI owner. */
public final class PerfOverlayRelay implements PerfOverlayListener {
    private final Object lock = new Object();
    private PerfOverlayListener observer;
    private PerfOverlayStats latest;

    @AnyThread
    public void attach(PerfOverlayListener newObserver) {
        Objects.requireNonNull(newObserver, "newObserver");
        PerfOverlayStats snapshot;
        synchronized (lock) {
            observer = newObserver;
            snapshot = latest == null ? null : copy(latest);
        }
        if (snapshot != null) {
            newObserver.onPerfUpdate(snapshot);
        }
    }

    @AnyThread
    public void detach(PerfOverlayListener expectedObserver) {
        synchronized (lock) {
            if (observer == expectedObserver) {
                observer = null;
            }
        }
    }

    @Override
    public void onPerfUpdate(PerfOverlayStats stats) {
        Objects.requireNonNull(stats, "stats");
        PerfOverlayListener target;
        synchronized (lock) {
            latest = copy(stats);
            target = observer;
        }
        if (target != null) {
            target.onPerfUpdate(stats);
        }
    }

    private static PerfOverlayStats copy(PerfOverlayStats source) {
        PerfOverlayStats result = new PerfOverlayStats();
        result.width = source.width;
        result.height = source.height;
        result.targetBitrateKbps = source.targetBitrateKbps;
        result.targetFps = source.targetFps;
        result.networkLatencyMs = source.networkLatencyMs;
        result.networkLatencyVarianceMs =
                source.networkLatencyVarianceMs;
        result.networkLatencyAvailable =
                source.networkLatencyAvailable;
        result.totalFps = source.totalFps;
        result.receivedFps = source.receivedFps;
        result.renderedFps = source.renderedFps;
        result.packetLossPercent = source.packetLossPercent;
        result.decodeTimeMs = source.decodeTimeMs;
        result.decoderLatencyAvailable =
                source.decoderLatencyAvailable;
        result.hostProcessingLatencyMs =
                source.hostProcessingLatencyMs;
        result.networkRateKbps = source.networkRateKbps;
        result.videoRateKbps = source.videoRateKbps;
        result.audioRateKbps = source.audioRateKbps;
        result.videoBytes = source.videoBytes;
        result.audioBytes = source.audioBytes;
        result.totalNetworkBytes = source.totalNetworkBytes;
        result.hdr = source.hdr;
        result.codecName = source.codecName;
        result.decoderName = source.decoderName;
        return result;
    }
}
