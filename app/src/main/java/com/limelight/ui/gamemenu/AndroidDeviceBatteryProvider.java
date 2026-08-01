package com.limelight.ui.gamemenu;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

/** Android adapter for the current device battery snapshot. */
public final class AndroidDeviceBatteryProvider {
    private final Context applicationContext;

    public AndroidDeviceBatteryProvider(Context context) {
        Context checked = Objects.requireNonNull(context, "context");
        Context application = checked.getApplicationContext();
        applicationContext = application == null ? checked : application;
    }

    public int getBatteryPercent() {
        Intent battery = applicationContext.registerReceiver(
                null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return GameMenuState.UNKNOWN_BATTERY_PERCENT;
        }
        int level = battery.getIntExtra("level", -1);
        int scale = battery.getIntExtra("scale", -1);
        if (level < 0 || scale <= 0) {
            return GameMenuState.UNKNOWN_BATTERY_PERCENT;
        }
        return Math.max(0, Math.min(100, 100 * level / scale));
    }
}
