package com.limelight.computers.reachability;

/** Canonical public endpoint used by Moonlight connectivity diagnostics. */
public final class ClientConnectivityEndpoint {
    public static final String HOST =
            "android.conntest.moonlight-stream.org";
    public static final int HTTPS_PORT = 443;

    private ClientConnectivityEndpoint() {
    }
}
