package com.limelight.ui.gamemenu;

import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.shortcuts.GameMenuShortcut;

import java.util.List;

/**
 * Lifecycle-bound capabilities exposed by the streaming Activity to its menu.
 *
 * <p>The menu obtains this contract from its attached Activity. It must not
 * retain the Activity after {@code onDetach()}.</p>
 */
public interface GameMenuHost extends GameDisplayHost {
    boolean isInputReady();

    boolean isMicUplinkActive();

    boolean getScreenMoveZoom();

    boolean isGamepadMouseEmulationAvailable();

    boolean isVirtualControllerVisible();

    boolean isVirtualKeysVisible();

    GameMenuCardLayoutLoadResult loadGameMenuCardLayout();

    void saveGameMenuCardLayout(GameMenuCardLayout layout);

    List<GameMenuShortcut> loadGameMenuShortcuts();

    boolean saveGameMenuShortcut(GameMenuShortcut shortcut);

    boolean deleteGameMenuShortcut(String shortcutId);

    VirtualControlEditMode getVirtualGamepadEditMode();

    VirtualControlEditMode getVirtualKeysEditMode();

    void handleStreamBackPressed();

    void cancelPendingStreamBackExit();

    void requestStreamDisconnect();

    void requestStreamQuit();

    void requestSoftKeyboard();

    void onGameMenuDismissed(GameMenuFragment menu);

    void sendKeyboardChord(short[] keyCodes);

    void sendAndroidKeyChord(int[] keyCodes);

    void sendClipboardText();

    void switchLandscapePortraitScreen();

    void toggleVirtualGamepad();

    void toggleVirtualKeys();

    void toggleFullKeyboard();

    void showHUD();

    void switchHUD();

    void screenMoveZoom();

    void switchMic();

    void toggleGamepadMouseEmulation();

    void switchMouseLocalCursor();

    boolean toggleAbsoluteMouseMode();

    void switchMouseModel(int mode);

    InputSettings getInputSettings();

    ControllerSettings getControllerSettings();

    void applyInputSettingsUpdate(InputSettingsUpdate update);

    void applyControllerSettingsUpdate(
            ControllerSettingsUpdate update);

    StreamUiSettings getStreamUiSettings();

    void applyStreamUiSettingsUpdate(
            StreamUiSettingsUpdate update);

    void applyDualSenseTriggerSettings();

    void updateVirtualView();

    VirtualControlSettings getVirtualControlSettings();

    boolean isOnscreenControllerRumbleEnabled();

    void applyVirtualControlSettingsUpdate(
            VirtualControlSettingsUpdate<?> update);

    void setOnscreenControllerRumbleEnabled(boolean enabled);

    void setVirtualGamepadEditMode(
            VirtualControlEditMode mode);

    void setVirtualKeysEditMode(
            VirtualControlEditMode mode);

    void pullRemoteClipboardFiles();
}
