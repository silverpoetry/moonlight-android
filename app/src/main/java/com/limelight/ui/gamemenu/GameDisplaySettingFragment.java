package com.limelight.ui.gamemenu;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.limelight.R;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.SeekBarValueRange;

import java.util.Objects;

/**
 * Edits cross-domain live stream settings through typed intents.
 */
public final class GameDisplaySettingFragment
        extends BaseGameMenuDialog
        implements SeekBar.OnSeekBarChangeListener {
    private static final SeekBarValueRange GYRO_SENSITIVITY_RANGE =
            new SeekBarValueRange(50, 200);
    private static final SeekBarValueRange PERFORMANCE_SCALE_RANGE =
            new SeekBarValueRange(50, 230);
    private static final SeekBarValueRange PERFORMANCE_MARGIN_RANGE =
            new SeekBarValueRange(0, 100);
    private static final SeekBarValueRange AUDIO_HAPTICS_STRENGTH_RANGE =
            new SeekBarValueRange(25, 200);

    private int titleRes = R.string.game_menu_misc_title;
    private StreamUiSettings uiSettings;
    private InputSettings inputSettings;
    private ControllerSettings controllerSettings;
    private StreamAudioSettings audioSettings;
    private Listener listener;

    private CheckBox floatingControl;
    private CheckBox rememberFloatingPosition;
    private CheckBox audioMute;
    private CheckBox compactPerformanceDetails;
    private CheckBox compactPerformanceInteractive;
    private CheckBox forceStrongVibrations;
    private CheckBox stopStrongVibrationPulse;
    private CheckBox rumbleOverlay;
    private RadioGroup mouseEmulationMode;
    private RadioGroup softKeyboardGesture;
    private RadioGroup floatingAction;
    private CheckBox forceGyro;
    private CheckBox forceGyroRequiresLeftTrigger;
    private CheckBox forceGyroAxesSwapped;
    private RadioGroup audioHapticsEnabled;
    private RadioGroup audioHapticsVoiceFilter;
    private RadioGroup audioHapticsOutputTarget;
    private RadioGroup keepControllerRumble;
    private LinearLayout audioHapticsDetails;
    private TextView keepControllerRumbleLabel;
    private SeekBar performanceScale;
    private SeekBar performanceMargin;
    private SeekBar gyroSensitivity;
    private SeekBar audioHapticsStrength;
    private TextView performanceScaleValue;
    private TextView performanceMarginValue;
    private TextView gyroSensitivityValue;
    private TextView audioHapticsStrengthValue;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_setting;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        requireSettings();
        bindControls(view);
        configureSeekBars();
        ((TextView) view.findViewById(R.id.tx_title))
                .setText(titleRes);
        renderAll();
        bindListeners(view);
    }

    public void setTitle(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    public void setSettings(
            StreamUiSettings uiSettings,
            InputSettings inputSettings,
            ControllerSettings controllerSettings,
            StreamAudioSettings audioSettings) {
        this.uiSettings = Objects.requireNonNull(
                uiSettings,
                "uiSettings");
        this.inputSettings = Objects.requireNonNull(
                inputSettings,
                "inputSettings");
        this.controllerSettings = Objects.requireNonNull(
                controllerSettings,
                "controllerSettings");
        this.audioSettings = Objects.requireNonNull(
                audioSettings,
                "audioSettings");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void requireSettings() {
        if (uiSettings == null ||
                inputSettings == null ||
                controllerSettings == null ||
                audioSettings == null) {
            throw new IllegalStateException(
                    "Misc settings must be supplied before showing");
        }
    }

    private void bindControls(View view) {
        floatingControl =
                view.findViewById(R.id.btn_game_float_ball);
        rememberFloatingPosition =
                view.findViewById(
                        R.id.btn_game_float_ball_postion);
        audioMute =
                view.findViewById(R.id.btn_game_audio_mute);
        compactPerformanceDetails =
                view.findViewById(R.id.btn_game_lite_ext);
        compactPerformanceInteractive =
                view.findViewById(R.id.btn_game_lite_click);
        forceStrongVibrations =
                view.findViewById(R.id.btn_game_rumble_force);
        stopStrongVibrationPulse =
                view.findViewById(
                        R.id.btn_game_rumble_force_stop);
        rumbleOverlay =
                view.findViewById(R.id.btn_game_rumble_hud);
        mouseEmulationMode =
                view.findViewById(R.id.rg_game_setting_control);
        softKeyboardGesture =
                view.findViewById(R.id.rg_game_setting_touch);
        floatingAction =
                view.findViewById(
                        R.id.rg_game_setting_float_ball);
        forceGyro =
                view.findViewById(R.id.btn_game_force_gyro);
        forceGyroRequiresLeftTrigger =
                view.findViewById(
                        R.id.btn_game_force_gyro_left_trgger);
        forceGyroAxesSwapped =
                view.findViewById(
                        R.id.btn_game_force_gyro_switch);
        audioHapticsEnabled =
                view.findViewById(
                        R.id.rg_game_audio_haptics_enable);
        audioHapticsVoiceFilter =
                view.findViewById(
                        R.id.rg_game_audio_haptics_voice_filter);
        audioHapticsOutputTarget =
                view.findViewById(
                        R.id.rg_game_audio_haptics_output_target);
        keepControllerRumble =
                view.findViewById(
                        R.id.rg_game_audio_haptics_keep_controller_rumble);
        audioHapticsDetails =
                view.findViewById(
                        R.id.layout_game_audio_haptics_details);
        keepControllerRumbleLabel =
                view.findViewById(
                        R.id.tx_game_audio_haptics_keep_controller_rumble);
        performanceScale =
                view.findViewById(
                        R.id.sb_game_setting_pref_zoom);
        performanceMargin =
                view.findViewById(
                        R.id.sb_game_setting_pref_magin_top);
        gyroSensitivity =
                view.findViewById(
                        R.id.sb_game_setting_gyro_sensitivity);
        audioHapticsStrength =
                view.findViewById(
                        R.id.sb_game_audio_haptics_strength);
        performanceScaleValue =
                view.findViewById(
                        R.id.tx_game_setting_pref_zoom);
        performanceMarginValue =
                view.findViewById(
                        R.id.tx_game_setting_pref_magin_top);
        gyroSensitivityValue =
                view.findViewById(
                        R.id.tx_game_setting_gyro_sensitivity);
        audioHapticsStrengthValue =
                view.findViewById(
                        R.id.tx_game_audio_haptics_strength);
    }

    private void configureSeekBars() {
        performanceScale.setMax(
                PERFORMANCE_SCALE_RANGE.getProgressMaximum());
        performanceMargin.setMax(
                PERFORMANCE_MARGIN_RANGE.getProgressMaximum());
        gyroSensitivity.setMax(
                GYRO_SENSITIVITY_RANGE.getProgressMaximum());
        audioHapticsStrength.setMax(
                AUDIO_HAPTICS_STRENGTH_RANGE
                        .getProgressMaximum());
    }

    private void renderAll() {
        floatingControl.setChecked(
                uiSettings.isFloatingControlEnabled());
        rememberFloatingPosition.setChecked(
                uiSettings.shouldRememberFloatingPosition());
        audioMute.setChecked(audioSettings.isMuted());
        compactPerformanceDetails.setChecked(
                uiSettings
                        .areCompactPerformanceDetailsEnabled());
        compactPerformanceInteractive.setChecked(
                uiSettings
                        .isCompactPerformanceInteractive());
        forceStrongVibrations.setChecked(
                controllerSettings
                        .isForceStrongVibrationsEnabled());
        stopStrongVibrationPulse.setChecked(
                controllerSettings
                        .isForceStrongVibrationsStopPulseEnabled());
        rumbleOverlay.setChecked(
                uiSettings.isRumbleOverlayEnabled());
        forceGyro.setChecked(
                controllerSettings.isForceGyroEnabled());
        forceGyroRequiresLeftTrigger.setChecked(
                controllerSettings
                        .isForceGyroLeftTriggerRequired());
        forceGyroAxesSwapped.setChecked(
                controllerSettings.areForceGyroAxesSwapped());

        renderMouseEmulationMode();
        renderSoftKeyboardGesture();
        renderFloatingAction();
        renderPerformanceSliders();
        renderGyroSensitivity();
        renderAudioHaptics();
    }

    private void renderMouseEmulationMode() {
        if (!controllerSettings.isMouseEmulationEnabled()) {
            mouseEmulationMode.check(
                    R.id.rbt_game_setting_control_1);
            return;
        }
        int button = controllerSettings.getMouseEmulationButton();
        mouseEmulationMode.check(
                button == 1
                        ? R.id.rbt_game_setting_control_3
                        : button == 2
                                ? R.id.rbt_game_setting_control_4
                                : R.id.rbt_game_setting_control_2);
    }

    private void renderSoftKeyboardGesture() {
        int fingers = inputSettings.getSoftKeyboardGestureFingers();
        softKeyboardGesture.check(
                fingers == 3
                        ? R.id.rbt_game_setting_touch_2
                        : fingers == 4
                                ? R.id.rbt_game_setting_touch_3
                                : fingers == 5
                                        ? R.id.rbt_game_setting_touch_4
                                        : R.id.rbt_game_setting_touch_1);
    }

    private void renderFloatingAction() {
        int id;
        switch (uiSettings.getFloatingAction()) {
            case SOFT_KEYBOARD:
                id = R.id.rbt_game_setting_float_ball_2;
                break;
            case FULL_KEYBOARD:
                id = R.id.rbt_game_setting_float_ball_3;
                break;
            case GAME_MENU:
            default:
                id = R.id.rbt_game_setting_float_ball_1;
                break;
        }
        floatingAction.check(id);
    }

    private void renderPerformanceSliders() {
        int scale =
                uiSettings.getCompactPerformanceScalePercent();
        int margin =
                uiSettings.getCompactPerformanceMarginTopDp();
        setSeekBarValue(
                performanceScale,
                PERFORMANCE_SCALE_RANGE,
                scale);
        setSeekBarValue(
                performanceMargin,
                PERFORMANCE_MARGIN_RANGE,
                margin);
        performanceScaleValue.setText(getString(
                R.string.game_menu_performance_scale_format,
                scale));
        performanceMarginValue.setText(getString(
                R.string.game_menu_performance_margin_format,
                margin));
    }

    private void renderGyroSensitivity() {
        int value =
                controllerSettings
                        .getForceGyroSensitivityPercent();
        setSeekBarValue(
                gyroSensitivity,
                GYRO_SENSITIVITY_RANGE,
                value);
        gyroSensitivityValue.setText(getString(
                R.string.game_menu_gyro_sensitivity_format,
                value));
    }

    private void renderAudioHaptics() {
        audioHapticsEnabled.check(
                audioSettings.areAudioHapticsEnabled()
                        ? R.id.rbt_game_audio_haptics_enable_on
                        : R.id.rbt_game_audio_haptics_enable_off);
        audioHapticsOutputTarget.check(
                audioSettings.isControllerHapticsTarget()
                        ? R.id.rbt_game_audio_haptics_output_target_controller
                        : R.id.rbt_game_audio_haptics_output_target_phone);
        int voiceFilterId;
        switch (audioSettings.getVoiceFilter()) {
            case LOW:
                voiceFilterId =
                        R.id.rbt_game_audio_haptics_voice_filter_2;
                break;
            case MEDIUM:
                voiceFilterId =
                        R.id.rbt_game_audio_haptics_voice_filter_3;
                break;
            case HIGH:
                voiceFilterId =
                        R.id.rbt_game_audio_haptics_voice_filter_4;
                break;
            case OFF:
            default:
                voiceFilterId =
                        R.id.rbt_game_audio_haptics_voice_filter_1;
                break;
        }
        audioHapticsVoiceFilter.check(voiceFilterId);
        keepControllerRumble.check(
                audioSettings.shouldKeepControllerRumble()
                        ? R.id.rbt_game_audio_haptics_keep_controller_rumble_on
                        : R.id.rbt_game_audio_haptics_keep_controller_rumble_off);
        int strength =
                audioSettings.getHapticsStrengthPercent();
        setSeekBarValue(
                audioHapticsStrength,
                AUDIO_HAPTICS_STRENGTH_RANGE,
                strength);
        audioHapticsStrengthValue.setText(getString(
                R.string.game_menu_audio_haptics_strength_format,
                strength));
        updateAudioHapticsVisibility();
    }

    private static void setSeekBarValue(
            SeekBar seekBar,
            SeekBarValueRange range,
            int value) {
        int progress = range.valueToProgress(value);
        if (seekBar.getProgress() != progress) {
            seekBar.setProgress(progress);
        }
    }

    private void updateAudioHapticsVisibility() {
        boolean enabled = audioSettings.areAudioHapticsEnabled();
        audioHapticsDetails.setVisibility(
                enabled ? View.VISIBLE : View.GONE);
        boolean showKeepRumble =
                enabled &&
                        audioSettings.isControllerHapticsTarget();
        int visibility =
                showKeepRumble ? View.VISIBLE : View.GONE;
        keepControllerRumbleLabel.setVisibility(visibility);
        keepControllerRumble.setVisibility(visibility);
    }

    private void bindListeners(View view) {
        view.findViewById(R.id.ibtn_back)
                .setOnClickListener(clicked -> dismiss());
        floatingControl.setOnCheckedChangeListener(
                (button, checked) -> dispatchUi(
                        StreamUiSettingsUpdate
                                .floatingControlEnabled(checked)));
        rememberFloatingPosition.setOnCheckedChangeListener(
                (button, checked) -> dispatchUi(
                        StreamUiSettingsUpdate
                                .rememberFloatingPosition(checked)));
        audioMute.setOnCheckedChangeListener(
                (button, checked) -> dispatchAudio(
                        StreamAudioSettingsUpdate.muted(checked)));
        compactPerformanceDetails.setOnCheckedChangeListener(
                (button, checked) -> dispatchUi(
                        StreamUiSettingsUpdate
                                .compactPerformanceDetails(checked)));
        compactPerformanceInteractive.setOnCheckedChangeListener(
                (button, checked) -> dispatchUi(
                        StreamUiSettingsUpdate
                                .compactPerformanceInteractive(
                                        checked)));
        forceStrongVibrations.setOnCheckedChangeListener(
                (button, checked) -> dispatchController(
                        ControllerSettingsUpdate
                                .forceStrongVibrationsEnabled(
                                        checked)));
        stopStrongVibrationPulse.setOnCheckedChangeListener(
                (button, checked) -> dispatchController(
                        ControllerSettingsUpdate
                                .forceStrongVibrationsStopPulseEnabled(
                                        checked)));
        rumbleOverlay.setOnCheckedChangeListener(
                (button, checked) -> dispatchUi(
                        StreamUiSettingsUpdate
                                .rumbleOverlayEnabled(checked)));
        forceGyro.setOnCheckedChangeListener(
                (button, checked) -> dispatchController(
                        ControllerSettingsUpdate
                                .forceGyroEnabled(checked)));
        forceGyroRequiresLeftTrigger.setOnCheckedChangeListener(
                (button, checked) -> dispatchController(
                        ControllerSettingsUpdate
                                .forceGyroRequiresLeftTrigger(
                                        checked)));
        forceGyroAxesSwapped.setOnCheckedChangeListener(
                (button, checked) -> dispatchController(
                        ControllerSettingsUpdate
                                .forceGyroAxesSwapped(checked)));

        mouseEmulationMode.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_setting_control_1) {
                        dispatchController(
                                ControllerSettingsUpdate
                                        .mouseEmulation(false, 0));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_control_2) {
                        dispatchController(
                                ControllerSettingsUpdate
                                        .mouseEmulation(true, 0));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_control_3) {
                        dispatchController(
                                ControllerSettingsUpdate
                                        .mouseEmulation(true, 1));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_control_4) {
                        dispatchController(
                                ControllerSettingsUpdate
                                        .mouseEmulation(true, 2));
                    }
                });
        softKeyboardGesture.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    int fingers;
                    if (checkedId ==
                            R.id.rbt_game_setting_touch_2) {
                        fingers = 3;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_touch_3) {
                        fingers = 4;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_touch_4) {
                        fingers = 5;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_touch_1) {
                        fingers = 0;
                    }
                    else {
                        return;
                    }
                    dispatchInput(
                            InputSettingsUpdate
                                    .softKeyboardGestureFingers(
                                            fingers));
                });
        floatingAction.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    StreamUiSettings.FloatingAction action;
                    if (checkedId ==
                            R.id.rbt_game_setting_float_ball_2) {
                        action = StreamUiSettings.FloatingAction
                                .SOFT_KEYBOARD;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_float_ball_3) {
                        action = StreamUiSettings.FloatingAction
                                .FULL_KEYBOARD;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_setting_float_ball_1) {
                        action = StreamUiSettings.FloatingAction
                                .GAME_MENU;
                    }
                    else {
                        return;
                    }
                    dispatchUi(
                            StreamUiSettingsUpdate
                                    .floatingAction(action));
                });
        audioHapticsEnabled.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_audio_haptics_enable_on) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .hapticsEnabled(true));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_audio_haptics_enable_off) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .hapticsEnabled(false));
                    }
                    updateAudioHapticsVisibility();
                });
        audioHapticsVoiceFilter.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    StreamAudioSettings.VoiceFilter filter;
                    if (checkedId ==
                            R.id.rbt_game_audio_haptics_voice_filter_2) {
                        filter = StreamAudioSettings.VoiceFilter.LOW;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_audio_haptics_voice_filter_3) {
                        filter =
                                StreamAudioSettings.VoiceFilter.MEDIUM;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_audio_haptics_voice_filter_4) {
                        filter = StreamAudioSettings.VoiceFilter.HIGH;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_audio_haptics_voice_filter_1) {
                        filter = StreamAudioSettings.VoiceFilter.OFF;
                    }
                    else {
                        return;
                    }
                    dispatchAudio(
                            StreamAudioSettingsUpdate
                                    .voiceFilter(filter));
                });
        audioHapticsOutputTarget.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_audio_haptics_output_target_phone) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .hapticsOutputTarget(
                                                StreamAudioSettings
                                                        .HapticsOutputTarget
                                                        .PHONE));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_audio_haptics_output_target_controller) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .hapticsOutputTarget(
                                                StreamAudioSettings
                                                        .HapticsOutputTarget
                                                        .CONTROLLER));
                    }
                    updateAudioHapticsVisibility();
                });
        keepControllerRumble.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_audio_haptics_keep_controller_rumble_on) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .keepControllerRumble(true));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_audio_haptics_keep_controller_rumble_off) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .keepControllerRumble(false));
                    }
                });

        performanceScale.setOnSeekBarChangeListener(this);
        performanceMargin.setOnSeekBarChangeListener(this);
        gyroSensitivity.setOnSeekBarChangeListener(this);
        audioHapticsStrength.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(
            SeekBar seekBar,
            int progress,
            boolean fromUser) {
        if (!fromUser) {
            return;
        }
        if (seekBar == performanceScale) {
            dispatchUi(
                    StreamUiSettingsUpdate
                            .compactPerformanceScalePercent(
                                    PERFORMANCE_SCALE_RANGE
                                            .progressToValue(
                                                    progress)));
            renderPerformanceSliders();
        }
        else if (seekBar == performanceMargin) {
            dispatchUi(
                    StreamUiSettingsUpdate
                            .compactPerformanceMarginTopDp(
                                    PERFORMANCE_MARGIN_RANGE
                                            .progressToValue(
                                                    progress)));
            renderPerformanceSliders();
        }
        else if (seekBar == gyroSensitivity) {
            dispatchController(
                    ControllerSettingsUpdate
                            .forceGyroSensitivityPercent(
                                    GYRO_SENSITIVITY_RANGE
                                            .progressToValue(
                                                    progress)));
            renderGyroSensitivity();
        }
        else if (seekBar == audioHapticsStrength) {
            dispatchAudio(
                    StreamAudioSettingsUpdate
                            .hapticsStrengthPercent(
                                    AUDIO_HAPTICS_STRENGTH_RANGE
                                            .progressToValue(
                                                    progress)));
            renderAudioHaptics();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void dispatchUi(StreamUiSettingsUpdate update) {
        uiSettings = update.applyTo(uiSettings);
        if (listener != null) {
            listener.onStreamUiSettingsUpdate(update);
        }
    }

    private void dispatchInput(InputSettingsUpdate update) {
        inputSettings = update.applyTo(inputSettings);
        if (listener != null) {
            listener.onInputSettingsUpdate(update);
        }
    }

    private void dispatchController(
            ControllerSettingsUpdate update) {
        controllerSettings =
                update.applyTo(controllerSettings);
        if (listener != null) {
            listener.onControllerSettingsUpdate(update);
        }
    }

    private void dispatchAudio(
            StreamAudioSettingsUpdate update) {
        audioSettings = update.applyTo(audioSettings);
        if (listener != null) {
            listener.onStreamAudioSettingsUpdate(update);
        }
    }

    public interface Listener {
        void onStreamUiSettingsUpdate(
                StreamUiSettingsUpdate update);

        void onInputSettingsUpdate(InputSettingsUpdate update);

        void onControllerSettingsUpdate(
                ControllerSettingsUpdate update);

        void onStreamAudioSettingsUpdate(
                StreamAudioSettingsUpdate update);
    }
}
