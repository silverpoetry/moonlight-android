package com.limelight.binding;

import android.content.Context;

import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.http.LimelightCryptoProvider;

public class PlatformBinding {
    private static LimelightCryptoProvider cryptoProvider;

    public static LimelightCryptoProvider getCryptoProvider(Context c) {
        synchronized (PlatformBinding.class) {
            if (cryptoProvider == null) {
                cryptoProvider = new AndroidCryptoProvider(c.getApplicationContext());
            }
            return cryptoProvider;
        }
    }
}
