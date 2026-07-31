package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.UiHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description
 * Date: 2024-10-20
 * Time: 16:07
 */
public class GameMenuFragment extends BaseGameMenuDialog
        implements View.OnClickListener {
    public static final String FRAGMENT_TAG = "stream_game_menu";
    private static final String ARG_WIDTH_PX = "width_px";
    private static final long POWER_MENU_STEP_DELAY_MS = 200;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());
    private GameMenuHost host;
    private BackNavigationRegistration backNavigationRegistration;

    public static GameMenuFragment newInstance(int widthPx) {
        GameMenuFragment fragment = new GameMenuFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_WIDTH_PX, widthPx);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof GameMenuHost)) {
            throw new IllegalStateException(
                    "GameMenuFragment host must implement GameMenuHost");
        }
        host = (GameMenuHost) activity;
    }

    @Override
    public void onDetach() {
        mainHandler.removeCallbacksAndMessages(null);
        host = null;
        super.onDetach();
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public int getViewSize() {
        Bundle arguments = getArguments();
        return arguments != null ?
                arguments.getInt(ARG_WIDTH_PX, super.getViewSize()) :
                super.getViewSize();
    }

    private void refreshMicButton() {
        if (btn_mic != null && host != null) {
            btn_mic.setBackgroundResource(
                    host.isMicUplinkActive() ?
                            R.drawable.ic_game_menu_btn_green_selector :
                            R.drawable.ic_game_menu_btn_selector);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null) {
            if (backNavigationRegistration == null) {
                backNavigationRegistration = BackNavigationRegistration.register(
                        getDialog(), this::handleStreamBack);
            }
            getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode != KeyEvent.KEYCODE_BACK) {
                        return false;
                    }
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        handleStreamBack();
                    }
                    return true;
                }
            });
        }
    }

    private void handleStreamBack() {
        if (host != null) {
            host.handleStreamBackPressed();
        }
    }

    @Override
    public void onPause() {
        if (backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }
        super.onPause();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (cardEditor != null) {
            cardEditor.dismiss();
            cardEditor = null;
        }
        if (host != null) {
            host.onGameMenuDismissed(this);
        }
        super.onDismiss(dialog);
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
                GameMenuCardConfiguration.load(
                        host.loadGameMenuCardLayout(),
                        catalog);
        LinearLayout row = null;
        int column = 0;
        int displayedCount = 0;
        for (GameMenuCardCatalog.Card card : configuration.visible) {
            if (card.requiresGamepad() &&
                    !host.isGamepadMouseEmulationAvailable()) {
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
                !host.getStreamUiSettings()
                        .shouldHideBuiltInShortcuts();
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
                if (host != null) {
                    host.switchHUD();
                }
                return true;
            });
        }
    }

    private void refreshActionButtonStates() {
        if (host == null) {
            return;
        }
        setActionButtonActive(
                btn_performance,
                host.getStreamUiSettings()
                        .isPerformanceOverlayEnabled());
        setActionButtonActive(
                btn_game_pad,
                host.isVirtualControllerVisible());
        setActionButtonActive(
                btn_v_keyboard,
                host.isVirtualKeysVisible());
        setActionButtonActive(
                btn_screen_move, host.getScreenMoveZoom());
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
        List<GameMenuCardCatalog.Card> catalog =
                loadCardCatalog();
        GameMenuCardConfiguration.State configuration =
                GameMenuCardConfiguration.load(
                        host.loadGameMenuCardLayout(),
                        catalog);
        cardEditor = new GameMenuCardEditor(
                getActivity(),
                catalog,
                configuration,
                new GameMenuCardEditor.Listener() {
                    @Override
                    public void onSave(
                            GameMenuCardLayout layout) {
                        if (host != null) {
                            host.saveGameMenuCardLayout(layout);
                            rebuildActionGrid();
                        }
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
        if (host == null) {
            return;
        }
        host.cancelPendingStreamBackExit();

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
                    if (host == null || !host.isInputReady()) {
                        return;
                    }
                    switch (index){
                        case 0://注销
                            sendPowerMenuSequence(
                                    KeyboardTranslator.VK_I);
                            break;
                        case 1://关机
                            sendPowerMenuSequence(
                                    KeyboardTranslator.VK_U);
                            break;
                        case 2://睡眠
                            sendPowerMenuSequence(
                                    KeyboardTranslator.VK_S);
                            break;
                        case 3://重启
                            sendPowerMenuSequence(
                                    KeyboardTranslator.VK_R);
                            break;
                        case 4://任务管理器
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE});
                            break;
                        case 5://发送剪切板
                            host.sendClipboardText();
                            break;
                        case 6://打开剪切板
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_V});
                            break;
                        case 7://系统设置
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_I});
                            break;
                        case 8://我的电脑
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_E});
                            break;
                        case 9://移动中心
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_X});
                            break;
                        case 10://Win+P
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_P});
                            break;
                        case 11://显示器1
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F1});
                            break;
                        case 12://显示器2
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F2});
                            break;
                        case 13://显示器3
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F3});
                            break;
                        case 14://显示器4
                            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F4});
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
            host.requestStreamDisconnect();
            return;
        }
        if(v.getId()==R.id.btn_exit){
            dismiss();
            host.requestStreamQuit();
            return;
        }

        if(v.getId()==R.id.btn_swicth_screen){
            dismiss();
            host.switchLandscapePortraitScreen();
            return;
        }
        if(v.getId()==R.id.btn_game_pad){
            host.showHideVirtualController();
            setActionButtonActive(
                    btn_game_pad,
                    host.isVirtualControllerVisible());
            return;
        }

        if(v.getId()==R.id.btn_performance){
            host.showHUD();
            setActionButtonActive(
                    btn_performance,
                    host.getStreamUiSettings()
                            .isPerformanceOverlayEnabled());
            return;
        }

        if(v.getId()==R.id.btn_v_keyboard){
            host.showHideKeyboardController();
            setActionButtonActive(
                    btn_v_keyboard,
                    host.isVirtualKeysVisible());
            return;
        }

        if(v.getId()==R.id.btn_keyboard){
            host.showHidekeyBoardLayoutController();
            return;
        }

        if(v.getId()==R.id.btn_soft_keyboard){
            dismiss();
            host.requestSoftKeyboard();
            return;
        }

        if(v.getId()==R.id.btn_screen_move){
            dismiss();
            host.screenMoveZoom();
            return;
        }

        if(v.getId()==R.id.btn_desktop){
            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D});
            return;
        }

        if(v.getId()==R.id.btn_window){
            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_TAB});
            return;
        }

        if(v.getId()==R.id.btn_hdr){
            sendKeyboardChord(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B});
            return;
        }

        if(v.getId()==R.id.btn_mic){
            host.switchMic();
            refreshMicButton();
            btn_mic.postDelayed(this::refreshMicButton, 400);
            btn_mic.postDelayed(this::refreshMicButton, 1200);
            return;
        }

        if(v.getId()==R.id.btn_gamepad_mouse){
            host.toggleGamepadMouseEmulation();
            return;
        }


        if(v.getId()==R.id.bt_quick_list){
            GameListQuickFragment fragment=new GameListQuickFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("快捷键(字体倾斜项可长按删除)");
            fragment.setHideBuiltInShortcuts(
                    host.getStreamUiSettings()
                            .shouldHideBuiltInShortcuts());
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
                    if (host == null || index < 0) {
                        return;
                    }
                    if(index==7){
                        host.switchMouseLocalCursor();
                        return;
                    }
                    if(index==8){
                        host.toggleAbsoluteMouseMode();
                        return;
                    }
                    if(index==9){
                        sendKeyboardChord(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_N});
                        return;
                    }
                    host.switchMouseModel(index);
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_touch_sensitivity){
            GameTouchFragment fragment=new GameTouchFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("触控灵敏度");
            fragment.setSettings(
                    host.getInputSettings(),
                    host.getControllerSettings());
            fragment.setListener(new GameTouchFragment.Listener() {
                @Override
                public void onInputSettingsUpdate(
                        InputSettingsUpdate update) {
                    if (host != null) {
                        host.applyInputSettingsUpdate(update);
                    }
                }

                @Override
                public void onControllerSettingsUpdate(
                        ControllerSettingsUpdate update) {
                    if (host != null) {
                        host.applyControllerSettingsUpdate(update);
                    }
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_display){
            GameDisplayFragment fragment =
                    GameDisplayFragment.newInstance(true);
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_device){
            GameDisplayDeviceFragment fragment=new GameDisplayDeviceFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_devices_title);
            fragment.setListener(
                    new GameDisplayDeviceFragment.Listener() {
                        @Override
                        public void onApplyAdaptiveTrigger() {
                            if (host != null) {
                                host.applyDualSenseTriggerSettings();
                            }
                        }

                        @Override
                        public void onControllerSettingsUpdate(
                                ControllerSettingsUpdate update) {
                            if (host != null) {
                                host.applyControllerSettingsUpdate(
                                        update);
                            }
                        }
                    });
            fragment.setSettings(host.getControllerSettings());
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId() == R.id.bt_other_setting){
            GameDisplaySettingFragment fragment=new GameDisplaySettingFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_misc_title);
            fragment.setSettings(
                    host.getStreamUiSettings(),
                    host.getInputSettings(),
                    host.getControllerSettings(),
                    host.getStreamAudioSettings());
            fragment.setListener(
                    new GameDisplaySettingFragment.Listener() {
                @Override
                public void onStreamUiSettingsUpdate(
                        StreamUiSettingsUpdate update) {
                    if (host != null) {
                        host.applyStreamUiSettingsUpdate(update);
                    }
                }

                @Override
                public void onInputSettingsUpdate(
                        InputSettingsUpdate update) {
                    if (host != null) {
                        host.applyInputSettingsUpdate(update);
                    }
                }

                @Override
                public void onControllerSettingsUpdate(
                        ControllerSettingsUpdate update) {
                    if (host != null) {
                        host.applyControllerSettingsUpdate(update);
                    }
                }

                @Override
                public void onStreamAudioSettingsUpdate(
                        StreamAudioSettingsUpdate update) {
                    if (host != null) {
                        host.applyStreamAudioSettingsUpdate(update);
                    }
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_virtual_view){
            GameMenuVirtualViewFragment fragment=new GameMenuVirtualViewFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_virtual_controls_title);
            fragment.setGamePadMode(host.getVirtualControllerMode());
            fragment.setGameKeyMode(host.getVirtualKeyControllerMode());
            fragment.setSettings(
                    host.getVirtualControlSettings());
            fragment.setOnscreenControllerRumbleEnabled(
                    host.isOnscreenControllerRumbleEnabled());
            fragment.setListener(new GameMenuVirtualViewFragment.Listener() {
                @Override
                public void onRefreshRequested() {
                    if (host == null) {
                        return;
                    }
                    host.updateVirtualView();
                }

                @Override
                public void onVirtualControlSettingsUpdate(
                        VirtualControlSettingsUpdate<?> update) {
                    if (host != null) {
                        host.applyVirtualControlSettingsUpdate(
                                update);
                    }
                }

                @Override
                public void onOnscreenControllerRumbleChanged(
                        boolean enabled) {
                    if (host != null) {
                        host.setOnscreenControllerRumbleEnabled(
                                enabled);
                    }
                }

                @Override
                public void onGamepadModeSelected(
                        KeyBoardController.ControllerMode mode) {
                    if (host == null) {
                        return;
                    }
                    host.switchVirtualController(mode);
                }

                @Override
                public void onVirtualKeyModeSelected(
                        KeyBoardController.ControllerMode mode) {
                    if (host == null) {
                        return;
                    }
                    host.switchVirtualKeyController(mode);
                }
            });
            fragment.show(getFragmentManager());
            return;
        }

        if (v.getId() == R.id.btn_pull_clipboard_files) {
            dismiss();
            host.pullRemoteClipboardFiles();
        }
    }


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

    private void sendKeyboardChord(short[] keyCodes) {
        if (host != null) {
            host.sendKeyboardChord(keyCodes);
        }
    }

    private void sendPowerMenuSequence(int actionKey) {
        sendKeyboardChord(new short[]{
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_X});
        mainHandler.postDelayed(
                () -> sendKeyboardChord(new short[]{
                        KeyboardTranslator.VK_U, (short) actionKey}),
                POWER_MENU_STEP_DELAY_MS);
    }

    private void sendQuickKeylist(String codes) {
        if (TextUtils.isEmpty(codes) || host == null) {
            return;
        }
        String[] encodedKeys = codes.split(",");
        int[] keyCodes = new int[encodedKeys.length];
        for (int index = 0; index < encodedKeys.length; index++) {
            keyCodes[index] = Integer.parseInt(encodedKeys[index]);
        }
        host.sendAndroidKeyChord(keyCodes);
    }

    private void executeShortcut(GameMenuQuickBean shortcut) {
        if (shortcut == null) {
            return;
        }
        short[] keys = shortcut.getDatas();
        if (keys != null && keys.length > 0) {
            sendKeyboardChord(keys);
            return;
        }
        sendQuickKeylist(shortcut.getCodes());
    }
}
