package com.limelight.settings.android;

import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.limelight.settings.stream.StreamResolutionCodec;

import java.util.Objects;

/**
 * Android adapter that exposes the physical display aspect to the
 * platform-independent resolution codec.
 */
public final class AndroidDisplayAspectProvider {
    private AndroidDisplayAspectProvider() {
    }

    public static StreamResolutionCodec.DisplayAspect get(
            Context context) {
        Objects.requireNonNull(context, "context");
        int width = 16;
        int height = 9;
        WindowManager windowManager =
                (WindowManager) context.getSystemService(
                        Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            if (display != null) {
                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.M) {
                    Display.Mode mode = display.getMode();
                    width = mode.getPhysicalWidth();
                    height = mode.getPhysicalHeight();
                }
                else {
                    width = display.getWidth();
                    height = display.getHeight();
                }
            }
        }
        return new StreamResolutionCodec.DisplayAspect(
                width,
                height);
    }
}
