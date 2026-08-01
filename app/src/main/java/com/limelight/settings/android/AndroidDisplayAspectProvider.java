package com.limelight.settings.android;

import android.content.Context;
import android.graphics.Point;

import com.limelight.platform.AndroidDisplayCompat;

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
        Point size = AndroidDisplayCompat.getPhysicalDisplaySize(
                context);
        return new StreamResolutionCodec.DisplayAspect(
                size.x,
                size.y);
    }
}
