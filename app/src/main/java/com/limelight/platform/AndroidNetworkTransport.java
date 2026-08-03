package com.limelight.platform;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.util.Objects;

/** Snapshot of the active Android network transport. */
public enum AndroidNetworkTransport {
    NONE,
    CELLULAR,
    VPN,
    OTHER;

    public static AndroidNetworkTransport getActive(Context context) {
        Objects.requireNonNull(context, "context");
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return NONE;
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return NONE;
        }
        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return NONE;
        }
        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_VPN) ||
                !capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
            return VPN;
        }
        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return CELLULAR;
        }
        return OTHER;
    }
}
