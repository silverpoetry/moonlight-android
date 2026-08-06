package com.limelight.ui.stream;

import android.app.Activity;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.platform.AndroidDeviceCategory;
import com.limelight.platform.AndroidDisplayCompat;
import com.limelight.ui.StreamLayoutGeometry;
import com.limelight.ui.StreamView;
import com.limelight.ui.StreamWindowPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Applies one stream's physical display mode and render-surface geometry. */
public final class AndroidStreamDisplayController {
    public static final class Preparation {
        private final float effectiveDisplayRefreshRate;
        private final float selectedDisplayRefreshRate;
        private final boolean systemManagedRefreshRate;

        private Preparation(
                float effectiveDisplayRefreshRate,
                float selectedDisplayRefreshRate,
                boolean systemManagedRefreshRate) {
            this.effectiveDisplayRefreshRate =
                    effectiveDisplayRefreshRate;
            this.selectedDisplayRefreshRate =
                    selectedDisplayRefreshRate;
            this.systemManagedRefreshRate =
                    systemManagedRefreshRate;
        }

        public float getEffectiveDisplayRefreshRate() {
            return effectiveDisplayRefreshRate;
        }

        public float getSelectedDisplayRefreshRate() {
            return selectedDisplayRefreshRate;
        }

        public boolean isSystemManagedRefreshRate() {
            return systemManagedRefreshRate;
        }
    }

    private final Activity activity;
    private final StreamView streamView;
    private final StreamDecoderSettings decoderSettings;
    private final StreamDisplaySettings displaySettings;
    private final StreamVideoSettings videoSettings;
    private final boolean television;
    private final boolean systemManagedRefreshRate;

    public AndroidStreamDisplayController(
            Activity activity,
            StreamView streamView,
            StreamDecoderSettings decoderSettings,
            StreamDisplaySettings displaySettings,
            StreamVideoSettings videoSettings) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.streamView = Objects.requireNonNull(streamView, "streamView");
        this.decoderSettings = Objects.requireNonNull(
                decoderSettings,
                "decoderSettings");
        this.displaySettings = Objects.requireNonNull(
                displaySettings,
                "displaySettings");
        this.videoSettings = Objects.requireNonNull(
                videoSettings,
                "videoSettings");
        television = AndroidDeviceCategory.isTelevision(activity);
        systemManagedRefreshRate =
                StreamDisplayRefreshPolicy
                        .shouldLetSystemManageRefreshRate(
                                television,
                                Build.MANUFACTURER,
                                Build.BRAND);
    }

    @MainThread
    public static boolean matchesPhysicalDisplayMode(
            Activity activity,
            int width,
            int height) {
        Objects.requireNonNull(activity, "activity");
        Display display = AndroidDisplayCompat.getActivityDisplay(
                activity);
        for (Display.Mode candidate : display.getSupportedModes()) {
            if (StreamWindowPolicy.matchesPhysicalResolution(
                    width,
                    height,
                    candidate.getPhysicalWidth(),
                    candidate.getPhysicalHeight())) {
                return true;
            }
        }
        return false;
    }

    @MainThread
    public Preparation prepare(
            StreamDecoderSettings.FramePacing framePacing) {
        Display display = AndroidDisplayCompat.getActivityDisplay(
                activity);
        WindowManager.LayoutParams windowLayoutParams =
                activity.getWindow().getAttributes();
        boolean mayReduceRefreshRate =
                StreamDisplayRefreshPolicy.mayReduceRefreshRate(
                        framePacing,
                        decoderSettings
                                .isRefreshRateReductionEnabled());
        float selectedRefreshRate = prepareDisplayMode(
                display,
                windowLayoutParams,
                mayReduceRefreshRate);

        configureRenderSurface();
        float effectiveRefreshRate = television
                ? selectedRefreshRate
                : Math.min(
                        display.getRefreshRate(),
                        selectedRefreshRate);
        return new Preparation(
                effectiveRefreshRate,
                selectedRefreshRate,
                systemManagedRefreshRate);
    }

    /**
     * Resolves the display timing used by transport negotiation without
     * mutating the current Activity window.
     */
    @MainThread
    public static Preparation inspect(
            Activity activity,
            StreamDecoderSettings decoderSettings,
            StreamDisplaySettings displaySettings,
            StreamDecoderSettings.FramePacing framePacing) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(decoderSettings, "decoderSettings");
        Objects.requireNonNull(displaySettings, "displaySettings");
        Objects.requireNonNull(framePacing, "framePacing");

        Display display = AndroidDisplayCompat.getActivityDisplay(activity);
        boolean television = AndroidDeviceCategory.isTelevision(activity);
        boolean systemManaged =
                StreamDisplayRefreshPolicy
                        .shouldLetSystemManageRefreshRate(
                                television,
                                Build.MANUFACTURER,
                                Build.BRAND);
        boolean mayReduceRefreshRate =
                StreamDisplayRefreshPolicy.mayReduceRefreshRate(
                        framePacing,
                        decoderSettings
                                .isRefreshRateReductionEnabled());
        Display.Mode bestMode = selectDisplayMode(
                display,
                decoderSettings,
                displaySettings,
                mayReduceRefreshRate);
        float selectedRefreshRate = bestMode.getRefreshRate();
        float effectiveRefreshRate = television
                ? selectedRefreshRate
                : Math.min(
                        display.getRefreshRate(),
                        selectedRefreshRate);
        return new Preparation(
                effectiveRefreshRate,
                selectedRefreshRate,
                systemManaged);
    }

    private float prepareDisplayMode(
            Display display,
            WindowManager.LayoutParams windowLayoutParams,
            boolean mayReduceRefreshRate) {
        Display.Mode currentMode = display.getMode();
        LimeLog.info(
                "Current display mode: " +
                        currentMode.getPhysicalWidth() + "x" +
                        currentMode.getPhysicalHeight() + "x" +
                        currentMode.getRefreshRate());

        List<Display.Mode> platformModes = new ArrayList<>();
        for (Display.Mode candidate : display.getSupportedModes()) {
            LimeLog.info(
                    "Examining display mode: " +
                            candidate.getPhysicalWidth() + "x" +
                            candidate.getPhysicalHeight() + "x" +
                            candidate.getRefreshRate());
            platformModes.add(candidate);
        }
        Display.Mode bestMode = selectDisplayMode(
                display,
                decoderSettings,
                displaySettings,
                mayReduceRefreshRate);
        LimeLog.info(
                "Best display mode: " +
                        bestMode.getPhysicalWidth() + "x" +
                        bestMode.getPhysicalHeight() + "x" +
                        bestMode.getRefreshRate());

        if (currentMode.getModeId() != bestMode.getModeId() &&
                !systemManagedRefreshRate) {
            if (videoSettings.shouldEnforceDisplayMode() ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    currentMode.getPhysicalWidth() !=
                            bestMode.getPhysicalWidth() ||
                    currentMode.getPhysicalHeight() !=
                            bestMode.getPhysicalHeight()) {
                windowLayoutParams.preferredDisplayModeId =
                        bestMode.getModeId();
                activity.getWindow().setAttributes(
                        windowLayoutParams);
            }
            else {
                LimeLog.info(
                        "Using setFrameRate() instead of " +
                                "preferredDisplayModeId due to " +
                                "matching resolution");
            }
        }
        else if (systemManagedRefreshRate) {
            LimeLog.info(
                    "Leaving refresh rate selection to the system " +
                            "on this device");
        }
        else {
            LimeLog.info(
                    "Current display mode is already the best " +
                            "display mode");
        }
        return bestMode.getRefreshRate();
    }

    private static Display.Mode selectDisplayMode(
            Display display,
            StreamDecoderSettings decoderSettings,
            StreamDisplaySettings displaySettings,
            boolean mayReduceRefreshRate) {
        Display.Mode currentMode = display.getMode();
        List<Display.Mode> platformModes = new ArrayList<>();
        List<StreamDisplayModeSelector.Mode> selectorModes =
                new ArrayList<>();
        for (Display.Mode candidate : display.getSupportedModes()) {
            platformModes.add(candidate);
            selectorModes.add(toSelectorMode(candidate));
        }
        StreamDisplayModeSelector.Mode selectedMode =
                StreamDisplayModeSelector.select(
                        decoderSettings.getWidth(),
                        decoderSettings.getHeight(),
                        decoderSettings.getFps(),
                        displaySettings.isNativeResolution(),
                        mayReduceRefreshRate,
                        toSelectorMode(currentMode),
                        selectorModes);
        return findPlatformMode(
                currentMode,
                platformModes,
                selectedMode.id);
    }

    private void configureRenderSurface() {
        if (displaySettings.isStretchVideo()) {
            streamView.getHolder().setFixedSize(
                    decoderSettings.getWidth(),
                    decoderSettings.getHeight());
            streamView.setDesiredAspectRatio(0.0);
            return;
        }
        double desiredAspectRatio =
                StreamLayoutGeometry.getAspectRatio(
                        decoderSettings.getWidth(),
                        decoderSettings.getHeight());
        streamView.setDesiredAspectRatio(desiredAspectRatio);
        LimeLog.info("surfaceChanged-->" + desiredAspectRatio);
    }

    private static Display.Mode findPlatformMode(
            Display.Mode currentMode,
            List<Display.Mode> platformModes,
            int selectedModeId) {
        for (Display.Mode mode : platformModes) {
            if (mode.getModeId() == selectedModeId) {
                return mode;
            }
        }
        return currentMode;
    }

    private static StreamDisplayModeSelector.Mode toSelectorMode(
            Display.Mode mode) {
        return new StreamDisplayModeSelector.Mode(
                mode.getModeId(),
                mode.getPhysicalWidth(),
                mode.getPhysicalHeight(),
                mode.getRefreshRate());
    }
}
