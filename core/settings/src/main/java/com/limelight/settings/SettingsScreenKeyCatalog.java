package com.limelight.settings;

import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.audio.StreamAudioSettingKeys;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamDecoderSettingKeys;
import com.limelight.settings.stream.StreamDisplaySettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.ui.StreamUiSettingKeys;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical schema exposed by the application settings screen.
 *
 * <p>The XML resource describes presentation only. Every persisted value
 * shown by that resource must resolve through this catalog so the screen and
 * runtime use the same key, storage type, validation, and default.</p>
 */
public final class SettingsScreenKeyCatalog {
    private static final Map<String, SettingKey<?>> KEYS =
            createCatalog();

    private SettingsScreenKeyCatalog() {
    }

    public static SettingKey<?> find(String name) {
        return name == null ? null : KEYS.get(name);
    }

    public static SettingKey<?> require(String name) {
        SettingKey<?> key = find(name);
        if (key == null) {
            throw new IllegalArgumentException(
                    "No settings-screen schema key named: " + name);
        }
        return key;
    }

    public static Collection<SettingKey<?>> all() {
        return KEYS.values();
    }

    private static Map<String, SettingKey<?>> createCatalog() {
        Builder builder = new Builder();

        builder.add(
                StreamResolutionSettingKeys.RESOLUTION,
                StreamResolutionSettingKeys.ASPECT_RATIO,
                StreamResolutionSettingKeys.FPS,
                StreamVideoSettingKeys.BITRATE_KBPS,
                StreamVideoSettingKeys.SCREEN_ON_POLICY,
                StreamDecoderSettingKeys.FRAME_PACING,
                StreamVideoSettingKeys.LOW_LATENCY_EXPERIMENT,
                StreamDisplaySettingKeys.STRETCH_VIDEO,
                StreamDisplaySettingKeys.DISPLAY_CUTOUT,
                VirtualControlSettingKeys.AUTOMATIC_SCREEN_ORIENTATION,
                StreamDisplaySettingKeys.GRAVITY,
                AppPresentationSettingKeys.THEME_MODE);

        builder.add(
                StreamAudioSettingKeys.CHANNEL_CONFIGURATION,
                StreamAudioSettingKeys.AUDIO_EFFECTS,
                StreamAudioSettingKeys.PLAY_HOST_AUDIO,
                StreamAudioSettingKeys.MUTED);

        builder.add(
                ControllerSettingKeys.STICK_DEADZONE_PERCENT,
                ControllerSettingKeys.MULTI_CONTROLLER,
                ControllerSettingKeys.USB_DRIVER,
                ControllerSettingKeys.CLAIM_ALL_USB_DEVICES,
                ControllerSettingKeys.MOUSE_EMULATION,
                ControllerSettingKeys.MOUSE_SENSITIVITY_PERCENT,
                ControllerSettingKeys.ANALOG_STICK_FOR_SCROLLING,
                ControllerSettingKeys.FALLBACK_DEVICE_RUMBLE,
                ControllerSettingKeys
                        .FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT,
                ControllerSettingKeys.FLIP_FACE_BUTTONS,
                ControllerSettingKeys.FLIP_RUMBLE_MOTORS,
                ControllerSettingKeys.TOUCHPAD_AS_MOUSE,
                ControllerSettingKeys.MOTION_SENSORS,
                ControllerSettingKeys
                        .MOTION_SENSORS_FALLBACK_TO_DEVICE,
                ControllerSettingKeys.DISABLE_TRIGGER_DEADZONE,
                ControllerSettingKeys.ONSCREEN_CONTROLLER,
                ControllerSettingKeys.ONSCREEN_RUMBLE,
                ControllerSettingKeys.JOY_CON_FIX,
                ControllerSettingKeys.BATTERY_REPORTING,
                ControllerSettingKeys.DEVICE_RUMBLE,
                ControllerSettingKeys.VIRTUAL_CONTROLLER_MOTION,
                ControllerSettingKeys
                        .MOUSE_EMULATION_OPENS_GAME_MENU,
                ControllerSettingKeys.MOUSE_EMULATION_BUTTON,
                ControllerSettingKeys.FORCE_STRONG_VIBRATIONS,
                ControllerSettingKeys
                        .FORCE_STRONG_VIBRATIONS_STOP_PULSE,
                ControllerSettingKeys.FORCE_GYRO,
                ControllerSettingKeys
                        .FORCE_GYRO_REQUIRES_LEFT_TRIGGER,
                ControllerSettingKeys.FORCE_GYRO_SWAP_AXES,
                ControllerSettingKeys
                        .FORCE_GYRO_SENSITIVITY_PERCENT);

        builder.add(
                InputSettingKeys.TOUCH_MODE,
                InputSettingKeys.LOCAL_SYSTEM_CURSOR,
                InputSettingKeys.MOUSE_NAVIGATION_BUTTONS,
                InputSettingKeys.ABSOLUTE_MOUSE_MODE,
                InputSettingKeys.BAROMETER_FORCE_PRESS,
                InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD,
                InputSettingKeys
                        .BAROMETER_FORCE_PRESS_MINIMUM_DURATION,
                InputSettingKeys.TOUCHPAD_LONG_PRESS_DURATION,
                InputSettingKeys.SOFT_KEYBOARD_GESTURE_FINGERS,
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_X,
                InputSettingKeys.TOUCHPAD_POINTER_SENSITIVITY_Y,
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_X,
                InputSettingKeys.VIRTUAL_TOUCHPAD_SENSITIVITY_Y,
                InputSettingKeys.EXTERNAL_TOUCHPAD_SENSITIVITY_X,
                InputSettingKeys.EXTERNAL_TOUCHPAD_SENSITIVITY_Y,
                InputSettingKeys.EXTERNAL_TOUCHPAD_SCROLL_AMOUNT,
                InputSettingKeys.MOUSE_WHEEL_SCROLL_AMOUNT,
                InputSettingKeys.DISABLE_ADAPTIVE_INPUT_THROTTLING,
                InputSettingKeys.ACCESSIBILITY_KEY_LOGGING);

        builder.add(
                TransferSettingKeys.CLIPBOARD_SYNC,
                TransferSettingKeys.CLIPBOARD_FILE_DIRECTORY_URI);

        builder.add(
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID,
                VirtualControlSettingKeys.CONTROL_OPACITY_PERCENT,
                VirtualControlSettingKeys.DISABLE_STICK_CLICK,
                VirtualControlSettingKeys.KEYBOARD_OPACITY_PERCENT,
                VirtualControlSettingKeys.KEYBOARD_HEIGHT_DP,
                VirtualControlSettingKeys.KEYBOARD_COMBINATION_MODE,
                VirtualControlSettingKeys.SHOW_VIRTUAL_KEYS_ON_START,
                VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID,
                VirtualControlSettingKeys.KEYBOARD_HAPTICS);

        builder.add(AppPresentationSettingKeys.LANGUAGE);

        builder.add(
                StreamVideoSettingKeys.OPTIMIZE_GAME_SETTINGS,
                StreamVideoSettingKeys.UNLOCK_FPS,
                StreamVideoSettingKeys.VIDEO_FORMAT,
                StreamVideoSettingKeys.HDR_ENABLED,
                StreamVideoSettingKeys.PORTRAIT,
                StreamVideoSettingKeys.EXTERNAL_DISPLAY,
                StreamDecoderSettingKeys.REDUCE_REFRESH_RATE,
                StreamDecoderSettingKeys.FULL_RANGE);

        builder.add(
                StreamUiSettingKeys.PICTURE_IN_PICTURE,
                StreamUiSettingKeys.CONNECTION_WARNINGS_DISABLED,
                StreamUiSettingKeys.PERFORMANCE_OVERLAY_ENABLED,
                StreamUiSettingKeys.COMPACT_PERFORMANCE_OVERLAY,
                StreamUiSettingKeys.COMPACT_PERFORMANCE_INTERACTIVE,
                StreamUiSettingKeys.COMPACT_PERFORMANCE_DETAILS,
                StreamUiSettingKeys.COMPACT_PERFORMANCE_MARGIN_TOP_DP,
                StreamUiSettingKeys.LATENCY_TOAST,
                StreamUiSettingKeys.FLOATING_CONTROL_ENABLED,
                StreamUiSettingKeys.FLOATING_ACTION,
                StreamUiSettingKeys.REMEMBER_FLOATING_POSITION,
                StreamUiSettingKeys.RUMBLE_OVERLAY_ENABLED,
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_SCALE_PERCENT,
                StreamUiSettingKeys.HIDE_BUILT_IN_SHORTCUTS,
                StreamUiSettingKeys.GAME_MODE_INTEGRATION_DISABLED);

        return builder.build();
    }

    private static final class Builder {
        private final LinkedHashMap<String, SettingKey<?>> keys =
                new LinkedHashMap<>();

        void add(SettingKey<?>... additions) {
            Objects.requireNonNull(additions, "additions");
            for (SettingKey<?> key : additions) {
                Objects.requireNonNull(key, "key");
                SettingKey<?> previous =
                        keys.put(key.getName(), key);
                if (previous != null && previous != key) {
                    throw new IllegalStateException(
                            "Duplicate settings-screen key: " +
                                    key.getName());
                }
            }
        }

        Map<String, SettingKey<?>> build() {
            return Collections.unmodifiableMap(
                    new LinkedHashMap<>(keys));
        }
    }
}
