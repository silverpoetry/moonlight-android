package com.limelight.grid.assets;

import android.content.Context;

import com.limelight.LimeLog;
import com.limelight.computers.http.android.AndroidNvHttpClientFactory;
import com.limelight.nvstream.http.NvHTTP;

import java.io.IOException;
import java.io.InputStream;

public class NetworkAssetLoader {
    private final Context context;
    private final String uniqueId;

    public NetworkAssetLoader(Context context, String uniqueId) {
        this.context = context;
        this.uniqueId = uniqueId;
    }

    public InputStream getBitmapStream(CachedAppAssetLoader.LoaderTuple tuple) {
        InputStream in = null;
        try {
            NvHTTP http = AndroidNvHttpClientFactory.create(
                    context,
                    tuple.host,
                    uniqueId);
            in = http.getBoxArt(tuple.app);
        } catch (IOException ignored) {}

        if (in != null) {
            LimeLog.info("Network asset load complete: " + tuple);
        }
        else {
            LimeLog.info("Network asset load failed: " + tuple);
        }

        return in;
    }
}
