package com.limelight.ui.gamemenu;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.activity.ComponentDialog;
import androidx.annotation.NonNull;
import androidx.compose.ui.platform.ComposeView;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.UiHelper;

import java.util.List;

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
    private GameMenuComposeRenderer composeRenderer;

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
        if (composeRenderer != null) {
            composeRenderer.destroy();
            composeRenderer = null;
        }
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
        if (currentHost != null) {
            menuState = currentHost.getState();
            refreshComposeMenu();
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
        GameMenuHost currentHost = resolveHost();
        if (currentHost != null) {
            currentHost.onGameMenuDismissed(this);
        }
        super.onDismiss(dialog);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_compose;
    }

    @Override
    public void bindView(View v) {
        super.bindView(v);
        host = resolveHost();
        if (host == null) {
            throw new IllegalStateException(
                    "GameMenuHost is not initialized");
        }
        menuState = host.getState();
        ComposeView composeView = v.findViewById(R.id.game_menu_compose);
        composeRenderer = new GameMenuComposeRenderer(
                composeView,
                new GameMenuComposeRenderer.Listener() {
                    @Override
                    public void onActionRequested(
                            int viewId,
                            Object shortcutEntry) {
                        View actionView = new View(requireContext());
                        actionView.setId(viewId);
                        actionView.setTag(shortcutEntry);
                        onClick(actionView);
                        refreshComposeMenu();
                    }

                    @Override
                    public void onActionLongPressed(int viewId) {
                        if (viewId == R.id.btn_performance && host != null) {
                            host.cancelPendingStreamBackExit();
                            host.switchHUD();
                            refreshComposeMenu();
                        }
                    }

                    @Override
                    public void onCardLayoutChanged(
                            GameMenuCardLayout layout) {
                        if (host != null) {
                            host.cancelPendingStreamBackExit();
                            host.saveGameMenuCardLayout(layout);
                            refreshComposeMenu();
                        }
                    }

                    @Override
                    public void onDismissRequested() {
                        if (host != null) {
                            host.cancelPendingStreamBackExit();
                        }
                        dismiss();
                    }
                });
        refreshComposeMenu();
    }

    @Override
    protected boolean usesLegacyViewChrome() {
        return false;
    }

    private void refreshComposeMenu() {
        if (composeRenderer == null || host == null) {
            return;
        }
        menuState = host.getState();
        List<GameMenuCardCatalog.Card> catalog = loadCardCatalog();
        GameMenuCardConfiguration.State configuration =
                GameMenuCardConfiguration.load(
                        menuState.getCardLayout(),
                        catalog);
        composeRenderer.update(menuState, configuration);
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
        if(v.getId()==R.id.btn_soft_function || v.getId()==R.id.btn_windows_actions){
            GameFunctionFragment fragment=new GameFunctionFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setTitle("操作");
            fragment.setActionSelectionListener(new GameFunctionFragment.ActionSelectionListener() {
                @Override
                public void onActionSelected(String title, int index) {
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
            return;
        }

        if(v.getId()==R.id.btn_performance){
            host.showHUD();
            return;
        }

        if(v.getId()==R.id.btn_v_keyboard){
            host.toggleVirtualKeys();
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
            return;
        }

        if (v.getId() == R.id.btn_video_visibility) {
            host.toggleVideoVisibility();
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
                    this::refreshComposeMenu);
            fragment.show(getParentFragmentManager());
            return;
        }
        if(v.getId()==R.id.bt_touch_list){
            GameMouseTouchFragment fragment =
                    new GameMouseTouchFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.show(getParentFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_touch_sensitivity){
            GameTouchSensitivityFragment fragment =
                    new GameTouchSensitivityFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.show(getParentFragmentManager());
            return;
        }

        if(v.getId()==R.id.bt_virtual_view){
            GameMenuVirtualViewFragment fragment=new GameMenuVirtualViewFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
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
