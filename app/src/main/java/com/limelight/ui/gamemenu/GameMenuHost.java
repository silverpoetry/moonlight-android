package com.limelight.ui.gamemenu;

import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;

/**
 * Lifecycle-bound capabilities exposed by the streaming Activity to its menu.
 *
 * <p>The menu obtains this contract from its attached Activity. It must not
 * retain the Activity after {@code onDetach()}.</p>
 */
public interface GameMenuHost {
    PreferenceConfiguration getStreamPreferences();

    boolean isInputReady();

    boolean isMicUplinkActive();

    boolean getScreenMoveZoom();

    boolean isGamepadMouseEmulationAvailable();

    KeyBoardController.ControllerMode getVirtualControllerMode();

    KeyBoardController.ControllerMode getVirtualKeyControllerMode();

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

    void showHideVirtualController();

    void showHideKeyboardController();

    void showHidekeyBoardLayoutController();

    void showHUD();

    void switchHUD();

    void screenMoveZoom();

    void switchMic();

    void toggleGamepadMouseEmulation();

    void switchMouseLocalCursor();

    boolean toggleAbsoluteMouseMode();

    void switchMouseModel(int mode);

    void applyInputSettingsFromStorage();

    void applyControllerSettingsFromStorage();

    InputSettings getInputSettings();

    ControllerSettings getControllerSettings();

    void applyInputSettingsUpdate(InputSettingsUpdate update);

    void applyControllerSettingsUpdate(
            ControllerSettingsUpdate<?> update);

    void applyDualSenseTriggerSettings();

    void showFloatView();

    void hideFloatView();

    void applyRumbleOverlayVisibility();

    void applyPerformanceOverlayInteractivity();

    void applyPerformanceOverlayScale();

    void applyMotionEmulationSettings();

    void applyPerformanceOverlayMargin();

    void applyAudioSettingsFromStorage();

    void updateVirtualView();

    VirtualControlSettings getVirtualControlSettings();

    boolean isOnscreenControllerRumbleEnabled();

    void applyVirtualControlSettingsUpdate(
            VirtualControlSettingsUpdate<?> update);

    void setOnscreenControllerRumbleEnabled(boolean enabled);

    void switchVirtualController(
            KeyBoardController.ControllerMode mode);

    void switchVirtualKeyController(
            KeyBoardController.ControllerMode mode);

    void pullRemoteClipboardFiles();
}
