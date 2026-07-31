package com.limelight.ui.stream;

import java.util.Objects;

/** Resolves an HDR stream request from settings and sampled device facts. */
public final class StreamHdrRequestPolicy {
    public enum Warning {
        NONE,
        ANDROID_VERSION_UNSUPPORTED,
        DISPLAY_HDR10_UNSUPPORTED
    }

    /** Immutable Android capabilities sampled before media startup. */
    public static final class DeviceCapabilities {
        private final boolean hdrCapabilityQuerySupported;
        private final boolean hdrStreamingAllowed;
        private final boolean displaySupportsHdr10;

        public DeviceCapabilities(
                boolean hdrCapabilityQuerySupported,
                boolean hdrStreamingAllowed,
                boolean displaySupportsHdr10) {
            if (!hdrCapabilityQuerySupported && displaySupportsHdr10) {
                throw new IllegalArgumentException(
                        "HDR10 support requires platform capability queries");
            }
            this.hdrCapabilityQuerySupported =
                    hdrCapabilityQuerySupported;
            this.hdrStreamingAllowed = hdrStreamingAllowed;
            this.displaySupportsHdr10 = displaySupportsHdr10;
        }

        public boolean isHdrCapabilityQuerySupported() {
            return hdrCapabilityQuerySupported;
        }

        public boolean isHdrStreamingAllowed() {
            return hdrStreamingAllowed;
        }

        public boolean doesDisplaySupportHdr10() {
            return displaySupportsHdr10;
        }
    }

    /** Immutable decision consumed by media and transport composition. */
    public static final class Decision {
        private final boolean hdrRequested;
        private final Warning warning;

        private Decision(boolean hdrRequested, Warning warning) {
            this.hdrRequested = hdrRequested;
            this.warning = Objects.requireNonNull(warning, "warning");
        }

        public boolean isHdrRequested() {
            return hdrRequested;
        }

        public Warning getWarning() {
            return warning;
        }
    }

    private StreamHdrRequestPolicy() {
    }

    public static Decision decide(
            boolean hdrEnabled,
            boolean ignoreHdrCapability,
            DeviceCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");

        if (ignoreHdrCapability) {
            return new Decision(true, Warning.NONE);
        }
        if (!hdrEnabled || !capabilities.isHdrStreamingAllowed()) {
            return new Decision(false, Warning.NONE);
        }
        if (!capabilities.isHdrCapabilityQuerySupported()) {
            return new Decision(
                    false,
                    Warning.ANDROID_VERSION_UNSUPPORTED);
        }
        if (!capabilities.doesDisplaySupportHdr10()) {
            return new Decision(
                    false,
                    Warning.DISPLAY_HDR10_UNSUPPORTED);
        }
        return new Decision(true, Warning.NONE);
    }
}
