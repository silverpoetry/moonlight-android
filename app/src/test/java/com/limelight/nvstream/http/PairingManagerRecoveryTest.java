package com.limelight.nvstream.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public final class PairingManagerRecoveryTest {
    @Test
    public void authenticatedChallengeConfirmsAmbiguousCommit()
            throws Exception {
        PairingManager.confirmAmbiguousFinalExchange(
                new IOException("truncated final response"),
                () -> true);
    }

    @Test
    public void rejectedChallengeRethrowsOriginalTransportFailure() {
        IOException original = new IOException(
                "truncated final response");

        IOException thrown = assertThrows(
                IOException.class,
                () -> PairingManager.confirmAmbiguousFinalExchange(
                        original,
                        () -> false));

        assertSame(original, thrown);
    }

    @Test
    public void confirmationFailureIsSuppressedOnOriginalXmlFailure() {
        XmlPullParserException original =
                new XmlPullParserException("truncated XML");
        IOException confirmation = new IOException(
                "challenge unavailable");

        XmlPullParserException thrown = assertThrows(
                XmlPullParserException.class,
                () -> PairingManager.confirmAmbiguousFinalExchange(
                        original,
                        () -> {
                            throw confirmation;
                        }));

        assertSame(original, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(confirmation, thrown.getSuppressed()[0]);
    }

    @Test
    public void rejectsUnclassifiedFailures() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> PairingManager.confirmAmbiguousFinalExchange(
                        new IllegalStateException("programming error"),
                        () -> true));

        assertTrue(failure.getMessage().contains("Unsupported"));
    }
}
