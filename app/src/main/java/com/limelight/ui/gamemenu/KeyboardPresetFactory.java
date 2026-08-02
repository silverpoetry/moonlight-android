package com.limelight.ui.gamemenu;

import android.content.Context;
import androidx.annotation.StringRes;
import android.view.KeyEvent;

import com.limelight.R;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.virtualcontrols.action.VirtualControlAction;

import java.util.ArrayList;
import java.util.List;

final class KeyboardPresetFactory {
    private static final int BUTTON_TYPE_MOUSE = 1;
    private static final int BUTTON_TYPE_TOUCHPAD = 2;
    private static final int BUTTON_TYPE_JOYSTICK = 3;
    private static final int BUTTON_TYPE_KEYBOARD = 4;
    private static final int BUTTON_TYPE_DIRECTION_PAD = 5;
    private static final int[] FUNCTION_NAME_RESOURCES = {
            R.string.game_menu_soft_keyboard,
            R.string.game_menu_virtual_keys,
            R.string.keyboard_virtual_full_keyboard,
            R.string.game_menu_virtual_gamepad,
            R.string.game_menu_floating_ball,
            R.string.game_menu_action_performance,
            R.string.game_menu_game_menu
    };
    private static final VirtualControlAction[] FUNCTION_ACTIONS = {
            VirtualControlAction.TOGGLE_SOFT_KEYBOARD,
            VirtualControlAction.TOGGLE_VIRTUAL_KEYS,
            VirtualControlAction.TOGGLE_FULL_KEYBOARD,
            VirtualControlAction.TOGGLE_VIRTUAL_GAMEPAD,
            VirtualControlAction.TOGGLE_FLOATING_BUTTON,
            VirtualControlAction.TOGGLE_PERFORMANCE_OVERLAY,
            VirtualControlAction.OPEN_STREAM_MENU
    };

    private KeyboardPresetFactory() {
    }

    static List<GameMenuQuickBean> createMouseAndTouchItems(
            Context context) {
        List<GameMenuQuickBean> items = new ArrayList<>();
        addMouseButtonPair(
                context, items,
                R.string.keyboard_mouse_left, 1);
        addMouseButtonPair(
                context, items,
                R.string.keyboard_mouse_right, 3);
        addMouseButtonPair(
                context, items,
                R.string.keyboard_mouse_middle, 2);
        addMouseButtonPair(
                context, items,
                R.string.keyboard_wheel_up, 4);
        addMouseButtonPair(
                context, items,
                R.string.keyboard_wheel_down, 5);

        items.add(touchpadItem(
                context,
                R.string.keyboard_touchpad,
                10,
                R.string.keyboard_normal_mode));
        items.add(touchpadItem(
                context,
                R.string.keyboard_touchpad_left,
                11,
                R.string.keyboard_left_button));
        items.add(touchpadItem(
                context,
                R.string.keyboard_touchpad_right,
                9,
                R.string.keyboard_right_button));
        items.add(touchpadItem(
                context,
                R.string.keyboard_touchpad_middle,
                12,
                R.string.keyboard_middle_button));
        items.add(touchpadItem(
                context,
                R.string.keyboard_touchpad_view_only,
                13,
                R.string.keyboard_view_only));

        items.add(new GameMenuQuickBean(
                context.getString(R.string.keyboard_joystick),
                keyCodes(
                        KeyEvent.KEYCODE_W,
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_S,
                        KeyEvent.KEYCODE_D),
                "W-A-S-D",
                BUTTON_TYPE_JOYSTICK,
                false));
        items.add(new GameMenuQuickBean(
                context.getString(R.string.keyboard_joystick),
                keyCodes(
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT),
                context.getString(
                        R.string.keyboard_arrow_directions),
                BUTTON_TYPE_JOYSTICK,
                false));

        items.add(new GameMenuQuickBean(
                context.getString(
                        R.string.keyboard_free_joystick),
                keyCodes(
                        KeyEvent.KEYCODE_W,
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_S,
                        KeyEvent.KEYCODE_D),
                "W-A-S-D",
                BUTTON_TYPE_JOYSTICK,
                false).setFreeStick(true));
        items.add(new GameMenuQuickBean(
                context.getString(
                        R.string.keyboard_free_joystick),
                keyCodes(
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT),
                context.getString(
                        R.string.keyboard_arrow_directions),
                BUTTON_TYPE_JOYSTICK,
                false).setFreeStick(true));

        items.add(new GameMenuQuickBean(
                context.getString(
                        R.string.keyboard_direction_pad),
                keyCodes(
                        KeyEvent.KEYCODE_W,
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_S,
                        KeyEvent.KEYCODE_D),
                "W-A-S-D",
                BUTTON_TYPE_DIRECTION_PAD,
                false));
        items.add(new GameMenuQuickBean(
                context.getString(
                        R.string.keyboard_direction_pad),
                keyCodes(
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT),
                "↑-←-↓-→",
                BUTTON_TYPE_DIRECTION_PAD,
                false));
        return items;
    }

    static List<GameMenuQuickBean> createFunctionItems(
            Context context) {
        List<GameMenuQuickBean> items = new ArrayList<>();
        for (int index = 0;
                index < FUNCTION_ACTIONS.length;
                index++) {
            items.add(functionItem(
                    context,
                    FUNCTION_NAME_RESOURCES[index],
                    FUNCTION_ACTIONS[index]));
        }
        return items;
    }

    static VirtualControlAction[] functionActions() {
        return FUNCTION_ACTIONS.clone();
    }

    private static void addMouseButtonPair(
            Context context,
            List<GameMenuQuickBean> items,
            @StringRes int nameRes,
            int code) {
        items.add(new GameMenuQuickBean(
                context.getString(nameRes),
                code,
                context.getString(
                        R.string.keyboard_normal_mode),
                BUTTON_TYPE_MOUSE,
                false));
        items.add(new GameMenuQuickBean(
                context.getString(nameRes),
                code,
                context.getString(
                        R.string.keyboard_lock_mode),
                BUTTON_TYPE_MOUSE,
                true));
    }

    private static GameMenuQuickBean touchpadItem(
            Context context,
            @StringRes int nameRes,
            int code,
            @StringRes int descriptionRes) {
        return new GameMenuQuickBean(
                context.getString(nameRes),
                code,
                context.getString(descriptionRes),
                BUTTON_TYPE_TOUCHPAD,
                false).setShapeType(1);
    }

    private static GameMenuQuickBean functionItem(
            Context context,
            @StringRes int nameRes,
            VirtualControlAction action) {
        String name = context.getString(nameRes);
        return new GameMenuQuickBean(
                name,
                (String) null,
                name,
                BUTTON_TYPE_KEYBOARD,
                false).setLocalAction(action);
    }

    private static String keyCodes(int... keyCodes) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < keyCodes.length; index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append(keyCodes[index]);
        }
        return result.toString();
    }
}
