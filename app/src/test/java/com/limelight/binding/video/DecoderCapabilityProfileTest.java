package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DecoderCapabilityProfileTest {
    @Test
    public void capabilitySnapshotPreservesDetectedFeatures() {
        DecoderCapabilityProfile profile =
                DecoderCapabilityProfile.create(
                        true,
                        true,
                        true,
                        true,
                        2,
                        4,
                        0);

        assertTrue(profile.isDirectSubmitEnabled());
        assertTrue(
                profile
                        .isAvcReferenceFrameInvalidationEnabled());
        assertTrue(
                profile
                        .isHevcReferenceFrameInvalidationEnabled());
        assertTrue(
                profile
                        .isAv1ReferenceFrameInvalidationEnabled());
        assertEquals(4, profile.getOptimalSlicesPerFrame());
    }

    @Test
    public void oddCrashCountDisablesLegacyRfiOnly() {
        DecoderCapabilityProfile profile =
                DecoderCapabilityProfile.create(
                        true,
                        true,
                        true,
                        true,
                        1,
                        1,
                        3);

        assertFalse(
                profile
                        .isAvcReferenceFrameInvalidationEnabled());
        assertFalse(
                profile
                        .isHevcReferenceFrameInvalidationEnabled());
        assertTrue(
                profile
                        .isAv1ReferenceFrameInvalidationEnabled());
        assertTrue(profile.isDirectSubmitEnabled());
    }

    @Test
    public void evenCrashCountRestoresDetectedRfi() {
        DecoderCapabilityProfile profile =
                DecoderCapabilityProfile.create(
                        false,
                        true,
                        true,
                        false,
                        0,
                        0,
                        2);

        assertTrue(
                profile
                        .isAvcReferenceFrameInvalidationEnabled());
        assertTrue(
                profile
                        .isHevcReferenceFrameInvalidationEnabled());
        assertFalse(
                profile
                        .isAv1ReferenceFrameInvalidationEnabled());
    }
}
