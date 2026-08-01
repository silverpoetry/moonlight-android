package com.limelight.stream.launch.android;

import android.content.Intent;

import com.limelight.nvstream.StreamConfiguration;
import com.limelight.stream.launch.PendingStreamReconnect;

/** Maps a live Game launch Intent to the process handoff model. */
public final class AndroidPendingStreamReconnectMapper {
    private AndroidPendingStreamReconnectMapper() {
    }

    public static PendingStreamReconnect fromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        String hostId = intent.getStringExtra(
                AndroidStreamLaunchContract.EXTRA_HOST_ID);
        if (hostId == null || hostId.trim().isEmpty()) {
            return null;
        }
        return new PendingStreamReconnect(
                hostId,
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_NAME),
                intent.getIntExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_ID,
                        StreamConfiguration.INVALID_APP_ID),
                intent.getBooleanExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_HDR,
                        false));
    }
}
