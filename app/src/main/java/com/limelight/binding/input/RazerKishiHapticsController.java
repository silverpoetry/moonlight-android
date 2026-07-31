package com.limelight.binding.input;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;

import com.limelight.DebugLog;
import com.limelight.binding.input.driver.RazerKishiHapticsDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Owns discovery, refresh throttling, frame submission, and teardown for the
 * optional Razer Kishi USB audio-haptics sidecars.
 */
final class RazerKishiHapticsController {
    interface AudioPolicy {
        boolean shouldUseControllerAudioHaptics();
    }

    interface Clock {
        long uptimeMillis();
    }

    interface Backend {
        boolean isFeatureEnabled();

        List<Candidate> getCandidates();
    }

    interface Candidate {
        int getDeviceId();

        String getDescription();

        boolean hasPermission();

        Sidecar open();
    }

    interface Sidecar {
        boolean isStarted();

        boolean start();

        boolean submitFrame(byte[] frame, float intensityGain);

        void stop();
    }

    interface Logger {
        boolean isEnabled();

        void debug(String message);

        void info(String message);

        void warning(String message);

        void error(String message);
    }

    private static final String LOG_TAG = "RazerKishiDebug";
    private static final long REFRESH_INTERVAL_MS = 1_500;

    private final Backend backend;
    private final AudioPolicy audioPolicy;
    private final Clock clock;
    private final Logger logger;
    private final Map<Integer, Sidecar> sidecars = new HashMap<>();

    private long lastRefreshTimeMs;
    private boolean hasRefreshed;
    private boolean destroyed;

    static RazerKishiHapticsController create(
            UsbManager usbManager,
            AudioPolicy audioPolicy) {
        return new RazerKishiHapticsController(
                new AndroidBackend(usbManager),
                audioPolicy,
                SystemClock::uptimeMillis,
                new DebugLogger());
    }

    RazerKishiHapticsController(
            Backend backend,
            AudioPolicy audioPolicy,
            Clock clock) {
        this(
                backend,
                audioPolicy,
                clock,
                new NoOpLogger());
    }

    RazerKishiHapticsController(
            Backend backend,
            AudioPolicy audioPolicy,
            Clock clock,
            Logger logger) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.audioPolicy = Objects.requireNonNull(
                audioPolicy,
                "audioPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    boolean isFeatureEnabled() {
        return backend.isFeatureEnabled();
    }

    boolean canUseDevice(
            int vendorId,
            int productId,
            String name) {
        return RazerKishiHapticsDevice.canUseDevice(
                vendorId,
                productId,
                name);
    }

    synchronized void refresh() {
        if (destroyed) {
            return;
        }
        if (!backend.isFeatureEnabled()) {
            stopAll();
            return;
        }

        lastRefreshTimeMs = clock.uptimeMillis();
        hasRefreshed = true;
        boolean enabled =
                audioPolicy.shouldUseControllerAudioHaptics();
        Set<Integer> activeDeviceIds = new HashSet<>();

        if (enabled) {
            for (Candidate candidate : backend.getCandidates()) {
                int deviceId = candidate.getDeviceId();
                activeDeviceIds.add(deviceId);
                logger.debug(
                        "Matched Kishi haptics candidate: " +
                                candidate.getDescription() +
                                " permission=" +
                                candidate.hasPermission());

                Sidecar existing = sidecars.get(deviceId);
                if (existing != null && existing.isStarted()) {
                    continue;
                }
                if (existing != null) {
                    existing.stop();
                    sidecars.remove(deviceId);
                }
                if (!candidate.hasPermission()) {
                    logger.warning(
                            "Kishi device missing USB permission: " +
                                    "deviceId=" + deviceId);
                    continue;
                }

                Sidecar sidecar = candidate.open();
                if (sidecar == null) {
                    logger.error(
                            "Unable to open Kishi deviceId=" + deviceId);
                    continue;
                }
                if (sidecar.start()) {
                    sidecars.put(deviceId, sidecar);
                    logger.info(
                            "Kishi haptics sidecar started for deviceId=" +
                                    deviceId);
                } else {
                    sidecar.stop();
                    logger.error(
                            "Kishi haptics sidecar failed to start for " +
                                    "deviceId=" + deviceId);
                }
            }
        }

        List<Integer> staleDeviceIds = new ArrayList<>();
        for (Integer deviceId : sidecars.keySet()) {
            if (!enabled || !activeDeviceIds.contains(deviceId)) {
                staleDeviceIds.add(deviceId);
            }
        }
        for (Integer deviceId : staleDeviceIds) {
            logger.info(
                    "Removing Kishi haptics sidecar for deviceId=" +
                            deviceId);
            Sidecar sidecar = sidecars.remove(deviceId);
            if (sidecar != null) {
                sidecar.stop();
            }
        }
    }

    synchronized boolean submitFrame(
            byte[] frame,
            float intensityGain) {
        if (destroyed || !backend.isFeatureEnabled() ||
                !audioPolicy.shouldUseControllerAudioHaptics() ||
                frame == null || frame.length == 0) {
            return false;
        }

        if (sidecars.isEmpty()) {
            refreshIfDue();
        }

        boolean submitted = false;
        for (Sidecar sidecar : sidecars.values()) {
            submitted |= sidecar.submitFrame(frame, intensityGain);
        }
        if (!submitted && logger.isEnabled()) {
            logger.warning(
                    "Kishi audio haptics frame was not submitted to " +
                            "any sidecar");
        }
        return submitted;
    }

    synchronized void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        stopAll();
    }

    private void refreshIfDue() {
        long now = clock.uptimeMillis();
        if (hasRefreshed &&
                now - lastRefreshTimeMs < REFRESH_INTERVAL_MS) {
            return;
        }
        refresh();
    }

    private void stopAll() {
        for (Sidecar sidecar : sidecars.values()) {
            sidecar.stop();
        }
        sidecars.clear();
    }

    private static final class AndroidBackend implements Backend {
        private final UsbManager usbManager;

        private AndroidBackend(UsbManager usbManager) {
            this.usbManager = usbManager;
        }

        @Override
        public boolean isFeatureEnabled() {
            return RazerKishiHapticsDevice.isFeatureEnabled();
        }

        @Override
        public List<Candidate> getCandidates() {
            List<Candidate> candidates = new ArrayList<>();
            if (usbManager == null) {
                return candidates;
            }
            for (UsbDevice usbDevice :
                    usbManager.getDeviceList().values()) {
                if (RazerKishiHapticsDevice.canUseDevice(usbDevice)) {
                    candidates.add(
                            new AndroidCandidate(
                                    usbManager,
                                    usbDevice));
                }
            }
            return candidates;
        }
    }

    private static final class AndroidCandidate implements Candidate {
        private final UsbManager usbManager;
        private final UsbDevice usbDevice;

        private AndroidCandidate(
                UsbManager usbManager,
                UsbDevice usbDevice) {
            this.usbManager = usbManager;
            this.usbDevice = usbDevice;
        }

        @Override
        public int getDeviceId() {
            return usbDevice.getDeviceId();
        }

        @Override
        public String getDescription() {
            return "vid=0x" +
                    Integer.toHexString(usbDevice.getVendorId()) +
                    " pid=0x" +
                    Integer.toHexString(usbDevice.getProductId()) +
                    " name=" + usbDevice.getProductName();
        }

        @Override
        public boolean hasPermission() {
            return usbManager.hasPermission(usbDevice);
        }

        @Override
        public Sidecar open() {
            UsbDeviceConnection connection =
                    usbManager.openDevice(usbDevice);
            return connection == null
                    ? null
                    : new AndroidSidecar(
                            new RazerKishiHapticsDevice(
                                    usbDevice,
                                    connection));
        }
    }

    private static final class AndroidSidecar implements Sidecar {
        private final RazerKishiHapticsDevice device;

        private AndroidSidecar(RazerKishiHapticsDevice device) {
            this.device = device;
        }

        @Override
        public boolean isStarted() {
            return device.isStarted();
        }

        @Override
        public boolean start() {
            return device.start();
        }

        @Override
        public boolean submitFrame(
                byte[] frame,
                float intensityGain) {
            return device.submitFrame(frame, intensityGain);
        }

        @Override
        public void stop() {
            device.stop();
        }
    }

    private static final class DebugLogger implements Logger {
        @Override
        public boolean isEnabled() {
            return DebugLog.isEnabled();
        }

        @Override
        public void debug(String message) {
            DebugLog.debug(LOG_TAG, message);
        }

        @Override
        public void info(String message) {
            DebugLog.info(LOG_TAG, message);
        }

        @Override
        public void warning(String message) {
            DebugLog.warning(LOG_TAG, message);
        }

        @Override
        public void error(String message) {
            DebugLog.error(LOG_TAG, message);
        }
    }

    private static final class NoOpLogger implements Logger {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void warning(String message) {
        }

        @Override
        public void error(String message) {
        }
    }
}
