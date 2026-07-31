package com.limelight.ui.stream;

import android.app.Activity;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.utils.UiHelper;

import java.util.Objects;

/** Applies negotiated HDR mode to the decoder and Android window. */
public final class AndroidStreamHdrModeController {
    public interface BooleanValue {
        boolean get();
    }

    private final Activity activity;
    private final StreamMediaResourceOwner mediaResourceOwner;
    private final BooleanValue highBrightnessEnabled;

    public AndroidStreamHdrModeController(
            Activity activity,
            StreamMediaResourceOwner mediaResourceOwner,
            BooleanValue highBrightnessEnabled) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.mediaResourceOwner = Objects.requireNonNull(
                mediaResourceOwner,
                "mediaResourceOwner");
        this.highBrightnessEnabled = Objects.requireNonNull(
                highBrightnessEnabled,
                "highBrightnessEnabled");
    }

    @MainThread
    public void onHdrModeChanged(
            boolean enabled,
            byte[] hdrMetadata) {
        LimeLog.info(
                "Display HDR mode: " +
                        (enabled ? "enabled" : "disabled"));
        mediaResourceOwner.setHdrMode(enabled, hdrMetadata);
        UiHelper.notifyHdrWindowStatus(
                activity,
                enabled,
                highBrightnessEnabled.get());
    }

    @MainThread
    public void clearWindowState() {
        UiHelper.notifyHdrWindowStatus(
                activity,
                false,
                highBrightnessEnabled.get());
    }
}
