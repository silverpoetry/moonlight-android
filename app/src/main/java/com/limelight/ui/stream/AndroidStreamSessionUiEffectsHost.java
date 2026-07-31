package com.limelight.ui.stream;

import android.app.Activity;
import android.view.WindowManager;

import com.limelight.binding.input.capture.AndroidStreamInputCaptureController;
import com.limelight.utils.UiHelper;

import java.util.Objects;

/** Android window, GameManager, and input effects for session transitions. */
public final class AndroidStreamSessionUiEffectsHost
        implements StreamSessionUiEffects.Host {
    public interface BooleanValue {
        boolean get();
    }

    private final Activity activity;
    private final AndroidStreamInputCaptureController
            inputCaptureController;
    private final BooleanValue gameModeIntegrationDisabled;

    public AndroidStreamSessionUiEffectsHost(
            Activity activity,
            AndroidStreamInputCaptureController
                    inputCaptureController,
            BooleanValue gameModeIntegrationDisabled) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.inputCaptureController = Objects.requireNonNull(
                inputCaptureController,
                "inputCaptureController");
        this.gameModeIntegrationDisabled = Objects.requireNonNull(
                gameModeIntegrationDisabled,
                "gameModeIntegrationDisabled");
    }

    @Override
    public void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            activity.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void notifyStreamConnecting() {
        UiHelper.notifyStreamConnecting(
                activity,
                gameModeIntegrationDisabled.get());
    }

    @Override
    public void notifyStreamConnected() {
        UiHelper.notifyStreamConnected(
                activity,
                gameModeIntegrationDisabled.get());
    }

    @Override
    public void notifyStreamEnded() {
        UiHelper.notifyStreamEnded(
                activity,
                gameModeIntegrationDisabled.get());
    }

    @Override
    public void setInputGrabbed(boolean grabbed) {
        inputCaptureController.setInputGrabbed(grabbed);
    }
}
