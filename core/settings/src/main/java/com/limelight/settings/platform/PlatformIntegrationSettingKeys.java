package com.limelight.settings.platform;

import com.limelight.settings.SettingKey;

/** Canonical settings for optional device-vendor integrations. */
public final class PlatformIntegrationSettingKeys {
    public static final SettingKey<Boolean>
            XIAOMI_REFRESH_RATE_LIMIT_SUPPRESSION =
            SettingKey.booleanKey(
                    "platform.xiaomi.refresh_rate_limit_suppression",
                    false);

    private PlatformIntegrationSettingKeys() {
    }
}
