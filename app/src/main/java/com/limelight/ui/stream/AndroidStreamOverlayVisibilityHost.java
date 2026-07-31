package com.limelight.ui.stream;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.virtual_controller.keyboard.StreamVirtualControlsController;
import com.limelight.ui.performance.StreamPerformanceOverlayController;
import com.limelight.utils.UiHelper;

import java.util.Objects;
/** Android side effects for stream overlay visibility transitions. */
public final class AndroidStreamOverlayVisibilityHost
        implements StreamOverlayVisibilityController.Host {
    public interface BooleanValue {
        boolean get();
    }

    private final Activity activity;
    private final StreamVirtualControlsController virtualControlsController;
    private final StreamPerformanceOverlayController
            performanceOverlayController;
    private final TextView notificationOverlayView;
    private final ControllerHandler controllerHandler;
    private final BooleanValue gameModeIntegrationDisabled;

    public AndroidStreamOverlayVisibilityHost(
            Activity activity,
            StreamVirtualControlsController virtualControlsController,
            StreamPerformanceOverlayController
                    performanceOverlayController,
            TextView notificationOverlayView,
            ControllerHandler controllerHandler,
            BooleanValue gameModeIntegrationDisabled) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.virtualControlsController = Objects.requireNonNull(
                virtualControlsController,
                "virtualControlsController");
        this.performanceOverlayController = Objects.requireNonNull(
                performanceOverlayController,
                "performanceOverlayController");
        this.notificationOverlayView = Objects.requireNonNull(
                notificationOverlayView,
                "notificationOverlayView");
        this.controllerHandler = Objects.requireNonNull(
                controllerHandler,
                "controllerHandler");
        this.gameModeIntegrationDisabled = Objects.requireNonNull(
                gameModeIntegrationDisabled,
                "gameModeIntegrationDisabled");
    }

    @Override
    public void hideVirtualControls() {
        virtualControlsController.hideAll();
    }

    @Override
    public void hidePerformanceOverlay() {
        performanceOverlayController.hideForPictureInPicture();
    }

    @Override
    public void restorePerformanceOverlay() {
        performanceOverlayController.restoreAfterPictureInPicture();
    }

    @Override
    public void setConnectionWarningVisible(boolean visible) {
        notificationOverlayView.setVisibility(
                visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void disableControllerSensors() {
        controllerHandler.disableSensors();
    }

    @Override
    public void enableControllerSensors() {
        controllerHandler.enableSensors();
    }

    @Override
    public void notifyPictureInPictureEntered() {
        UiHelper.notifyStreamEnteringPiP(
                activity,
                gameModeIntegrationDisabled.get());
    }

    @Override
    public void notifyPictureInPictureExited() {
        UiHelper.notifyStreamExitingPiP(
                activity,
                gameModeIntegrationDisabled.get());
    }
}
