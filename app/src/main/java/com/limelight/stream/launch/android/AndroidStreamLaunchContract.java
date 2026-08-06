package com.limelight.stream.launch.android;

/**
 * Stable Android Intent contract for launching a stream.
 *
 * <p>The contract belongs to the launch boundary rather than the destination
 * Activity. Producers and consumers can therefore agree on the serialized
 * representation without depending on UI implementation details.</p>
 */
public final class AndroidStreamLaunchContract {
    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_PORT = "Port";
    public static final String EXTRA_HTTPS_PORT = "HttpsPort";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_ID = "AppId";
    public static final String EXTRA_UNIQUE_ID = "UniqueId";
    public static final String EXTRA_HOST_ID = "UUID";
    public static final String EXTRA_HOST_NAME = "PcName";
    public static final String EXTRA_APP_HDR = "HDR";
    public static final String EXTRA_SERVER_CERTIFICATE = "ServerCert";
    public static final String EXTRA_SESSION_TOKEN = "StreamSessionToken";

    private AndroidStreamLaunchContract() {
    }
}
