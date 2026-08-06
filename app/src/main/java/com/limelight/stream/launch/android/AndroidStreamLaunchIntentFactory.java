package com.limelight.stream.launch.android;

import android.content.Context;
import android.content.Intent;

import com.limelight.Game;
import com.limelight.stream.launch.StreamLaunchRequest;

import java.util.Objects;

/** The only adapter that targets the Game Activity with a launch document. */
public final class AndroidStreamLaunchIntentFactory {
    private AndroidStreamLaunchIntentFactory() {
    }

    public static Intent create(
            Context context,
            StreamLaunchRequest request,
            String sessionToken) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(request, "request");
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "sessionToken is required");
        }
        Intent intent = new Intent(context, Game.class);
        // The launcher remains visible until the prepared session reports
        // ready. Game is therefore an opaque, transition-free handoff target.
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_SESSION_TOKEN,
                sessionToken);
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_HOST,
                request.getHostAddress());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_PORT,
                request.getHostPort());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_HTTPS_PORT,
                request.getHttpsPort());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_APP_NAME,
                request.getAppName());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_APP_ID,
                request.getAppId());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_APP_HDR,
                request.supportsHdr());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_UNIQUE_ID,
                request.getClientId());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_HOST_ID,
                request.getHostId());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_HOST_NAME,
                request.getHostName());
        byte[] certificate = request.getServerCertificate();
        if (certificate != null) {
            intent.putExtra(
                    AndroidStreamLaunchContract
                            .EXTRA_SERVER_CERTIFICATE,
                    certificate);
        }
        return intent;
    }
}
