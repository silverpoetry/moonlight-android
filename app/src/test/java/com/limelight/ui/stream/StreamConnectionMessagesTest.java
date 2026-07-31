package com.limelight.ui.stream;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamConnectionMessagesTest {
    private static final int INCONCLUSIVE = -7;

    private final StreamConnectionMessages messages =
            new StreamConnectionMessages(
                    new TestLabels(),
                    flags -> "ports:" + flags,
                    INCONCLUSIVE);

    @Test
    public void stageMessagesPreserveFailureAndDiagnosticOrder() {
        assertEquals("starting video", messages.stageStarting("video"));
        assertEquals(
                "connection error video (error -3)\n\n" +
                        "check ports\nports:5\n\nblocked",
                messages.stageFailure("video", -3, 5, 1));
        assertEquals(
                "connection error audio (error 2)",
                messages.stageFailure("audio", 2, 0, INCONCLUSIVE));
    }

    @Test
    public void terminationReasonsMapEveryKnownProtocolError() {
        assertEquals(
                "no video",
                messages.terminationFailure(
                        MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC,
                        0,
                        0));
        assertEquals(
                "no frame",
                messages.terminationFailure(
                        MoonBridge.ML_ERROR_NO_VIDEO_FRAME,
                        0,
                        0));
        assertEquals(
                "early",
                messages.terminationFailure(
                        MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION,
                        0,
                        0));
        assertEquals(
                "early",
                messages.terminationFailure(
                        MoonBridge.ML_ERROR_PROTECTED_CONTENT,
                        0,
                        0));
        assertEquals(
                "conversion",
                messages.terminationFailure(
                        MoonBridge.ML_ERROR_FRAME_CONVERSION,
                        0,
                        0));
    }

    @Test
    public void unknownTerminationCodesRetainDecimalAndHexFormatting() {
        assertEquals(
                "terminated\n\ncode 999",
                messages.terminationFailure(999, 0, 0));
        assertEquals(
                "terminated\n\ncode 3e9",
                messages.terminationFailure(1001, 0, 0));
        assertEquals(
                "terminated\n\ncode -2147483648",
                messages.terminationFailure(Integer.MIN_VALUE, 0, 0));
    }

    @Test
    public void blockedProbeReplacesTerminationReasonButKeepsPorts() {
        assertEquals(
                "blocked\n\ncheck ports\nports:9",
                messages.terminationFailure(
                        MoonBridge.ML_ERROR_NO_VIDEO_FRAME,
                        9,
                        2));
        assertFalse(messages.isBlockedProbeResult(0));
        assertFalse(messages.isBlockedProbeResult(INCONCLUSIVE));
        assertTrue(messages.isBlockedProbeResult(2));
    }

    private static final class TestLabels
            implements StreamConnectionMessages.Labels {
        @Override
        public String connectionStarting() {
            return "starting";
        }

        @Override
        public String connectionError() {
            return "connection error";
        }

        @Override
        public String connectionErrorTitle() {
            return "connection error title";
        }

        @Override
        public String checkPorts() {
            return "check ports";
        }

        @Override
        public String blockedNetwork() {
            return "blocked";
        }

        @Override
        public String connectionTerminatedTitle() {
            return "termination title";
        }

        @Override
        public String noVideoReceived() {
            return "no video";
        }

        @Override
        public String noFrameReceived() {
            return "no frame";
        }

        @Override
        public String earlyTermination() {
            return "early";
        }

        @Override
        public String frameConversion() {
            return "conversion";
        }

        @Override
        public String connectionTerminated() {
            return "terminated";
        }

        @Override
        public String errorCodePrefix() {
            return "code";
        }

        @Override
        public String videoDecoderInitializationFailed() {
            return "decoder";
        }
    }
}
