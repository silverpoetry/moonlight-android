package com.limelight.ui.stream;

import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/**
 * Builds all user-facing connection failure messages from immutable labels.
 *
 * <p>The Android resource adapter resolves localized labels once. This class
 * then owns the exact message composition and protocol error mapping without
 * depending on an Activity, Resources, or a View.</p>
 */
public final class StreamConnectionMessages {
    public interface PortFormatter {
        String format(int portFlags);
    }

    public interface Labels {
        String connectionStarting();

        String connectionError();

        String connectionErrorTitle();

        String checkPorts();

        String blockedNetwork();

        String connectionTerminatedTitle();

        String noVideoReceived();

        String noFrameReceived();

        String earlyTermination();

        String frameConversion();

        String connectionTerminated();

        String errorCodePrefix();

        String videoDecoderInitializationFailed();
    }

    private final Labels labels;
    private final PortFormatter portFormatter;
    private final int inconclusiveProbeResult;

    public StreamConnectionMessages(
            Labels labels,
            PortFormatter portFormatter,
            int inconclusiveProbeResult) {
        this.labels = Objects.requireNonNull(labels, "labels");
        this.portFormatter = Objects.requireNonNull(
                portFormatter,
                "portFormatter");
        this.inconclusiveProbeResult = inconclusiveProbeResult;
    }

    public String stageStarting(String stage) {
        return labels.connectionStarting() + " " + stage;
    }

    public String stageFailure(
            String stage,
            int errorCode,
            int portFlags,
            int probeResult) {
        String message = labels.connectionError() + " " + stage +
                " (error " + errorCode + ")";
        message = appendPorts(message, portFlags);
        if (isBlockedProbeResult(probeResult)) {
            message += "\n\n" + labels.blockedNetwork();
        }
        return message;
    }

    public String terminationFailure(
            int errorCode,
            int portFlags,
            int probeResult) {
        String message = isBlockedProbeResult(probeResult)
                ? labels.blockedNetwork()
                : terminationReason(errorCode);
        return appendPorts(message, portFlags);
    }

    public String connectionErrorTitle() {
        return labels.connectionErrorTitle();
    }

    public String connectionTerminatedTitle() {
        return labels.connectionTerminatedTitle();
    }

    public String videoDecoderInitializationFailed() {
        return labels.videoDecoderInitializationFailed();
    }

    public boolean isBlockedProbeResult(int probeResult) {
        return probeResult != inconclusiveProbeResult &&
                probeResult != 0;
    }

    private String terminationReason(int errorCode) {
        switch (errorCode) {
            case MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC:
                return labels.noVideoReceived();

            case MoonBridge.ML_ERROR_NO_VIDEO_FRAME:
                return labels.noFrameReceived();

            case MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION:
            case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
                return labels.earlyTermination();

            case MoonBridge.ML_ERROR_FRAME_CONVERSION:
                return labels.frameConversion();

            default:
                String errorCodeText = Math.abs(errorCode) > 1000
                        ? Integer.toHexString(errorCode)
                        : Integer.toString(errorCode);
                return labels.connectionTerminated() + "\n\n" +
                        labels.errorCodePrefix() + " " + errorCodeText;
        }
    }

    private String appendPorts(String message, int portFlags) {
        if (portFlags == 0) {
            return message;
        }
        return message + "\n\n" + labels.checkPorts() + "\n" +
                portFormatter.format(portFlags);
    }
}
