package com.limelight.ui.gamemenu;

import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.SeekBarValueRange;

import static com.limelight.preferences.PreferenceConfiguration.TOUCH_SENSITIVITY;

/**
 * Description
 * Date: 2024-10-20
 * Time: 16:07
 */
public class GameTouchFragment extends BaseGameMenuDialog implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    public interface Listener {
        void onInputSettingsChanged();

        void onControllerSettingsChanged();
    }

    private static final SeekBarValueRange MULTITOUCH_RANGE =
            new SeekBarValueRange(10, 800);
    private static final SeekBarValueRange SENSITIVITY_RANGE =
            new SeekBarValueRange(10, 300);
    private static final SeekBarValueRange DISTANCE_RANGE =
            new SeekBarValueRange(1, 30);

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_touch;
    }

    private ImageButton ibtn_back;
    private TextView tx_title;

    private String title;

    private Button btn_touch_switch;

    private Button btn_touch_center;

    private Button btn_touch_all;

    private SeekBar sb_touch_x;

    private SeekBar sb_touch_y;

    private SeekBar sb_touchpad_x;

    private SeekBar sb_touchpad_y;

    private SeekBar sb_touchpad_view_x;

    private SeekBar sb_touchpad_view_y;

    private TextView tx_touch_x;

    private TextView tx_touch_y;

    private TextView tx_touchpad_x;

    private TextView tx_touchpad_y;

    private TextView tx_touchpad_view_x;

    private TextView tx_touchpad_view_y;
    private SeekBar sb_mouse_gamepad_sensitity;
    private TextView tx_mouse_gamepad_sensitity;

    private SeekBar sb_mouse_sc_amount;
    private TextView tx_mouse_sc_amount;
    private SeekBar sb_touchpad_equipment_view_x;
    private SeekBar sb_touchpad_equipment_view_y;
    private SeekBar sb_touchpad_equipment_amount;
    private TextView tx_touchpad_equipment_view_x;
    private TextView tx_touchpad_equipment_view_y;
    private TextView tx_touchpad_equipment_amount;

    @Override
    public void bindView(View v) {
        super.bindView(v);
        ibtn_back=v.findViewById(R.id.ibtn_back);
        tx_title=v.findViewById(R.id.tx_title);

        btn_touch_switch=v.findViewById(R.id.btn_touch_switch);
        btn_touch_center=v.findViewById(R.id.btn_touch_center);
        btn_touch_all=v.findViewById(R.id.btn_touch_all);

        sb_touch_x=v.findViewById(R.id.sb_touch_x);
        sb_touch_y=v.findViewById(R.id.sb_touch_y);
        sb_touchpad_x=v.findViewById(R.id.sb_touchpad_x);
        sb_touchpad_y=v.findViewById(R.id.sb_touchpad_y);
        sb_touchpad_view_x=v.findViewById(R.id.sb_touchpad_view_x);
        sb_touchpad_view_y=v.findViewById(R.id.sb_touchpad_view_y);

        tx_touch_x=v.findViewById(R.id.tx_touch_x);
        tx_touch_y=v.findViewById(R.id.tx_touch_y);
        tx_touchpad_x=v.findViewById(R.id.tx_touchpad_x);
        tx_touchpad_y=v.findViewById(R.id.tx_touchpad_y);
        tx_touchpad_view_x=v.findViewById(R.id.tx_touchpad_view_x);
        tx_touchpad_view_y=v.findViewById(R.id.tx_touchpad_view_y);

        sb_mouse_gamepad_sensitity=v.findViewById(R.id.sb_mouse_gamepad_sensitity);
        tx_mouse_gamepad_sensitity=v.findViewById(R.id.tx_mouse_gamepad_sensitity);

        sb_mouse_sc_amount=v.findViewById(R.id.sb_mouse_sc_amount);
        tx_mouse_sc_amount=v.findViewById(R.id.tx_mouse_sc_amount);
        sb_touchpad_equipment_view_x=v.findViewById(R.id.sb_touchpad_equipment_view_x);
        sb_touchpad_equipment_view_y=v.findViewById(R.id.sb_touchpad_equipment_view_y);
        sb_touchpad_equipment_amount=v.findViewById(R.id.sb_touchpad_equipment_amount);
        tx_touchpad_equipment_view_x=v.findViewById(R.id.tx_touchpad_equipment_view_x);
        tx_touchpad_equipment_view_y=v.findViewById(R.id.tx_touchpad_equipment_view_y);
        tx_touchpad_equipment_amount=v.findViewById(R.id.tx_touchpad_equipment_amount);

        configureSeekBars();
        tx_title.setText(title);
        initViewData();
        initViewTouch();
        initViewTouchPad();
        initViewTouchPadView();
        initViewMouseGamePadView();

        initViewMouseSCView();
        initViewExternalTouchPadView();

        ibtn_back.setOnClickListener(this);
        btn_touch_switch.setOnClickListener(this);
        btn_touch_center.setOnClickListener(this);
        btn_touch_all.setOnClickListener(this);

        v.findViewById(R.id.btn_right).setOnClickListener(this);

        sb_touch_x.setOnSeekBarChangeListener(this);
        sb_touch_y.setOnSeekBarChangeListener(this);
        sb_touchpad_x.setOnSeekBarChangeListener(this);
        sb_touchpad_y.setOnSeekBarChangeListener(this);
        sb_touchpad_view_x.setOnSeekBarChangeListener(this);
        sb_touchpad_view_y.setOnSeekBarChangeListener(this);
        sb_mouse_gamepad_sensitity.setOnSeekBarChangeListener(this);
        sb_mouse_sc_amount.setOnSeekBarChangeListener(this);
        sb_touchpad_equipment_view_x.setOnSeekBarChangeListener(this);
        sb_touchpad_equipment_view_y.setOnSeekBarChangeListener(this);
        sb_touchpad_equipment_amount.setOnSeekBarChangeListener(this);
    }

    @Override
    public float getDimAmount() {
        return super.getDimAmount();
    }

    public void setTitle(String title) {
        this.title = title;
    }


    private void initViewData(){
        btn_touch_switch.setBackgroundResource(prefConfig.enableTouchSensitivity?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);
        btn_touch_center.setBackgroundResource(prefConfig.touchSensitivityRotationAuto?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);
        btn_touch_all.setBackgroundResource(prefConfig.touchSensitivityGlobal?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);
    }

    private void initViewTouch(){
        setSeekBarValue(
                sb_touch_x, MULTITOUCH_RANGE,
                prefConfig.touchSensitivityX);
        setSeekBarValue(
                sb_touch_y, MULTITOUCH_RANGE,
                prefConfig.touchSensitivityY);
        tx_touch_x.setText(getString(
                R.string.game_menu_axis_x_format,
                prefConfig.touchSensitivityX));
        tx_touch_y.setText(getString(
                R.string.game_menu_axis_y_format,
                prefConfig.touchSensitivityY));
    }

    private void initViewTouchPad(){
        setSeekBarValue(
                sb_touchpad_x, SENSITIVITY_RANGE,
                prefConfig.mouseTouchPadSensitityX);
        setSeekBarValue(
                sb_touchpad_y, SENSITIVITY_RANGE,
                prefConfig.mouseTouchPadSensitityY);
        tx_touchpad_x.setText(getString(
                R.string.game_menu_axis_x_format,
                prefConfig.mouseTouchPadSensitityX));
        tx_touchpad_y.setText(getString(
                R.string.game_menu_axis_y_format,
                prefConfig.mouseTouchPadSensitityY));
    }

    private void initViewTouchPadView(){
        setSeekBarValue(
                sb_touchpad_view_x, SENSITIVITY_RANGE,
                prefConfig.touchPadSensitivity);
        setSeekBarValue(
                sb_touchpad_view_y, SENSITIVITY_RANGE,
                prefConfig.touchPadYSensitity);
        tx_touchpad_view_x.setText(getString(
                R.string.game_menu_axis_x_format,
                prefConfig.touchPadSensitivity));
        tx_touchpad_view_y.setText(getString(
                R.string.game_menu_axis_y_format,
                prefConfig.touchPadYSensitity));
    }

    private void initViewMouseGamePadView(){
        setSeekBarValue(
                sb_mouse_gamepad_sensitity, SENSITIVITY_RANGE,
                prefConfig.mouseGamePadSensitity);
        tx_mouse_gamepad_sensitity.setText(getString(
                R.string.game_menu_sensitivity_format,
                prefConfig.mouseGamePadSensitity));
    }

    private void initViewMouseSCView(){
        setSeekBarValue(
                sb_mouse_sc_amount, DISTANCE_RANGE,
                prefConfig.mouseSCAmount);
        tx_mouse_sc_amount.setText(getString(
                R.string.game_menu_distance_format,
                prefConfig.mouseSCAmount));
    }

    private void initViewExternalTouchPadView(){
        setSeekBarValue(
                sb_touchpad_equipment_view_x, SENSITIVITY_RANGE,
                prefConfig.externalTouchPadSensitityX);
        setSeekBarValue(
                sb_touchpad_equipment_view_y, SENSITIVITY_RANGE,
                prefConfig.externalTouchPadSensitityY);
        setSeekBarValue(
                sb_touchpad_equipment_amount, DISTANCE_RANGE,
                prefConfig.externalTouchPadScrollAmount);
        tx_touchpad_equipment_view_x.setText(getString(
                R.string.game_menu_axis_x_format,
                prefConfig.externalTouchPadSensitityX));
        tx_touchpad_equipment_view_y.setText(getString(
                R.string.game_menu_axis_y_format,
                prefConfig.externalTouchPadSensitityY));
        tx_touchpad_equipment_amount.setText(getString(
                R.string.game_menu_scroll_speed_format,
                prefConfig.externalTouchPadScrollAmount));
    }

    private void configureSeekBars() {
        configureSeekBar(sb_touch_x, MULTITOUCH_RANGE);
        configureSeekBar(sb_touch_y, MULTITOUCH_RANGE);
        configureSeekBar(sb_touchpad_x, SENSITIVITY_RANGE);
        configureSeekBar(sb_touchpad_y, SENSITIVITY_RANGE);
        configureSeekBar(sb_touchpad_view_x, SENSITIVITY_RANGE);
        configureSeekBar(sb_touchpad_view_y, SENSITIVITY_RANGE);
        configureSeekBar(sb_mouse_gamepad_sensitity, SENSITIVITY_RANGE);
        configureSeekBar(sb_mouse_sc_amount, DISTANCE_RANGE);
        configureSeekBar(
                sb_touchpad_equipment_view_x, SENSITIVITY_RANGE);
        configureSeekBar(
                sb_touchpad_equipment_view_y, SENSITIVITY_RANGE);
        configureSeekBar(
                sb_touchpad_equipment_amount, DISTANCE_RANGE);
    }

    private static void configureSeekBar(
            SeekBar seekBar, SeekBarValueRange range) {
        seekBar.setMax(range.getProgressMaximum());
    }

    private static void setSeekBarValue(
            SeekBar seekBar, SeekBarValueRange range, int value) {
        int progress = range.valueToProgress(value);
        if (seekBar.getProgress() != progress) {
            seekBar.setProgress(progress);
        }
    }

    private SeekBarValueRange getRange(SeekBar seekBar) {
        if (seekBar == sb_touch_x || seekBar == sb_touch_y) {
            return MULTITOUCH_RANGE;
        }
        if (seekBar == sb_mouse_sc_amount ||
                seekBar == sb_touchpad_equipment_amount) {
            return DISTANCE_RANGE;
        }
        return SENSITIVITY_RANGE;
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.ibtn_back){
            dismiss();
            return;
        }

        if(v.getId()==R.id.btn_right){
            //鼠标触控板模式
            prefConfig.mouseTouchPadSensitityX=100;
            prefConfig.mouseTouchPadSensitityY=100;
            //多点触控屏幕灵敏度
            prefConfig.enableTouchSensitivity=false;
            prefConfig.touchSensitivityX=100;
            prefConfig.touchSensitivityY=100;
            prefConfig.touchSensitivityGlobal=false;
            prefConfig.touchSensitivityRotationAuto=true;

            //触控板模式灵敏度
            prefConfig.touchPadSensitivity=100;
            prefConfig.touchPadYSensitity=100;
            prefConfig.externalTouchPadSensitityX=100;
            prefConfig.externalTouchPadSensitityY=100;
            prefConfig.externalTouchPadScrollAmount=5;

            prefConfig.mouseSCAmount=5;

            prefConfig.mouseGamePadSensitity=100;

            saveSetting(TOUCH_SENSITIVITY,100);
            saveSetting("seekbar_touch_sensitivity_opacity_y",100);
            saveSetting("seekbar_mouse_touchpad_sensitivity_x_opacity",100);
            saveSetting("seekbar_mouse_touchpad_sensitivity_y_opacity",100);
            saveSetting("seekbar_touchpad_sensitivity_opacity",100);
            saveSetting("seekbar_touchpad_sensitivity_y_opacity",100);
            saveSetting("touchpad_equipment_view_x",100);
            saveSetting("touchpad_equipment_view_y",100);
            saveSetting("touchpad_equipment_amount",5);
            saveSetting("mouse_gamepad_sensitity",100);
            saveSetting("mouse_sc_amount",5);

            saveSetting("checkbox_enable_touch_sensitivity",prefConfig.enableTouchSensitivity);
            saveSetting("checkbox_enable_touch_sensitivity_rotation_auto",prefConfig.touchSensitivityRotationAuto);
            saveSetting("checkbox_enable_global_touch_sensitivity",prefConfig.touchSensitivityGlobal);

            initViewData();
            initViewTouch();
            initViewTouchPad();
            initViewTouchPadView();

            initViewMouseGamePadView();
            initViewMouseSCView();
            initViewExternalTouchPadView();
            notifyInputSettingsChanged();
            notifyControllerSettingsChanged();

            return;
        }
        if(v.getId()==R.id.btn_touch_switch){
            prefConfig.enableTouchSensitivity=!prefConfig.enableTouchSensitivity;
            saveSetting("checkbox_enable_touch_sensitivity",prefConfig.enableTouchSensitivity);
            initViewData();
            notifyInputSettingsChanged();
            return;
        }

        if(v.getId()==R.id.btn_touch_center){
            prefConfig.touchSensitivityRotationAuto=!prefConfig.touchSensitivityRotationAuto;
            saveSetting("checkbox_enable_touch_sensitivity_rotation_auto",prefConfig.touchSensitivityRotationAuto);
            initViewData();
            notifyInputSettingsChanged();
            return;
        }

        if(v.getId()==R.id.btn_touch_all){
            prefConfig.touchSensitivityGlobal=!prefConfig.touchSensitivityGlobal;
            saveSetting("checkbox_enable_global_touch_sensitivity",prefConfig.touchSensitivityGlobal);
            initViewData();
            notifyInputSettingsChanged();
            return;
        }
    }

    private PreferenceConfiguration prefConfig;
    private Listener listener;

    public void setPrefConfig(PreferenceConfiguration prefConfig) {
        this.prefConfig = prefConfig;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int value = getRange(seekBar).progressToValue(progress);
        if(seekBar==sb_touch_x){
            prefConfig.touchSensitivityX=value;
            saveSetting(TOUCH_SENSITIVITY,value);
            initViewTouch();
        }
        if(seekBar==sb_touch_y){
            prefConfig.touchSensitivityY=value;
            saveSetting("seekbar_touch_sensitivity_opacity_y",value);
            initViewTouch();
        }
        if(seekBar==sb_touchpad_x){
            prefConfig.mouseTouchPadSensitityX=value;
            saveSetting("seekbar_mouse_touchpad_sensitivity_x_opacity",value);
            initViewTouchPad();
        }
        if(seekBar==sb_touchpad_y){
            prefConfig.mouseTouchPadSensitityY=value;
            saveSetting("seekbar_mouse_touchpad_sensitivity_y_opacity",value);
            initViewTouchPad();
        }
        if(seekBar==sb_touchpad_view_x){
            prefConfig.touchPadSensitivity=value;
            saveSetting("seekbar_touchpad_sensitivity_opacity",value);
            initViewTouchPadView();
        }
        if(seekBar==sb_touchpad_view_y){
            prefConfig.touchPadYSensitity=value;
            saveSetting("seekbar_touchpad_sensitivity_y_opacity",value);
            initViewTouchPadView();
        }

        if(seekBar==sb_mouse_gamepad_sensitity){
            prefConfig.mouseGamePadSensitity=value;
            saveSetting("mouse_gamepad_sensitity",value);
            initViewMouseGamePadView();
        }

        if(seekBar==sb_mouse_sc_amount){
            prefConfig.mouseSCAmount=value;
            saveSetting("mouse_sc_amount",value);
            initViewMouseSCView();
        }

        if(seekBar==sb_touchpad_equipment_view_x){
            prefConfig.externalTouchPadSensitityX=value;
            saveSetting("touchpad_equipment_view_x",value);
            initViewExternalTouchPadView();
        }

        if(seekBar==sb_touchpad_equipment_view_y){
            prefConfig.externalTouchPadSensitityY=value;
            saveSetting("touchpad_equipment_view_y",value);
            initViewExternalTouchPadView();
        }

        if(seekBar==sb_touchpad_equipment_amount){
            prefConfig.externalTouchPadScrollAmount=value;
            saveSetting("touchpad_equipment_amount",value);
            initViewExternalTouchPadView();
        }

        if (fromUser &&
                seekBar == sb_mouse_gamepad_sensitity) {
            notifyControllerSettingsChanged();
        }
        else if (fromUser &&
                (seekBar == sb_touch_x ||
                        seekBar == sb_touch_y ||
                        seekBar == sb_touchpad_x ||
                        seekBar == sb_touchpad_y ||
                        seekBar ==
                                sb_touchpad_equipment_view_x ||
                        seekBar ==
                                sb_touchpad_equipment_view_y ||
                        seekBar ==
                                sb_touchpad_equipment_amount)) {
            notifyInputSettingsChanged();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void saveSetting(String name,int value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putInt(name,value)
                .apply();
    }


    private void saveSetting(String name,boolean value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putBoolean(name,value)
                .apply();
    }

    private void notifyInputSettingsChanged() {
        if (listener != null) {
            listener.onInputSettingsChanged();
        }
    }

    private void notifyControllerSettingsChanged() {
        if (listener != null) {
            listener.onControllerSettingsChanged();
        }
    }
}
