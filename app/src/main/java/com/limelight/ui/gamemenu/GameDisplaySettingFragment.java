package com.limelight.ui.gamemenu;

import android.preference.PreferenceManager;
import androidx.annotation.StringRes;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.SeekBarValueRange;

import static com.limelight.preferences.PreferenceConfiguration.TOUCH_SENSITIVITY;

/**
 * 游戏菜单-杂项
 */
public class GameDisplaySettingFragment extends BaseGameMenuDialog {
    private static final SeekBarValueRange GYRO_SENSITIVITY_RANGE =
            new SeekBarValueRange(50, 200);
    private static final SeekBarValueRange PERFORMANCE_SCALE_RANGE =
            new SeekBarValueRange(50, 230);
    private static final SeekBarValueRange PERFORMANCE_MARGIN_RANGE =
            new SeekBarValueRange(0, 100);
    private static final SeekBarValueRange AUDIO_HAPTICS_STRENGTH_RANGE =
            new SeekBarValueRange(25, 200);

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_setting;
    }

    private ImageButton ibtn_back;
    private TextView tx_title;

    private int titleRes = R.string.game_menu_misc_title;

    private CheckBox btn_game_float_ball;
    private CheckBox btn_game_audio_mute;
    private CheckBox btn_game_lite_ext;
    private CheckBox btn_game_lite_click;
    private CheckBox btn_game_rumble_force;
    private CheckBox btn_game_rumble_force_stop;

    //悬浮球记住位置
    private CheckBox btn_game_float_ball_postion;

    private CheckBox btn_game_rumble_hud;

    private RadioGroup rg_game_setting_control;

    private RadioGroup rg_game_setting_touch;

    private RadioGroup rg_game_setting_float_ball;


    private CheckBox btn_game_force_gyro;
    private CheckBox btn_game_force_gyro_left_trgger;
    private CheckBox btn_game_force_gyro_switch;
    private RadioGroup rg_game_audio_haptics_enable;

    private SeekBar sb_game_setting_pref_zoom;

    private SeekBar sb_game_setting_pref_magin_top;
    private SeekBar sb_game_audio_haptics_strength;

    private TextView tx_game_setting_pref_magin_top;

    private TextView tx_game_setting_pref_zoom;

    private TextView tx_game_setting_gyro_sensitivity;
    private TextView tx_game_audio_haptics_strength;

    private SeekBar sb_game_setting_gyro_sensitivity;
    private RadioGroup rg_game_audio_haptics_voice_filter;
    private RadioGroup rg_game_audio_haptics_output_target;
    private RadioGroup rg_game_audio_haptics_keep_controller_rumble;
    private LinearLayout layout_game_audio_haptics_details;
    private TextView tx_game_audio_haptics_keep_controller_rumble;

    @Override
    public void bindView(View v) {
        super.bindView(v);
        ibtn_back=v.findViewById(R.id.ibtn_back);
        tx_title=v.findViewById(R.id.tx_title);

        btn_game_float_ball=v.findViewById(R.id.btn_game_float_ball);
        btn_game_audio_mute=v.findViewById(R.id.btn_game_audio_mute);
        btn_game_lite_ext=v.findViewById(R.id.btn_game_lite_ext);
        btn_game_lite_click=v.findViewById(R.id.btn_game_lite_click);
        btn_game_float_ball_postion=v.findViewById(R.id.btn_game_float_ball_postion);
        rg_game_setting_control=v.findViewById(R.id.rg_game_setting_control);
        rg_game_setting_touch =v.findViewById(R.id.rg_game_setting_touch);
        btn_game_rumble_force=v.findViewById(R.id.btn_game_rumble_force);
        btn_game_rumble_force_stop=v.findViewById(R.id.btn_game_rumble_force_stop);
        btn_game_rumble_hud=v.findViewById(R.id.btn_game_rumble_hud);
        rg_game_setting_float_ball=v.findViewById(R.id.rg_game_setting_float_ball);
        btn_game_force_gyro=v.findViewById(R.id.btn_game_force_gyro);
        btn_game_force_gyro_left_trgger=v.findViewById(R.id.btn_game_force_gyro_left_trgger);
        btn_game_force_gyro_switch=v.findViewById(R.id.btn_game_force_gyro_switch);
        rg_game_audio_haptics_enable=v.findViewById(R.id.rg_game_audio_haptics_enable);
        sb_game_setting_pref_zoom=v.findViewById(R.id.sb_game_setting_pref_zoom);
        tx_game_setting_pref_zoom=v.findViewById(R.id.tx_game_setting_pref_zoom);

        tx_game_setting_pref_magin_top=v.findViewById(R.id.tx_game_setting_pref_magin_top);
        sb_game_setting_pref_magin_top=v.findViewById(R.id.sb_game_setting_pref_magin_top);

        tx_game_setting_gyro_sensitivity=v.findViewById(R.id.tx_game_setting_gyro_sensitivity);
        sb_game_setting_gyro_sensitivity=v.findViewById(R.id.sb_game_setting_gyro_sensitivity);
        tx_game_audio_haptics_strength=v.findViewById(R.id.tx_game_audio_haptics_strength);
        sb_game_audio_haptics_strength=v.findViewById(R.id.sb_game_audio_haptics_strength);
        rg_game_audio_haptics_voice_filter=v.findViewById(R.id.rg_game_audio_haptics_voice_filter);
        rg_game_audio_haptics_output_target=v.findViewById(R.id.rg_game_audio_haptics_output_target);
        rg_game_audio_haptics_keep_controller_rumble=v.findViewById(R.id.rg_game_audio_haptics_keep_controller_rumble);
        layout_game_audio_haptics_details=v.findViewById(R.id.layout_game_audio_haptics_details);
        tx_game_audio_haptics_keep_controller_rumble=v.findViewById(R.id.tx_game_audio_haptics_keep_controller_rumble);

        tx_title.setText(titleRes);
        configureSeekBars();
        initViewData();
        initControl();
        initTouchNumber();
        initFloatBall();
        initPrefZoom();
        initPrefMagin();
        initGyroSensitivity();
        initAudioHaptics();
        ibtn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btn_game_float_ball.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefConfig.enableAXFloating=isChecked;
            setSetting("checkbox_enable_ax_floating",isChecked);
            if(onClick!=null){
                onClick.click(0,isChecked);
            }
        });

        btn_game_float_ball_postion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.axFloatingPostionAuto =isChecked;
                setSetting("ax_floating_postion_auto",isChecked);
                if(!isChecked){
                    saveSettingFloat("ax_floating_postion_x",-1);
                    saveSettingFloat("ax_floating_postion_y",-1);
                }
            }
        });

        btn_game_audio_mute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefConfig.audioMute=isChecked;
            setSetting("ax_audio_mute",isChecked);
            notifyAudioSettingsChanged();
        });
        btn_game_lite_ext.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefConfig.enablePerfOverlayLiteExt=isChecked;
            setSetting("checkbox_enable_perf_overlay_lite_ext",isChecked);
        });
        btn_game_lite_click.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefConfig.enablePerfOverlayLiteDialog=isChecked;
            setSetting("checkbox_enable_perf_overlay_lite_dialog",isChecked);
            if(onClick!=null){
                onClick.click(2,isChecked);
            }
        });

        btn_game_rumble_force.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.enableForceStrongVibrations=isChecked;
                setSetting("enable_force_strong_vibrations",isChecked);
                notifyControllerSettingsChanged();
            }
        });
        btn_game_rumble_force_stop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.enableForceStrongVibrationsStop=isChecked;
                setSetting("enable_force_strong_vibrations_stop",isChecked);
                notifyControllerSettingsChanged();
            }
        });

        btn_game_rumble_hud.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.showRumbleHUD=isChecked;
                setSetting("rumble_HUD_show",isChecked);
                if(onClick!=null){
                    onClick.click(1,isChecked);
                }
            }
        });

        rg_game_setting_control.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //关闭
                if(checkedId==R.id.rbt_game_setting_control_1){
                    prefConfig.mouseEmulation=false;
                    saveSetting("checkbox_mouse_emulation",false);
                    prefConfig.mouseEmulationGameMenu=0;
                    saveSetting("ax_quick_game_menu_key",0);
                    notifyControllerSettingsChanged();
                    return;
                }
                //开始键长按
                if(checkedId==R.id.rbt_game_setting_control_2){
                    prefConfig.mouseEmulation=true;
                    saveSetting("checkbox_mouse_emulation",true);
                    prefConfig.mouseEmulationGameMenu=0;
                    saveSetting("ax_quick_game_menu_key",0);
                    notifyControllerSettingsChanged();
                    return;
                }
                //xbox键单机
                if(checkedId==R.id.rbt_game_setting_control_3){
                    prefConfig.mouseEmulation=true;
                    saveSetting("checkbox_mouse_emulation",true);
                    prefConfig.mouseEmulationGameMenu=1;
                    saveSetting("ax_quick_game_menu_key",1);
                    notifyControllerSettingsChanged();
                    return;
                }
                //菜单键 长按
                if(checkedId==R.id.rbt_game_setting_control_4){
                    prefConfig.mouseEmulation=true;
                    saveSetting("checkbox_mouse_emulation",true);
                    prefConfig.mouseEmulationGameMenu=2;
                    saveSetting("ax_quick_game_menu_key",2);
                    notifyControllerSettingsChanged();
                    return;
                }
            }
        });

        rg_game_setting_touch.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.rbt_game_setting_touch_1){
                    prefConfig.quickSoftKeyboardFingers=0;
                    saveSetting("touch_number_quick_soft_keyboard",0);
                    notifyInputSettingsChanged();
                    return;
                }
                if(checkedId==R.id.rbt_game_setting_touch_2){
                    prefConfig.quickSoftKeyboardFingers=3;
                    saveSetting("touch_number_quick_soft_keyboard",3);
                    notifyInputSettingsChanged();
                    return;
                }
                if(checkedId==R.id.rbt_game_setting_touch_3){
                    prefConfig.quickSoftKeyboardFingers=4;
                    saveSetting("touch_number_quick_soft_keyboard",4);
                    notifyInputSettingsChanged();
                    return;
                }
                if(checkedId==R.id.rbt_game_setting_touch_4){
                    prefConfig.quickSoftKeyboardFingers=5;
                    saveSetting("touch_number_quick_soft_keyboard",5);
                    notifyInputSettingsChanged();
                    return;
                }
            }
        });

        rg_game_setting_float_ball.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.rbt_game_setting_float_ball_1){
                    prefConfig.axFloatingOperate=0;
                    saveSetting("ax_floating_operate",0);
                    return;
                }
                if(checkedId==R.id.rbt_game_setting_float_ball_2){
                    prefConfig.axFloatingOperate=1;
                    saveSetting("ax_floating_operate",1);
                    return;
                }
                if(checkedId==R.id.rbt_game_setting_float_ball_3){
                    prefConfig.axFloatingOperate=2;
                    saveSetting("ax_floating_operate",2);
                    return;
                }

            }
        });

        btn_game_force_gyro.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.gameForceGyro=isChecked;
                setSetting("gameForceGyro",isChecked);
                if(onClick!=null){
                    onClick.click(4,isChecked);
                }
            }
        });

        btn_game_force_gyro_left_trgger.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.gameForceGyroLeftTrigger=isChecked;
                setSetting("gameForceGyroLeftTrigger",isChecked);
                notifyControllerSettingsChanged();
            }
        });
        btn_game_force_gyro_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefConfig.gameForceGyroXYSwitch=isChecked;
                setSetting("gameForceGyroXYSwitch",isChecked);
                notifyControllerSettingsChanged();
            }
        });

        rg_game_audio_haptics_enable.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbt_game_audio_haptics_enable_on) {
                prefConfig.enableAudioHaptics = true;
            }
            else if (checkedId == R.id.rbt_game_audio_haptics_enable_off) {
                prefConfig.enableAudioHaptics = false;
            }
            else {
                return;
            }

            setSetting("checkbox_enable_audio_haptics", prefConfig.enableAudioHaptics);
            updateAudioHapticsVisibility();
            notifyAudioSettingsChanged();
        });

        rg_game_audio_haptics_voice_filter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbt_game_audio_haptics_voice_filter_1) {
                prefConfig.audioHapticsVoiceFilter = "off";
            }
            else if (checkedId == R.id.rbt_game_audio_haptics_voice_filter_2) {
                prefConfig.audioHapticsVoiceFilter = "low";
            }
            else if (checkedId == R.id.rbt_game_audio_haptics_voice_filter_3) {
                prefConfig.audioHapticsVoiceFilter = "medium";
            }
            else if (checkedId == R.id.rbt_game_audio_haptics_voice_filter_4) {
                prefConfig.audioHapticsVoiceFilter = "high";
            }
            else {
                return;
            }

            saveSetting("list_audio_haptics_voice_filter", prefConfig.audioHapticsVoiceFilter);
            notifyAudioSettingsChanged();
        });

        rg_game_audio_haptics_output_target.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbt_game_audio_haptics_output_target_phone) {
                prefConfig.audioHapticsOutputTarget = "phone";
            }
            else if (checkedId == R.id.rbt_game_audio_haptics_output_target_controller) {
                prefConfig.audioHapticsOutputTarget = "controller";
            }
            else {
                return;
            }

            saveSetting("list_audio_haptics_output_target", prefConfig.audioHapticsOutputTarget);
            updateAudioHapticsVisibility();
            notifyAudioSettingsChanged();
        });

        rg_game_audio_haptics_keep_controller_rumble.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbt_game_audio_haptics_keep_controller_rumble_on) {
                prefConfig.audioHapticsKeepControllerRumble = true;
            }
            else if (checkedId == R.id.rbt_game_audio_haptics_keep_controller_rumble_off) {
                prefConfig.audioHapticsKeepControllerRumble = false;
            }
            else {
                return;
            }

            saveSetting("checkbox_audio_haptics_keep_controller_rumble", prefConfig.audioHapticsKeepControllerRumble);
            updateAudioHapticsVisibility();
            notifyAudioSettingsChanged();
        });

        sb_game_setting_pref_zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = PERFORMANCE_SCALE_RANGE
                        .progressToValue(progress);
                prefConfig.gameSettingPrefZoom=value;
                saveSetting("game_setting_pref_zoom",value);
                initPrefZoom();
                if(onClick!=null){
                    onClick.click(3,false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_game_setting_pref_magin_top.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = PERFORMANCE_MARGIN_RANGE
                        .progressToValue(progress);
                prefConfig.performanceOverlayLiteMaginTop=value;
                saveSetting("performance_overlayLite_magin_top",value);
                initPrefMagin();
                if(onClick!=null){
                    onClick.click(5,false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_game_setting_gyro_sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = GYRO_SENSITIVITY_RANGE
                        .progressToValue(progress);
                prefConfig.gameForceGyroSensitivity=value;
                saveSetting("gameForceGyroSensitivity",value);
                initGyroSensitivity();
                notifyControllerSettingsChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_game_audio_haptics_strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = AUDIO_HAPTICS_STRENGTH_RANGE
                        .progressToValue(progress);
                prefConfig.audioHapticsStrength = value;
                saveSetting("seekbar_audio_haptics_strength", value);
                initAudioHapticsStrength();
                notifyAudioSettingsChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void configureSeekBars() {
        sb_game_setting_gyro_sensitivity.setMax(
                GYRO_SENSITIVITY_RANGE.getProgressMaximum());
        sb_game_setting_pref_zoom.setMax(
                PERFORMANCE_SCALE_RANGE.getProgressMaximum());
        sb_game_setting_pref_magin_top.setMax(
                PERFORMANCE_MARGIN_RANGE.getProgressMaximum());
        sb_game_audio_haptics_strength.setMax(
                AUDIO_HAPTICS_STRENGTH_RANGE.getProgressMaximum());
    }


    private void initControl(){
        boolean enableKey=PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("checkbox_mouse_emulation",false);
        if(!enableKey){
            rg_game_setting_control.check(R.id.rbt_game_setting_control_1);
            return;
        }
        int keyFlag=PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("ax_quick_game_menu_key",0);
        switch (keyFlag){
            case 0:
                rg_game_setting_control.check(R.id.rbt_game_setting_control_2);
                break;
            case 1:
                rg_game_setting_control.check(R.id.rbt_game_setting_control_3);
                break;
            case 2:
                rg_game_setting_control.check(R.id.rbt_game_setting_control_4);
                break;
        }
    }

    private void initTouchNumber(){
        int keyFlag=PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("touch_number_quick_soft_keyboard",0);
        switch (keyFlag){
            case 0:
                rg_game_setting_touch.check(R.id.rbt_game_setting_touch_1);
                break;
            case 3:
                rg_game_setting_touch.check(R.id.rbt_game_setting_touch_2);
                break;
            case 4:
                rg_game_setting_touch.check(R.id.rbt_game_setting_touch_3);
                break;
            case 5:
                rg_game_setting_touch.check(R.id.rbt_game_setting_touch_4);
                break;
        }
    }

    private void initFloatBall(){
        int keyFlag=PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("ax_floating_operate",0);
        switch (keyFlag){
            case 0:
                rg_game_setting_float_ball.check(R.id.rbt_game_setting_float_ball_1);
                break;
            case 1:
                rg_game_setting_float_ball.check(R.id.rbt_game_setting_float_ball_2);
                break;
            case 2:
                rg_game_setting_float_ball.check(R.id.rbt_game_setting_float_ball_3);
                break;
        }
    }

    private void saveSetting(String name,int value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putInt(name,value)
                .apply();
    }

    private void saveSetting(String name,String value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putString(name,value)
                .apply();
    }

    private void saveSettingFloat(String name,float value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putFloat(name,value)
                .apply();
    }

    private void saveSetting(String name,boolean value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putBoolean(name,value)
                .apply();
    }

    private void setSetting(String name,boolean value){
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putBoolean(name,value)
                .apply();
        initViewData();
    }

    private void initViewData() {
        if(prefConfig==null){
            return;
        }
        btn_game_float_ball.setChecked(prefConfig.enableAXFloating);
        btn_game_float_ball_postion.setChecked(prefConfig.axFloatingPostionAuto);
        btn_game_audio_mute.setChecked(prefConfig.audioMute);
        btn_game_lite_ext.setChecked(prefConfig.enablePerfOverlayLiteExt);
        btn_game_lite_click.setChecked(prefConfig.enablePerfOverlayLiteDialog);
        btn_game_rumble_force.setChecked(prefConfig.enableForceStrongVibrations);
        btn_game_rumble_force_stop.setChecked(prefConfig.enableForceStrongVibrationsStop);
        btn_game_rumble_hud.setChecked(prefConfig.showRumbleHUD);

        btn_game_force_gyro.setChecked(prefConfig.gameForceGyro);
        btn_game_force_gyro_left_trgger.setChecked(prefConfig.gameForceGyroLeftTrigger);
        btn_game_force_gyro_switch.setChecked(prefConfig.gameForceGyroXYSwitch);
        if (prefConfig.enableAudioHaptics) {
            rg_game_audio_haptics_enable.check(R.id.rbt_game_audio_haptics_enable_on);
        }
        else {
            rg_game_audio_haptics_enable.check(R.id.rbt_game_audio_haptics_enable_off);
        }
    }

    private void initPrefZoom(){
        tx_game_setting_pref_zoom.setText(getString(
                R.string.game_menu_performance_scale_format,
                prefConfig.gameSettingPrefZoom));
        sb_game_setting_pref_zoom.setProgress(
                PERFORMANCE_SCALE_RANGE.valueToProgress(
                        prefConfig.gameSettingPrefZoom));
    }

    private void initPrefMagin(){
        tx_game_setting_pref_magin_top.setText(getString(
                R.string.game_menu_performance_margin_format,
                prefConfig.performanceOverlayLiteMaginTop));
        sb_game_setting_pref_magin_top.setProgress(
                PERFORMANCE_MARGIN_RANGE.valueToProgress(
                        prefConfig.performanceOverlayLiteMaginTop));
    }

    private void initGyroSensitivity(){
        tx_game_setting_gyro_sensitivity.setText(getString(
                R.string.game_menu_gyro_sensitivity_format,
                prefConfig.gameForceGyroSensitivity));
        sb_game_setting_gyro_sensitivity.setProgress(
                GYRO_SENSITIVITY_RANGE.valueToProgress(
                        prefConfig.gameForceGyroSensitivity));
    }

    private void initAudioHaptics() {
        initAudioHapticsOutputTarget();
        initAudioHapticsVoiceFilter();
        initAudioHapticsStrength();
        updateAudioHapticsVisibility();
    }

    private void initAudioHapticsOutputTarget() {
        if ("controller".equals(prefConfig.audioHapticsOutputTarget)) {
            rg_game_audio_haptics_output_target.check(R.id.rbt_game_audio_haptics_output_target_controller);
        }
        else {
            rg_game_audio_haptics_output_target.check(R.id.rbt_game_audio_haptics_output_target_phone);
        }
    }

    private void initAudioHapticsVoiceFilter() {
        String filter = prefConfig.audioHapticsVoiceFilter;
        if ("low".equals(filter)) {
            rg_game_audio_haptics_voice_filter.check(R.id.rbt_game_audio_haptics_voice_filter_2);
        }
        else if ("medium".equals(filter)) {
            rg_game_audio_haptics_voice_filter.check(R.id.rbt_game_audio_haptics_voice_filter_3);
        }
        else if ("high".equals(filter)) {
            rg_game_audio_haptics_voice_filter.check(R.id.rbt_game_audio_haptics_voice_filter_4);
        }
        else {
            rg_game_audio_haptics_voice_filter.check(R.id.rbt_game_audio_haptics_voice_filter_1);
        }
    }

    private void initAudioHapticsStrength() {
        tx_game_audio_haptics_strength.setText(getString(
                R.string.game_menu_audio_haptics_strength_format,
                prefConfig.audioHapticsStrength));
        sb_game_audio_haptics_strength.setProgress(
                AUDIO_HAPTICS_STRENGTH_RANGE.valueToProgress(
                        prefConfig.audioHapticsStrength));
    }

    private void updateAudioHapticsVisibility() {
        if (layout_game_audio_haptics_details != null) {
            layout_game_audio_haptics_details.setVisibility(prefConfig.enableAudioHaptics ? View.VISIBLE : View.GONE);
        }
        boolean showKeepControllerRumble = prefConfig.enableAudioHaptics &&
                "controller".equals(prefConfig.audioHapticsOutputTarget);
        if (tx_game_audio_haptics_keep_controller_rumble != null) {
            tx_game_audio_haptics_keep_controller_rumble.setVisibility(
                    showKeepControllerRumble ? View.VISIBLE : View.GONE);
        }
        if (rg_game_audio_haptics_keep_controller_rumble != null) {
            rg_game_audio_haptics_keep_controller_rumble.check(prefConfig.audioHapticsKeepControllerRumble ?
                    R.id.rbt_game_audio_haptics_keep_controller_rumble_on :
                    R.id.rbt_game_audio_haptics_keep_controller_rumble_off);
            rg_game_audio_haptics_keep_controller_rumble.setVisibility(
                    showKeepControllerRumble ? View.VISIBLE : View.GONE);
        }
    }

    private void notifyAudioSettingsChanged() {
        if (onClick != null) {
            onClick.onAudioSettingsChanged();
        }
    }

    @Override
    public float getDimAmount() {
        return super.getDimAmount();
    }

    public void setTitle(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    private PreferenceConfiguration prefConfig;

    public void setPrefConfig(PreferenceConfiguration prefConfig) {
        this.prefConfig = prefConfig;
    }

    private onClick onClick;

    public interface onClick{
        void click(int index,boolean flag);

        default void onInputSettingsChanged() {
        }

        default void onControllerSettingsChanged() {
        }

        default void onAudioSettingsChanged() {
        }
    }

    public void setOnClick(onClick onClick) {
        this.onClick = onClick;
    }

    private void notifyInputSettingsChanged() {
        if (onClick != null) {
            onClick.onInputSettingsChanged();
        }
    }

    private void notifyControllerSettingsChanged() {
        if (onClick != null) {
            onClick.onControllerSettingsChanged();
        }
    }

}
