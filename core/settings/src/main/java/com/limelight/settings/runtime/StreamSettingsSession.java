package com.limelight.settings.runtime;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsLoader;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsLoader;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsLoader;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsLoader;
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

        void onAdaptiveTriggerSettingsChanged();

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

    /**
     * Reconciles settings edited outside the in-stream menu with the active
     * runtime.
     *
     * <p>The Activity invokes this method at its foreground lifecycle
     * boundary. Every domain is rebuilt atomically from the canonical
     * repository, then published before its runtime effect is dispatched.
     * Stream-negotiation settings are deliberately excluded because changing
     * them requires rebuilding the connection rather than mutating an active
     * decoder or transport in place.</p>
     */
    public void refreshRuntimeSettings() {
        InputSettings previousInput = inputState.get();
        InputSettings currentInput =
                InputSettingsLoader.load(repository);
        inputState.replace(currentInput);
        effects.onInputSettingsChanged(
                previousInput,
                currentInput);

        ControllerSettings previousController =
                controllerState.get();
        ControllerSettings currentController =
                ControllerSettingsLoader.load(repository);
        controllerState.replace(currentController);
        publishControllerEffects(
                previousController,
                currentController);

        StreamAudioSettings currentAudio =
                StreamAudioSettingsLoader.load(repository);
        audioState.replace(currentAudio);

        StreamUiSettings previousUi = uiState.get();
        StreamUiSettings currentUi =
                StreamUiSettingsLoader.load(repository);
        uiState.replace(currentUi);
        effects.onUiSettingsChanged(previousUi, currentUi);

        virtualControlState.replace(
                VirtualControlSettingsLoader.load(repository));
        effects.onVirtualControlSettingsReloaded();
    }

    /**
     * Reconciles externally edited force-press values into the active stream.
     *
     * <p>Only the three settings that the touch controller can apply safely
     * without reconnecting are copied. Session-scoped transport and pointer
     * mode decisions remain unchanged until the next stream.</p>
     */
    public void refreshForcePressSettings() {
        InputSettings previous = inputState.get();
        InputSettings persisted = InputSettingsLoader.load(repository);
        InputSettings current = previous.toBuilder()
                .setBarometerForcePressEnabled(
                        persisted.isBarometerForcePressEnabled())
                .setBarometerForcePressThresholdHpa(
                        persisted.getBarometerForcePressThresholdHpa())
                .setBarometerForcePressMinimumDurationMs(
                        persisted.getBarometerForcePressMinimumDurationMs())
                .build();
        if (!forcePressSettingsDiffer(previous, current)) {
            return;
        }
        inputState.replace(current);
        effects.onInputSettingsChanged(previous, current);
    }

    public void applyController(ControllerSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        ControllerSettings previous = controllerState.get();
        ControllerSettings current = update.applyTo(previous);
        update.persist(repository);
        controllerState.replace(current);
        publishControllerEffects(previous, current);
    }

    private void publishControllerEffects(
            ControllerSettings previous,
            ControllerSettings current) {
        if (previous.isBatteryReportingEnabled() !=
                current.isBatteryReportingEnabled()) {
            effects.onBatteryReportingChanged();
        }
        if (!previous.isForceGyroEnabled() &&
                current.isForceGyroEnabled()) {
            effects.onForceGyroEnabled();
        }
        if (adaptiveTriggerSettingsDiffer(previous, current)) {
            effects.onAdaptiveTriggerSettingsChanged();
        }
    }

    public void applyAudio(StreamAudioSettingsUpdate update) {
        Objects.requireNonNull(update, "update");
        StreamAudioSettings current =
                update.applyTo(audioState.get());
        update.persist(repository);
        audioState.replace(current);
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
        effects.onVirtualControlSettingsReloaded();
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

    private static boolean forcePressSettingsDiffer(
            InputSettings first,
            InputSettings second) {
        return first.isBarometerForcePressEnabled() !=
                        second.isBarometerForcePressEnabled() ||
                Float.compare(
                        first.getBarometerForcePressThresholdHpa(),
                        second.getBarometerForcePressThresholdHpa()) != 0 ||
                first.getBarometerForcePressMinimumDurationMs() !=
                        second.getBarometerForcePressMinimumDurationMs();
    }

    private static boolean adaptiveTriggerSettingsDiffer(
            ControllerSettings first,
            ControllerSettings second) {
        return first.getAdaptiveTriggerMode() !=
                        second.getAdaptiveTriggerMode() ||
                first.getAdaptiveTriggerStrength() !=
                        second.getAdaptiveTriggerStrength() ||
                first.getAdaptiveTriggerFrequency() !=
                        second.getAdaptiveTriggerFrequency() ||
                first.getAdaptiveTriggerStartPosition() !=
                        second.getAdaptiveTriggerStartPosition() ||
                first.getAdaptiveTriggerEndPosition() !=
                        second.getAdaptiveTriggerEndPosition();
    }
}
