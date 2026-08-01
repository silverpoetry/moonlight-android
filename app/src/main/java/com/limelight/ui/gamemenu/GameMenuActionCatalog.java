package com.limelight.ui.gamemenu;

import com.limelight.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class GameMenuActionCatalog {
    static final class Action {
        final String id;
        final int viewId;
        final int labelRes;
        final int contentDescriptionRes;
        final int iconRes;
        final boolean requiresGamepad;

        Action(String id, int viewId, int labelRes,
               int contentDescriptionRes, int iconRes,
               boolean requiresGamepad) {
            this.id = id;
            this.viewId = viewId;
            this.labelRes = labelRes;
            this.contentDescriptionRes = contentDescriptionRes;
            this.iconRes = iconRes;
            this.requiresGamepad = requiresGamepad;
        }
    }

    private static final List<Action> ACTIONS =
            Collections.unmodifiableList(Arrays.asList(
                    new Action(
                            "disconnect",
                            R.id.btn_unlink,
                            R.string.game_menu_action_disconnect,
                            R.string.game_menu_action_disconnect,
                            R.drawable.ic_axi_unlink,
                            false),
                    new Action(
                            "quit_stream",
                            R.id.btn_exit,
                            R.string.game_menu_action_quit_stream,
                            R.string.game_menu_action_quit_stream,
                            R.drawable.ic_axi_exit,
                            false),
                    new Action(
                            "rotate",
                            R.id.btn_swicth_screen,
                            R.string.game_menu_action_rotate,
                            R.string.game_menu_action_rotate,
                            R.drawable.ic_axi_screen,
                            false),
                    new Action(
                            "performance",
                            R.id.btn_performance,
                            R.string.game_menu_action_performance,
                            R.string.game_menu_action_performance,
                            R.drawable.ic_axi_performance,
                            false),
                    new Action(
                            "virtual_gamepad",
                            R.id.btn_game_pad,
                            R.string.game_menu_action_virtual_gamepad,
                            R.string.game_menu_action_virtual_gamepad,
                            R.drawable.ic_axi_game_pad,
                            false),
                    new Action(
                            "virtual_buttons",
                            R.id.btn_v_keyboard,
                            R.string.game_menu_action_virtual_buttons,
                            R.string.game_menu_action_virtual_buttons,
                            R.drawable.ic_axi_vkeyboard,
                            false),
                    new Action(
                            "full_keyboard",
                            R.id.btn_keyboard,
                            R.string.game_menu_action_full_keyboard,
                            R.string.game_menu_action_full_keyboard,
                            R.drawable.ic_axi_quick,
                            false),
                    new Action(
                            "screen_zoom",
                            R.id.btn_screen_move,
                            R.string.game_menu_action_screen_zoom,
                            R.string.game_menu_action_screen_zoom,
                            R.drawable.ic_axi_zoom,
                            false),
                    new Action(
                            "display",
                            R.id.btn_display_1,
                            R.string.game_menu_action_display,
                            R.string.game_menu_action_display,
                            R.drawable.ic_axi_switch_screen,
                            false),
                    new Action(
                            "microphone",
                            R.id.btn_mic,
                            R.string.game_menu_action_microphone,
                            R.string.game_menu_action_microphone,
                            R.drawable.ic_axi_mic,
                            false),
                    new Action(
                            "audio_mute",
                            R.id.btn_audio_mute,
                            R.string.game_menu_action_audio_mute,
                            R.string.game_menu_action_audio_mute,
                            R.drawable.ic_axi_audio_mute,
                            false),
                    new Action(
                            "hdr",
                            R.id.btn_hdr,
                            R.string.game_menu_action_hdr,
                            R.string.game_menu_action_hdr,
                            R.drawable.ic_axi_hdr,
                            false),
                    new Action(
                            "clipboard_files",
                            R.id.btn_pull_clipboard_files,
                            R.string.clipboard_file_pull_short,
                            R.string.clipboard_file_pull,
                            R.drawable.ic_axi_clipboard_send,
                            false),
                    new Action(
                            "gamepad_mouse",
                            R.id.btn_gamepad_mouse,
                            R.string.game_menu_action_gamepad_mouse,
                            R.string.game_menu_action_gamepad_mouse,
                            R.drawable.ic_axi_joystick,
                            true)
            ));

    private GameMenuActionCatalog() {
    }

    static List<Action> all() {
        return ACTIONS;
    }

    static Action findById(String id) {
        for (Action action : ACTIONS) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }
}
