package com.limelight.ui.gamemenu;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.activity.ComponentDialog;
import androidx.annotation.NonNull;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
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
    private GameMenuHostProvider hostProvider;
    private GameMenuHost host;
    private GameMenuState menuState;
    private BackNavigationRegistration backNavigationRegistration;

    public static GameMenuFragment newInstance(int widthPx) {
        GameMenuFragment fragment = new GameMenuFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_WIDTH_PX, widthPx);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof GameMenuHostProvider)) {
            throw new IllegalStateException(
                    "GameMenuFragment host must provide GameMenuHost");
        }
        hostProvider = (GameMenuHostProvider) context;
    }

    @Override
    public void onDetach() {
        mainHandler.removeCallbacksAndMessages(null);
        host = null;
        menuState = null;
        hostProvider = null;
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

    public void refreshMicrophoneState() {
        GameMenuHost currentHost = resolveHost();
        if (btn_mic != null && currentHost != null) {
            menuState = currentHost.getState();
            btn_mic.setBackgroundResource(
                    menuState.isMicrophoneActive() ?
                            R.drawable.ic_game_menu_btn_green_selector :
                            R.drawable.ic_game_menu_btn_selector);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null) {
            if (!(dialog instanceof ComponentDialog)) {
                throw new IllegalStateException(
                        "Game menu dialog must support AndroidX back dispatch");
            }
            if (backNavigationRegistration == null) {
                backNavigationRegistration = BackNavigationRegistration.register(
                        (ComponentDialog) dialog,
                        this::handleStreamBack);
            }
        }
    }

    private void handleStreamBack() {
        GameMenuHost currentHost = resolveHost();
        if (currentHost != null) {
            currentHost.handleStreamBackPressed();
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
        GameMenuHost currentHost = resolveHost();
        if (currentHost != null) {
            currentHost.onGameMenuDismissed(this);
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

    private Button btn_audio_mute;

    private LinearLayout actionGrid;
    private final Map<Integer, Button> actionButtons = new HashMap<>();
    private GameMenuCardEditor cardEditor;

    @Override
    public void bindView(View v) {
        super.bindView(v);
        host = resolveHost();
        if (host == null) {
            throw new IllegalStateException(
                    "GameMenuHost is not initialized");
        }
        menuState = host.getState();
        actionGrid = v.findViewById(R.id.game_menu_action_grid);
        createActionButtons();
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
        v.findViewById(R.id.btn_soft_function).setOnClickListener(this);
        v.findViewById(R.id.btn_customize_actions)
                .setOnClickListener(view -> showCardEditor());

        tx_title_battery=v.findViewById(R.id.tx_title_battery);
        int batteryPercent = menuState.getBatteryPercent();
        tx_title_battery.setText(
                batteryPercent == GameMenuState.UNKNOWN_BATTERY_PERCENT ?
                        getString(R.string.game_menu_battery_unknown) :
                        getString(
                                R.string.game_menu_battery_percent,
                                batteryPercent));
    }

    private void createActionButtons() {
        actionButtons.clear();
        for (GameMenuActionCatalog.Action action :
                GameMenuActionCatalog.all()) {
            Button button = inflateCardButton(
                    getString(action.labelRes),
                    getString(action.contentDescriptionRes),
                    action.iconRes);
            button.setId(action.viewId);
            button.setOnClickListener(this);
            actionButtons.put(action.viewId, button);
        }
    }

    private void rebuildActionGrid() {
        menuState = host.getState();
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
                        menuState.getCardLayout(),
                        catalog);
        LinearLayout row = null;
        int column = 0;
        int displayedCount = 0;
        for (GameMenuCardCatalog.Card card : configuration.visible) {
            if (card.requiresGamepad() &&
                    !menuState.isMouseEmulationAvailable()) {
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
                !menuState.getUiSettings()
                        .shouldHideBuiltInShortcuts();
        return GameMenuCardCatalog.load(
                getActivity(),
                menuState.getShortcuts(),
                includeBuiltInShortcuts);
    }

    private Button createShortcutButton(
            GameMenuCardCatalog.Card card) {
        Button button = inflateCardButton(
                card.label,
                card.contentDescription,
                card.iconRes);
        button.setTag(card.shortcut);
        button.setOnClickListener(this);
        return button;
    }

    private Button inflateCardButton(
            CharSequence label,
            CharSequence contentDescription,
            int iconRes) {
        Button button = (Button) getActivity()
                .getLayoutInflater()
                .inflate(R.layout.item_game_menu_card, actionGrid, false);
        button.setText(label);
        button.setContentDescription(contentDescription);
        button.setCompoundDrawablesWithIntrinsicBounds(
                0, iconRes, 0, 0);
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
        btn_audio_mute = actionButtons.get(R.id.btn_audio_mute);
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
        menuState = host.getState();
        setActionButtonActive(
                btn_performance,
                menuState.getUiSettings()
                        .isPerformanceOverlayEnabled());
        setActionButtonActive(
                btn_game_pad,
                menuState.isVirtualControllerVisible());
        setActionButtonActive(
                btn_v_keyboard,
                menuState.isVirtualKeysVisible());
        setActionButtonActive(
                btn_screen_move, menuState.isScreenMoveZoom());
        setActionButtonActive(
                btn_mic, menuState.isMicrophoneActive());
        setActionButtonActive(
                btn_audio_mute,
                menuState.getAudioSettings().isMuted());
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
        menuState = host.getState();
        List<GameMenuCardCatalog.Card> catalog =
                loadCardCatalog();
        GameMenuCardConfiguration.State configuration =
                GameMenuCardConfiguration.load(
                        menuState.getCardLayout(),
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
        menuState = host.getState();
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
                    if (host == null ||
                            !host.getState().isInputReady()) {
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
            fragment.show(getParentFragmentManager());
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
            host.toggleVirtualGamepad();
            menuState = host.getState();
            setActionButtonActive(
                    btn_game_pad,
                    menuState.isVirtualControllerVisible());
            return;
        }

        if(v.getId()==R.id.btn_performance){
            host.showHUD();
            menuState = host.getState();
            setActionButtonActive(
                    btn_performance,
                    menuState.getUiSettings()
                            .isPerformanceOverlayEnabled());
            return;
        }

        if(v.getId()==R.id.btn_v_keyboard){
            host.toggleVirtualKeys();
            menuState = host.getState();
            setActionButtonActive(
                    btn_v_keyboard,
                    menuState.isVirtualKeysVisible());
            return;
        }

        if(v.getId()==R.id.btn_keyboard){
            host.toggleFullKeyboard();
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
            return;
        }

        if (v.getId() == R.id.btn_audio_mute) {
            boolean muted = !menuState.getAudioSettings().isMuted();
            host.applyStreamAudioSettingsUpdate(
                    StreamAudioSettingsUpdate.muted(muted));
            menuState = host.getState();
            setActionButtonActive(
                    btn_audio_mute,
                    menuState.getAudioSettings().isMuted());
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
                    menuState.getUiSettings()
                            .shouldHideBuiltInShortcuts());
            fragment.setOnShortcutSelectedListener(
                    this::executeShortcut);
            fragment.setOnShortcutsChangedListener(
                    this::rebuildActionGrid);
            fragment.show(getParentFragmentManager());
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
            fragment.show(getParentFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_touch_sensitivity){
            GameTouchFragment fragment=new GameTouchFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("触控灵敏度");
            fragment.setSettings(
                    menuState.getInputSettings(),
                    menuState.getControllerSettings());
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
            fragment.show(getParentFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_display){
            GameDisplayFragment fragment =
                    GameDisplayFragment.newInstance(true);
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.show(getParentFragmentManager());
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
            fragment.setSettings(menuState.getControllerSettings());
            fragment.show(getParentFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_virtual_view){
            GameMenuVirtualViewFragment fragment=new GameMenuVirtualViewFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle(R.string.game_menu_virtual_controls_title);
            fragment.setGamePadMode(
                    menuState.getVirtualGamepadEditMode());
            fragment.setGameKeyMode(
                    menuState.getVirtualKeysEditMode());
            fragment.setSettings(
                    menuState.getVirtualControlSettings());
            fragment.setOnscreenControllerRumbleEnabled(
                    menuState.getControllerSettings()
                            .isOnscreenRumbleEnabled());
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
                        VirtualControlEditMode mode) {
                    if (host == null) {
                        return;
                    }
                    host.setVirtualGamepadEditMode(mode);
                }

                @Override
                public void onVirtualKeyModeSelected(
                        VirtualControlEditMode mode) {
                    if (host == null) {
                        return;
                    }
                    host.setVirtualKeysEditMode(mode);
                }
            });
            fragment.show(getParentFragmentManager());
            return;
        }

        if (v.getId() == R.id.btn_pull_clipboard_files) {
            dismiss();
            host.pullRemoteClipboardFiles();
        }
    }

    private GameMenuHost resolveHost() {
        if (host == null && hostProvider != null) {
            host = hostProvider.getGameMenuHost();
        }
        return host;
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

    private void executeShortcut(GameMenuShortcut shortcut) {
        if (shortcut == null) {
            return;
        }
        if (shortcut.usesMoonlightKeyCodes()) {
            sendKeyboardChord(
                    shortcut.getMoonlightKeyCodes());
            return;
        }
        if (host != null) {
            host.sendAndroidKeyChord(
                    shortcut.getAndroidKeyCodes());
        }
    }
}
