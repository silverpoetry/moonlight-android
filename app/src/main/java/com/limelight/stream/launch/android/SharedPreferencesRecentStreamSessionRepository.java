package com.limelight.stream.launch.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.stream.launch.RecentStreamSession;
import com.limelight.stream.launch.RecentStreamSessionRepository;

import java.util.Objects;

/** Android persistence adapter for the per-host recent stream identity. */
public final class SharedPreferencesRecentStreamSessionRepository
        implements RecentStreamSessionRepository {
    private static final String PREFERENCES_NAME = "RecentSessions";
    private static final String APP_ID_SUFFIX = ".appId";
    private static final String APP_NAME_SUFFIX = ".appName";
    private static final String APP_HDR_SUFFIX = ".appHdr";

    private final SharedPreferences preferences;

    public SharedPreferencesRecentStreamSessionRepository(
            Context context) {
        Objects.requireNonNull(context, "context");
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE);
    }

    @Override
    public RecentStreamSession find(String hostId) {
        if (!hasHostId(hostId)) {
            return null;
        }
        try {
            int appId = preferences.getInt(
                    key(hostId, APP_ID_SUFFIX),
                    0);
            if (appId <= 0) {
                return null;
            }
            return new RecentStreamSession(
                    preferences.getString(
                            key(hostId, APP_NAME_SUFFIX),
                            "app"),
                    appId,
                    preferences.getBoolean(
                            key(hostId, APP_HDR_SUFFIX),
                            false));
        }
        catch (ClassCastException corruptEntry) {
            return null;
        }
    }

    @Override
    public void save(
            String hostId,
            RecentStreamSession session) {
        if (!hasHostId(hostId)) {
            return;
        }
        Objects.requireNonNull(session, "session");
        preferences.edit()
                .putInt(
                        key(hostId, APP_ID_SUFFIX),
                        session.getAppId())
                .putString(
                        key(hostId, APP_NAME_SUFFIX),
                        session.getAppName())
                .putBoolean(
                        key(hostId, APP_HDR_SUFFIX),
                        session.supportsHdr())
                .apply();
    }

    private static String key(String hostId, String suffix) {
        return hostId + suffix;
    }

    private static boolean hasHostId(String hostId) {
        return hostId != null && !hostId.trim().isEmpty();
    }
}
