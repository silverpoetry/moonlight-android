package com.limelight.binding.video.gl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class GlDeviceSnapshotTest {
    private static final String FINGERPRINT =
            "vendor/device/product:version/build:user/release-keys";
    private static final String RENDERER = "Adreno (TM) 750";

    @Test
    public void availableSnapshotPreservesAtomicPair() {
        GlDeviceSnapshot snapshot = GlDeviceSnapshot.available(
                FINGERPRINT,
                RENDERER);

        assertTrue(snapshot.isAvailable());
        assertTrue(snapshot.isCurrentFor(FINGERPRINT));
        assertFalse(snapshot.isCurrentFor(FINGERPRINT + "-new"));
        assertEquals(FINGERPRINT, snapshot.getBuildFingerprint());
        assertEquals(RENDERER, snapshot.getRenderer());
        assertEquals(
                snapshot,
                GlDeviceSnapshot.fromUntrusted(
                        FINGERPRINT,
                        RENDERER));
    }

    @Test
    public void unavailableSnapshotUsesConservativeRenderer() {
        GlDeviceSnapshot snapshot = GlDeviceSnapshot.unavailable();

        assertFalse(snapshot.isAvailable());
        assertFalse(snapshot.isCurrentFor(""));
        assertEquals("", snapshot.getBuildFingerprint());
        assertEquals("", snapshot.getRenderer());
        assertSame(
                snapshot,
                GlDeviceSnapshot.fromUntrusted(null, RENDERER));
        assertSame(
                snapshot,
                GlDeviceSnapshot.fromUntrusted(FINGERPRINT, ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void availableRejectsMissingFingerprint() {
        GlDeviceSnapshot.available("", RENDERER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void availableRejectsOversizedRenderer() {
        GlDeviceSnapshot.available(
                FINGERPRINT,
                repeat('r', GlDeviceSnapshot.MAX_RENDERER_LENGTH + 1));
    }

    @Test
    public void untrustedOversizedValuesFailClosed() {
        assertFalse(GlDeviceSnapshot.fromUntrusted(
                repeat(
                        'f',
                        GlDeviceSnapshot
                                .MAX_BUILD_FINGERPRINT_LENGTH + 1),
                RENDERER).isAvailable());
        assertFalse(GlDeviceSnapshot.fromUntrusted(
                FINGERPRINT,
                repeat(
                        'r',
                        GlDeviceSnapshot.MAX_RENDERER_LENGTH + 1))
                .isAvailable());
    }

    private static String repeat(char value, int count) {
        char[] characters = new char[count];
        java.util.Arrays.fill(characters, value);
        return new String(characters);
    }
}
