package com.limelight.ui.gamemenu;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.limelight.R;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.SeekBarValueRange;

import java.util.Objects;

/**
 * Edits live physical-controller policy through typed settings intents.
 */
public final class GameDisplayDeviceFragment
        extends BaseGameMenuDialog
        implements SeekBar.OnSeekBarChangeListener {
    private static final SeekBarValueRange STRENGTH_RANGE =
            new SeekBarValueRange(10, 255);
    private static final SeekBarValueRange FREQUENCY_RANGE =
            new SeekBarValueRange(5, 15);
    private static final SeekBarValueRange POSITION_RANGE =
            new SeekBarValueRange(10, 255);

    private int titleRes = R.string.game_menu_devices_title;
    private ControllerSettings settings;
    private Listener listener;

    private CheckBox claimAllUsbDevices;
    private CheckBox flipGripRumble;
    private CheckBox ignoreTriggerDeadzone;
    private CheckBox forceDeviceRumble;
    private CheckBox useDeviceGyroscope;
    private CheckBox joyConCompatibility;
    private CheckBox reportBattery;
    private CheckBox usbGyroscope;
    private CheckBox linkTriggerRumble;
    private RadioGroup adaptiveTriggerMode;
    private SeekBar triggerStrength;
    private SeekBar triggerFrequency;
    private SeekBar triggerStart;
    private SeekBar triggerEnd;
    private TextView triggerStrengthValue;
    private TextView triggerFrequencyValue;
    private TextView triggerStartValue;
    private TextView triggerEndValue;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_control;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        requireSettings();
        bindControls(view);
        configureSeekBars();

        ((TextView) view.findViewById(R.id.tx_title))
                .setText(titleRes);
        ((Button) view.findViewById(R.id.btn_right))
                .setText(R.string.game_menu_apply_configuration);

        renderSettings();
        bindControlListeners();

        view.findViewById(R.id.ibtn_back)
                .setOnClickListener(clickedView -> dismiss());
        view.findViewById(R.id.btn_right)
                .setOnClickListener(clickedView -> {
                    if (listener != null) {
                        listener.onApplyAdaptiveTrigger();
                    }
                });
    }

    public void setTitle(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    public void setSettings(ControllerSettings settings) {
        this.settings = Objects.requireNonNull(
                settings,
                "settings");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void requireSettings() {
        if (settings == null) {
            throw new IllegalStateException(
                    "Controller settings must be supplied before showing");
        }
    }

    private void bindControls(View view) {
        claimAllUsbDevices =
                view.findViewById(R.id.btn_game_usb);
        flipGripRumble =
                view.findViewById(R.id.btn_game_grip);
        ignoreTriggerDeadzone =
                view.findViewById(R.id.btn_game_trigger);
        forceDeviceRumble =
                view.findViewById(R.id.btn_game_shake);
        useDeviceGyroscope =
                view.findViewById(R.id.btn_game_gyroscope);
        joyConCompatibility =
                view.findViewById(R.id.btn_game_joycon);
        reportBattery =
                view.findViewById(R.id.btn_game_battery);
        usbGyroscope =
                view.findViewById(R.id.btn_game_usb_gyroscope);
        linkTriggerRumble =
                view.findViewById(R.id.btn_game_trigger_rumble);
        adaptiveTriggerMode =
                view.findViewById(R.id.rg_game_control_ds5);
        triggerStrength =
                view.findViewById(
                        R.id.sb_game_control_ds5_strength);
        triggerFrequency =
                view.findViewById(
                        R.id.sb_game_control_ds5_frequency);
        triggerStart =
                view.findViewById(
                        R.id.sb_game_control_ds5_start);
        triggerEnd =
                view.findViewById(
                        R.id.sb_game_control_ds5_end);
        triggerStrengthValue =
                view.findViewById(
                        R.id.tx_game_control_ds5_strength);
        triggerFrequencyValue =
                view.findViewById(
                        R.id.tx_game_control_ds5_frequency);
        triggerStartValue =
                view.findViewById(
                        R.id.tx_game_control_ds5_start);
        triggerEndValue =
                view.findViewById(
                        R.id.tx_game_control_ds5_end);
    }

    private void configureSeekBars() {
        triggerStrength.setMax(
                STRENGTH_RANGE.getProgressMaximum());
        triggerFrequency.setMax(
                FREQUENCY_RANGE.getProgressMaximum());
        triggerStart.setMax(
                POSITION_RANGE.getProgressMaximum());
        triggerEnd.setMax(
                POSITION_RANGE.getProgressMaximum());
    }

    private void renderSettings() {
        claimAllUsbDevices.setChecked(
                settings.shouldClaimAllUsbDevices());
        flipGripRumble.setChecked(
                settings.areRumbleMotorsFlipped());
        ignoreTriggerDeadzone.setChecked(
                settings.isTriggerDeadzoneDisabled());
        forceDeviceRumble.setChecked(
                settings.isDeviceRumbleEnabled());
        useDeviceGyroscope.setChecked(
                settings.isVirtualControllerMotionEnabled());
        joyConCompatibility.setChecked(
                settings.isJoyConFixEnabled());
        reportBattery.setChecked(
                settings.isBatteryReportingEnabled());
        usbGyroscope.setChecked(
                settings.isUsbGyroscopeReportingEnabled());
        linkTriggerRumble.setChecked(
                settings.isTriggerRumbleLinkEnabled());

        checkAdaptiveTriggerMode(
                settings.getAdaptiveTriggerMode());
        setSeekBarValue(
                triggerStrength,
                STRENGTH_RANGE,
                settings.getAdaptiveTriggerStrength());
        setSeekBarValue(
                triggerFrequency,
                FREQUENCY_RANGE,
                settings.getAdaptiveTriggerFrequency());
        setSeekBarValue(
                triggerStart,
                POSITION_RANGE,
                settings.getAdaptiveTriggerStartPosition());
        setSeekBarValue(
                triggerEnd,
                POSITION_RANGE,
                settings.getAdaptiveTriggerEndPosition());
        updateTriggerValueLabels();
    }

    private void checkAdaptiveTriggerMode(int mode) {
        if (mode == 1) {
            adaptiveTriggerMode.check(
                    R.id.rbt_game_control_ds5_2);
        }
        else if (mode == 2) {
            adaptiveTriggerMode.check(
                    R.id.rbt_game_control_ds5_3);
        }
        else if (mode == 6) {
            adaptiveTriggerMode.check(
                    R.id.rbt_game_control_ds5_4);
        }
        else {
            adaptiveTriggerMode.check(
                    R.id.rbt_game_control_ds5_1);
        }
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

    private void bindControlListeners() {
        claimAllUsbDevices.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .claimAllUsbDevices(checked)));
        flipGripRumble.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .rumbleMotorsFlipped(checked)));
        ignoreTriggerDeadzone.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .triggerDeadzoneDisabled(checked)));
        forceDeviceRumble.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .deviceRumbleEnabled(checked)));
        useDeviceGyroscope.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .virtualControllerMotionEnabled(
                                        checked)));
        joyConCompatibility.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .joyConFixEnabled(checked)));
        reportBattery.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .batteryReportingEnabled(checked)));
        usbGyroscope.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .usbGyroscopeReportingEnabled(
                                        checked)));
        linkTriggerRumble.setOnCheckedChangeListener(
                (button, checked) -> dispatch(
                        ControllerSettingsUpdate
                                .triggerRumbleLinkEnabled(checked)));

        triggerStrength.setOnSeekBarChangeListener(this);
        triggerFrequency.setOnSeekBarChangeListener(this);
        triggerStart.setOnSeekBarChangeListener(this);
        triggerEnd.setOnSeekBarChangeListener(this);
        adaptiveTriggerMode.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_control_ds5_2) {
                        dispatch(
                                ControllerSettingsUpdate
                                        .adaptiveTriggerMode(1));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_control_ds5_3) {
                        dispatch(
                                ControllerSettingsUpdate
                                        .adaptiveTriggerMode(2));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_control_ds5_4) {
                        dispatch(
                                ControllerSettingsUpdate
                                        .adaptiveTriggerMode(6));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_control_ds5_1) {
                        dispatch(
                                ControllerSettingsUpdate
                                        .adaptiveTriggerMode(0));
                    }
                });
    }

    private void updateTriggerValueLabels() {
        triggerStrengthValue.setText(getString(
                R.string.game_menu_integer_value,
                settings.getAdaptiveTriggerStrength()));
        triggerFrequencyValue.setText(getString(
                R.string.game_menu_integer_value,
                settings.getAdaptiveTriggerFrequency()));
        triggerStartValue.setText(getString(
                R.string.game_menu_integer_value,
                settings.getAdaptiveTriggerStartPosition()));
        triggerEndValue.setText(getString(
                R.string.game_menu_integer_value,
                settings.getAdaptiveTriggerEndPosition()));
    }

    @Override
    public void onProgressChanged(
            SeekBar seekBar,
            int progress,
            boolean fromUser) {
        if (!fromUser) {
            return;
        }

        if (seekBar == triggerStrength) {
            dispatch(ControllerSettingsUpdate
                    .adaptiveTriggerStrength(
                            STRENGTH_RANGE
                                    .progressToValue(progress)));
        }
        else if (seekBar == triggerFrequency) {
            dispatch(ControllerSettingsUpdate
                    .adaptiveTriggerFrequency(
                            FREQUENCY_RANGE
                                    .progressToValue(progress)));
        }
        else if (seekBar == triggerStart) {
            dispatch(ControllerSettingsUpdate
                    .adaptiveTriggerStartPosition(
                            POSITION_RANGE
                                    .progressToValue(progress)));
        }
        else if (seekBar == triggerEnd) {
            dispatch(ControllerSettingsUpdate
                    .adaptiveTriggerEndPosition(
                            POSITION_RANGE
                                    .progressToValue(progress)));
        }
        updateTriggerValueLabels();
    }

    private void dispatch(ControllerSettingsUpdate update) {
        settings = update.applyTo(settings);
        if (listener != null) {
            listener.onControllerSettingsUpdate(update);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public interface Listener {
        void onApplyAdaptiveTrigger();

        void onControllerSettingsUpdate(
                ControllerSettingsUpdate update);
    }
}
