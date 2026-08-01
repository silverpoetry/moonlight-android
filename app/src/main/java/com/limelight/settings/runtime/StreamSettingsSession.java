package com.limelight.settings.runtime;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;

import java.util.Objects;

/**
 * Single mutation owner for settings changed during one streaming session.
 *
 * <p>Each update is normalized by its typed intent, persisted through the
 * canonical repository, published as an immutable snapshot, and only then
 * forwarded to runtime effects. UI and Activity code therefore cannot
 * independently reimplement ordering or change detection.</p>
 */
public final class StreamSettingsSession {
    public interface Effects {
        void onInputSettingsChanged(
                InputSettings previous,
                InputSettings current);

        void onBatteryReportingChanged();

        void onForceGyroEnabled();

        void onAudioSettingsChanged(StreamAudioSettings settings);

        void onUiSettingsChanged(
                StreamUiSettings previous,
                StreamUiSettings current);

        void onVirtualControlSettingsReloaded();
    }

    private final SettingsRepository repository;
    private final InputSettingsState inputState;
    private final ControllerSettingsState controllerState;
    private final StreamAudioSettingsState audioState;
    private final StreamVideoSettingsState videoState;
    private final StreamUiSettingsState uiState;
    private final VirtualControlSettingsState virtualControlState;
    private final Effects effects;

    public StreamSettingsSession(
            SettingsRepository repository,
            InputSettingsState inputState,
            ControllerSettingsState controllerState,
            StreamAudioSettingsState audioState,
            StreamVideoSettingsState videoState,
            StreamUiSettingsState uiState,
            VirtualControlSettingsState virtualControlState,
            Effects effects) {
        this.repository = Objects.requireNonNull(
                repository, "repository");
        this.inputState = Objects.requireNonNull(
                inputState, "inputState");
        this.controllerState = Objects.requireNonNull(
                controllerState, "controllerState");
        this.audioState = Objects.requireNonNull(
                audioState, "audioState");
        this.videoState = Objects.requireNonNull(
                videoState, "videoState");
        this.uiState = Objects.requireNonNull(uiState, "uiState");
        this.virtualControlState = Objects.requireNonNull(
                virtualControlState, "virtualControlState");
        this.effects = Objects.requireNonNull(effects, "effects");
    }

    public InputSettings getInputSettings() {
        return inputState.get();
    }

    public ControllerSettings getControllerSettings() {
        return controllerState.get();
    }

    public StreamAudioSettings getAudioSettings() {
        return audioState.get();
    }

    public StreamVideoSettings getVideoSettings() {
        return videoState.get();
    }

    public StreamUiSettings getUiSettings() {
        return uiState.get();
    }

    public VirtualControlSettings getVirtualControlSettings() {
        return virtualControlState.get();
    }

    public void applyInput(InputSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        InputSettings previous = inputState.get();
        InputSettings current = update.applyTo(previous);
        update.persist(repository);
        inputState.replace(current);
        effects.onInputSettingsChanged(previous, current);
    }

    public void applyController(ControllerSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        ControllerSettings previous = controllerState.get();
        ControllerSettings current = update.applyTo(previous);
        update.persist(repository);
        controllerState.replace(current);

        if (previous.isBatteryReportingEnabled() !=
                current.isBatteryReportingEnabled()) {
            effects.onBatteryReportingChanged();
        }
        if (!previous.isForceGyroEnabled() &&
                current.isForceGyroEnabled()) {
            effects.onForceGyroEnabled();
        }
    }

    public void applyAudio(StreamAudioSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        StreamAudioSettings current =
                update.applyTo(audioState.get());
        update.persist(repository);
        audioState.replace(current);
        effects.onAudioSettingsChanged(current);
    }

    public void applyVideo(StreamVideoSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        StreamVideoSettings current =
                update.applyTo(videoState.get());
        update.persist(repository);
        videoState.replace(current);
    }

    public void applyUi(StreamUiSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        StreamUiSettings previous = uiState.get();
        StreamUiSettings current = update.applyTo(previous);
        update.persist(repository);
        uiState.replace(current);
        effects.onUiSettingsChanged(previous, current);
    }

    public void applyVirtualControls(
            VirtualControlSettingsUpdate<?> update) {
        Objects.requireNonNull(update, "update");
        VirtualControlSettings current =
                update.applyTo(virtualControlState.get());
        update.persist(repository);
        virtualControlState.replace(current);
    }

    public void setOnscreenRumbleEnabled(boolean enabled) {
        applyController(
                ControllerSettingsUpdate
                        .onscreenRumbleEnabled(enabled));
    }

    public void reloadVirtualControls() {
        virtualControlState.replace(
                VirtualControlSettingsLoader.load(repository));
        effects.onVirtualControlSettingsReloaded();
    }
}
