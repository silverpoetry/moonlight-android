package com.limelight.nvstream.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Test;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

public final class NvHttpTlsStateTest {
    private final X509KeyManager keyManager;
    private final X509TrustManager trustManager;

    public NvHttpTlsStateTest() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        KeyManagerFactory keys = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keys.init(keyStore, new char[0]);
        keyManager = (X509KeyManager) keys.getKeyManagers()[0];
        TrustManagerFactory trust = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trust.init((KeyStore) null);
        trustManager = (X509TrustManager) trust.getTrustManagers()[0];
    }

    @Test
    public void requestsAndTimeoutVariantsShareOneTlsSessionContext() {
        NvHttpTlsState state = new NvHttpTlsState();
        state.setSessionReuseEnabled("1");
        OkHttpClient original = new OkHttpClient();
        OkHttpClient first = state.apply(original, keyManager, trustManager);
        OkHttpClient second = state.apply(original.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS).build(), keyManager, trustManager);

        assertSame(first.sslSocketFactory(), second.sslSocketFactory());
        assertEquals(3000, second.connectTimeoutMillis());
        assertSame(original.connectionPool(), second.connectionPool());
    }

    @Test
    public void certificateChangesAndSeparateHostsCannotShareSessionCaches() {
        NvHttpTlsState state = new NvHttpTlsState();
        state.setSessionReuseEnabled("1");
        OkHttpClient original = new OkHttpClient();
        OkHttpClient first = state.apply(original, keyManager, trustManager);
        state.invalidate();
        OkHttpClient second = state.apply(original, keyManager, trustManager);
        OkHttpClient otherHost = new NvHttpTlsState().apply(
                original, keyManager, trustManager);

        assertNotSame(first.sslSocketFactory(), second.sslSocketFactory());
        assertNotSame(second.sslSocketFactory(), otherHost.sslSocketFactory());
    }

    @Test
    public void legacyHostsUseFreshContextsUntilAuthenticatedOptIn() {
        NvHttpTlsState state = new NvHttpTlsState();
        OkHttpClient original = new OkHttpClient();
        OkHttpClient first = state.apply(original, keyManager, trustManager);
        OkHttpClient second = state.apply(original, keyManager, trustManager);
        assertNotSame(first.sslSocketFactory(), second.sslSocketFactory());
        state.setSessionReuseEnabled("1");
        assertSame(second.sslSocketFactory(),
                state.apply(original, keyManager, trustManager).sslSocketFactory());
        for (String unsupported : new String[] {null, "", "0", "2", "1x"}) {
            state.setSessionReuseEnabled(unsupported);
            OkHttpClient previous = state.apply(original, keyManager, trustManager);
            assertNotSame(previous.sslSocketFactory(),
                    state.apply(original, keyManager, trustManager).sslSocketFactory());
        }
    }

    @Test
    public void cachedContextRetainsOkHttpProtocolRestrictions() {
        OkHttpClient original = new OkHttpClient();
        OkHttpClient client = new NvHttpTlsState().apply(
                original, keyManager, trustManager);
        assertEquals(original.connectionSpecs(), client.connectionSpecs());
        for (ConnectionSpec spec : client.connectionSpecs()) {
            if (spec.isTls()) {
                assertFalse(spec.tlsVersions().contains(TlsVersion.SSL_3_0));
            }
        }
    }
}
