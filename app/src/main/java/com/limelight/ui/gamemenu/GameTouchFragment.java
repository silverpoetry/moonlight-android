package com.limelight.ui.gamemenu;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.SeekBarValueRange;

import java.util.Objects;

/**
 * Edits live input sensitivity through typed settings intents.
 */
public final class GameTouchFragment
        extends BaseGameMenuDialog
        implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    public interface Listener {
        void onInputSettingsUpdate(InputSettingsUpdate update);

        void onControllerSettingsUpdate(
                ControllerSettingsUpdate update);
    }

    private static final SeekBarValueRange MULTITOUCH_RANGE =
            new SeekBarValueRange(10, 800);
    private static final SeekBarValueRange SENSITIVITY_RANGE =
            new SeekBarValueRange(10, 300);
    private static final SeekBarValueRange DISTANCE_RANGE =
            new SeekBarValueRange(1, 30);

    private ImageButton backButton;
    private TextView titleView;
    private Button directTouchToggle;
    private Button recenterToggle;
    private Button globalSensitivityToggle;
    private SeekBar directTouchX;
    private SeekBar directTouchY;
    private SeekBar touchpadPointerX;
    private SeekBar touchpadPointerY;
    private SeekBar virtualTouchpadX;
    private SeekBar virtualTouchpadY;
    private TextView directTouchXValue;
    private TextView directTouchYValue;
    private TextView touchpadPointerXValue;
    private TextView touchpadPointerYValue;
    private TextView virtualTouchpadXValue;
    private TextView virtualTouchpadYValue;
    private SeekBar controllerMouseSensitivity;
    private TextView controllerMouseSensitivityValue;
    private SeekBar mouseWheelAmount;
    private TextView mouseWheelAmountValue;
    private SeekBar externalTouchpadX;
    private SeekBar externalTouchpadY;
    private SeekBar externalTouchpadScrollAmount;
    private TextView externalTouchpadXValue;
    private TextView externalTouchpadYValue;
    private TextView externalTouchpadScrollAmountValue;

    private String title;
    private InputSettings inputSettings;
    private ControllerSettings controllerSettings;
    private Listener listener;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_touch;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        requireSettings();

        backButton = view.findViewById(R.id.ibtn_back);
        titleView = view.findViewById(R.id.tx_title);
        directTouchToggle =
                view.findViewById(R.id.btn_touch_switch);
        recenterToggle =
                view.findViewById(R.id.btn_touch_center);
        globalSensitivityToggle =
                view.findViewById(R.id.btn_touch_all);
        directTouchX = view.findViewById(R.id.sb_touch_x);
        directTouchY = view.findViewById(R.id.sb_touch_y);
        touchpadPointerX =
                view.findViewById(R.id.sb_touchpad_x);
        touchpadPointerY =
                view.findViewById(R.id.sb_touchpad_y);
        virtualTouchpadX =
                view.findViewById(R.id.sb_touchpad_view_x);
        virtualTouchpadY =
                view.findViewById(R.id.sb_touchpad_view_y);
        directTouchXValue =
                view.findViewById(R.id.tx_touch_x);
        directTouchYValue =
                view.findViewById(R.id.tx_touch_y);
        touchpadPointerXValue =
                view.findViewById(R.id.tx_touchpad_x);
        touchpadPointerYValue =
                view.findViewById(R.id.tx_touchpad_y);
        virtualTouchpadXValue =
                view.findViewById(R.id.tx_touchpad_view_x);
        virtualTouchpadYValue =
                view.findViewById(R.id.tx_touchpad_view_y);
        controllerMouseSensitivity =
                view.findViewById(
                        R.id.sb_mouse_gamepad_sensitity);
        controllerMouseSensitivityValue =
                view.findViewById(
                        R.id.tx_mouse_gamepad_sensitity);
        mouseWheelAmount =
                view.findViewById(R.id.sb_mouse_sc_amount);
        mouseWheelAmountValue =
                view.findViewById(R.id.tx_mouse_sc_amount);
        externalTouchpadX =
                view.findViewById(
                        R.id.sb_touchpad_equipment_view_x);
        externalTouchpadY =
                view.findViewById(
                        R.id.sb_touchpad_equipment_view_y);
        externalTouchpadScrollAmount =
                view.findViewById(
                        R.id.sb_touchpad_equipment_amount);
        externalTouchpadXValue =
                view.findViewById(
                        R.id.tx_touchpad_equipment_view_x);
        externalTouchpadYValue =
                view.findViewById(
                        R.id.tx_touchpad_equipment_view_y);
        externalTouchpadScrollAmountValue =
                view.findViewById(
                        R.id.tx_touchpad_equipment_amount);

        configureSeekBars();
        titleView.setText(title);
        renderAll();

        backButton.setOnClickListener(this);
        directTouchToggle.setOnClickListener(this);
        recenterToggle.setOnClickListener(this);
        globalSensitivityToggle.setOnClickListener(this);
        view.findViewById(R.id.btn_right)
                .setOnClickListener(this);

        directTouchX.setOnSeekBarChangeListener(this);
        directTouchY.setOnSeekBarChangeListener(this);
        touchpadPointerX.setOnSeekBarChangeListener(this);
        touchpadPointerY.setOnSeekBarChangeListener(this);
        virtualTouchpadX.setOnSeekBarChangeListener(this);
        virtualTouchpadY.setOnSeekBarChangeListener(this);
        controllerMouseSensitivity
                .setOnSeekBarChangeListener(this);
        mouseWheelAmount.setOnSeekBarChangeListener(this);
        externalTouchpadX.setOnSeekBarChangeListener(this);
        externalTouchpadY.setOnSeekBarChangeListener(this);
        externalTouchpadScrollAmount
                .setOnSeekBarChangeListener(this);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSettings(
            InputSettings inputSettings,
            ControllerSettings controllerSettings) {
        this.inputSettings = Objects.requireNonNull(
                inputSettings,
                "inputSettings");
        this.controllerSettings = Objects.requireNonNull(
                controllerSettings,
                "controllerSettings");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void requireSettings() {
        if (inputSettings == null || controllerSettings == null) {
            throw new IllegalStateException(
                    "Touch settings must be supplied before showing");
        }
    }

    private void renderAll() {
        renderToggles();
        renderDirectTouch();
        renderTouchpadPointer();
        renderVirtualTouchpad();
        renderControllerMouse();
        renderMouseWheel();
        renderExternalTouchpad();
    }

    private void renderToggles() {
        directTouchToggle.setBackgroundResource(
                selector(
                        inputSettings
                                .isDirectTouchSensitivityEnabled()));
        recenterToggle.setBackgroundResource(
                selector(
                        inputSettings
                                .isDirectTouchRecenterEnabled()));
        globalSensitivityToggle.setBackgroundResource(
                selector(
                        inputSettings
                                .isDirectTouchSensitivityGlobal()));
    }

    private static int selector(boolean enabled) {
        return enabled
                ? R.drawable.ic_game_menu_btn_accent_selector
                : R.drawable.ic_game_menu_btn_selector;
    }

    private void renderDirectTouch() {
        int x = inputSettings.getDirectTouchSensitivityX();
        int y = inputSettings.getDirectTouchSensitivityY();
        setSeekBarValue(directTouchX, MULTITOUCH_RANGE, x);
        setSeekBarValue(directTouchY, MULTITOUCH_RANGE, y);
        directTouchXValue.setText(getString(
                R.string.game_menu_axis_x_format,
                x));
        directTouchYValue.setText(getString(
                R.string.game_menu_axis_y_format,
                y));
    }

    private void renderTouchpadPointer() {
        int x = inputSettings.getTouchpadPointerSensitivityX();
        int y = inputSettings.getTouchpadPointerSensitivityY();
        setSeekBarValue(
                touchpadPointerX,
                SENSITIVITY_RANGE,
                x);
        setSeekBarValue(
                touchpadPointerY,
                SENSITIVITY_RANGE,
                y);
        touchpadPointerXValue.setText(getString(
                R.string.game_menu_axis_x_format,
                x));
        touchpadPointerYValue.setText(getString(
                R.string.game_menu_axis_y_format,
                y));
    }

    private void renderVirtualTouchpad() {
        int x = inputSettings.getVirtualTouchpadSensitivityX();
        int y = inputSettings.getVirtualTouchpadSensitivityY();
        setSeekBarValue(
                virtualTouchpadX,
                SENSITIVITY_RANGE,
                x);
        setSeekBarValue(
                virtualTouchpadY,
                SENSITIVITY_RANGE,
                y);
        virtualTouchpadXValue.setText(getString(
                R.string.game_menu_axis_x_format,
                x));
        virtualTouchpadYValue.setText(getString(
                R.string.game_menu_axis_y_format,
                y));
    }

    private void renderControllerMouse() {
        int value =
                controllerSettings.getMouseSensitivityPercent();
        setSeekBarValue(
                controllerMouseSensitivity,
                SENSITIVITY_RANGE,
                value);
        controllerMouseSensitivityValue.setText(getString(
                R.string.game_menu_sensitivity_format,
                value));
    }

    private void renderMouseWheel() {
        int value = inputSettings.getMouseWheelScrollAmount();
        setSeekBarValue(
                mouseWheelAmount,
                DISTANCE_RANGE,
                value);
        mouseWheelAmountValue.setText(getString(
                R.string.game_menu_distance_format,
                value));
    }

    private void renderExternalTouchpad() {
        int x = inputSettings.getExternalTouchpadSensitivityX();
        int y = inputSettings.getExternalTouchpadSensitivityY();
        int scroll =
                inputSettings.getExternalTouchpadScrollAmount();
        setSeekBarValue(
                externalTouchpadX,
                SENSITIVITY_RANGE,
                x);
        setSeekBarValue(
                externalTouchpadY,
                SENSITIVITY_RANGE,
                y);
        setSeekBarValue(
                externalTouchpadScrollAmount,
                DISTANCE_RANGE,
                scroll);
        externalTouchpadXValue.setText(getString(
                R.string.game_menu_axis_x_format,
                x));
        externalTouchpadYValue.setText(getString(
                R.string.game_menu_axis_y_format,
                y));
        externalTouchpadScrollAmountValue.setText(getString(
                R.string.game_menu_scroll_speed_format,
                scroll));
    }

    private void configureSeekBars() {
        configureSeekBar(directTouchX, MULTITOUCH_RANGE);
        configureSeekBar(directTouchY, MULTITOUCH_RANGE);
        configureSeekBar(touchpadPointerX, SENSITIVITY_RANGE);
        configureSeekBar(touchpadPointerY, SENSITIVITY_RANGE);
        configureSeekBar(virtualTouchpadX, SENSITIVITY_RANGE);
        configureSeekBar(virtualTouchpadY, SENSITIVITY_RANGE);
        configureSeekBar(
                controllerMouseSensitivity,
                SENSITIVITY_RANGE);
        configureSeekBar(mouseWheelAmount, DISTANCE_RANGE);
        configureSeekBar(
                externalTouchpadX,
                SENSITIVITY_RANGE);
        configureSeekBar(
                externalTouchpadY,
                SENSITIVITY_RANGE);
        configureSeekBar(
                externalTouchpadScrollAmount,
                DISTANCE_RANGE);
    }

    private static void configureSeekBar(
            SeekBar seekBar,
            SeekBarValueRange range) {
        seekBar.setMax(range.getProgressMaximum());
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

    private SeekBarValueRange getRange(SeekBar seekBar) {
        if (seekBar == directTouchX || seekBar == directTouchY) {
            return MULTITOUCH_RANGE;
        }
        if (seekBar == mouseWheelAmount ||
                seekBar == externalTouchpadScrollAmount) {
            return DISTANCE_RANGE;
        }
        return SENSITIVITY_RANGE;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ibtn_back) {
            dismiss();
            return;
        }
        if (id == R.id.btn_right) {
            dispatchInput(InputSettingsUpdate.resetSensitivity());
            dispatchController(
                    ControllerSettingsUpdate
                            .mouseSensitivityPercent(100));
            renderAll();
            return;
        }
        if (id == R.id.btn_touch_switch) {
            dispatchInput(
                    InputSettingsUpdate
                            .directTouchSensitivityEnabled(
                                    !inputSettings
                                            .isDirectTouchSensitivityEnabled()));
            renderToggles();
            return;
        }
        if (id == R.id.btn_touch_center) {
            dispatchInput(
                    InputSettingsUpdate
                            .directTouchRecenterEnabled(
                                    !inputSettings
                                            .isDirectTouchRecenterEnabled()));
            renderToggles();
            return;
        }
        if (id == R.id.btn_touch_all) {
            dispatchInput(
                    InputSettingsUpdate
                            .directTouchSensitivityGlobal(
                                    !inputSettings
                                            .isDirectTouchSensitivityGlobal()));
            renderToggles();
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
        int value = getRange(seekBar).progressToValue(progress);
        if (seekBar == directTouchX) {
            dispatchInput(
                    InputSettingsUpdate
                            .directTouchSensitivityX(value));
            renderDirectTouch();
        }
        else if (seekBar == directTouchY) {
            dispatchInput(
                    InputSettingsUpdate
                            .directTouchSensitivityY(value));
            renderDirectTouch();
        }
        else if (seekBar == touchpadPointerX) {
            dispatchInput(
                    InputSettingsUpdate
                            .touchpadPointerSensitivityX(value));
            renderTouchpadPointer();
        }
        else if (seekBar == touchpadPointerY) {
            dispatchInput(
                    InputSettingsUpdate
                            .touchpadPointerSensitivityY(value));
            renderTouchpadPointer();
        }
        else if (seekBar == virtualTouchpadX) {
            dispatchInput(
                    InputSettingsUpdate
                            .virtualTouchpadSensitivityX(value));
            renderVirtualTouchpad();
        }
        else if (seekBar == virtualTouchpadY) {
            dispatchInput(
                    InputSettingsUpdate
                            .virtualTouchpadSensitivityY(value));
            renderVirtualTouchpad();
        }
        else if (seekBar == controllerMouseSensitivity) {
            dispatchController(
                    ControllerSettingsUpdate
                            .mouseSensitivityPercent(value));
            renderControllerMouse();
        }
        else if (seekBar == mouseWheelAmount) {
            dispatchInput(
                    InputSettingsUpdate
                            .mouseWheelScrollAmount(value));
            renderMouseWheel();
        }
        else if (seekBar == externalTouchpadX) {
            dispatchInput(
                    InputSettingsUpdate
                            .externalTouchpadSensitivityX(value));
            renderExternalTouchpad();
        }
        else if (seekBar == externalTouchpadY) {
            dispatchInput(
                    InputSettingsUpdate
                            .externalTouchpadSensitivityY(value));
            renderExternalTouchpad();
        }
        else if (seekBar == externalTouchpadScrollAmount) {
            dispatchInput(
                    InputSettingsUpdate
                            .externalTouchpadScrollAmount(value));
            renderExternalTouchpad();
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
        controllerSettings = update.applyTo(controllerSettings);
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
}
