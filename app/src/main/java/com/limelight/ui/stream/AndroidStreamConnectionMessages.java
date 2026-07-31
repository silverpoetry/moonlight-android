package com.limelight.ui.stream;

import android.content.Context;

import com.limelight.R;
import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/** Resolves Android resources for {@link StreamConnectionMessages}. */
public final class AndroidStreamConnectionMessages {
    private AndroidStreamConnectionMessages() {
    }

    public static StreamConnectionMessages create(Context context) {
        Objects.requireNonNull(context, "context");
        String connectionStarting = context.getString(
                R.string.conn_starting);
        String connectionError = context.getString(R.string.conn_error_msg);
        String connectionErrorTitle = context.getString(
                R.string.conn_error_title);
        String checkPorts = context.getString(R.string.check_ports_msg);
        String blockedNetwork = context.getString(
                R.string.nettest_text_blocked);
        String connectionTerminatedTitle = context.getString(
                R.string.conn_terminated_title);
        String noVideoReceived = context.getString(
                R.string.no_video_received_error);
        String noFrameReceived = context.getString(
                R.string.no_frame_received_error);
        String earlyTermination = context.getString(
                R.string.early_termination_error);
        String frameConversion = context.getString(
                R.string.frame_conversion_error);
        String connectionTerminated = context.getString(
                R.string.conn_terminated_msg);
        String errorCodePrefix = context.getString(
                R.string.error_code_prefix);
        String videoDecoderInitializationFailed = context.getString(
                R.string.video_decoder_init_failed);
        return new StreamConnectionMessages(
                new StreamConnectionMessages.Labels() {
                    @Override
                    public String connectionStarting() {
                        return connectionStarting;
                    }

                    @Override
                    public String connectionError() {
                        return connectionError;
                    }

                    @Override
                    public String connectionErrorTitle() {
                        return connectionErrorTitle;
                    }

                    @Override
                    public String checkPorts() {
                        return checkPorts;
                    }

                    @Override
                    public String blockedNetwork() {
                        return blockedNetwork;
                    }

                    @Override
                    public String connectionTerminatedTitle() {
                        return connectionTerminatedTitle;
                    }

                    @Override
                    public String noVideoReceived() {
                        return noVideoReceived;
                    }

                    @Override
                    public String noFrameReceived() {
                        return noFrameReceived;
                    }

                    @Override
                    public String earlyTermination() {
                        return earlyTermination;
                    }

                    @Override
                    public String frameConversion() {
                        return frameConversion;
                    }

                    @Override
                    public String connectionTerminated() {
                        return connectionTerminated;
                    }

                    @Override
                    public String errorCodePrefix() {
                        return errorCodePrefix;
                    }

                    @Override
                    public String videoDecoderInitializationFailed() {
                        return videoDecoderInitializationFailed;
                    }
                },
                portFlags -> MoonBridge.stringifyPortFlags(
                        portFlags,
                        "\n"),
                MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
    }
}
