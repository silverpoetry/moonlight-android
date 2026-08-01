package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.runtime.StreamSettingsSession;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StreamGameMenuHostTest {
    @Test
    public void ownsTypedSettingsAndMenuSessionTransitions() {
        Fixture fixture = new Fixture();

        fixture.host.applyControllerSettingsUpdate(
                ControllerSettingsUpdate
                        .onscreenRumbleEnabled(true));
        fixture.host.toggleGamepadMouseEmulation();
        fixture.host.onGameMenuDismissed(null);

        assertTrue(fixture.host
                .isOnscreenControllerRumbleEnabled());
        assertEquals(
                true,
                fixture.repository.values.get(
                        ControllerSettingKeys.ONSCREEN_RUMBLE
                                .getName()));
        assertEquals(1, fixture.menuSession.toggleCount);
        assertEquals(1, fixture.menuSession.dismissedCount);
        assertEquals(1, fixture.actions.cancelBackCount);
    }

    @Test
    public void displayApplyDismissesMenuBeforeDisconnecting() {
        Fixture fixture = new Fixture();
        fixture.menuSession.actions = fixture.actions;

        fixture.host.onDisplayConfigurationApplied();

        assertTrue(fixture.menuSession.dismissedBeforeDisconnect);
        assertEquals(1, fixture.actions.disconnectCount);
    }

    private static final class Fixture {
        private final FakeRepository repository =
                new FakeRepository();
        private final RecordingActions actions =
                new RecordingActions();
        private final RecordingMenuSession menuSession =
                new RecordingMenuSession();
        private final StreamGameMenuHost host =
                new StreamGameMenuHost(
                        createSettingsSession(repository),
                        new NoOpCustomResolutionRepository(),
                        new NoOpCardLayoutRepository(),
                        new NoOpShortcutRepository(),
                        menuSession,
                        actions);
    }

    private static StreamSettingsSession createSettingsSession(
            SettingsRepository repository) {
        return new StreamSettingsSession(
                repository,
                new InputSettingsState(
                        InputSettings.builder().build()),
                new ControllerSettingsState(
                        ControllerSettings.builder().build()),
                new StreamAudioSettingsState(
                        StreamAudioSettings.builder().build()),
                new StreamVideoSettingsState(
                        StreamVideoSettings.builder().build()),
                new StreamUiSettingsState(
                        StreamUiSettings.builder().build()),
                new VirtualControlSettingsState(
                        VirtualControlSettings.builder().build()),
                new StreamSettingsSession.Effects() {
                    @Override
                    public void onBatteryReportingChanged() {
                    }

                    @Override
                    public void onForceGyroEnabled() {
                    }

                    @Override
                    public void onAudioSettingsChanged(
                            StreamAudioSettings settings) {
                    }

                    @Override
                    public void onUiSettingsChanged(
                            StreamUiSettings previous,
                            StreamUiSettings current) {
                    }

                    @Override
                    public void onVirtualControlSettingsReloaded() {
                    }
                });
    }

    private static final class RecordingMenuSession
            implements StreamGameMenuHost.MenuSession {
        private RecordingActions actions;
        private int toggleCount;
        private int dismissedCount;
        private boolean dismissedBeforeDisconnect;

        @Override
        public boolean isMouseEmulationAvailable() {
            return true;
        }

        @Override
        public void toggleMouseEmulation() {
            toggleCount++;
        }

        @Override
        public void onDismissed(GameMenuFragment menu) {
            dismissedCount++;
        }

        @Override
        public void dismiss() {
            dismissedBeforeDisconnect =
                    actions == null || actions.disconnectCount == 0;
        }
    }

    private static final class RecordingActions
            implements StreamGameMenuHost.Actions {
        private int cancelBackCount;
        private int disconnectCount;

        @Override
        public boolean isInputReady() {
            return true;
        }

        @Override
        public boolean isMicUplinkActive() {
            return false;
        }

        @Override
        public boolean getScreenMoveZoom() {
            return false;
        }

        @Override
        public boolean isVirtualControllerVisible() {
            return false;
        }

        @Override
        public boolean isVirtualKeysVisible() {
            return false;
        }

        @Override
        public VirtualControlEditMode getVirtualGamepadEditMode() {
            return VirtualControlEditMode.NONE;
        }

        @Override
        public VirtualControlEditMode getVirtualKeysEditMode() {
            return VirtualControlEditMode.NONE;
        }

        @Override
        public void handleStreamBackPressed() {
        }

        @Override
        public void cancelPendingStreamBackExit() {
            cancelBackCount++;
        }

        @Override
        public void requestStreamDisconnect() {
            disconnectCount++;
        }

        @Override
        public void requestStreamQuit() {
        }

        @Override
        public void requestSoftKeyboard() {
        }

        @Override
        public void sendKeyboardChord(short[] keyCodes) {
        }

        @Override
        public void sendAndroidKeyChord(int[] keyCodes) {
        }

        @Override
        public void sendClipboardText() {
        }

        @Override
        public void switchLandscapePortraitScreen() {
        }

        @Override
        public void toggleVirtualGamepad() {
        }

        @Override
        public void toggleVirtualKeys() {
        }

        @Override
        public void toggleFullKeyboard() {
        }

        @Override
        public void showHUD() {
        }

        @Override
        public void switchHUD() {
        }

        @Override
        public void screenMoveZoom() {
        }

        @Override
        public void switchMic() {
        }

        @Override
        public void switchMouseLocalCursor() {
        }

        @Override
        public boolean toggleAbsoluteMouseMode() {
            return false;
        }

        @Override
        public void switchMouseModel(int mode) {
        }

        @Override
        public void applyDualSenseTriggerSettings() {
        }

        @Override
        public void setVirtualGamepadEditMode(
                VirtualControlEditMode mode) {
        }

        @Override
        public void setVirtualKeysEditMode(
                VirtualControlEditMode mode) {
        }

        @Override
        public void pullRemoteClipboardFiles() {
        }
    }

    private static final class NoOpCustomResolutionRepository
            implements com.limelight.settings.stream
                    .CustomResolutionRepository {
        @Override
        public java.util.Set<com.limelight.settings.stream.CustomResolution>
                load() {
            return Collections.emptySet();
        }

        @Override
        public void add(
                com.limelight.settings.stream.CustomResolution resolution) {
        }

        @Override
        public void remove(
                com.limelight.settings.stream.CustomResolution resolution) {
        }
    }

    private static final class NoOpCardLayoutRepository
            implements com.limelight.settings.ui
                    .GameMenuCardLayoutRepository {
        @Override
        public com.limelight.settings.ui.GameMenuCardLayoutLoadResult
                load() {
            return null;
        }

        @Override
        public void save(
                com.limelight.settings.ui.GameMenuCardLayout layout) {
        }
    }

    private static final class NoOpShortcutRepository
            implements com.limelight.shortcuts
                    .GameMenuShortcutRepository {
        @Override
        public java.util.List<com.limelight.shortcuts.GameMenuShortcut>
                load() {
            return Collections.emptyList();
        }

        @Override
        public boolean save(
                com.limelight.shortcuts.GameMenuShortcut shortcut) {
            return false;
        }

        @Override
        public boolean delete(String shortcutId) {
            return false;
        }
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public <T> Editor put(SettingKey<T> key, T value) {
                    values.put(key.getName(), key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    values.remove(key.getName());
                    return this;
                }

                @Override
                public void apply() {
                }

                @Override
                public boolean commit() {
                    return true;
                }
            };
        }
    }
}
