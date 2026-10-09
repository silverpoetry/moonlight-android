package com.limelight.nvstream.http;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Owns mutual-TLS sessions for one host transport and one pinned certificate.
 *
 * <p>Session resumption is enabled only by an authenticated host capability.
 * Older hosts require a fresh SSLContext for each request. Certificate changes
 * invalidate both the capability and cached sessions before further use.</p>
 */
final class NvHttpTlsState {
    private SSLSocketFactory socketFactory;
    private boolean sessionReuseEnabled;

    synchronized OkHttpClient apply(
            OkHttpClient client,
            X509KeyManager keyManager,
            X509TrustManager trustManager) {
        if (socketFactory == null || !sessionReuseEnabled) {
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(new KeyManager[] {keyManager},
                        new TrustManager[] {trustManager}, new SecureRandom());
                socketFactory = context.getSocketFactory();
            } catch (GeneralSecurityException error) {
                throw new IllegalStateException("Unable to initialize host TLS", error);
            }
        }
        return client.newBuilder()
                .sslSocketFactory(socketFactory, trustManager)
                .build();
    }

    synchronized void invalidate() {
        socketFactory = null;
        sessionReuseEnabled = false;
    }

    synchronized void setSessionReuseEnabled(String advertisedVersion) {
        sessionReuseEnabled = "1".equals(advertisedVersion);
    }
}
