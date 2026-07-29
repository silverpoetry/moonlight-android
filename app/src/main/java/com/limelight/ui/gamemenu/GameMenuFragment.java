package com.limelight.ui.gamemenu;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.limelight.Game;
import com.limelight.GameMenu;
import com.limelight.R;
import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.utils.UiHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description
 * Date: 2024-10-20
 * Time: 16:07
 */
public class GameMenuFragment extends BaseGameMenuDialog implements View.OnClickListener{

    private void refreshMicButton() {
        if (btn_mic != null && game != null) {
            btn_mic.setBackgroundResource(game.isMicUplinkActive() ? R.drawable.ic_game_menu_btn_green_selector : R.drawable.ic_game_menu_btn_selector);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null) {
            getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode != KeyEvent.KEYCODE_BACK) {
                        return false;
                    }
                    if (event.getAction() == KeyEvent.ACTION_UP && game != null) {
                        game.handleStreamBackPressed();
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (cardEditor != null) {
            cardEditor.dismiss();
            cardEditor = null;
        }
        super.onDismiss(dialog);
        if (game != null) {
            game.cancelPendingStreamBackExit();
        }
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu;
    }

    private Button btn_performance;

    private Button btn_game_pad;

    private Button btn_v_keyboard;

    private Button btn_gamepad_mouse;

    private Button btn_screen_move;

    private TextView tx_title_battery;

    private Button btn_mic;

    private LinearLayout actionGrid;
    private final Map<Integer, Button> actionButtons = new HashMap<>();
    private GameMenuCardEditor cardEditor;

    private Game game;
    private NvConnection conn;
    private GameInputDevice device;

    @Override
    public void bindView(View v) {
        super.bindView(v);
        actionGrid = v.findViewById(R.id.game_menu_action_grid);
        collectActionButtons(v);
        rebuildActionGrid();

        v.findViewById(R.id.bt_touch_sensitivity).setOnClickListener(this);
        v.findViewById(R.id.bt_quick_list).setOnClickListener(this);
        v.findViewById(R.id.bt_touch_list).setOnClickListener(this);
        v.findViewById(R.id.btn_soft_keyboard).setOnClickListener(this);
        v.findViewById(R.id.btn_desktop).setOnClickListener(this);
        v.findViewById(R.id.btn_window).setOnClickListener(this);
        v.findViewById(R.id.bt_touch_sensitivity).setOnClickListener(this);
        v.findViewById(R.id.bt_virtual_view).setOnClickListener(this);
        v.findViewById(R.id.bt_display).setOnClickListener(this);
        v.findViewById(R.id.bt_device).setOnClickListener(this);
        v.findViewById(R.id.bt_other_setting).setOnClickListener(this);
        v.findViewById(R.id.btn_soft_function).setOnClickListener(this);
        v.findViewById(R.id.btn_customize_actions)
                .setOnClickListener(view -> showCardEditor());

        tx_title_battery=v.findViewById(R.id.tx_title_battery);
        tx_title_battery.setText(getString(
                R.string.game_menu_battery_percent,
                getPhoneBattery(getActivity())));
    }

    private void collectActionButtons(View root) {
        actionButtons.clear();
        for (GameMenuActionCatalog.Action action :
                GameMenuActionCatalog.all()) {
            Button button = root.findViewById(action.viewId);
            if (button == null) {
                continue;
            }
            detachFromParent(button);
            button.setText(action.labelRes);
            button.setContentDescription(getString(
                    action.contentDescriptionRes));
            button.setTextSize(11);
            button.setOnClickListener(this);
            actionButtons.put(action.viewId, button);
        }
        actionGrid.removeAllViews();
    }

    private void rebuildActionGrid() {
        // Buttons are reused so their active state and listeners remain
        // attached. Detach them from the old row before removing that row.
        // Removing the row alone does not clear each button's parent pointer.
        for (Button button : actionButtons.values()) {
            detachFromParent(button);
        }
        actionGrid.removeAllViews();
        List<GameMenuCardCatalog.Card> catalog = loadCardCatalog();
        GameMenuCardConfiguration.State configuration =
                GameMenuCardConfiguration.load(getActivity(), catalog);
        LinearLayout row = null;
        int column = 0;
        int displayedCount = 0;
        for (GameMenuCardCatalog.Card card : configuration.visible) {
            if (card.requiresGamepad() && device == null) {
                continue;
            }
            Button button = card.action != null ?
                    actionButtons.get(card.action.viewId) :
                    createShortcutButton(card);
            if (button == null) {
                continue;
            }
            if (column == 0) {
                row = createActionRow();
                actionGrid.addView(row);
            }
            row.addView(button, createActionCellParams(column));
            column++;
            displayedCount++;
            if (column == 4) {
                column = 0;
            }
        }
        if (row != null && column != 0) {
            while (column < 4) {
                row.addView(new Space(getActivity()),
                        createActionCellParams(column));
                column++;
            }
        }
        if (displayedCount == 0) {
            TextView empty = new TextView(getActivity());
            empty.setText(R.string.game_menu_customize_no_visible);
            empty.setTextColor(0xBFFFFFFF);
            empty.setTextSize(12);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(
                    UiHelper.dpToPx(getActivity(), 12),
                    UiHelper.dpToPx(getActivity(), 18),
                    UiHelper.dpToPx(getActivity(), 12),
                    UiHelper.dpToPx(getActivity(), 18));
            actionGrid.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        bindActionButtonFields();
        refreshActionButtonStates();
    }

    private List<GameMenuCardCatalog.Card> loadCardCatalog() {
        boolean includeBuiltInShortcuts =
                game == null ||
                        !game.prefConfig.enableClearDefaultSpecial;
        return GameMenuCardCatalog.load(
                getActivity(), includeBuiltInShortcuts);
    }

    private Button createShortcutButton(
            GameMenuCardCatalog.Card card) {
        Button button = (Button) getActivity()
                .getLayoutInflater()
                .inflate(R.layout.item_game_menu_card, actionGrid, false);
        button.setText(card.label);
        button.setContentDescription(card.contentDescription);
        button.setCompoundDrawablesWithIntrinsicBounds(
                0, card.iconRes, 0, 0);
        button.setTag(card.shortcut);
        button.setOnClickListener(this);
        return button;
    }

    private static void detachFromParent(View view) {
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

    private LinearLayout createActionRow() {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiHelper.dpToPx(getActivity(), 54));
        int margin = UiHelper.dpToPx(getActivity(), 5);
        params.setMargins(margin, margin, margin, margin);
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout.LayoutParams createActionCellParams(
            int column) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        if (column < 3) {
            params.setMarginEnd(UiHelper.dpToPx(getActivity(), 5));
        }
        return params;
    }

    private void bindActionButtonFields() {
        btn_performance = actionButtons.get(R.id.btn_performance);
        btn_game_pad = actionButtons.get(R.id.btn_game_pad);
        btn_v_keyboard = actionButtons.get(R.id.btn_v_keyboard);
        btn_gamepad_mouse = actionButtons.get(R.id.btn_gamepad_mouse);
        btn_screen_move = actionButtons.get(R.id.btn_screen_move);
        btn_mic = actionButtons.get(R.id.btn_mic);
        if (btn_performance != null) {
            btn_performance.setOnLongClickListener(view -> {
                if (game != null) {
                    game.switchHUD();
                }
                return true;
            });
        }
    }

    private void refreshActionButtonStates() {
        if (game == null) {
            return;
        }
        setActionButtonActive(
                btn_performance, game.prefConfig.enablePerfOverlay);
        setActionButtonActive(
                btn_game_pad, game.prefConfig.onscreenController);
        setActionButtonActive(
                btn_v_keyboard, game.prefConfig.enableKeyboard);
        setActionButtonActive(
                btn_screen_move, game.getScreenMoveZoom());
        refreshMicButton();
    }

    private void setActionButtonActive(Button button, boolean active) {
        if (button != null) {
            button.setBackgroundResource(
                    active ?
                            R.drawable.ic_game_menu_btn_green_selector :
                            R.drawable.ic_game_menu_btn_selector);
        }
    }

    private void showCardEditor() {
        if (cardEditor != null || getActivity() == null) {
            return;
        }
        cardEditor = new GameMenuCardEditor(
                getActivity(),
                loadCardCatalog(),
                new GameMenuCardEditor.Listener() {
                    @Override
                    public void onSaved() {
                        rebuildActionGrid();
                    }

                    @Override
                    public void onDismissed() {
                        cardEditor = null;
                    }
                });
        cardEditor.show();
    }

    @Override
    public float getDimAmount() {
        return super.getDimAmount();
    }

    @Override
    public void onClick(View v) {
        if (game != null) {
            game.cancelPendingStreamBackExit();
        }

        if (v.getTag() instanceof GameMenuShortcutCatalog.Entry) {
            executeShortcut(
                    ((GameMenuShortcutCatalog.Entry) v.getTag()).shortcut);
            return;
        }

        //操作 或 显示器
        if(v.getId()==R.id.btn_soft_function || v.getId()==R.id.btn_display_1){
            GameFunctionFragment fragment=new GameFunctionFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("操作");
            fragment.setOnClick(new GameFunctionFragment.onClick() {
                @Override
                public void click(String title, int index) {
                    if(conn==null){
                        return;
                    }
                    switch (index){
                        case 0://注销
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_X});
                            new Handler().postDelayed((() -> sendKeys(new short[]{KeyboardTranslator.VK_U, KeyboardTranslator.VK_I})), 200);
                            break;
                        case 1://关机
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_X});
                            new Handler().postDelayed((() -> sendKeys(new short[]{KeyboardTranslator.VK_U, KeyboardTranslator.VK_U})), 200);
                            break;
                        case 2://睡眠
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_X});
                            new Handler().postDelayed((() -> sendKeys(new short[]{KeyboardTranslator.VK_U, KeyboardTranslator.VK_S})), 200);
                            break;
                        case 3://重启
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_X});
                            new Handler().postDelayed((() -> sendKeys(new short[]{KeyboardTranslator.VK_U, KeyboardTranslator.VK_R})), 200);
                            break;
                        case 4://任务管理器
                            sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE});
                            break;
                        case 5://发送剪切板
                            if(game!=null){
                                game.sendClipboardText();
                            }
                            break;
                        case 6://打开剪切板
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_V});
                            break;
                        case 7://系统设置
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_I});
                            break;
                        case 8://我的电脑
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_E});
                            break;
                        case 9://移动中心
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_X});
                            break;
                        case 10://Win+P
                            sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_P});
                            break;
                        case 11://显示器1
                            sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F1});
                            break;
                        case 12://显示器2
                            sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F2});
                            break;
                        case 13://显示器3
                            sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F3});
                            break;
                        case 14://显示器4
                            sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F4});
                            break;
                    }
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        //断开链接
        if(v.getId()==R.id.btn_unlink){
            dismiss();
            if(game!=null){
                game.finish();
            }
            return;
        }
        if(v.getId()==R.id.btn_exit){
            dismiss();
            if(game!=null){
                game.isQuitSteamingFlag=true;
                game.disconnect();
            }
            return;
        }

        if(v.getId()==R.id.btn_swicth_screen){
            dismiss();
            if(game!=null){
                game.switchLandscapePortraitScreen();
            }
            return;
        }
        if(v.getId()==R.id.btn_game_pad){
            if(game!=null){
                game.showHideVirtualController();
                btn_game_pad.setBackgroundResource(game.prefConfig.onscreenController?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);
            }
            return;
        }

        if(v.getId()==R.id.btn_performance){
            if(game!=null){
                game.showHUD();
                btn_performance.setBackgroundResource(game.prefConfig.enablePerfOverlay?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);
            }
            return;
        }

        if(v.getId()==R.id.btn_v_keyboard){
            if(game!=null){
                game.showHideKeyboardController();
                btn_v_keyboard.setBackgroundResource(game.prefConfig.enableKeyboard?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);
            }
            return;
        }

        if(v.getId()==R.id.btn_keyboard){
            if(game!=null){
                game.showHidekeyBoardLayoutController();
            }
            return;
        }

        if(v.getId()==R.id.btn_soft_keyboard){
            dismiss();
            if(game!=null){
                showKeyboard();
            }
            return;
        }

        if(v.getId()==R.id.btn_screen_move){
            dismiss();
            if(game!=null){
                game.screenMoveZoom();
            }
            return;
        }

        if(v.getId()==R.id.btn_desktop){
            if(conn!=null){
                sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D});
            }
            return;
        }

        if(v.getId()==R.id.btn_window){
            if(conn!=null){
                sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_TAB});
            }
            return;
        }

        if(v.getId()==R.id.btn_hdr){
            if(conn!=null){
                sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B});
            }
            return;
        }

        if(v.getId()==R.id.btn_mic){
//            if(conn!=null){
//                sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F12});
//            }
            if(game!=null){
                game.switchMic();
                refreshMicButton();
                btn_mic.postDelayed(this::refreshMicButton, 400);
                btn_mic.postDelayed(this::refreshMicButton, 1200);
            }
            return;
        }

        if(v.getId()==R.id.btn_gamepad_mouse){
            if (device != null) {
                List<GameMenu.MenuOption> menuOptions=device.getGameMenuOptions();
                if(menuOptions!=null&& !menuOptions.isEmpty()){
                    menuOptions.get(0).runnable.run();
                }
            }
            return;
        }


        if(v.getId()==R.id.bt_quick_list){
            GameListQuickFragment fragment=new GameListQuickFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("快捷键(字体倾斜项可长按删除)");
            if(game!=null){
                fragment.setEnableClearDefaultSpecial(game.prefConfig.enableClearDefaultSpecial);
            }
            fragment.setOnClick(new GameListQuickFragment.onClick() {
                @Override
                public void click(GameMenuQuickBean bean) {
                    executeShortcut(bean);
                }
            });
            fragment.setOnShortcutsChangedListener(
                    this::rebuildActionGrid);
            fragment.show(getFragmentManager());
            return;
        }
        if(v.getId()==R.id.bt_touch_list){
            GameListMouseFragment fragment=new GameListMouseFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("鼠标与触控");
            fragment.setOnClick(new GameListMouseFragment.onClick() {
                @Override
                public void click(String title, int index) {
                    if(game==null||index<0){
                        return;
                    }
                    if(index==7){
                        game.switchMouseLocalCursor();
                        return;
                    }
                    if(index==8){
                        game.toggleAbsoluteMouseMode();
                        return;
                    }
                    if(index==9){
                        if(conn!=null){
                            sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_N});
                        }
                        return;
                    }
                    game.switchMouseModel(index);
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_touch_sensitivity){
            GameTouchFragment fragment=new GameTouchFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("触控灵敏度");
            fragment.setPrefConfig(game==null?new PreferenceConfiguration():game.prefConfig);
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_display){
            GameDisplayFragment fragment=new GameDisplayFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_display_title);
            fragment.setListener(new GameDisplayFragment.Listener() {
                @Override
                public void onDisplayConfigurationApplied() {
                    dismiss();
                    if(game!=null){
                        game.finish();
                    }
                }
            });
            fragment.setPrefConfig(game==null?new PreferenceConfiguration():game.prefConfig);
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_device){
            GameDisplayDeviceFragment fragment=new GameDisplayDeviceFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_devices_title);
            fragment.setListener(() -> {
                if (game != null) {
                    game.setDualSenseTrigger();
                }
            });
            fragment.setPrefConfig(game==null?new PreferenceConfiguration():game.prefConfig);
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId() == R.id.bt_other_setting){
            GameDisplaySettingFragment fragment=new GameDisplaySettingFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_misc_title);
            fragment.setOnClick(new GameDisplaySettingFragment.onClick() {
                @Override
                public void click(int index,boolean flag) {
                    if(game==null){
                        return;
                    }
                    //悬浮球
                    if(index==0){
                        if(flag){
                            game.showFloatView();
                            return;
                        }
                        game.hideFloatView();
                        return;
                    }
                    //显示震动信息
                    if(index==1){
                        game.switchPerformanceRumbleHUD();
                        return;
                    }
                    //性能信息点击
                    if(index==2){
                        game.switchPerformanceLiteHudclick();
                        return;
                    }
                    //性能信息缩放
                    if(index==3){
                        game.setPerformanceOverlayZoom();
                        return;
                    }
                    //模拟体感
                    if(index==4){
                        game.setMotionForceGyro();
                        return;
                    }
                    //性能信息 边距
                    if(index==5){
                        game.setPerformanceOverlayLiteMagin();
                        return;
                    }
                    if(index==6){
                        game.setAudioHapticsSettings();
                        return;
                    }

                }
            });
            fragment.setPrefConfig(game==null?new PreferenceConfiguration():game.prefConfig);
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_virtual_view){
            GameMenuVirtualViewFragment fragment=new GameMenuVirtualViewFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_virtual_controls_title);
            fragment.setGamePadMode(game==null? KeyBoardController.ControllerMode.NONE:game.getVirtualControllerMode());
            fragment.setGameKeyMode(game==null? KeyBoardController.ControllerMode.NONE:game.getVirtualKeyControllerMode());
            fragment.setPrefConfig(game==null?new PreferenceConfiguration():game.prefConfig);
            fragment.setListener(new GameMenuVirtualViewFragment.Listener() {
                @Override
                public void onRefreshRequested() {
                    if(game==null){
                        return;
                    }
                    game.updateVirtualView();
                }

                @Override
                public void onGamepadModeSelected(
                        KeyBoardController.ControllerMode mode) {
                    if(game==null){
                        return;
                    }
                    game.switchVirtualController(mode);
                }

                @Override
                public void onVirtualKeyModeSelected(
                        KeyBoardController.ControllerMode mode) {
                    if(game==null){
                        return;
                    }
                    game.switchVirtualKeyController(mode);
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        if (v.getId() == R.id.btn_pull_clipboard_files) {
            dismiss();
            if (game != null) {
                game.pullRemoteClipboardFiles();
            }
        }
    }

    private void showKeyboard(){
        if (!game.hasWindowFocus()) {
            new Handler().postDelayed(() -> showKeyboard(),10);
            return;
        }
        game.showKeyboard();
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setConn(NvConnection conn) {
        this.conn = conn;
    }

    public void setDevice(GameInputDevice device) {
        this.device = device;
    }


    private static byte getModifier(short key) {
        switch (key) {
            case KeyboardTranslator.VK_LSHIFT:
                return KeyboardPacket.MODIFIER_SHIFT;
            case KeyboardTranslator.VK_LCONTROL:
                return KeyboardPacket.MODIFIER_CTRL;
            case KeyboardTranslator.VK_LWIN:
                return KeyboardPacket.MODIFIER_META;
            case KeyboardTranslator.VK_LMENU:
                return KeyboardPacket.MODIFIER_ALT;
            default:
                return 0;
        }
    }

    public void sendKeys(short[] keys) {
        sendKeys(conn,keys);
    }

    public static void sendKeys(NvConnection conn, short[] keys) {
        final byte[] modifier = {(byte) 0};

        for (short key : keys) {
            conn.sendKeyboardInput(key, KeyboardPacket.KEY_DOWN, modifier[0], (byte) 0);

            // Apply the modifier of the pressed key, e.g. CTRL first issues a CTRL event (without
            // modifier) and then sends the following keys with the CTRL modifier applied
            modifier[0] |= getModifier(key);
        }

        new Handler().postDelayed((() -> {

            for (int pos = keys.length - 1; pos >= 0; pos--) {
                short key = keys[pos];

                // Remove the keys modifier before releasing the key
                modifier[0] &= ~getModifier(key);

                conn.sendKeyboardInput(key, KeyboardPacket.KEY_UP, modifier[0], (byte) 0);
            }
        }), KEY_UP_DELAY);
    }

    private static final long KEY_UP_DELAY = 25;


    private int getPhoneBattery(Context context) {
        try{
            int level = 0;
            Intent batteryInfoIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            level = batteryInfoIntent.getIntExtra("level", 0);
            int batterySum = batteryInfoIntent.getIntExtra("scale", 100);
            return 100 * level / batterySum;
        }catch (Exception e){
            e.printStackTrace();
        }
        return 100;
    }

    private void sendQuickKeylist(String codes){
        if(TextUtils.isEmpty(codes)){
            return;
        }
        String[] keys=codes.split(",");
        for (int i = 0; i < keys.length; i++) {
            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN,Integer.parseInt(keys[i]));
            keyEvent.setSource(0);
            Game.instance.onKey(null, keyEvent.getKeyCode(), keyEvent);
        }
        new Handler().postDelayed((() -> {
            for (int i = keys.length - 1; i >= 0; i--) {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP,Integer.parseInt(keys[i]));
                keyEvent.setSource(0);
                Game.instance.onKey(null, keyEvent.getKeyCode(), keyEvent);
            }
        }), KEY_UP_DELAY);
    }

    private void executeShortcut(GameMenuQuickBean shortcut) {
        if (shortcut == null) {
            return;
        }
        short[] keys = shortcut.getDatas();
        if (keys != null && keys.length > 0) {
            if (conn != null) {
                sendKeys(keys);
            }
            return;
        }
        sendQuickKeylist(shortcut.getCodes());
    }
}
