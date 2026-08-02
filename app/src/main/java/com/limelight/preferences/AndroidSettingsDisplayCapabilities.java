package com.limelight.preferences;

import android.app.Activity;
import android.graphics.Point;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Range;
import android.view.Display;
import android.view.DisplayCutout;

import com.limelight.LimeLog;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.binding.video.gl.GlDeviceSnapshot;
import com.limelight.settings.android.AndroidHdrCompatibility;
import com.limelight.platform.AndroidDeviceCategory;
import com.limelight.platform.AndroidDisplayCompat;

import java.util.Objects;

/** Android adapter for display, cutout, decoder, and HDR capabilities. */
final class AndroidSettingsDisplayCapabilities {
    private static final String MIME_AVC = "video/avc";
    private static final String MIME_HEVC = "video/hevc";

    private AndroidSettingsDisplayCapabilities() {
    }

    static SettingsDisplayCapabilities collect(
            Activity activity,
            DisplayCutout androidPieCutout,
            GlDeviceSnapshot glDeviceSnapshot) {
        Objects.requireNonNull(glDeviceSnapshot, "glDeviceSnapshot");
        Display display = AndroidDisplayCompat.getActivityDisplay(
                activity);
        SettingsDisplayCapabilities.Builder capabilities =
                SettingsDisplayCapabilities.builder();
        float maximumRefreshRate = display.getRefreshRate();
        int maximumPresetWidth = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasInsets = addInsetAdjustedResolution(
                    capabilities,
                    display,
                    androidPieCutout);
            boolean television = AndroidDeviceCategory.isTelevision(
                    activity);

            for (Display.Mode mode : display.getSupportedModes()) {
                int width = Math.max(
                        mode.getPhysicalWidth(),
                        mode.getPhysicalHeight());
                int height = Math.min(
                        mode.getPhysicalWidth(),
                        mode.getPhysicalHeight());

                if (!television || width > 3840 || height > 2160) {
                    capabilities.addNativeResolution(
                            width,
                            height,
                            hasInsets);
                }
                maximumPresetWidth = Math.max(
                        maximumPresetWidth,
                        standardPresetWidth(width, height));
                maximumRefreshRate = Math.max(
                        maximumRefreshRate,
                        mode.getRefreshRate());
            }

            MediaCodecHelper.initialize(
                    activity,
                    glDeviceSnapshot.getRenderer());
            maximumPresetWidth = probeDecoderWidth(
                    maximumPresetWidth,
                    MIME_AVC);
            maximumPresetWidth = probeDecoderWidth(
                    maximumPresetWidth,
                    MIME_HEVC);
        }
        else {
            Point size = AndroidDisplayCompat.getPhysicalDisplaySize(
                    activity);
            capabilities.addNativeResolution(
                    Math.max(size.x, size.y),
                    Math.min(size.x, size.y),
                    false);
        }

        return capabilities
                .maximumSupportedPresetWidth(maximumPresetWidth)
                .maximumRefreshRate(maximumRefreshRate)
                .hdrState(hdrState(display))
                .build();
    }

    private static boolean addInsetAdjustedResolution(
            SettingsDisplayCapabilities.Builder capabilities,
            Display display,
            DisplayCutout androidPieCutout) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        DisplayCutout cutout = Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
                ? display.getCutout()
                : androidPieCutout;
        if (cutout == null) {
            return false;
        }

        int horizontalInsets = cutout.getSafeInsetLeft() +
                cutout.getSafeInsetRight();
        int verticalInsets = cutout.getSafeInsetTop() +
                cutout.getSafeInsetBottom();
        if (horizontalInsets == 0 && verticalInsets == 0) {
            return false;
        }

        Point size = new Point();
        Display.Mode mode = display.getMode();
        size.set(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        int adjustedWidth = size.x - horizontalInsets;
        int adjustedHeight = size.y - verticalInsets;
        if (adjustedWidth <= 0 || adjustedHeight <= 0) {
            LimeLog.warning(
                    "Ignoring invalid cutout-adjusted display dimensions");
            return false;
        }
        capabilities.addNativeResolution(
                Math.max(adjustedWidth, adjustedHeight),
                Math.min(adjustedWidth, adjustedHeight),
                false);
        return true;
    }

    private static int standardPresetWidth(int width, int height) {
        if (width >= 3840 || height >= 2160) {
            return 3840;
        }
        if (width >= 2560 || height >= 1440) {
            return 2560;
        }
        if (width >= 1920 || height >= 1080) {
            return 1920;
        }
        return 0;
    }

    private static int probeDecoderWidth(
            int currentMaximum,
            String mimeType) {
        try {
            MediaCodecInfo decoder =
                    MediaCodecHelper.findProbableSafeDecoder(
                            mimeType,
                            -1);
            if (decoder == null) {
                return currentMaximum;
            }

            Range<Integer> widthRange = decoder
                    .getCapabilitiesForType(mimeType)
                    .getVideoCapabilities()
                    .getSupportedWidths();
            LimeLog.info(
                    mimeType + " supported width range: " +
                            widthRange.getLower() + " - " +
                            widthRange.getUpper());
            if (!widthRange.contains(1280)) {
                return currentMaximum;
            }
            if (widthRange.contains(3840)) {
                return Math.max(currentMaximum, 3840);
            }
            if (widthRange.contains(1920)) {
                return Math.max(currentMaximum, 1920);
            }
            return Math.max(currentMaximum, 1280);
        }
        catch (RuntimeException error) {
            LimeLog.warning(
                    "Unable to query " + mimeType +
                            " decoder widths: " + error.getMessage());
            return currentMaximum;
        }
    }

    private static SettingsDisplayCapabilities.HdrState hdrState(
            Display display) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return SettingsDisplayCapabilities.HdrState.UNAVAILABLE;
        }
        for (int hdrType :
                AndroidDisplayCompat.getSupportedHdrTypes(display)) {
            if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                return AndroidHdrCompatibility.isHdrStreamingAllowed()
                        ? SettingsDisplayCapabilities.HdrState.AVAILABLE
                        : SettingsDisplayCapabilities.HdrState
                                .BLOCKED_BY_FIRMWARE;
            }
        }
        return SettingsDisplayCapabilities.HdrState.UNAVAILABLE;
    }
}
