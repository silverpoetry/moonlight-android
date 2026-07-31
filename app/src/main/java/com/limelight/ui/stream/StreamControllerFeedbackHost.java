package com.limelight.ui.stream;

import androidx.annotation.AnyThread;

import com.limelight.LimeLog;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.ui.performance.StreamPerformanceOverlayController;

import java.util.Locale;
import java.util.Objects;

/**
 * Routes host controller-feedback requests to local controller devices.
 *
 * <p>This path intentionally stays on the connection callback thread. UI
 * observers are responsible for performing their own main-thread dispatch.</p>
 */
public final class StreamControllerFeedbackHost
        implements StreamSessionCallbackRouter.FeedbackHost {
    interface DeviceFeedback {
        void rumble(
                short controllerNumber,
                short lowFrequencyMotor,
                short highFrequencyMotor);

        void rumbleTriggers(
                short controllerNumber,
                short leftTrigger,
                short rightTrigger);

        void setMotionEventState(
                short controllerNumber,
                byte motionType,
                short reportRateHz);

        void setControllerLed(
                short controllerNumber,
                byte red,
                byte green,
                byte blue);
    }

    interface TriggerRumbleLinkPolicy {
        boolean isEnabled();
    }

    interface RumbleObserver {
        void onRumble(
                short controllerNumber,
                short lowFrequencyMotor,
                short highFrequencyMotor);
    }

    private final DeviceFeedback deviceFeedback;
    private final TriggerRumbleLinkPolicy triggerRumbleLinkPolicy;
    private final RumbleObserver rumbleObserver;

    public StreamControllerFeedbackHost(
            ControllerHandler controllerHandler,
            ControllerSettingsState controllerSettingsState,
            StreamPerformanceOverlayController
                    performanceOverlayController) {
        ControllerSettingsState settingsState = Objects.requireNonNull(
                controllerSettingsState,
                "controllerSettingsState");
        this.deviceFeedback = createDeviceFeedback(controllerHandler);
        this.triggerRumbleLinkPolicy = () -> settingsState
                .get()
                .isTriggerRumbleLinkEnabled();
        this.rumbleObserver = Objects.requireNonNull(
                        performanceOverlayController,
                        "performanceOverlayController")
                ::updateRumble;
    }

    StreamControllerFeedbackHost(
            DeviceFeedback deviceFeedback,
            TriggerRumbleLinkPolicy triggerRumbleLinkPolicy,
            RumbleObserver rumbleObserver) {
        this.deviceFeedback = Objects.requireNonNull(
                deviceFeedback,
                "deviceFeedback");
        this.triggerRumbleLinkPolicy = Objects.requireNonNull(
                triggerRumbleLinkPolicy,
                "triggerRumbleLinkPolicy");
        this.rumbleObserver = Objects.requireNonNull(
                rumbleObserver,
                "rumbleObserver");
    }

    @AnyThread
    @Override
    public void onRumble(
            short controllerNumber,
            short lowFreqMotor,
            short highFreqMotor) {
        LimeLog.info(String.format(
                Locale.ROOT,
                "Rumble on gamepad %d: %04x %04x",
                controllerNumber,
                lowFreqMotor,
                highFreqMotor));
        deviceFeedback.rumble(
                controllerNumber,
                lowFreqMotor,
                highFreqMotor);
        if (triggerRumbleLinkPolicy.isEnabled()) {
            routeTriggerRumble(
                    controllerNumber,
                    lowFreqMotor,
                    highFreqMotor);
        }
        rumbleObserver.onRumble(
                controllerNumber,
                lowFreqMotor,
                highFreqMotor);
    }

    @AnyThread
    @Override
    public void onRumbleTriggers(
            short controllerNumber,
            short leftTrigger,
            short rightTrigger) {
        routeTriggerRumble(
                controllerNumber,
                leftTrigger,
                rightTrigger);
    }

    @AnyThread
    @Override
    public void onMotionEventState(
            short controllerNumber,
            byte motionType,
            short reportRateHz) {
        LimeLog.info(String.format(
                Locale.ROOT,
                "Motion state on gamepad %d: type=%d rate=%d Hz",
                controllerNumber,
                motionType,
                reportRateHz));
        deviceFeedback.setMotionEventState(
                controllerNumber,
                motionType,
                reportRateHz);
    }

    @AnyThread
    @Override
    public void onControllerLed(
            short controllerNumber,
            byte red,
            byte green,
            byte blue) {
        deviceFeedback.setControllerLed(
                controllerNumber,
                red,
                green,
                blue);
    }

    private void routeTriggerRumble(
            short controllerNumber,
            short leftTrigger,
            short rightTrigger) {
        LimeLog.info(String.format(
                Locale.ROOT,
                "Rumble on gamepad triggers %d: %04x %04x",
                controllerNumber,
                leftTrigger,
                rightTrigger));
        deviceFeedback.rumbleTriggers(
                controllerNumber,
                leftTrigger,
                rightTrigger);
    }

    private static DeviceFeedback createDeviceFeedback(
            ControllerHandler controllerHandler) {
        ControllerHandler handler = Objects.requireNonNull(
                controllerHandler,
                "controllerHandler");
        return new DeviceFeedback() {
            @Override
            public void rumble(
                    short controllerNumber,
                    short lowFrequencyMotor,
                    short highFrequencyMotor) {
                handler.handleRumble(
                        controllerNumber,
                        lowFrequencyMotor,
                        highFrequencyMotor);
            }

            @Override
            public void rumbleTriggers(
                    short controllerNumber,
                    short leftTrigger,
                    short rightTrigger) {
                handler.handleRumbleTriggers(
                        controllerNumber,
                        leftTrigger,
                        rightTrigger);
            }

            @Override
            public void setMotionEventState(
                    short controllerNumber,
                    byte motionType,
                    short reportRateHz) {
                handler.handleSetMotionEventState(
                        controllerNumber,
                        motionType,
                        reportRateHz);
            }

            @Override
            public void setControllerLed(
                    short controllerNumber,
                    byte red,
                    byte green,
                    byte blue) {
                handler.handleSetControllerLED(
                        controllerNumber,
                        red,
                        green,
                        blue);
            }
        };
    }
}
