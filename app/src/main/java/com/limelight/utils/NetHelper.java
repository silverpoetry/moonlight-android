package com.limelight.utils;

import android.content.Context;
import com.limelight.platform.AndroidNetworkTransport;

public class NetHelper {
    public static boolean isActiveNetworkVpn(Context context) {
        return AndroidNetworkTransport.getActive(context) ==
                AndroidNetworkTransport.VPN;
    }
}
