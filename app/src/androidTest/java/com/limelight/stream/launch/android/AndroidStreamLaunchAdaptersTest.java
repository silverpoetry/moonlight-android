package com.limelight.stream.launch.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.Game;
import com.limelight.MoonlightApplication;
import com.limelight.ShortcutTrampoline;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.PendingStreamReconnect;
import com.limelight.stream.launch.RecentStreamSession;
import com.limelight.stream.launch.StreamLaunchRequest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AndroidStreamLaunchAdaptersTest {
    @Test
    public void launchIntentContainsTheImmutableRequestExactly() {
        Context context = targetContext();
        byte[] certificate = new byte[] {1, 2, 3};
        StreamLaunchRequest request = new StreamLaunchRequest(
                "host",
                47989,
                0,
                "Desktop",
                7,
                true,
                "client",
                "host-id",
                "host-name",
                certificate);

        Intent intent = AndroidStreamLaunchIntentFactory.create(
                context,
                request);

        assertEquals(
                Game.class.getName(),
                intent.getComponent().getClassName());
        assertEquals(
                "host",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_HOST));
        assertEquals(
                47989,
                intent.getIntExtra(
                        AndroidStreamLaunchContract.EXTRA_PORT,
                        0));
        assertEquals(
                0,
                intent.getIntExtra(
                        AndroidStreamLaunchContract.EXTRA_HTTPS_PORT,
                        -1));
        assertEquals(
                7,
                intent.getIntExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_ID,
                        0));
        assertEquals(
                "Desktop",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_NAME));
        assertTrue(intent.getBooleanExtra(
                AndroidStreamLaunchContract.EXTRA_APP_HDR,
                false));
        assertEquals(
                "client",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_UNIQUE_ID));
        assertEquals(
                "host-id",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_HOST_ID));
        assertEquals(
                "host-name",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_HOST_NAME));
        assertArrayEquals(
                certificate,
                intent.getByteArrayExtra(
                        AndroidStreamLaunchContract
                                .EXTRA_SERVER_CERTIFICATE));

        PendingStreamReconnect reconnect =
                AndroidPendingStreamReconnectMapper.fromIntent(intent);
        assertEquals("host-id", reconnect.getHostId());
        assertEquals("Desktop", reconnect.getAppName());
        assertEquals(7, reconnect.getAppId());
        assertTrue(reconnect.supportsHdr());
    }

    @Test
    public void shortcutIntentRetainsTheEstablishedStringAppIdContract() {
        Context context = targetContext();
        ComputerDetails computer = new ComputerDetails();
        computer.uuid = "host-id";
        computer.name = "host-name";
        NvApp app = new NvApp("Desktop", 7, true);

        Intent intent = AndroidShortcutIntentFactory.createAppIntent(
                context,
                computer,
                app);

        assertEquals(
                ShortcutTrampoline.class.getName(),
                intent.getComponent().getClassName());
        assertEquals(
                "7",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_ID));
        assertEquals(
                "Desktop",
                intent.getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_APP_NAME));
    }

    @Test
    public void recentSessionAdapterPreservesLegacyStorageContract() {
        Context context = targetContext();
        String hostId = "launch-adapter-test-host";
        String appIdKey = hostId + ".appId";
        String appNameKey = hostId + ".appName";
        String appHdrKey = hostId + ".appHdr";
        SharedPreferences preferences = context.getSharedPreferences(
                "RecentSessions",
                Context.MODE_PRIVATE);
        SharedPreferencesRecentStreamSessionRepository repository =
                new SharedPreferencesRecentStreamSessionRepository(
                        context);
        try {
            repository.save(
                    hostId,
                    new RecentStreamSession(
                            "Desktop",
                            7,
                            true));

            assertEquals(7, preferences.getInt(appIdKey, 0));
            assertEquals(
                    "Desktop",
                    preferences.getString(appNameKey, null));
            assertTrue(preferences.getBoolean(appHdrKey, false));
            assertEquals(
                    new RecentStreamSession("Desktop", 7, true),
                    repository.find(hostId));

            preferences.edit()
                    .putString(appIdKey, "corrupt")
                    .commit();
            assertNull(repository.find(hostId));
        }
        finally {
            preferences.edit()
                    .remove(appIdKey)
                    .remove(appNameKey)
                    .remove(appHdrKey)
                    .commit();
        }
    }

    @Test
    public void manifestInstallsTheNarrowProcessCompositionRoot() {
        assertSame(
                MoonlightApplication.class,
                targetContext()
                        .getApplicationContext()
                        .getClass());
    }

    private static Context targetContext() {
        return InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
    }
}
