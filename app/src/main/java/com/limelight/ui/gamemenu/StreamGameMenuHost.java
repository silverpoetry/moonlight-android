package com.limelight.ui.gamemenu;

import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.runtime.StreamSettingsSession;
import com.limelight.settings.stream.CustomResolutionRepository;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.GameMenuCardLayoutRepository;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.shortcuts.GameMenuShortcutRepository;

import java.util.Objects;

/**
 * Session-scoped application host for the in-stream menu hierarchy.
 *
 * <p>Repositories and typed setting transactions terminate here. The Activity
 * is exposed only through the platform-action port needed to operate Android
 * windows and already-composed stream subsystems.</p>
 */
public final class StreamGameMenuHost implements GameMenuHost {
    public interface MenuSession {
        boolean isMouseEmulationAvailable();

        void toggleMouseEmulation();

        void onDismissed(GameMenuFragment menu);

        void dismiss();
    }

    public interface Actions {
        boolean isInputReady();

        boolean isMicUplinkActive();

        boolean getScreenMoveZoom();

        boolean isVideoHidden();

        boolean isVirtualControllerVisible();

        boolean isVirtualKeysVisible();

        int getDeviceBatteryPercent();

        VirtualControlEditMode getVirtualGamepadEditMode();

        VirtualControlEditMode getVirtualKeysEditMode();

        void handleStreamBackPressed();

        void cancelPendingStreamBackExit();

        void requestStreamDisconnect();

        void requestStreamRestart();

        void requestStreamQuit();

        void requestSoftKeyboard();

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

        void switchMouseLocalCursor();

        boolean toggleAbsoluteMouseMode();

        void switchMouseModel(int mode);

        void applyDualSenseTriggerSettings();

        void setVirtualGamepadEditMode(VirtualControlEditMode mode);

        void setVirtualKeysEditMode(VirtualControlEditMode mode);

        void pullRemoteClipboardFiles();
    }

    @Override
    public GameMenuState getState() {
        return new GameMenuState(
                actions.isInputReady(),
                actions.isMicUplinkActive(),
                actions.getScreenMoveZoom(),
                menuSession.isMouseEmulationAvailable(),
                actions.isVirtualControllerVisible(),
                actions.isVirtualKeysVisible(),
                actions.isVideoHidden(),
                actions.getDeviceBatteryPercent(),
                cardLayoutRepository.load(),
                shortcutRepository.load(),
                settingsSession.getInputSettings(),
                settingsSession.getControllerSettings(),
                settingsSession.getAudioSettings(),
                settingsSession.getUiSettings(),
                settingsSession.getVirtualControlSettings(),
                actions.getVirtualGamepadEditMode(),
                actions.getVirtualKeysEditMode());
    }

    private final StreamSettingsSession settingsSession;
    private final CustomResolutionRepository customResolutionRepository;
    private final GameMenuCardLayoutRepository cardLayoutRepository;
    private final GameMenuShortcutRepository shortcutRepository;
    private final MenuSession menuSession;
    private final Actions actions;

    public StreamGameMenuHost(
            StreamSettingsSession settingsSession,
            CustomResolutionRepository customResolutionRepository,
            GameMenuCardLayoutRepository cardLayoutRepository,
            GameMenuShortcutRepository shortcutRepository,
            MenuSession menuSession,
            Actions actions) {
        this.settingsSession = Objects.requireNonNull(
                settingsSession, "settingsSession");
        this.customResolutionRepository = Objects.requireNonNull(
                customResolutionRepository,
                "customResolutionRepository");
        this.cardLayoutRepository = Objects.requireNonNull(
                cardLayoutRepository, "cardLayoutRepository");
        this.shortcutRepository = Objects.requireNonNull(
                shortcutRepository, "shortcutRepository");
        this.menuSession = Objects.requireNonNull(
                menuSession, "menuSession");
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    @Override
    public void saveGameMenuCardLayout(GameMenuCardLayout layout) {
        cardLayoutRepository.save(layout);
    }

    @Override
    public boolean saveGameMenuShortcut(GameMenuShortcut shortcut) {
        return shortcutRepository.save(shortcut);
    }

    @Override
    public boolean deleteGameMenuShortcut(String shortcutId) {
        return shortcutRepository.delete(shortcutId);
    }

    @Override
    public void handleStreamBackPressed() {
        actions.handleStreamBackPressed();
    }

    @Override
    public void cancelPendingStreamBackExit() {
        actions.cancelPendingStreamBackExit();
    }

    @Override
    public void requestStreamDisconnect() {
        actions.requestStreamDisconnect();
    }

    @Override
    public void requestStreamRestart() {
        actions.requestStreamRestart();
    }

    @Override
    public void requestStreamQuit() {
        actions.requestStreamQuit();
    }

    @Override
    public void requestSoftKeyboard() {
        actions.requestSoftKeyboard();
    }

    @Override
    public void onGameMenuDismissed(GameMenuFragment menu) {
        actions.cancelPendingStreamBackExit();
        menuSession.onDismissed(menu);
    }

    @Override
    public void sendKeyboardChord(short[] keyCodes) {
        actions.sendKeyboardChord(keyCodes);
    }

    @Override
    public void sendAndroidKeyChord(int[] keyCodes) {
        actions.sendAndroidKeyChord(keyCodes);
    }

    @Override
    public void sendClipboardText() {
        actions.sendClipboardText();
    }

    @Override
    public void switchLandscapePortraitScreen() {
        actions.switchLandscapePortraitScreen();
    }

    @Override
    public void toggleVirtualGamepad() {
        actions.toggleVirtualGamepad();
    }

    @Override
    public void toggleVirtualKeys() {
        actions.toggleVirtualKeys();
    }

    @Override
    public void toggleFullKeyboard() {
        actions.toggleFullKeyboard();
    }

    @Override
    public void showHUD() {
        actions.showHUD();
    }

    @Override
    public void switchHUD() {
        actions.switchHUD();
    }

    @Override
    public void screenMoveZoom() {
        actions.screenMoveZoom();
    }

    @Override
    public void toggleVideoVisibility() {
        actions.toggleVideoVisibility();
    }

    @Override
    public void switchMic() {
        actions.switchMic();
    }

    @Override
    public void toggleGamepadMouseEmulation() {
        menuSession.toggleMouseEmulation();
    }

    @Override
    public void switchMouseLocalCursor() {
        actions.switchMouseLocalCursor();
    }

    @Override
    public boolean toggleAbsoluteMouseMode() {
        return actions.toggleAbsoluteMouseMode();
    }

    @Override
    public void switchMouseModel(int mode) {
        actions.switchMouseModel(mode);
    }

    @Override
    public void applyInputSettingsUpdate(InputSettingsUpdate update) {
        settingsSession.applyInput(update);
    }

    @Override
    public void applyControllerSettingsUpdate(
            ControllerSettingsUpdate update) {
        settingsSession.applyController(update);
    }

    @Override
    public void applyStreamUiSettingsUpdate(
            StreamUiSettingsUpdate update) {
        settingsSession.applyUi(update);
    }

    @Override
    public void applyDualSenseTriggerSettings() {
        actions.applyDualSenseTriggerSettings();
    }

    @Override
    public void updateVirtualView() {
        settingsSession.reloadVirtualControls();
    }

    @Override
    public void applyVirtualControlSettingsUpdate(
            VirtualControlSettingsUpdate<?> update) {
        settingsSession.applyVirtualControls(update);
    }

    @Override
    public void setOnscreenControllerRumbleEnabled(boolean enabled) {
        settingsSession.setOnscreenRumbleEnabled(enabled);
    }

    @Override
    public void setVirtualGamepadEditMode(
            VirtualControlEditMode mode) {
        actions.setVirtualGamepadEditMode(mode);
    }

    @Override
    public void setVirtualKeysEditMode(VirtualControlEditMode mode) {
        actions.setVirtualKeysEditMode(mode);
    }

    @Override
    public void pullRemoteClipboardFiles() {
        actions.pullRemoteClipboardFiles();
    }

    @Override
    public StreamVideoSettings getStreamVideoSettings() {
        return settingsSession.getVideoSettings();
    }

    @Override
    public void applyStreamVideoSettingsUpdate(
            StreamVideoSettingsUpdate update) {
        settingsSession.applyVideo(update);
    }

    @Override
    public CustomResolutionRepository getCustomResolutionRepository() {
        return customResolutionRepository;
    }

    @Override
    public StreamAudioSettings getStreamAudioSettings() {
        return settingsSession.getAudioSettings();
    }

    @Override
    public void applyStreamAudioSettingsUpdate(
            StreamAudioSettingsUpdate update) {
        settingsSession.applyAudio(update);
    }

    @Override
    public void onDisplayConfigurationApplied() {
        menuSession.dismiss();
        actions.requestStreamRestart();
    }
}
