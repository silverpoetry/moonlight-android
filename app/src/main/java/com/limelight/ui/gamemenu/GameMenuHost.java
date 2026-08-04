package com.limelight.ui.gamemenu;

import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.shortcuts.GameMenuShortcut;

/**
 * Lifecycle-bound capabilities exposed by the streaming Activity to its menu.
 *
 * <p>The menu obtains this contract from its attached Activity. It must not
 * retain the Activity after {@code onDetach()}.</p>
 */
public interface GameMenuHost {
    GameMenuState getState();

    void saveGameMenuCardLayout(GameMenuCardLayout layout);

    boolean saveGameMenuShortcut(GameMenuShortcut shortcut);

    boolean deleteGameMenuShortcut(String shortcutId);

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

    void toggleVideoVisibility();

    void switchMic();

    void applyStreamAudioSettingsUpdate(
            StreamAudioSettingsUpdate update);

    void toggleGamepadMouseEmulation();

    boolean isLocalSystemCursorVisible();

    void switchMouseLocalCursor();

    boolean toggleAbsoluteMouseMode();

    void switchMouseModel(int mode);

    void applyInputSettingsUpdate(InputSettingsUpdate update);

    void applyControllerSettingsUpdate(
            ControllerSettingsUpdate update);

    void applyStreamUiSettingsUpdate(
            StreamUiSettingsUpdate update);

    void updateVirtualView();

    void applyVirtualControlSettingsUpdate(
            VirtualControlSettingsUpdate<?> update);

    void setOnscreenControllerRumbleEnabled(boolean enabled);

    void setVirtualGamepadEditMode(
            VirtualControlEditMode mode);

    void setVirtualKeysEditMode(
            VirtualControlEditMode mode);

    void pullRemoteClipboardFiles();
}
