package com.limelight.ui.gamemenu;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;

public class GameDisplayDeviceFragment
        extends BaseGameMenuDialog
        implements SeekBar.OnSeekBarChangeListener {
    private static final String KEY_USB_DRIVER =
            "checkbox_usb_driver";
    private static final String KEY_USB_BIND_ALL =
            "checkbox_usb_bind_all";
    private static final String KEY_FLIP_RUMBLE =
            "checkbox_flip_rumble_ff";
    private static final String KEY_DISABLE_TRIGGER_DEADZONE =
            "checkbox_disable_trigger_deadzone";
    private static final String KEY_DEVICE_RUMBLE =
            "checkbox_enable_device_rumble";
    private static final String KEY_VIRTUAL_MOTION =
            "checkbox_enable_virtual_motion";
    private static final String KEY_JOYCON_FIX =
            "checkbox_enable_joyconfix";
    private static final String KEY_BATTERY_REPORT =
            "checkbox_gamepad_enable_battery_report";
    private static final String KEY_USB_GYROSCOPE =
            "usbGyroscopeReport";
    private static final String KEY_TRIGGER_RUMBLE_LINK =
            "gameTriggerRumbleLink";
    private static final String KEY_TRIGGER_MODE =
            "ds5TriggerMode";
    private static final String KEY_TRIGGER_STRENGTH =
            "ds5TriggerStrength";
    private static final String KEY_TRIGGER_FREQUENCY =
            "ds5TriggerFrequency";
    private static final String KEY_TRIGGER_START =
            "ds5TriggerStart";
    private static final String KEY_TRIGGER_END =
            "ds5TriggerEnd";

    private static final SeekBarValueRange STRENGTH_RANGE =
            new SeekBarValueRange(10, 255);
    private static final SeekBarValueRange FREQUENCY_RANGE =
            new SeekBarValueRange(5, 15);
    private static final SeekBarValueRange POSITION_RANGE =
            new SeekBarValueRange(10, 255);

    private int titleRes = R.string.game_menu_devices_title;
    private PreferenceConfiguration prefConfig =
            new PreferenceConfiguration();
    private Listener listener;

    private CheckBox usbDriver;
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
        bindControls(view);
        configureSeekBars();

        ((TextView) view.findViewById(R.id.tx_title))
                .setText(titleRes);
        ((Button) view.findViewById(R.id.btn_right))
                .setText(R.string.game_menu_apply_configuration);

        initializeControls();
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

    private void bindControls(View view) {
        usbDriver = view.findViewById(R.id.btn_game_usb);
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
                view.findViewById(R.id.sb_game_control_ds5_strength);
        triggerFrequency =
                view.findViewById(R.id.sb_game_control_ds5_frequency);
        triggerStart =
                view.findViewById(R.id.sb_game_control_ds5_start);
        triggerEnd =
                view.findViewById(R.id.sb_game_control_ds5_end);
        triggerStrengthValue =
                view.findViewById(R.id.tx_game_control_ds5_strength);
        triggerFrequencyValue =
                view.findViewById(R.id.tx_game_control_ds5_frequency);
        triggerStartValue =
                view.findViewById(R.id.tx_game_control_ds5_start);
        triggerEndValue =
                view.findViewById(R.id.tx_game_control_ds5_end);
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

    private void initializeControls() {
        usbDriver.setChecked(prefConfig.bindAllUsb);
        flipGripRumble.setChecked(
                prefConfig.enableFlipRumbleFF);
        ignoreTriggerDeadzone.setChecked(
                prefConfig.disableTriggerDeadzone);
        forceDeviceRumble.setChecked(
                prefConfig.enableDeviceRumble);
        useDeviceGyroscope.setChecked(
                prefConfig.enableVirtualControllerMotion);
        joyConCompatibility.setChecked(
                prefConfig.enableJoyConFix);
        reportBattery.setChecked(
                prefConfig.enableBatteryReport);
        usbGyroscope.setChecked(
                prefConfig.usbGyroscopeReport);
        linkTriggerRumble.setChecked(
                prefConfig.gameTriggerRumbleLink);

        checkAdaptiveTriggerMode(prefConfig.ds5TriggerMode);
        setSeekBarValue(
                triggerStrength,
                STRENGTH_RANGE,
                prefConfig.ds5TriggerStrength);
        setSeekBarValue(
                triggerFrequency,
                FREQUENCY_RANGE,
                prefConfig.ds5TriggerFrequency);
        setSeekBarValue(
                triggerStart,
                POSITION_RANGE,
                prefConfig.ds5TriggerStart);
        setSeekBarValue(
                triggerEnd,
                POSITION_RANGE,
                prefConfig.ds5TriggerEnd);
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
        seekBar.setProgress(range.valueToProgress(value));
    }

    private void bindControlListeners() {
        usbDriver.setOnCheckedChangeListener(
                (button, checked) -> {
                    if (checked) {
                        prefConfig.usbDriver = true;
                        saveBoolean(KEY_USB_DRIVER, true);
                    }
                    prefConfig.bindAllUsb = checked;
                    saveBoolean(KEY_USB_BIND_ALL, checked);
                });
        flipGripRumble.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.enableFlipRumbleFF = checked;
                    saveBoolean(KEY_FLIP_RUMBLE, checked);
                });
        ignoreTriggerDeadzone.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.disableTriggerDeadzone = checked;
                    saveBoolean(
                            KEY_DISABLE_TRIGGER_DEADZONE,
                            checked);
                });
        forceDeviceRumble.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.enableDeviceRumble = checked;
                    saveBoolean(KEY_DEVICE_RUMBLE, checked);
                });
        useDeviceGyroscope.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.enableVirtualControllerMotion =
                            checked;
                    saveBoolean(KEY_VIRTUAL_MOTION, checked);
                });
        joyConCompatibility.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.enableJoyConFix = checked;
                    saveBoolean(KEY_JOYCON_FIX, checked);
                });
        reportBattery.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.enableBatteryReport = checked;
                    saveBoolean(KEY_BATTERY_REPORT, checked);
                });
        usbGyroscope.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.usbGyroscopeReport = checked;
                    saveBoolean(KEY_USB_GYROSCOPE, checked);
                });
        linkTriggerRumble.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefConfig.gameTriggerRumbleLink = checked;
                    saveBoolean(
                            KEY_TRIGGER_RUMBLE_LINK,
                            checked);
                });

        triggerStrength.setOnSeekBarChangeListener(this);
        triggerFrequency.setOnSeekBarChangeListener(this);
        triggerStart.setOnSeekBarChangeListener(this);
        triggerEnd.setOnSeekBarChangeListener(this);
        adaptiveTriggerMode.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_control_ds5_2) {
                        saveAdaptiveTriggerMode(1);
                    }
                    else if (checkedId ==
                            R.id.rbt_game_control_ds5_3) {
                        saveAdaptiveTriggerMode(2);
                    }
                    else if (checkedId ==
                            R.id.rbt_game_control_ds5_4) {
                        saveAdaptiveTriggerMode(6);
                    }
                    else if (checkedId ==
                            R.id.rbt_game_control_ds5_1) {
                        saveAdaptiveTriggerMode(0);
                    }
                });
    }

    private void saveAdaptiveTriggerMode(int mode) {
        prefConfig.ds5TriggerMode = mode;
        saveInteger(KEY_TRIGGER_MODE, mode);
    }

    private void updateTriggerValueLabels() {
        triggerStrengthValue.setText(getString(
                R.string.game_menu_integer_value,
                STRENGTH_RANGE.progressToValue(
                        triggerStrength.getProgress())));
        triggerFrequencyValue.setText(getString(
                R.string.game_menu_integer_value,
                FREQUENCY_RANGE.progressToValue(
                        triggerFrequency.getProgress())));
        triggerStartValue.setText(getString(
                R.string.game_menu_integer_value,
                POSITION_RANGE.progressToValue(
                        triggerStart.getProgress())));
        triggerEndValue.setText(getString(
                R.string.game_menu_integer_value,
                POSITION_RANGE.progressToValue(
                        triggerEnd.getProgress())));
    }

    private void saveBoolean(String key, boolean value) {
        preferences().edit()
                .putBoolean(key, value)
                .apply();
    }

    private void saveInteger(String key, int value) {
        preferences().edit()
                .putInt(key, value)
                .apply();
    }

    private SharedPreferences preferences() {
        return PreferenceManager.getDefaultSharedPreferences(
                getActivity());
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
            int value =
                    STRENGTH_RANGE.progressToValue(progress);
            prefConfig.ds5TriggerStrength = value;
            saveInteger(KEY_TRIGGER_STRENGTH, value);
            triggerStrengthValue.setText(getString(
                    R.string.game_menu_integer_value, value));
        }
        else if (seekBar == triggerFrequency) {
            int value =
                    FREQUENCY_RANGE.progressToValue(progress);
            prefConfig.ds5TriggerFrequency = value;
            saveInteger(KEY_TRIGGER_FREQUENCY, value);
            triggerFrequencyValue.setText(getString(
                    R.string.game_menu_integer_value, value));
        }
        else if (seekBar == triggerStart) {
            int value =
                    POSITION_RANGE.progressToValue(progress);
            prefConfig.ds5TriggerStart = value;
            saveInteger(KEY_TRIGGER_START, value);
            triggerStartValue.setText(getString(
                    R.string.game_menu_integer_value, value));
        }
        else if (seekBar == triggerEnd) {
            int value =
                    POSITION_RANGE.progressToValue(progress);
            prefConfig.ds5TriggerEnd = value;
            saveInteger(KEY_TRIGGER_END, value);
            triggerEndValue.setText(getString(
                    R.string.game_menu_integer_value, value));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void setTitle(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    public void setPrefConfig(
            PreferenceConfiguration prefConfig) {
        if (prefConfig != null) {
            this.prefConfig = prefConfig;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onApplyAdaptiveTrigger();
    }
}
