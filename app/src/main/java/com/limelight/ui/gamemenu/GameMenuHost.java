package com.limelight.ui.gamemenu;

import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.preferences.PreferenceConfiguration;

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

    void applyDualSenseTriggerSettings();

    void showFloatView();

    void hideFloatView();

    void applyRumbleOverlayVisibility();

    void applyPerformanceOverlayInteractivity();

    void applyPerformanceOverlayScale();

    void applyMotionEmulationSettings();

    void applyPerformanceOverlayMargin();

    void applyAudioHapticsSettings();

    void updateVirtualView();

    void switchVirtualController(
            KeyBoardController.ControllerMode mode);

    void switchVirtualKeyController(
            KeyBoardController.ControllerMode mode);

    void pullRemoteClipboardFiles();
}
