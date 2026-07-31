package com.limelight.ui.stream;

import android.app.Activity;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.ui.StreamView;
import com.limelight.utils.Dialog;
import com.limelight.utils.UiToast;

import java.util.Objects;

/** Android presentation endpoints for the stream-session policy owner. */
public final class AndroidStreamSessionPresentationHost
        implements StreamSessionPresentationController.Host {
    private final Activity activity;
    private final StreamView streamView;
    private final ControllerHandler controllerHandler;
    private final StreamUiSettingsState uiSettingsState;
    private final StreamDecoderSettings decoderSettings;
    private final AndroidStreamConnectingIndicator connectingIndicator;
    private final AndroidStreamConnectionWarningPresenter warningPresenter;
    private final AndroidStreamHdrModeController hdrModeController;
    private final AndroidStreamNativeCursorController nativeCursorController;
    private final Runnable stopConnection;
    private final Runnable sessionConnected;

    public AndroidStreamSessionPresentationHost(
            Activity activity,
            StreamView streamView,
            ControllerHandler controllerHandler,
            StreamUiSettingsState uiSettingsState,
            StreamDecoderSettings decoderSettings,
            AndroidStreamConnectingIndicator connectingIndicator,
            AndroidStreamConnectionWarningPresenter warningPresenter,
            AndroidStreamHdrModeController hdrModeController,
            AndroidStreamNativeCursorController nativeCursorController,
            Runnable stopConnection,
            Runnable sessionConnected) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.streamView = Objects.requireNonNull(streamView, "streamView");
        this.controllerHandler = Objects.requireNonNull(
                controllerHandler,
                "controllerHandler");
        this.uiSettingsState = Objects.requireNonNull(
                uiSettingsState,
                "uiSettingsState");
        this.decoderSettings = Objects.requireNonNull(
                decoderSettings,
                "decoderSettings");
        this.connectingIndicator = Objects.requireNonNull(
                connectingIndicator,
                "connectingIndicator");
        this.warningPresenter = Objects.requireNonNull(
                warningPresenter,
                "warningPresenter");
        this.hdrModeController = Objects.requireNonNull(
                hdrModeController,
                "hdrModeController");
        this.nativeCursorController = Objects.requireNonNull(
                nativeCursorController,
                "nativeCursorController");
        this.stopConnection = Objects.requireNonNull(
                stopConnection,
                "stopConnection");
        this.sessionConnected = Objects.requireNonNull(
                sessionConnected,
                "sessionConnected");
    }

    @Override
    public boolean canPresentSessionUi() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    @Override
    public void updateConnectingMessage(String message) {
        connectingIndicator.updateMessage(message);
    }

    @Override
    public void dismissConnectingIndicator() {
        connectingIndicator.dismiss();
    }

    @Override
    public boolean isRenderSurfaceValid() {
        return streamView.getHolder().getSurface().isValid();
    }

    @Override
    public void showLongMessage(String message) {
        UiToast.makeText(
                activity,
                message,
                UiToast.LENGTH_LONG).show();
    }

    @Override
    public void showFailureDialog(
            String title,
            String message) {
        Dialog.displayDialog(
                activity,
                title,
                message,
                true);
    }

    @Override
    public void stopControllerInput() {
        controllerHandler.stop();
    }

    @Override
    public void stopConnection() {
        stopConnection.run();
    }

    @Override
    public void finishGracefully() {
        activity.finish();
    }

    @Override
    public boolean areConnectionWarningsDisabled() {
        return uiSettingsState
                .get()
                .areConnectionWarningsDisabled();
    }

    @Override
    public int getBitrateKbps() {
        return decoderSettings.getBitrateKbps();
    }

    @Override
    public void setConnectionWarning(
            StreamSessionPresentationController.ConnectionWarning warning) {
        warningPresenter.setWarning(warning);
    }

    @Override
    public void onSessionConnected() {
        sessionConnected.run();
    }

    @Override
    public void onHdrModeChanged(
            boolean enabled,
            byte[] hdrMetadata) {
        hdrModeController.onHdrModeChanged(enabled, hdrMetadata);
    }

    @Override
    public void onNativeCursor(
            boolean visible,
            boolean shapeChanged,
            int format,
            int x,
            int y,
            int width,
            int height,
            int hotspotX,
            int hotspotY,
            int shapeId,
            int scaleX,
            int scaleY,
            byte[] imageData) {
        nativeCursorController.onNativeCursor(
                visible,
                shapeChanged,
                format,
                width,
                height,
                hotspotX,
                hotspotY,
                shapeId,
                scaleX,
                scaleY,
                imageData);
    }
}
