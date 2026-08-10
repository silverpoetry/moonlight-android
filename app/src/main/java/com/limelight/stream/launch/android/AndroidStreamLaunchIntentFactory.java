package com.limelight.stream.launch.android;

import android.content.Context;
import android.content.Intent;

import com.limelight.Game;
import com.limelight.LandscapeGameActivity;
import com.limelight.PortraitGameActivity;
import com.limelight.stream.launch.StreamLaunchRequest;

import java.util.Objects;

/** The only adapter that selects a concrete stream Activity entry. */
public final class AndroidStreamLaunchIntentFactory {
    private AndroidStreamLaunchIntentFactory() {
    }

    public static Intent create(
            Context context,
            StreamLaunchRequest request,
            String sessionToken,
            StreamInitialOrientation initialOrientation) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(
                initialOrientation,
                "initialOrientation");
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "sessionToken is required");
        }
        Intent intent = new Intent(
                context,
                resolveDestinationActivity(initialOrientation));
        // The launcher remains visible until the prepared session reports
        // ready. The stream Activity is an opaque, transition-free target.
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

    private static Class<? extends Game> resolveDestinationActivity(
            StreamInitialOrientation initialOrientation) {
        switch (initialOrientation) {
            case LANDSCAPE:
                return LandscapeGameActivity.class;
            case PORTRAIT:
                return PortraitGameActivity.class;
            default:
                throw new IllegalStateException(
                        "Unhandled initial orientation: " +
                                initialOrientation);
        }
    }
}
