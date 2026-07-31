package com.limelight.binding.input.driver;

import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettingsState;

import java.util.Objects;

/**
 * Adapts the local USB service Binder to a revocable session endpoint.
 */
public final class UsbDriverServiceEndpoint
        implements UsbDriverSessionController.Endpoint {
    private final UsbDriverService.UsbDriverBinder binder;
    private final ControllerSettingsState controllerSettingsState;
    private final StreamAudioSettingsState audioSettingsState;
    private final UsbDriverListener listener;
    private final UsbDriverService.UsbDriverStateListener stateListener;

    private long leaseId;

    public UsbDriverServiceEndpoint(
            UsbDriverService.UsbDriverBinder binder,
            ControllerSettingsState controllerSettingsState,
            StreamAudioSettingsState audioSettingsState,
            UsbDriverListener listener,
            UsbDriverService.UsbDriverStateListener stateListener) {
        this.binder = Objects.requireNonNull(binder, "binder");
        this.controllerSettingsState = Objects.requireNonNull(
                controllerSettingsState,
                "controllerSettingsState");
        this.audioSettingsState = Objects.requireNonNull(
                audioSettingsState,
                "audioSettingsState");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.stateListener = Objects.requireNonNull(
                stateListener,
                "stateListener");
    }

    @Override
    public void activate() {
        if (leaseId != 0) {
            return;
        }
        leaseId = binder.attachSession(
                controllerSettingsState,
                audioSettingsState,
                listener,
                stateListener);
    }

    @Override
    public void deactivate() {
        if (leaseId == 0) {
            return;
        }
        long previousLeaseId = leaseId;
        leaseId = 0;
        binder.detachSession(previousLeaseId);
    }
}
