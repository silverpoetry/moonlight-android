/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.limelight.utils.UiToast;

import com.google.gson.Gson;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.StreamInputGateway;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;
import com.limelight.ui.StreamUiActions;
import com.limelight.ui.gamemenu.GameKeyboardUpdateFragment;
import com.limelight.ui.gamemenu.GamePadAddFragment;
import com.limelight.ui.gamemenu.LegacyVirtualControlActionMigration;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.utils.SeekBarValueRange;
import com.limelight.utils.AndroidVibratorCompat;
import com.limelight.utils.UiHelper;
import com.limelight.virtualcontrols.action.VirtualControlAction;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutDocument;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutReadResult;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KeyBoardController implements EditableVirtualControlOverlay {
    private static final SeekBarValueRange BUTTON_SCALE_RANGE =
            new SeekBarValueRange(50, 300);

    public static class ControllerInputContext {
        //        public short inputMap = 0x0000;
        public int inputMap = 0;
        public byte leftTrigger = 0x00;
        public byte rightTrigger = 0x00;
        public short rightStickX = 0x0000;
        public short rightStickY = 0x0000;
        public short leftStickX = 0x0000;
        public short leftStickY = 0x0000;
    }


    private final ControllerHandler controllerHandler;
    private final StreamInputGateway inputGateway;
    private final StreamUiActions uiActions;

    ControllerInputContext inputContext = new ControllerInputContext();

    private final FragmentActivity context;
    private final Handler handler;

    private final Runnable delayedRetransmitRunnable = new Runnable() {
        @Override
        public void run() {
            sendControllerInputContextInternal();
        }
    };

    private FrameLayout frame_layout = null;

    VirtualControlEditMode currentMode = VirtualControlEditMode.NONE;
    private boolean destroyed;

    private View buttonConfigure = null;

    private Vibrator vibrator;
    private final List<KeyboardVirtualControllerElement> elements = new ArrayList<>();

    private final InputSettingsState inputSettingsState;
    private final VirtualControlSettingsState virtualControlSettingsState;
    private boolean isShow=true;
    private ImageView iv_game_virtual_pad;
    private LinearLayout lv_right_view;
    private View lv_left_view = null;

    private TextView txName;
    private TextView txDesc;
    private TextView txZoom;
    private CheckBox cb_round;
    private CheckBox cb_switch_mode;
    private SeekBar sb_zoom_x;

    private TextView tx_zoom_w;
    private TextView tx_zoom_h;
    private SeekBar sb_zoom_w;
    private SeekBar sb_zoom_h;
    private TextView tx_margin;

    private int currentIndex=-1;

    private int buttonWidth;
    private int buttonHeight;

    private VirtualControlLayoutKey layoutKey;

    private boolean isGamePadMode;
    private final VirtualControlLayoutRepository layoutRepository;

    public KeyBoardController(final ControllerHandler controllerHandler,
                              FrameLayout layout,
                              final FragmentActivity context,
                              InputSettingsState inputSettingsState,
                              VirtualControlSettingsState
                                      virtualControlSettingsState,
                              VirtualControlLayoutRepository layoutRepository,
                              boolean isGamePadMode,
                              StreamInputGateway inputGateway,
                              StreamUiActions uiActions) {
        this.controllerHandler = controllerHandler;
        this.inputGateway = Objects.requireNonNull(inputGateway, "inputGateway");
        this.uiActions = Objects.requireNonNull(uiActions, "uiActions");
        this.frame_layout = layout;
        this.context = context;
        this.isGamePadMode=isGamePadMode;
        this.handler = new Handler(Looper.getMainLooper());
        this.inputSettingsState = Objects.requireNonNull(
                inputSettingsState,
                "inputSettingsState");
        this.virtualControlSettingsState = Objects.requireNonNull(
                virtualControlSettingsState,
                "virtualControlSettingsState");
        this.layoutRepository = Objects.requireNonNull(
                layoutRepository,
                "layoutRepository");
        this.vibrator = Objects.requireNonNull(
                ContextCompat.getSystemService(context, Vibrator.class),
                "vibrator");
        buttonConfigure=View.inflate(context,R.layout.view_virtual_keyboard_top_right,null);
        buttonConfigure.setAlpha(
                getSettings().getControlOpacityPercent() /
                        100.0f /
                        2f);
        lv_left_view=View.inflate(context,R.layout.view_virtual_keyboard_top_left,null);
        buttonWidth=UiHelper.dpToPx(context,50);
        buttonHeight=UiHelper.dpToPx(context,50);
        initTopView();
    }

    private void initTopView(){
        iv_game_virtual_pad= buttonConfigure.findViewById(R.id.iv_game_virtual_pad);
        lv_right_view=buttonConfigure.findViewById(R.id.lv_right_view);

        txName=lv_left_view.findViewById(R.id.tx_name);
        txDesc=lv_left_view.findViewById(R.id.tx_desc);
        txZoom=lv_left_view.findViewById(R.id.tx_zoom);
        cb_round=lv_left_view.findViewById(R.id.cb_round);
        cb_switch_mode=lv_left_view.findViewById(R.id.cb_switch_mode);
        sb_zoom_x=lv_left_view.findViewById(R.id.sb_zoom_x);
        sb_zoom_w=lv_left_view.findViewById(R.id.sb_zoom_w);
        sb_zoom_h=lv_left_view.findViewById(R.id.sb_zoom_h);
        tx_zoom_w=lv_left_view.findViewById(R.id.tx_zoom_w);
        tx_zoom_h=lv_left_view.findViewById(R.id.tx_zoom_h);
        tx_margin=lv_left_view.findViewById(R.id.tx_margin);
        sb_zoom_x.setMax(BUTTON_SCALE_RANGE.getProgressMaximum());
        sb_zoom_w.setMax(BUTTON_SCALE_RANGE.getProgressMaximum());
        sb_zoom_h.setMax(BUTTON_SCALE_RANGE.getProgressMaximum());

        iv_game_virtual_pad.setOnClickListener(v -> {
            if(lv_right_view.getVisibility()==View.GONE){
                iv_game_virtual_pad.setImageResource(R.drawable.ic_gamepad_top_right);
                lv_right_view.setVisibility(View.VISIBLE);
            }else{
                iv_game_virtual_pad.setImageResource(R.drawable.ic_gamepad_top_left);
                lv_right_view.setVisibility(View.GONE);
            }
        });
        buttonConfigure.findViewById(R.id.btn_game_virtual_add).setOnClickListener(v -> {
            if(isGamePadMode){
                GamePadAddFragment fragment=new GamePadAddFragment();
                if(isLandscape(context)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowMetrics windowMetrics = context.getWindowManager().getCurrentWindowMetrics();
                        Rect bounds = windowMetrics.getBounds();
                        fragment.setWidth(bounds.width());
                    }else{
                        fragment.setWidth(context.getResources().getDisplayMetrics().widthPixels);
                    }
                }else{
                    fragment.setWidth((context.getResources().getDisplayMetrics().heightPixels*2)/3);
                }
                fragment.setTitle(context.getString(
                        R.string.game_menu_gamepad_buttons));
                fragment.setElementSelectionListener(bean -> {
                    addItem(bean);
                });
                fragment.show(context.getSupportFragmentManager());
                return;
            }
            GameKeyboardUpdateFragment fragment=new GameKeyboardUpdateFragment();
            if(isLandscape(context)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowMetrics windowMetrics = context.getWindowManager().getCurrentWindowMetrics();
                    Rect bounds = windowMetrics.getBounds();
                    fragment.setWidth(bounds.width());
                }else{
                    fragment.setWidth(context.getResources().getDisplayMetrics().widthPixels);
                }
            }else{
                fragment.setWidth((context.getResources().getDisplayMetrics().heightPixels*2)/3);
            }
            fragment.setTitle(R.string.keyboard_chord_title);
            fragment.setSelectionListener(bean -> {
                addItem(bean);
            });
            fragment.show(context.getSupportFragmentManager());
        });

        buttonConfigure.findViewById(R.id.btn_game_virtual_save).setOnClickListener(v -> {
            save();
        });

        buttonConfigure.findViewById(R.id.btn_game_virtual_reset).setOnClickListener(v -> {
            refreshLayout();
            buttonConfigure.setVisibility(View.VISIBLE);
        });

        lv_left_view.findViewById(R.id.tx_cancel).setOnClickListener(v -> {
            View view=frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode));
            currentIndex=-1;
            if(view!=null){
                view.invalidate();
            }
            lv_left_view.setVisibility(View.GONE);
        });
        lv_left_view.findViewById(R.id.tx_del).setOnClickListener(v -> {
            beanList.remove(currentIndex);
            lv_left_view.setVisibility(View.GONE);
            updateItem();
        });
        cb_round.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //方形按钮
            lv_left_view.findViewById(R.id.lv_zoom_wh).setVisibility(isChecked?View.VISIBLE:View.GONE);
            txZoom.setVisibility(isChecked?View.GONE:View.VISIBLE);
            sb_zoom_x.setVisibility(isChecked?View.GONE:View.VISIBLE);

            cb_round.setChecked(isChecked);
            beanList.get(currentIndex).setShapeType(isChecked?1:0);
            KeyboardVirtualControllerElement element=frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode));
            element.setShapeType(beanList.get(currentIndex).getShapeType());
            element.invalidate();
        });

        cb_switch_mode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cb_switch_mode.setChecked(isChecked);
            beanList.get(currentIndex).setSwitchMode(isChecked);
            KeyboardVirtualControllerElement element=frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode));
            if(element instanceof KeyBoardDigitalButton){
                ((KeyBoardDigitalButton)element).setEnableSwitchDown(isChecked);
            }
            element.invalidate();
        });

        sb_zoom_x.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = BUTTON_SCALE_RANGE.progressToValue(progress);
                txZoom.setText(context.getString(
                        R.string.keyboard_scale_format, value));
                beanList.get(currentIndex).setZoom(value);
                switch (beanList.get(currentIndex).getBtnType()){
                    case 1://1鼠标 2触控板 3摇杆 4普通按钮 5十字键
                    case 4:
                        beanList.get(currentIndex).setWidth((int) (buttonWidth*value*0.01));
                        beanList.get(currentIndex).setHeight((int) (buttonHeight*value*0.01));
                        break;
                    case 2:
                        beanList.get(currentIndex).setWidth((int) (buttonWidth*4*value*0.01));
                        beanList.get(currentIndex).setHeight((int) (buttonHeight*2*value*0.01));
                        break;
                    case 3:
                        beanList.get(currentIndex).setWidth((int) (buttonWidth*2*value*0.01));
                        beanList.get(currentIndex).setHeight((int) (buttonHeight*2*value*0.01));
                        break;
                    case 5://十字键
                        beanList.get(currentIndex).setWidth((int) (buttonWidth*2*value*0.01));
                        beanList.get(currentIndex).setHeight((int) (buttonHeight*2*value*0.01));
                        break;
                }
                frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode)).getLayoutParams().width=beanList.get(currentIndex).getWidth();
                frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode)).getLayoutParams().height=beanList.get(currentIndex).getHeight();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_zoom_w.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = BUTTON_SCALE_RANGE.progressToValue(progress);
                tx_zoom_w.setText(context.getString(
                        R.string.keyboard_width_scale_format, value));
                beanList.get(currentIndex).setZoomW(value);
                switch (beanList.get(currentIndex).getBtnType()){
                    case 2:
                        beanList.get(currentIndex).setWidth((int) (buttonWidth*4*value*0.01));
                        break;
                    case 4:
                        beanList.get(currentIndex).setWidth((int) (buttonWidth*value*0.01));
                        break;
                }
                frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode)).getLayoutParams().width=beanList.get(currentIndex).getWidth();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_zoom_h.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int value = BUTTON_SCALE_RANGE.progressToValue(progress);
                tx_zoom_h.setText(context.getString(
                        R.string.keyboard_height_scale_format, value));
                beanList.get(currentIndex).setZoomH(value);
                switch (beanList.get(currentIndex).getBtnType()){
                    case 2:
                        beanList.get(currentIndex).setHeight((int) (buttonHeight*2*value*0.01));
                        break;
                    case 4:
                        beanList.get(currentIndex).setHeight((int) (buttonHeight*value*0.01));
                        break;
                }
                frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode)).getLayoutParams().height=beanList.get(currentIndex).getHeight();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private List<GameMenuQuickBean> beanList=new ArrayList<>();


    private void addItem(GameMenuQuickBean bean){
        int w=context.getResources().getDisplayMetrics().widthPixels;
        int h=context.getResources().getDisplayMetrics().heightPixels;
        bean.setmLeft(w/2);
        bean.setmTop(h/2);
        switch(bean.getBtnType()){
            case 1:
            case 4://鼠标&普通按钮
                bean.setWidth(buttonWidth);
                bean.setHeight(buttonHeight);
                break;
            case 2://触控板
                bean.setWidth(buttonWidth*4);
                bean.setHeight(buttonHeight*2);
                break;
            case 3://摇杆
                bean.setWidth(buttonWidth*2);
                bean.setHeight(buttonHeight*2);
                break;
            case 5://十字键
                bean.setWidth(buttonWidth*2);
                bean.setHeight(buttonHeight*2);
                break;
        }
        beanList.add(bean);
        updateItem();
    }


    private void updateItem(){
        removeElements();
        initView();
        buttonConfigure.setVisibility(View.VISIBLE);
        currentIndex=-1;
        for (int i = 0; i < beanList.size(); i++) {
            addView(beanList.get(i),i);
        }
    }

    private String tips;

    private void initData(){
        boolean layoutMissing = true;
        try {
            VirtualControlLayoutReadResult result =
                    layoutRepository.load(layoutKey);
            if (result.isFound()) {
                layoutMissing = false;
                try {
                    GameMenuQuickBean[] beans =
                            new Gson().fromJson(
                                    result.getDocument().getJson(),
                                    GameMenuQuickBean[].class);
                    if (beans != null) {
                        Collections.addAll(beanList, beans);
                        if (LegacyVirtualControlActionMigration
                                .migrate(beanList)) {
                            persistLayout("action-migrated");
                        }
                    }
                }
                catch (RuntimeException error) {
                    LimeLog.warning(
                            "Ignoring corrupt virtual-control layout " +
                                    layoutKey +
                                    ": " +
                                    error.getMessage());
                }
            }
        }
        catch (IOException error) {
            LimeLog.warning(
                    "Unable to read virtual-control layout " +
                            layoutKey +
                            ": " +
                    error.getMessage());
        }
        if (layoutMissing) {
            int viewportWidth = frame_layout.getWidth();
            int viewportHeight = frame_layout.getHeight();
            if (viewportWidth <= 0 || viewportHeight <= 0) {
                viewportWidth = context.getResources()
                        .getDisplayMetrics().widthPixels;
                viewportHeight = context.getResources()
                        .getDisplayMetrics().heightPixels;
            }
            List<GameMenuQuickBean> defaults =
                            VirtualControlDefaultLayoutFactory.create(
                                    layoutKey,
                                    viewportWidth,
                                    viewportHeight,
                                    buttonWidth,
                                    buttonHeight,
                                    context.getResources());
            if (!defaults.isEmpty()) {
                beanList.addAll(defaults);
            }
        }
        if(getControllerMode()==VirtualControlEditMode.ACTIVE&& beanList.isEmpty()){
            if (layoutKey.getOrientation() ==
                            VirtualControlLayoutOrientation.PORTRAIT &&
                    !getSettings()
                            .isAutomaticScreenOrientationEnabled()) {
                return;
            }
            if(TextUtils.isEmpty(tips)){
                tips=context.getString(
                        R.string.virtual_control_empty_hint);
                UiToast.makeText(context,tips,UiToast.LENGTH_LONG).show();
            }
//            switchMode(VirtualControlEditMode.MOVE_BUTTONS);
            return;
        }
        for (int i = 0; i < beanList.size(); i++) {
            GameMenuQuickBean bean=beanList.get(i);
            addView(bean,i);
        }
    }

    private void persistLayout(String source) {
        try {
            layoutRepository.save(
                    layoutKey,
                    VirtualControlLayoutDocument.fromJson(
                            new Gson().toJson(beanList)));
        }
        catch (IOException | IllegalArgumentException error) {
            LimeLog.warning(
                    "Unable to persist " + source +
                            " virtual-control layout " +
                            layoutKey +
                            ": " +
                            error.getMessage());
        }
    }

    private void addView(GameMenuQuickBean bean,int i){
        KeyboardVirtualControllerElement element = null;
        //普通按钮
        if(bean.getBtnType()==4){
            //游戏按钮
            if(bean.isGamePad()){
                //扳机按钮
                if(bean.getCode()==ControllerPacket.PADDLE3_FLAG||bean.getCode()==ControllerPacket.PADDLE4_FLAG){
                    element=new TriggerGamePad(this,bean.getId(),bean.getName(),bean.getCode()==ControllerPacket.PADDLE3_FLAG,bean.isSwitchMode(),1,context);
                }else{
                    element=KeyBoardControllerConfigurationLoader.createDigitalButtonGamePad(bean.getId(),bean.getCode(),0,1,bean.getName(),-1,bean.isSwitchMode(),this,context);
                }
            }else{
                VirtualControlAction localAction = bean.getLocalAction();
                if (localAction != null) {
                    element = KeyBoardControllerConfigurationLoader
                            .createLocalActionButton(
                                    bean.getId(),
                                    localAction,
                                    1,
                                    bean.getName(),
                                    -1,
                                    this,
                                    context);
                }
                else if (!TextUtils.isEmpty(bean.getCodes())) {
                    element = KeyBoardControllerConfigurationLoader
                            .createKeyChordButton(
                                    bean.getId(),
                                    bean.getCodes(),
                                    1,
                                    bean.getName(),
                                    -1,
                                    bean.isSwitchMode(),
                                    this,
                                    context);
                }
            }
        }
        //鼠标
        if(bean.getBtnType()==1){
            element = KeyBoardControllerConfigurationLoader
                    .createMouseButton(
                            bean.getId(),
                            bean.getCode(),
                            1,
                            bean.getName(),
                            -1,
                            bean.isSwitchMode(),
                            this,
                            context);
        }
        //触控板
        if(bean.getBtnType()==2){
            element=KeyBoardControllerConfigurationLoader.createDigitalTouchButton(bean.getId(),bean.getCode(),1,1,bean.getName(),-1,this,context);
        }
        //摇杆
        if(bean.getBtnType()==3){
            if(bean.isGamePad()){
                if(bean.isFreeStick()){
                    element=new AnalogStickFreeGamePad(this,bean.getId(),context,bean.getCode()==ControllerPacket.PADDLE5_FLAG,bean.isFixedStrokeFreeStick(),bean.isFreeeStickDrawNormal());
                }else{
                    element=new AnalogStickGamePad(this,bean.getId(),context,bean.getCode()==ControllerPacket.PADDLE5_FLAG,bean.isFixedStrokeFreeStick());
                }
            }else{
                String[] tips=bean.getDesc().split("-");
                String[] keys=bean.getCodes().split(",");
                int[] intArray = new int[keys.length];
                for (int j = 0; j < keys.length; j++) {
                    intArray[j] = Integer.parseInt(keys[j]); // 自动拓宽转换 (Widening Primitive Conversion)
                }
                if(bean.isFreeStick()){
                    element=KeyBoardControllerConfigurationLoader.createKeyBoardAnalogStickButton2(this,bean.getId(),context,intArray,tips);
                }else{
                    element=KeyBoardControllerConfigurationLoader.createKeyBoardAnalogStickButton(this,bean.getId(),context,intArray,tips);
                }
            }
        }
        //十字键
        if(bean.getBtnType()==5){
            if(bean.isGamePad()){
                String[] tips=bean.getDesc().split("-");
                element=KeyBoardControllerConfigurationLoader.createDiaitalPadButtonGamePad(bean.getId(),bean.getCode()==ControllerPacket.PADDLE2_FLAG , tips,this, context);
            }else{
                String[] keys=bean.getCodes().split(",");
                String[] tips=bean.getDesc().split("-");
                element=KeyBoardControllerConfigurationLoader.createDiaitalPadButton(bean.getId(),
                        Integer.parseInt(keys[2]), Integer.parseInt(keys[3]), Integer.parseInt(keys[0]), Integer.parseInt(keys[1]),
                        tips,this, context);
            }
        }

        if(element!=null){
            element.setShapeType(bean.getShapeType());
            element.setTag(new TagInfo(i,isGamePadMode));
            element.setOnItemClickListener(tag -> {
                updateItem(tag.index);
            });
            element.setOpacity(
                    getSettings().getControlOpacityPercent());
            addElement(element,bean.getmLeft(),bean.getmTop(),bean.getWidth(),bean.getHeight());
        }
    }


    private void updateItem(int index){
        if(beanList.size()<index){
            return;
        }
        lv_left_view.setVisibility(View.VISIBLE);
        View lastView=null;
        if(currentIndex!=-1){
            lastView=frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode));
        }
        currentIndex=index;
        if(lastView!=null){
            lastView.invalidate();
        }
        txName.setText(context.getString(
                R.string.keyboard_current_button_format,
                beanList.get(index).getName()));
        txDesc.setText(context.getString(
                R.string.keyboard_key_value_format,
                beanList.get(index).getDesc()));
        tx_margin.setText(context.getString(
                R.string.keyboard_coordinates_format,
                beanList.get(index).getmLeft(),
                beanList.get(index).getmTop()));

        if(beanList.get(index).getBtnType()==4||beanList.get(index).getBtnType()==2){
            cb_round.setChecked(beanList.get(index).getShapeType()==1);
            cb_round.setVisibility(beanList.get(index).getBtnType()==4?View.VISIBLE:View.GONE);

            cb_switch_mode.setChecked(beanList.get(index).isSwitchMode());
            if(beanList.get(index).getBtnType()==4){
                cb_switch_mode.setVisibility(
                        beanList.get(index).getLocalAction() == null &&
                                !TextUtils.isEmpty(
                                        beanList.get(index).getCodes())
                                ? View.VISIBLE
                                : View.GONE);
            }else{
                cb_switch_mode.setVisibility(View.GONE);
            }
            lv_left_view.findViewById(R.id.lv_zoom_wh).setVisibility(beanList.get(index).getShapeType()==1?View.VISIBLE:View.GONE);
            txZoom.setVisibility(beanList.get(index).getShapeType()==1?View.GONE:View.VISIBLE);
            sb_zoom_x.setVisibility(beanList.get(index).getShapeType()==1?View.GONE:View.VISIBLE);

            tx_zoom_w.setText(context.getString(
                    R.string.keyboard_width_scale_format,
                    beanList.get(index).getZoomW()));
            tx_zoom_h.setText(context.getString(
                    R.string.keyboard_height_scale_format,
                    beanList.get(index).getZoomH()));
            sb_zoom_w.setProgress(BUTTON_SCALE_RANGE.valueToProgress(
                    beanList.get(index).getZoomW()));
            sb_zoom_h.setProgress(BUTTON_SCALE_RANGE.valueToProgress(
                    beanList.get(index).getZoomH()));
        }else{
            cb_switch_mode.setVisibility(View.GONE);
            cb_round.setVisibility(View.GONE);
            lv_left_view.findViewById(R.id.lv_zoom_wh).setVisibility(View.GONE);
            txZoom.setVisibility(View.VISIBLE);
            sb_zoom_x.setVisibility(View.VISIBLE);
            txZoom.setText(context.getString(
                    R.string.keyboard_scale_format,
                    beanList.get(index).getZoom()));
            sb_zoom_x.setProgress(BUTTON_SCALE_RANGE.valueToProgress(
                    beanList.get(index).getZoom()));
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) frame_layout.findViewWithTag(new TagInfo(currentIndex,isGamePadMode)).getLayoutParams();
        beanList.get(currentIndex).setmLeft(layoutParams.leftMargin);
        beanList.get(currentIndex).setmTop(layoutParams.topMargin);
    }

    private void save(){
        try {
            layoutRepository.save(
                    layoutKey,
                    VirtualControlLayoutDocument.fromJson(
                            new Gson().toJson(beanList)));
        }
        catch (IOException | IllegalArgumentException error) {
            LimeLog.warning(
                    "Unable to save virtual-control layout " +
                            layoutKey +
                            ": " +
                            error.getMessage());
            UiToast.makeText(
                    context,
                    R.string.virtual_control_layout_save_failed,
                    UiToast.LENGTH_SHORT).show();
            return;
        }
        this.currentMode=VirtualControlEditMode.ACTIVE;
        buttonConfigure.setVisibility(View.GONE);
        lv_left_view.setVisibility(View.GONE);
        currentIndex=-1;
        for (KeyboardVirtualControllerElement element : elements) {
            element.invalidate();
        }
    }


    @Override
    public void switchMode(VirtualControlEditMode currentMode){
        this.currentMode=currentMode;
        String message="";
        switch (currentMode){
            case ACTIVE:
                message=context.getString(
                        R.string.virtual_control_mode_active);
                buttonConfigure.setVisibility(View.GONE);
                lv_left_view.setVisibility(View.GONE);
                break;
            case MOVE_BUTTONS:
                message=context.getString(
                        R.string.virtual_control_mode_edit);
                buttonConfigure.setVisibility(View.VISIBLE);
                break;
        }
        if(TextUtils.isEmpty(message)){
            return;
        }
        for (KeyboardVirtualControllerElement element : elements) {
            element.invalidate();
        }

    }

    Handler getHandler() {
        return handler;
    }
    
    public TagInfo getCurrentIndex() {
        return new TagInfo(currentIndex,isGamePadMode);
    }

    @Override
    public void hide() {
        handler.removeCallbacksAndMessages(null);
        for (KeyboardVirtualControllerElement element : elements) {
            element.setVisibility(View.GONE);
        }
        isShow=false;
        lv_left_view.setVisibility(View.GONE);
        buttonConfigure.setVisibility(View.GONE);
        this.currentMode = VirtualControlEditMode.NONE;
    }

    @Override
    public void show() {
        if (destroyed) {
            return;
        }
//        showEnabledElements();
        isShow=true;
        this.currentMode = VirtualControlEditMode.ACTIVE;
        refreshLayout();
    }

    @Override
    public void toggleVisibility() {
        if (destroyed) {
            return;
        }
        if (isShow) {
            hide();
        } else {
            show();
        }
    }

    @Override
    public boolean isVisible() {
        return !destroyed && isShow;
    }

    public void removeElements() {
        for (KeyboardVirtualControllerElement element : elements) {
            frame_layout.removeView(element);
        }
        elements.clear();

        frame_layout.removeView(buttonConfigure);
        frame_layout.removeView(lv_left_view);
    }

    public void setOpacity(int opacity) {
        for (KeyboardVirtualControllerElement element : elements) {
            element.setOpacity(opacity);
        }
    }

    public void addElement(KeyboardVirtualControllerElement element, int x, int y, int width, int height) {
        elements.add(element);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.setMargins(x, y, 0, 0);
        frame_layout.addView(element, layoutParams);
    }

    public List<KeyboardVirtualControllerElement> getElements() {
        return elements;
    }

    public void initView(){
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity= Gravity.END;
        frame_layout.addView(buttonConfigure, params);
        FrameLayout.LayoutParams params1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params1.gravity=Gravity.START;
        frame_layout.addView(lv_left_view, params1);
        buttonConfigure.setVisibility(this.currentMode==VirtualControlEditMode.MOVE_BUTTONS?View.VISIBLE:View.GONE);
        lv_left_view.setVisibility(View.GONE);
    }


    @Override
    public void refreshLayout() {
        if(destroyed || this.currentMode==VirtualControlEditMode.NONE){
            return;
        }
        removeElements();
        VirtualControlSettings settings = getSettings();
        String name = settings.getKeyboardLayoutId();
        if(isGamePadMode){
            name = settings.getGamepadLayoutId();
        }
        VirtualControlLayoutOrientation orientation =
                resolveLayoutOrientation()
                        ? VirtualControlLayoutOrientation.LANDSCAPE
                        : VirtualControlLayoutOrientation.PORTRAIT;
        layoutKey =
                isGamePadMode
                        ? VirtualControlLayoutKey.gamepad(
                                name,
                                orientation)
                        : VirtualControlLayoutKey.keyboard(
                                name,
                                orientation);
        LimeLog.info("Refreshing virtual-control layout: " + layoutKey);
        initView();
        currentIndex=-1;
        beanList.clear();
        initData();
    }


    @Override
    public VirtualControlEditMode getControllerMode() {
        return currentMode;
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        removeElements();
        isShow = false;
        currentMode = VirtualControlEditMode.NONE;
    }

    public void sendKeyEvent(KeyEvent keyEvent) {
        if (!inputGateway.isInputReady()) {
            return;
        }
        //1-鼠标 0-按键 2-摇杆 3-十字键
        if (keyEvent.getSource() == 1) {
            inputGateway.sendMouseButton(
                    keyEvent.getKeyCode(),
                    KeyEvent.ACTION_DOWN == keyEvent.getAction());
        } else {
            inputGateway.sendKeyEvent(keyEvent);
        }
        if (getSettings().isKeyboardHapticsEnabled() &&
                vibrator.hasVibrator() &&
                keyEvent.getSource() != 2) {
            AndroidVibratorCompat.vibrateOneShot(vibrator, 10);
        }
    }

    public void sendMouseMove(int x,int y){
        inputGateway.sendRelativeMouseMove(x, y);
    }

    public void sendHighResolutionScroll(boolean up) {
        inputGateway.sendHighResolutionScroll(up);
    }

    public void sendKeyChord(String codes, int action) {
        if (getSettings().isKeyboardHapticsEnabled() &&
                vibrator.hasVibrator()) {
            AndroidVibratorCompat.vibrateOneShot(vibrator, 10);
        }
        if (TextUtils.isEmpty(codes)) {
            return;
        }
        String[] keys = codes.split(",");
        int[] keyCodes = new int[keys.length];
        try {
            for (int index = 0; index < keys.length; index++) {
                keyCodes[index] = Integer.parseInt(keys[index]);
            }
        }
        catch (NumberFormatException error) {
            LimeLog.warning("Ignoring malformed virtual key chord");
            return;
        }
        for (int keyCode : keyCodes) {
            KeyEvent keyEvent = new KeyEvent(action, keyCode);
            keyEvent.setSource(0);
            inputGateway.sendKeyEvent(keyEvent);
        }
    }

    public void sendLocalAction(
            VirtualControlAction localAction,
            int action) {
        if (action == KeyEvent.ACTION_DOWN) {
            if (getSettings().isKeyboardHapticsEnabled() &&
                    vibrator.hasVibrator()) {
                AndroidVibratorCompat.vibrateOneShot(vibrator, 10);
            }
            return;
        }
        if (action == KeyEvent.ACTION_UP) {
            uiActions.performStreamUiAction(localAction);
        }
    }

    public ControllerInputContext getControllerInputContext() {
        return inputContext;
    }

    private void sendControllerInputContextInternal() {

        if (controllerHandler != null) {
            controllerHandler.reportOscState(
                    inputContext.inputMap,
                    inputContext.leftStickX,
                    inputContext.leftStickY,
                    inputContext.rightStickX,
                    inputContext.rightStickY,
                    inputContext.leftTrigger,
                    inputContext.rightTrigger
            );
        }
    }

    public void sendControllerInputContext() {
        // Cancel retransmissions of prior gamepad inputs
        handler.removeCallbacks(delayedRetransmitRunnable);

        sendControllerInputContextInternal();
        if (getSettings().isKeyboardHapticsEnabled() &&
                vibrator.hasVibrator()) {
            //摇杆不震动
            if(inputContext.inputMap!=0||inputContext.leftTrigger!=0x00||inputContext.rightTrigger!=0x00) {
                AndroidVibratorCompat.vibrateOneShot(vibrator, 10);
            }
        }
        // HACK: GFE sometimes discards gamepad packets when they are received
        // very shortly after another. This can be critical if an axis zeroing packet
        // is lost and causes an analog stick to get stuck. To avoid this, we retransmit
        // the gamepad state a few times unless another input event happens before then.
        handler.postDelayed(delayedRetransmitRunnable, 25);
        handler.postDelayed(delayedRetransmitRunnable, 50);
        handler.postDelayed(delayedRetransmitRunnable, 75);
    }


    public boolean isLandscape(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels>context.getResources().getDisplayMetrics().heightPixels;
    }

    private boolean resolveLayoutOrientation() {
        int width = frame_layout.getWidth();
        int height = frame_layout.getHeight();
        if (width > 0 && height > 0 && width != height) {
            return width > height;
        }
        return isLandscape(context);
    }

    public VirtualControlSettings getSettings() {
        return virtualControlSettingsState.get();
    }

    public InputSettings getInputSettings() {
        return inputSettingsState.get();
    }

}
