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
                            R.drawable.ic_m3_link_off,
                            false),
                    new Action(
                            "quit_stream",
                            R.id.btn_exit,
                            R.string.game_menu_action_quit_stream,
                            R.string.game_menu_action_quit_stream,
                            R.drawable.ic_m3_logout,
                            false),
                    new Action(
                            "rotate",
                            R.id.btn_swicth_screen,
                            R.string.game_menu_action_rotate,
                            R.string.game_menu_action_rotate,
                            R.drawable.ic_m3_rotate,
                            false),
                    new Action(
                            "performance",
                            R.id.btn_performance,
                            R.string.game_menu_action_performance,
                            R.string.game_menu_action_performance,
                            R.drawable.ic_m3_speed,
                            false),
                    new Action(
                            "virtual_gamepad",
                            R.id.btn_game_pad,
                            R.string.game_menu_action_virtual_gamepad,
                            R.string.game_menu_action_virtual_gamepad,
                            R.drawable.ic_m3_gamepad,
                            false),
                    new Action(
                            "virtual_buttons",
                            R.id.btn_v_keyboard,
                            R.string.game_menu_action_virtual_buttons,
                            R.string.game_menu_action_virtual_buttons,
                            R.drawable.ic_m3_grid_view,
                            false),
                    new Action(
                            "full_keyboard",
                            R.id.btn_keyboard,
                            R.string.game_menu_action_full_keyboard,
                            R.string.game_menu_action_full_keyboard,
                            R.drawable.ic_m3_keyboard,
                            false),
                    new Action(
                            "screen_zoom",
                            R.id.btn_screen_move,
                            R.string.game_menu_action_screen_zoom,
                            R.string.game_menu_action_screen_zoom,
                            R.drawable.ic_m3_zoom_out_map,
                            false),
                    new Action(
                            "windows_actions",
                            R.id.btn_windows_actions,
                            R.string.game_menu_action_windows,
                            R.string.game_menu_action_windows,
                            R.drawable.ic_m3_terminal,
                            false),
                    new Action(
                            "microphone",
                            R.id.btn_mic,
                            R.string.game_menu_action_microphone,
                            R.string.game_menu_action_microphone,
                            R.drawable.ic_m3_mic,
                            false),
                    new Action(
                            "audio_mute",
                            R.id.btn_audio_mute,
                            R.string.game_menu_action_audio_mute,
                            R.string.game_menu_action_audio_mute,
                            R.drawable.ic_m3_volume_off,
                            false),
                    new Action(
                            "video_visibility",
                            R.id.btn_video_visibility,
                            R.string.game_menu_action_hide_video,
                            R.string.game_menu_action_hide_video_description,
                            R.drawable.ic_m3_visibility_off,
                            false),
                    new Action(
                            "hdr",
                            R.id.btn_hdr,
                            R.string.game_menu_action_hdr,
                            R.string.game_menu_action_hdr,
                            R.drawable.ic_m3_hdr,
                            false),
                    new Action(
                            "clipboard_files",
                            R.id.btn_pull_clipboard_files,
                            R.string.clipboard_file_pull_short,
                            R.string.clipboard_file_pull,
                            R.drawable.ic_m3_download,
                            false),
                    new Action(
                            "gamepad_mouse",
                            R.id.btn_gamepad_mouse,
                            R.string.game_menu_action_gamepad_mouse,
                            R.string.game_menu_action_gamepad_mouse,
                            R.drawable.ic_m3_mouse,
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
