package com.limelight.ui.stream;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;

import java.util.Objects;

/**
 * Owns the Wi-Fi performance locks for one stream Activity.
 */
public final class StreamWifiLockController {
    interface LockHandle {
        void setReferenceCounted(boolean referenceCounted);

        void acquire();

        boolean isHeld();

        void release();
    }

    interface LockFactory {
        LockHandle create(int mode, String tag);
    }

    private static final String HIGH_PERFORMANCE_TAG =
            "Moonlight High Perf Lock";
    private static final String LOW_LATENCY_TAG =
            "Moonlight Low Latency Lock";
    private static final int NO_LOW_LATENCY_MODE = -1;

    private final LockFactory lockFactory;
    private final int lowLatencyMode;

    private LockHandle highPerformanceLock;
    private LockHandle lowLatencyLock;
    private boolean acquisitionAttempted;
    private boolean destroyed;

    @MainThread
    public static StreamWifiLockController create(Context context) {
        Objects.requireNonNull(context, "context");
        Context applicationContext = context.getApplicationContext();
        WifiManager wifiManager = (WifiManager) applicationContext
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return new StreamWifiLockController(
                    (mode, tag) -> {
                        throw new IllegalStateException(
                                "Wi-Fi service is unavailable");
                    },
                    NO_LOW_LATENCY_MODE);
        }

        int lowLatencyMode = Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
                ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                : NO_LOW_LATENCY_MODE;
        return new StreamWifiLockController(
                (mode, tag) -> new AndroidLockHandle(
                        wifiManager.createWifiLock(mode, tag)),
                lowLatencyMode);
    }

    StreamWifiLockController(
            LockFactory lockFactory,
            int lowLatencyMode) {
        this.lockFactory = Objects.requireNonNull(
                lockFactory,
                "lockFactory");
        this.lowLatencyMode = lowLatencyMode;
    }

    /**
     * Attempts each supported lock independently and at most once.
     *
     * @return {@code true} if at least one performance lock is held
     */
    @MainThread
    public boolean acquire() {
        if (destroyed || acquisitionAttempted) {
            return isAnyLockHeld();
        }
        acquisitionAttempted = true;

        highPerformanceLock = acquireLock(
                legacyHighPerformanceMode(),
                HIGH_PERFORMANCE_TAG);
        if (lowLatencyMode != NO_LOW_LATENCY_MODE) {
            lowLatencyLock = acquireLock(
                    lowLatencyMode,
                    LOW_LATENCY_TAG);
        }
        return isAnyLockHeld();
    }

    @SuppressWarnings("deprecation")
    private static int legacyHighPerformanceMode() {
        // LOW_LATENCY is not available before Android 10. Keep the old mode
        // isolated here for supported API 21-28 devices and as an independent
        // fallback on vendor implementations where the newer lock fails.
        return WifiManager.WIFI_MODE_FULL_HIGH_PERF;
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;

        releaseLock(lowLatencyLock, LOW_LATENCY_TAG);
        lowLatencyLock = null;
        releaseLock(highPerformanceLock, HIGH_PERFORMANCE_TAG);
        highPerformanceLock = null;
    }

    private LockHandle acquireLock(int mode, String tag) {
        try {
            LockHandle lock = lockFactory.create(mode, tag);
            lock.setReferenceCounted(false);
            lock.acquire();
            return lock;
        } catch (RuntimeException error) {
            LimeLog.warning(
                    "Unable to acquire " + tag + ": " + error);
            return null;
        }
    }

    private void releaseLock(LockHandle lock, String tag) {
        if (lock == null) {
            return;
        }
        try {
            if (lock.isHeld()) {
                lock.release();
            }
        } catch (RuntimeException error) {
            LimeLog.warning(
                    "Unable to release " + tag + ": " + error);
        }
    }

    private boolean isAnyLockHeld() {
        return isHeld(highPerformanceLock) ||
                isHeld(lowLatencyLock);
    }

    private static boolean isHeld(LockHandle lock) {
        if (lock == null) {
            return false;
        }
        try {
            return lock.isHeld();
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static final class AndroidLockHandle
            implements LockHandle {
        private final WifiManager.WifiLock wifiLock;

        private AndroidLockHandle(
                WifiManager.WifiLock wifiLock) {
            this.wifiLock = Objects.requireNonNull(
                    wifiLock,
                    "wifiLock");
        }

        @Override
        public void setReferenceCounted(boolean referenceCounted) {
            wifiLock.setReferenceCounted(referenceCounted);
        }

        @Override
        public void acquire() {
            wifiLock.acquire();
        }

        @Override
        public boolean isHeld() {
            return wifiLock.isHeld();
        }

        @Override
        public void release() {
            wifiLock.release();
        }
    }
}
