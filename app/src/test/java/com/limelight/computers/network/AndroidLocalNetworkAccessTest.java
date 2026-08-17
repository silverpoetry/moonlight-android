package com.limelight.computers.network;

import android.os.Build;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AndroidLocalNetworkAccessTest {
    @Test
    public void runtimePermissionStartsAtAndroid17() {
        assertFalse(AndroidLocalNetworkAccess
                .requiresRuntimePermission(Build.VERSION_CODES.BAKLAVA));
        assertTrue(AndroidLocalNetworkAccess
                .requiresRuntimePermission(
                        Build.VERSION_CODES.CINNAMON_BUN));
    }
}
