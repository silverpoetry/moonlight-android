package com.limelight.settings;

import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.audio.StreamAudioSettingKeys;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.ui.GameMenuCardSettingKeys;
import com.limelight.settings.ui.StreamUiSettingKeys;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete canonical registry for values stored in the default settings
 * repository.
 *
 * <p>The screen catalog is a presentation subset. This registry also owns
 * runtime-only values so schema migrations cannot leave hidden legacy keys
 * behind.</p>
 */
public final class SettingsKeyCatalog {
    private static final List<SettingKey<?>> KEYS = createCatalog();

    private SettingsKeyCatalog() {
    }

    public static Collection<SettingKey<?>> all() {
        return KEYS;
    }

    private static List<SettingKey<?>> createCatalog() {
        LinkedHashMap<String, SettingKey<?>> keys =
                new LinkedHashMap<>();
        add(keys, SettingsScreenKeyCatalog.all());

        add(
                keys,
                AppPresentationSettingKeys.SMALL_APP_ICONS,
                StreamAudioSettingKeys.MUTED,
                StreamResolutionSettingKeys.SELECTION,
                StreamVideoSettingKeys.HDR_HIGH_BRIGHTNESS,
                StreamVideoSettingKeys.IGNORE_HDR_CAPABILITY,
                StreamVideoSettingKeys.VIRTUAL_DISPLAY_MODE,
                StreamVideoSettingKeys.ENFORCE_DISPLAY_MODE,
                StreamVideoSettingKeys.SCREEN_ON_POLICY);

        add(
                keys,
                ControllerSettingKeys.ONLY_L3_R3,
                ControllerSettingKeys.MOUSE_SENSITIVITY_PERCENT,
                ControllerSettingKeys.FORCE_STRONG_VIBRATIONS,
                ControllerSettingKeys.FORCE_STRONG_VIBRATIONS_STOP_PULSE,
                ControllerSettingKeys.FORCE_GYRO,
                ControllerSettingKeys.FORCE_GYRO_REQUIRES_LEFT_TRIGGER,
                ControllerSettingKeys.FORCE_GYRO_SWAP_AXES,
                ControllerSettingKeys.FORCE_GYRO_SENSITIVITY_PERCENT,
                ControllerSettingKeys.MOUSE_EMULATION_BUTTON,
                ControllerSettingKeys.USB_GYROSCOPE_REPORTING,
                ControllerSettingKeys.TRIGGER_RUMBLE_LINK,
                ControllerSettingKeys.ADAPTIVE_TRIGGER_MODE,
                ControllerSettingKeys.ADAPTIVE_TRIGGER_STRENGTH,
                ControllerSettingKeys.ADAPTIVE_TRIGGER_FREQUENCY,
                ControllerSettingKeys.ADAPTIVE_TRIGGER_START_POSITION,
                ControllerSettingKeys.ADAPTIVE_TRIGGER_END_POSITION);

        add(
                keys,
                InputSettingKeys.SOFT_KEYBOARD_GESTURE_FINGERS,
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_X,
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_Y,
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_X,
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_Y,
                InputSettingKeys.EXTERNAL_TOUCHPAD_SENSITIVITY_X,
                InputSettingKeys.EXTERNAL_TOUCHPAD_SENSITIVITY_Y,
                InputSettingKeys.EXTERNAL_TOUCHPAD_SCROLL_AMOUNT,
                InputSettingKeys.MOUSE_WHEEL_SCROLL_AMOUNT);

        add(
                keys,
                StreamUiSettingKeys.FLOATING_ACTION,
                StreamUiSettingKeys.REMEMBER_FLOATING_POSITION,
                StreamUiSettingKeys.FLOATING_POSITION_X,
                StreamUiSettingKeys.FLOATING_POSITION_Y,
                StreamUiSettingKeys.FLOATING_POSITION_NEAREST_LEFT,
                StreamUiSettingKeys.RUMBLE_OVERLAY_ENABLED,
                StreamUiSettingKeys.COMPACT_PERFORMANCE_SCALE_PERCENT);

        add(
                keys,
                VirtualControlSettingKeys.NORMAL_COLOR,
                VirtualControlSettingKeys.GAMEPAD_SCALE_PERCENT);

        add(
                keys,
                GameMenuCardSettingKeys.ORDER_DOCUMENT,
                GameMenuCardSettingKeys.HIDDEN_CARD_IDS);

        validateLegacyNames(keys);
        return Collections.unmodifiableList(
                new ArrayList<>(keys.values()));
    }

    private static void add(
            Map<String, SettingKey<?>> keys,
            Collection<SettingKey<?>> additions) {
        for (SettingKey<?> key : additions) {
            add(keys, key);
        }
    }

    private static void add(
            Map<String, SettingKey<?>> keys,
            SettingKey<?>... additions) {
        for (SettingKey<?> key : additions) {
            SettingKey<?> previous = keys.put(key.getName(), key);
            if (previous != null && previous != key) {
                throw new IllegalStateException(
                        "Duplicate canonical setting key: " +
                                key.getName());
            }
        }
    }

    private static void validateLegacyNames(
            Map<String, SettingKey<?>> keys) {
        LinkedHashMap<String, String> owners =
                new LinkedHashMap<>();
        for (SettingKey<?> key : keys.values()) {
            for (String legacyName : key.getLegacyNames()) {
                if (keys.containsKey(legacyName)) {
                    throw new IllegalStateException(
                            "Legacy key collides with canonical key: " +
                                    legacyName);
                }
                String previous = owners.put(
                        legacyName,
                        key.getName());
                if (previous != null) {
                    throw new IllegalStateException(
                            "Legacy key " + legacyName +
                                    " is claimed by " + previous +
                                    " and " + key.getName());
                }
            }
        }
    }
}
