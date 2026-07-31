package com.limelight.ui.stream;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamSystemUiVisibilityPolicyTest {
    @Test
    public void disconnectedSessionNeverRestoresImmersiveMode() {
        assertFalse(StreamSystemUiVisibilityPolicy
                .shouldRestoreImmersiveMode(false, false, false));
    }

    @Test
    public void connectedSessionRestoresWhenEitherSystemBarAppears() {
        assertTrue(StreamSystemUiVisibilityPolicy
                .shouldRestoreImmersiveMode(true, false, true));
        assertTrue(StreamSystemUiVisibilityPolicy
                .shouldRestoreImmersiveMode(true, true, false));
    }

    @Test
    public void fullyImmersiveConnectedSessionNeedsNoRestore() {
        assertFalse(StreamSystemUiVisibilityPolicy
                .shouldRestoreImmersiveMode(true, true, true));
    }
}
