package com.limelight.preferences;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Range;
import android.view.Display;
import android.view.DisplayCutout;

import com.limelight.LimeLog;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.settings.android.AndroidHdrCompatibility;

/** Android adapter for display, cutout, decoder, and HDR capabilities. */
final class AndroidSettingsDisplayCapabilities {
    private static final String MIME_AVC = "video/avc";
    private static final String MIME_HEVC = "video/hevc";

    private AndroidSettingsDisplayCapabilities() {
    }

    static SettingsDisplayCapabilities collect(
            Activity activity,
            DisplayCutout androidPieCutout) {
        Display display = activity
                .getWindowManager()
                .getDefaultDisplay();
        SettingsDisplayCapabilities.Builder capabilities =
                SettingsDisplayCapabilities.builder();
        float maximumRefreshRate = display.getRefreshRate();
        int maximumPresetWidth = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasInsets = addInsetAdjustedResolution(
                    capabilities,
                    display,
                    androidPieCutout);
            boolean television = activity
                    .getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_TELEVISION);

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
                    GlPreferences.readPreferences(activity).glRenderer);
            maximumPresetWidth = probeDecoderWidth(
                    maximumPresetWidth,
                    MIME_AVC);
            maximumPresetWidth = probeDecoderWidth(
                    maximumPresetWidth,
                    MIME_HEVC);
        }
        else {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            capabilities.addNativeResolution(
                    Math.max(metrics.widthPixels, metrics.heightPixels),
                    Math.min(metrics.widthPixels, metrics.heightPixels),
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

        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int adjustedWidth = metrics.widthPixels - horizontalInsets;
        int adjustedHeight = metrics.heightPixels - verticalInsets;
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
        Display.HdrCapabilities hdrCapabilities =
                display.getHdrCapabilities();
        if (hdrCapabilities == null) {
            return SettingsDisplayCapabilities.HdrState.UNAVAILABLE;
        }
        for (int hdrType : hdrCapabilities.getSupportedHdrTypes()) {
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
