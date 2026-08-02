package com.limelight.ui.gamemenu;

import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.shortcuts.GameMenuShortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable render snapshot for the in-stream menu hierarchy. */
public final class GameMenuState {
    public static final int UNKNOWN_BATTERY_PERCENT = -1;

    private final boolean inputReady;
    private final boolean microphoneActive;
    private final boolean screenMoveZoom;
    private final boolean mouseEmulationAvailable;
    private final boolean virtualControllerVisible;
    private final boolean virtualKeysVisible;
    private final boolean videoHidden;
    private final int batteryPercent;
    private final GameMenuCardLayoutLoadResult cardLayout;
    private final List<GameMenuShortcut> shortcuts;
    private final InputSettings inputSettings;
    private final ControllerSettings controllerSettings;
    private final StreamAudioSettings audioSettings;
    private final StreamUiSettings uiSettings;
    private final VirtualControlSettings virtualControlSettings;
    private final VirtualControlEditMode virtualGamepadEditMode;
    private final VirtualControlEditMode virtualKeysEditMode;

    public GameMenuState(
            boolean inputReady,
            boolean microphoneActive,
            boolean screenMoveZoom,
            boolean mouseEmulationAvailable,
            boolean virtualControllerVisible,
            boolean virtualKeysVisible,
            boolean videoHidden,
            int batteryPercent,
            GameMenuCardLayoutLoadResult cardLayout,
            List<GameMenuShortcut> shortcuts,
            InputSettings inputSettings,
            ControllerSettings controllerSettings,
            StreamAudioSettings audioSettings,
            StreamUiSettings uiSettings,
            VirtualControlSettings virtualControlSettings,
            VirtualControlEditMode virtualGamepadEditMode,
            VirtualControlEditMode virtualKeysEditMode) {
        this.inputReady = inputReady;
        this.microphoneActive = microphoneActive;
        this.screenMoveZoom = screenMoveZoom;
        this.mouseEmulationAvailable = mouseEmulationAvailable;
        this.virtualControllerVisible = virtualControllerVisible;
        this.virtualKeysVisible = virtualKeysVisible;
        this.videoHidden = videoHidden;
        this.batteryPercent = normalizeBatteryPercent(batteryPercent);
        this.cardLayout = Objects.requireNonNull(
                cardLayout, "cardLayout");
        this.shortcuts = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(
                        shortcuts, "shortcuts")));
        this.inputSettings = Objects.requireNonNull(
                inputSettings, "inputSettings");
        this.controllerSettings = Objects.requireNonNull(
                controllerSettings, "controllerSettings");
        this.audioSettings = Objects.requireNonNull(
                audioSettings, "audioSettings");
        this.uiSettings = Objects.requireNonNull(
                uiSettings, "uiSettings");
        this.virtualControlSettings = Objects.requireNonNull(
                virtualControlSettings, "virtualControlSettings");
        this.virtualGamepadEditMode = Objects.requireNonNull(
                virtualGamepadEditMode, "virtualGamepadEditMode");
        this.virtualKeysEditMode = Objects.requireNonNull(
                virtualKeysEditMode, "virtualKeysEditMode");
    }

    private static int normalizeBatteryPercent(int value) {
        return value < 0 || value > 100 ?
                UNKNOWN_BATTERY_PERCENT : value;
    }

    public boolean isInputReady() {
        return inputReady;
    }

    public boolean isMicrophoneActive() {
        return microphoneActive;
    }

    public boolean isScreenMoveZoom() {
        return screenMoveZoom;
    }

    public boolean isMouseEmulationAvailable() {
        return mouseEmulationAvailable;
    }

    public boolean isVirtualControllerVisible() {
        return virtualControllerVisible;
    }

    public boolean isVirtualKeysVisible() {
        return virtualKeysVisible;
    }

    public boolean isVideoHidden() {
        return videoHidden;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public GameMenuCardLayoutLoadResult getCardLayout() {
        return cardLayout;
    }

    public List<GameMenuShortcut> getShortcuts() {
        return shortcuts;
    }

    public InputSettings getInputSettings() {
        return inputSettings;
    }

    public ControllerSettings getControllerSettings() {
        return controllerSettings;
    }

    public StreamAudioSettings getAudioSettings() {
        return audioSettings;
    }

    public StreamUiSettings getUiSettings() {
        return uiSettings;
    }

    public VirtualControlSettings getVirtualControlSettings() {
        return virtualControlSettings;
    }

    public VirtualControlEditMode getVirtualGamepadEditMode() {
        return virtualGamepadEditMode;
    }

    public VirtualControlEditMode getVirtualKeysEditMode() {
        return virtualKeysEditMode;
    }
}
