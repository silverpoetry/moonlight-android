package com.limelight.nvstream.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import okhttp3.HttpUrl;

public final class NvHTTPVerboseLoggingTest {
    @Test
    public void pairingRequestLogOmitsEveryQueryParameter() {
        HttpUrl url = HttpUrl.get(
                "http://host:47989/pair?clientcert=secret&uuid=id");

        String logged = NvHTTP.formatRequestForVerboseLog(url, "pair");

        assertEquals(
                "http://host:47989/pair?<pairing parameters redacted>",
                logged);
        assertFalse(logged.contains("secret"));
        assertFalse(logged.contains("uuid"));
    }

    @Test
    public void pairingResponseLogOmitsCryptographicPayload() {
        String response =
                "<root><pairingsecret>secret</pairingsecret>" +
                        "<paired>1</paired></root>";

        String logged = NvHTTP.formatResponseForVerboseLog(
                "pair",
                response);

        assertTrue(logged.contains("redacted"));
        assertTrue(logged.contains(response.length() + " bytes"));
        assertFalse(logged.contains("secret"));
        assertFalse(logged.contains("paired"));
    }

    @Test
    public void nonPairingLogsRemainUnchanged() {
        HttpUrl url = HttpUrl.get(
                "https://host:47984/applist?uniqueid=id");
        String response = "<root><status_code>200</status_code></root>";

        assertEquals(
                url.toString(),
                NvHTTP.formatRequestForVerboseLog(url, "applist"));
        assertEquals(
                response,
                NvHTTP.formatResponseForVerboseLog(
                        "applist",
                        response));
    }
}
