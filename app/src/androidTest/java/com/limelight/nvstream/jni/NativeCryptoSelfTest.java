package com.limelight.nvstream.jni;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeCryptoSelfTest {
    @Test
    public void bundledCryptoPassesKnownAnswerTests() {
        assertTrue(MoonBridge.isNativeCryptoOperational());
    }
}
