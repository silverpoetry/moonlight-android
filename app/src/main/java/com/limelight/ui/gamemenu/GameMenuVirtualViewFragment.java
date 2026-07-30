package com.limelight.ui.gamemenu;

import androidx.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.SeekBarValueRange;
import com.limelight.utils.UiToast;

public class GameMenuVirtualViewFragment
        extends BaseGameMenuDialog
        implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    private static final int COLOR_BLACK = 0xF0000000;
    private static final int COLOR_WHITE = 0xF0FFFFFF;
    private static final int COLOR_GRAY = 0xFF888888;

    private static final SeekBarValueRange OPACITY_RANGE =
            new SeekBarValueRange(0, 100);
    private static final SeekBarValueRange KEYBOARD_HEIGHT_RANGE =
            new SeekBarValueRange(100, 400);
    private static final SeekBarValueRange GAMEPAD_SCALE_RANGE =
            new SeekBarValueRange(20, 180);

    private enum LayoutKind {
        KEYBOARD,
        GAMEPAD
    }

    private static final int[] GAMEPAD_SCHEME_BUTTONS = {
            R.id.btn_game_virtual_game_scheme_1,
            R.id.btn_game_virtual_game_scheme_2,
            R.id.btn_game_virtual_game_scheme_3,
            R.id.btn_game_virtual_game_scheme_4,
            R.id.btn_game_virtual_game_scheme_5
    };
    private static final int[] KEY_SCHEME_BUTTONS = {
            R.id.btn_game_virtual_key_scheme_1,
            R.id.btn_game_virtual_key_scheme_2,
            R.id.btn_game_virtual_key_scheme_3,
            R.id.btn_game_virtual_key_scheme_4,
            R.id.btn_game_virtual_key_scheme_5
    };

    private int titleRes = R.string.game_menu_virtual_controls_title;
    private VirtualControlSettings settings =
            VirtualControlSettings.builder().build();
    private boolean onscreenControllerRumbleEnabled;
    private KeyBoardController.ControllerMode gamePadMode =
            KeyBoardController.ControllerMode.NONE;
    private KeyBoardController.ControllerMode gameKeyMode =
            KeyBoardController.ControllerMode.NONE;
    private Listener listener;

    private Button keyboardVibrationButton;
    private Button gamepadVibrationButton;
    private SeekBar keyboardOpacitySeekBar;
    private SeekBar keyboardHeightSeekBar;
    private SeekBar controlOpacitySeekBar;
    private SeekBar gamepadScaleSeekBar;
    private TextView keyboardOpacityValue;
    private TextView keyboardHeightValue;
    private TextView controlOpacityValue;
    private TextView gamepadScaleValue;
    private RadioGroup gamepadModeGroup;
    private RadioGroup keyModeGroup;
    private RadioGroup gamepadSchemeGroup;
    private RadioGroup keySchemeGroup;
    private RadioGroup controlColorGroup;
    private String[] gamepadSchemeValues;
    private String[] keySchemeValues;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_virtual_gamepad;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        bindControls(view);
        configureSeekBars();

        ((TextView) view.findViewById(R.id.tx_title))
                .setText(titleRes);
        ((Button) view.findViewById(R.id.btn_right))
                .setText(R.string.game_menu_update_configuration);

        initializeModeSelections();
        initializeSchemeSelections();
        initializeControlColor();
        updateVibrationButtons();
        updateSeekBarValues();

        view.findViewById(R.id.ibtn_back).setOnClickListener(this);
        view.findViewById(R.id.btn_right).setOnClickListener(this);
        keyboardVibrationButton.setOnClickListener(this);
        gamepadVibrationButton.setOnClickListener(this);
        controlOpacitySeekBar.setOnSeekBarChangeListener(this);
        keyboardOpacitySeekBar.setOnSeekBarChangeListener(this);
        keyboardHeightSeekBar.setOnSeekBarChangeListener(this);
        gamepadScaleSeekBar.setOnSeekBarChangeListener(this);

        bindModeListeners();
        bindSchemeListeners();
        bindColorListener();
    }

    private void bindControls(View view) {
        keyboardVibrationButton =
                view.findViewById(R.id.btn_vibration);
        gamepadVibrationButton =
                view.findViewById(R.id.btn_vibration_gamepad);
        keyboardOpacitySeekBar =
                view.findViewById(R.id.sb_adjust_keyboard_all);
        keyboardHeightSeekBar =
                view.findViewById(R.id.sb_height_keyboard_all);
        controlOpacitySeekBar =
                view.findViewById(R.id.sb_adjust_virtual_gamepad);
        gamepadScaleSeekBar =
                view.findViewById(R.id.sb_gamepad_scale_factor);
        keyboardOpacityValue =
                view.findViewById(R.id.tx_adjust_keyboard_all);
        keyboardHeightValue =
                view.findViewById(R.id.tx_height_keyboard_all);
        controlOpacityValue =
                view.findViewById(R.id.tx_adjust_virtual_gamepad);
        gamepadScaleValue =
                view.findViewById(R.id.tx_gamepad_scale_factor);
        gamepadModeGroup =
                view.findViewById(R.id.rg_game_virtual_pad);
        keyModeGroup =
                view.findViewById(R.id.rg_game_virtual_key);
        gamepadSchemeGroup =
                view.findViewById(R.id.rg_game_virtual_game_scheme);
        keySchemeGroup =
                view.findViewById(R.id.rg_game_virtual_key_scheme);
        controlColorGroup =
                view.findViewById(R.id.rg_game_virtual_key_color);
    }

    private void configureSeekBars() {
        controlOpacitySeekBar.setMax(
                OPACITY_RANGE.getProgressMaximum());
        keyboardOpacitySeekBar.setMax(
                OPACITY_RANGE.getProgressMaximum());
        keyboardHeightSeekBar.setMax(
                KEYBOARD_HEIGHT_RANGE.getProgressMaximum());
        gamepadScaleSeekBar.setMax(
                GAMEPAD_SCALE_RANGE.getProgressMaximum());
    }

    private void initializeModeSelections() {
        if (gamePadMode ==
                KeyBoardController.ControllerMode.Active) {
            gamepadModeGroup.check(
                    R.id.btn_game_virtual_nomall);
        }
        else if (gamePadMode ==
                KeyBoardController.ControllerMode.MoveButtons) {
            gamepadModeGroup.check(
                    R.id.btn_game_virtual_move);
        }

        if (gameKeyMode ==
                KeyBoardController.ControllerMode.Active) {
            keyModeGroup.check(
                    R.id.btn_game_virtual_key_nomall);
        }
        else if (gameKeyMode ==
                KeyBoardController.ControllerMode.MoveButtons) {
            keyModeGroup.check(
                    R.id.btn_game_virtual_key_move);
        }
    }

    private void bindModeListeners() {
        gamepadModeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (listener == null) {
                        return;
                    }
                    if (checkedId ==
                            R.id.btn_game_virtual_nomall) {
                        listener.onGamepadModeSelected(
                                KeyBoardController.ControllerMode.Active);
                    }
                    else if (checkedId ==
                            R.id.btn_game_virtual_move) {
                        listener.onGamepadModeSelected(
                                KeyBoardController.ControllerMode.MoveButtons);
                        showEditModeToast();
                    }
                });

        keyModeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (listener == null) {
                        return;
                    }
                    if (checkedId ==
                            R.id.btn_game_virtual_key_nomall) {
                        listener.onVirtualKeyModeSelected(
                                KeyBoardController.ControllerMode.Active);
                    }
                    else if (checkedId ==
                            R.id.btn_game_virtual_key_move) {
                        listener.onVirtualKeyModeSelected(
                                KeyBoardController.ControllerMode.MoveButtons);
                        showEditModeToast();
                    }
                });
    }

    private void showEditModeToast() {
        UiToast.makeText(
                getActivity(),
                R.string.game_menu_edit_mode_entered,
                UiToast.LENGTH_SHORT).show();
    }

    private void initializeSchemeSelections() {
        keySchemeValues =
                getResources().getStringArray(
                        R.array.keyboard_axi_values);
        gamepadSchemeValues =
                getResources().getStringArray(
                        R.array.gamepad_axi_values);

        String selectedKeyScheme = settings.getKeyboardLayoutId();
        checkSavedScheme(
                keySchemeGroup,
                KEY_SCHEME_BUTTONS,
                keySchemeValues,
                selectedKeyScheme);

        String selectedGamepadScheme = settings.getGamepadLayoutId();
        checkSavedScheme(
                gamepadSchemeGroup,
                GAMEPAD_SCHEME_BUTTONS,
                gamepadSchemeValues,
                selectedGamepadScheme);
    }

    private static void checkSavedScheme(
            RadioGroup group,
            int[] buttonIds,
            String[] values,
            String selectedValue) {
        int count = Math.min(buttonIds.length, values.length);
        for (int index = 0; index < count; index++) {
            if (TextUtils.equals(selectedValue, values[index])) {
                group.check(buttonIds[index]);
                return;
            }
        }
    }

    private void bindSchemeListeners() {
        keySchemeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> saveSelectedScheme(
                        LayoutKind.KEYBOARD,
                        KEY_SCHEME_BUTTONS,
                        keySchemeValues,
                        checkedId));
        gamepadSchemeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> saveSelectedScheme(
                        LayoutKind.GAMEPAD,
                        GAMEPAD_SCHEME_BUTTONS,
                        gamepadSchemeValues,
                        checkedId));
    }

    private void saveSelectedScheme(
            LayoutKind kind,
            int[] buttonIds,
            String[] values,
            int checkedId) {
        int index = indexOf(buttonIds, checkedId);
        if (index < 0 || index >= values.length) {
            return;
        }
        if (kind == LayoutKind.KEYBOARD) {
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .keyboardLayoutId(values[index]));
        }
        else {
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .gamepadLayoutId(values[index]));
        }
    }

    private static int indexOf(int[] values, int target) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == target) {
                return index;
            }
        }
        return -1;
    }

    private void initializeControlColor() {
        switch (settings.getNormalColor()) {
            case COLOR_BLACK:
                controlColorGroup.check(
                        R.id.btn_game_virtual_key_color_1);
                break;
            case COLOR_WHITE:
                controlColorGroup.check(
                        R.id.btn_game_virtual_key_color_2);
                break;
            default:
                controlColorGroup.check(
                        R.id.btn_game_virtual_key_color_3);
                break;
        }
    }

    private void bindColorListener() {
        controlColorGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    int color = COLOR_GRAY;
                    if (checkedId ==
                            R.id.btn_game_virtual_key_color_1) {
                        color = COLOR_BLACK;
                    }
                    else if (checkedId ==
                            R.id.btn_game_virtual_key_color_2) {
                        color = COLOR_WHITE;
                    }
                    publishUpdate(
                            VirtualControlSettingsUpdate
                                    .normalColor(color));
                });
    }

    private void updateVibrationButtons() {
        keyboardVibrationButton.setBackgroundResource(
                settings.isKeyboardHapticsEnabled() ?
                        R.drawable.ic_game_menu_btn_green_selector :
                        R.drawable.ic_game_menu_btn_selector);
        gamepadVibrationButton.setBackgroundResource(
                onscreenControllerRumbleEnabled ?
                        R.drawable.ic_game_menu_btn_green_selector :
                        R.drawable.ic_game_menu_btn_selector);
    }

    private void updateSeekBarValues() {
        setSeekBarValue(
                controlOpacitySeekBar,
                OPACITY_RANGE,
                settings.getControlOpacityPercent());
        setSeekBarValue(
                keyboardOpacitySeekBar,
                OPACITY_RANGE,
                settings.getKeyboardOpacityPercent());
        setSeekBarValue(
                keyboardHeightSeekBar,
                KEYBOARD_HEIGHT_RANGE,
                settings.getKeyboardHeightDp());
        setSeekBarValue(
                gamepadScaleSeekBar,
                GAMEPAD_SCALE_RANGE,
                settings.getGamepadScalePercent());

        controlOpacityValue.setText(getString(
                R.string.game_menu_opacity_format,
                OPACITY_RANGE.progressToValue(
                        controlOpacitySeekBar.getProgress())));
        keyboardOpacityValue.setText(getString(
                R.string.game_menu_opacity_format,
                OPACITY_RANGE.progressToValue(
                        keyboardOpacitySeekBar.getProgress())));
        keyboardHeightValue.setText(getString(
                R.string.game_menu_height_format,
                KEYBOARD_HEIGHT_RANGE.progressToValue(
                        keyboardHeightSeekBar.getProgress())));
        gamepadScaleValue.setText(getString(
                R.string.game_menu_scale_format,
                GAMEPAD_SCALE_RANGE.progressToValue(
                        gamepadScaleSeekBar.getProgress())));
    }

    private static void setSeekBarValue(
            SeekBar seekBar,
            SeekBarValueRange range,
            int value) {
        seekBar.setProgress(range.valueToProgress(value));
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.ibtn_back) {
            dismiss();
            return;
        }
        if (viewId == R.id.btn_right) {
            if (listener != null) {
                listener.onRefreshRequested();
            }
            return;
        }
        if (viewId == R.id.btn_vibration) {
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .keyboardHapticsEnabled(
                                    !settings
                                            .isKeyboardHapticsEnabled()));
            updateVibrationButtons();
            return;
        }
        if (viewId == R.id.btn_vibration_gamepad) {
            onscreenControllerRumbleEnabled =
                    !onscreenControllerRumbleEnabled;
            if (listener != null) {
                listener.onOnscreenControllerRumbleChanged(
                        onscreenControllerRumbleEnabled);
            }
            updateVibrationButtons();
        }
    }

    @Override
    public void onProgressChanged(
            SeekBar seekBar,
            int progress,
            boolean fromUser) {
        if (!fromUser) {
            return;
        }

        if (seekBar == controlOpacitySeekBar) {
            int value = OPACITY_RANGE.progressToValue(progress);
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .controlOpacityPercent(value));
            controlOpacityValue.setText(getString(
                    R.string.game_menu_opacity_format, value));
        }
        else if (seekBar == keyboardOpacitySeekBar) {
            int value = OPACITY_RANGE.progressToValue(progress);
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .keyboardOpacityPercent(value));
            keyboardOpacityValue.setText(getString(
                    R.string.game_menu_opacity_format, value));
        }
        else if (seekBar == keyboardHeightSeekBar) {
            int value =
                    KEYBOARD_HEIGHT_RANGE.progressToValue(progress);
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .keyboardHeightDp(value));
            keyboardHeightValue.setText(getString(
                    R.string.game_menu_height_format, value));
        }
        else if (seekBar == gamepadScaleSeekBar) {
            int value =
                    GAMEPAD_SCALE_RANGE.progressToValue(progress);
            publishUpdate(
                    VirtualControlSettingsUpdate
                            .gamepadScalePercent(value));
            gamepadScaleValue.setText(getString(
                    R.string.game_menu_scale_format, value));
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

    public void setGameKeyMode(
            KeyBoardController.ControllerMode gameKeyMode) {
        this.gameKeyMode = gameKeyMode;
    }

    public void setGamePadMode(
            KeyBoardController.ControllerMode gamePadMode) {
        this.gamePadMode = gamePadMode;
    }

    public void setSettings(VirtualControlSettings settings) {
        if (settings != null) {
            this.settings = settings;
        }
    }

    public void setOnscreenControllerRumbleEnabled(
            boolean enabled) {
        onscreenControllerRumbleEnabled = enabled;
    }

    private void publishUpdate(
            VirtualControlSettingsUpdate<?> update) {
        settings = update.applyTo(settings);
        if (listener != null) {
            listener.onVirtualControlSettingsUpdate(update);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onRefreshRequested();

        void onVirtualControlSettingsUpdate(
                VirtualControlSettingsUpdate<?> update);

        void onOnscreenControllerRumbleChanged(boolean enabled);

        void onGamepadModeSelected(
                KeyBoardController.ControllerMode mode);

        void onVirtualKeyModeSelected(
                KeyBoardController.ControllerMode mode);
    }
}
