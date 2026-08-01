package com.limelight.settings.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class StreamSettingsSessionTest {
    @Test
    public void controllerEffectsOnlyFollowSemanticTransitions() {
        Fixture fixture = new Fixture();

        fixture.session.applyController(
                ControllerSettingsUpdate
                        .batteryReportingEnabled(true));
        fixture.session.applyController(
                ControllerSettingsUpdate
                        .batteryReportingEnabled(true));
        fixture.session.applyController(
                ControllerSettingsUpdate.forceGyroEnabled(true));
        fixture.session.applyController(
                ControllerSettingsUpdate.forceGyroEnabled(true));

        assertEquals(1, fixture.effects.batteryChanges);
        assertEquals(1, fixture.effects.forceGyroEnables);
        assertTrue(fixture.session.getControllerSettings()
                .isBatteryReportingEnabled());
        assertTrue(fixture.session.getControllerSettings()
                .isForceGyroEnabled());
        assertEquals(
                true,
                fixture.repository.values.get(
                        ControllerSettingKeys.BATTERY_REPORTING
                                .getName()));
    }

    @Test
    public void inputEffectsObservePublishedForcePressState() {
        Fixture fixture = new Fixture();
        fixture.effects.fixture = fixture;

        fixture.session.applyInput(
                InputSettingsUpdate
                        .barometerForcePressEnabled(true));

        assertTrue(fixture.effects.inputObservedPublishedState);
        assertTrue(fixture.effects.lastInput
                .isBarometerForcePressEnabled());
    }

    @Test
    public void externalForcePressSettingsRefreshActiveSnapshotOnly() {
        Fixture fixture = new Fixture();
        fixture.effects.fixture = fixture;
        fixture.repository.edit()
                .put(InputSettingKeys.BAROMETER_FORCE_PRESS, true)
                .put(
                        InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD,
                        875)
                .put(
                        InputSettingKeys
                                .BAROMETER_FORCE_PRESS_MINIMUM_DURATION,
                        320)
                .apply();

        fixture.session.refreshForcePressSettings();
        fixture.session.refreshForcePressSettings();

        InputSettings refreshed = fixture.session.getInputSettings();
        assertTrue(refreshed.isBarometerForcePressEnabled());
        assertEquals(
                0.875f,
                refreshed.getBarometerForcePressThresholdHpa(),
                0.0001f);
        assertEquals(
                320,
                refreshed.getBarometerForcePressMinimumDurationMs());
        assertTrue(refreshed.isAbsoluteMouseMode());
        assertEquals(1, fixture.effects.inputChanges);
        assertEquals(1, fixture.repository.applyCount);
    }

    @Test
    public void runtimeEffectsObservePersistedPublishedSnapshots() {
        Fixture fixture = new Fixture();
        fixture.effects.fixture = fixture;

        fixture.session.applyAudio(
                StreamAudioSettingsUpdate.muted(true));
        fixture.session.applyUi(
                StreamUiSettingsUpdate
                        .floatingControlEnabled(true));

        assertTrue(fixture.effects.audioObservedPublishedState);
        assertTrue(fixture.effects.uiObservedPublishedState);
        assertSame(
                fixture.session.getAudioSettings(),
                fixture.effects.lastAudio);
        assertSame(
                fixture.session.getUiSettings(),
                fixture.effects.lastUi);
        assertEquals(2, fixture.repository.applyCount);
    }

    @Test
    public void onscreenRumbleUsesTypedControllerTransaction() {
        Fixture fixture = new Fixture();

        fixture.session.setOnscreenRumbleEnabled(true);

        assertTrue(fixture.session.getControllerSettings()
                .isOnscreenRumbleEnabled());
        assertEquals(
                true,
                fixture.repository.values.get(
                        ControllerSettingKeys.ONSCREEN_RUMBLE
                                .getName()));
        assertEquals(1, fixture.repository.applyCount);
        assertEquals(0, fixture.effects.batteryChanges);
        assertEquals(0, fixture.effects.forceGyroEnables);
    }

    private static final class Fixture {
        private final FakeRepository repository =
                new FakeRepository();
        private final RecordingEffects effects =
                new RecordingEffects();
        private final StreamSettingsSession session =
                new StreamSettingsSession(
                        repository,
                        new InputSettingsState(
                                InputSettings.builder()
                                        .setAbsoluteMouseMode(true)
                                        .build()),
                        new ControllerSettingsState(
                                ControllerSettings.builder()
                                        .setBatteryReportingEnabled(false)
                                        .build()),
                        new StreamAudioSettingsState(
                                StreamAudioSettings.builder().build()),
                        new StreamVideoSettingsState(
                                StreamVideoSettings.builder().build()),
                        new StreamUiSettingsState(
                                StreamUiSettings.builder().build()),
                        new VirtualControlSettingsState(
                                VirtualControlSettings.builder().build()),
                        effects);
    }

    private static final class RecordingEffects
            implements StreamSettingsSession.Effects {
        private Fixture fixture;
        private int batteryChanges;
        private int forceGyroEnables;
        private StreamAudioSettings lastAudio;
        private StreamUiSettings lastUi;
        private boolean audioObservedPublishedState;
        private boolean uiObservedPublishedState;
        private InputSettings lastInput;
        private boolean inputObservedPublishedState;
        private int inputChanges;

        @Override
        public void onInputSettingsChanged(
                InputSettings previous,
                InputSettings current) {
            lastInput = current;
            inputChanges++;
            inputObservedPublishedState =
                    fixture.repository.applyCount == 1 &&
                            fixture.session.getInputSettings() == current &&
                            previous != current;
        }

        @Override
        public void onBatteryReportingChanged() {
            batteryChanges++;
        }

        @Override
        public void onForceGyroEnabled() {
            forceGyroEnables++;
        }

        @Override
        public void onAudioSettingsChanged(
                StreamAudioSettings settings) {
            lastAudio = settings;
            audioObservedPublishedState =
                    fixture.repository.applyCount == 1 &&
                            fixture.session.getAudioSettings() == settings;
        }

        @Override
        public void onUiSettingsChanged(
                StreamUiSettings previous,
                StreamUiSettings current) {
            lastUi = current;
            uiObservedPublishedState =
                    fixture.repository.applyCount == 2 &&
                            fixture.session.getUiSettings() == current &&
                            previous != current;
        }

        @Override
        public void onVirtualControlSettingsReloaded() {
        }
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();
        private int applyCount;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(
                    values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public <T> Editor put(
                        SettingKey<T> key,
                        T value) {
                    values.put(
                            key.getName(),
                            key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    values.remove(key.getName());
                    return this;
                }

                @Override
                public void apply() {
                    applyCount++;
                }

                @Override
                public boolean commit() {
                    applyCount++;
                    return true;
                }
            };
        }
    }
}
