package com.limelight.binding.input.virtual_controller.keyboard;

import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.virtualcontrols.layout.VirtualControlElementIds;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKind;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates the first usable layout for the built-in profiles.
 *
 * <p>Custom profiles intentionally remain empty until the user adds controls.
 * A missing file is different from a saved empty layout: callers must only
 * invoke this factory for a missing document so an explicit user reset is
 * never overwritten.</p>
 */
public final class VirtualControlDefaultLayoutFactory {
    private static final int BUTTON_TYPE_MOUSE = 1;
    private static final int BUTTON_TYPE_TOUCHPAD = 2;
    private static final int BUTTON_TYPE_JOYSTICK = 3;
    private static final int BUTTON_TYPE_KEY = 4;
    private static final int BUTTON_TYPE_DPAD = 5;

    private VirtualControlDefaultLayoutFactory() {
    }

    public static List<GameMenuQuickBean> create(
            VirtualControlLayoutKey key,
            int viewportWidth,
            int viewportHeight,
            int buttonWidth,
            int buttonHeight) {
        if (key.getKind() == VirtualControlLayoutKind.GAMEPAD &&
                VirtualControlLayoutProfiles.DEFAULT_GAMEPAD.equals(
                        key.getProfileId())) {
            requireMatchingOrientation(
                    key.getOrientation(),
                    viewportWidth,
                    viewportHeight);
            return createGamepad(
                    key.getOrientation(),
                    viewportWidth,
                    viewportHeight);
        }
        if (key.getKind() == VirtualControlLayoutKind.KEYBOARD &&
                VirtualControlLayoutProfiles.DEFAULT_KEYBOARD.equals(
                        key.getProfileId())) {
            requireMatchingOrientation(
                    key.getOrientation(),
                    viewportWidth,
                    viewportHeight);
            return createKeyboard(
                    viewportWidth,
                    viewportHeight,
                    buttonWidth,
                    buttonHeight);
        }
        return Collections.emptyList();
    }

    private static List<GameMenuQuickBean> createGamepad(
            VirtualControlLayoutOrientation orientation,
            int viewportWidth,
            int viewportHeight) {
        int width = Math.max(1, viewportWidth);
        int height = Math.max(1, viewportHeight);
        boolean landscape =
                orientation == VirtualControlLayoutOrientation.LANDSCAPE;

        int shortSide = Math.min(width, height);
        int margin = Math.max(1, shortSide / 20);
        int shoulderGap = Math.max(1, shortSide / 30);
        int smallButtonSize = Math.max(1, shortSide / 10);
        int padSize = Math.max(1, shortSide * 3 / 10);
        int bottom = Math.max(0, height - margin - padSize);
        int leftPad = margin;
        int rightPad = Math.max(0, width - margin - padSize);

        int leftStick;
        int leftDpad;
        int rightStick;
        int rightAbxy;
        int padTop;
        if (landscape) {
            leftDpad = leftPad;
            leftStick = Math.min(
                    rightPad,
                    leftDpad + padSize + margin);
            rightAbxy = rightPad;
            rightStick = Math.max(
                    leftPad,
                    rightAbxy - margin - padSize);
            padTop = bottom;
        }
        else {
            leftStick = leftPad;
            leftDpad = leftPad;
            rightStick = rightPad;
            rightAbxy = rightPad;
            padTop = Math.max(0, (height - padSize) / 2);
        }

        int leftShoulder = margin;
        int leftTrigger = leftShoulder + smallButtonSize + shoulderGap;
        int rightTrigger = Math.max(
                0,
                width - margin - smallButtonSize);
        int rightShoulder = Math.max(
                0,
                rightTrigger - shoulderGap - smallButtonSize);
        int centerGroupWidth = smallButtonSize * 3;
        int selectLeft = Math.max(0, (width - centerGroupWidth) / 2);
        int startLeft = selectLeft + smallButtonSize * 2;
        int centerTop = landscape
                ? Math.max(0, height - margin - smallButtonSize)
                : Math.max(0, padTop - margin - smallButtonSize);

        List<GameMenuQuickBean> controls = new ArrayList<>();
        controls.add(gamepadStick(
                "左摇杆", ControllerPacket.PADDLE5_FLAG,
                leftStick, bottom, padSize, padSize));
        controls.add(gamepadDpad(
                "十字键", ControllerPacket.PADDLE1_FLAG,
                leftDpad, padTop, padSize, padSize));
        controls.add(gamepadStick(
                "右摇杆", ControllerPacket.PADDLE6_FLAG,
                rightStick, bottom, padSize, padSize));
        controls.add(gamepadDpad(
                "ABXY", ControllerPacket.PADDLE2_FLAG,
                rightAbxy, padTop, padSize, padSize));
        controls.add(gamepadButton(
                "L1", ControllerPacket.LB_FLAG,
                leftShoulder, margin,
                smallButtonSize, smallButtonSize));
        controls.add(gamepadButton(
                "L2", ControllerPacket.PADDLE3_FLAG,
                leftTrigger, margin,
                smallButtonSize, smallButtonSize));
        controls.add(gamepadButton(
                "R1", ControllerPacket.RB_FLAG,
                rightShoulder, margin,
                smallButtonSize, smallButtonSize));
        controls.add(gamepadButton(
                "R2", ControllerPacket.PADDLE4_FLAG,
                rightTrigger, margin,
                smallButtonSize, smallButtonSize));
        controls.add(gamepadButton(
                "SELECT", ControllerPacket.BACK_FLAG,
                selectLeft, centerTop,
                smallButtonSize, smallButtonSize));
        controls.add(gamepadButton(
                "START", ControllerPacket.PLAY_FLAG,
                startLeft, centerTop,
                smallButtonSize, smallButtonSize));
        return controls;
    }

    private static void requireMatchingOrientation(
            VirtualControlLayoutOrientation orientation,
            int viewportWidth,
            int viewportHeight) {
        boolean landscape = viewportWidth > viewportHeight;
        if (viewportWidth <= 0 || viewportHeight <= 0 ||
                viewportWidth == viewportHeight ||
                landscape !=
                        (orientation ==
                                VirtualControlLayoutOrientation.LANDSCAPE)) {
            throw new IllegalArgumentException(
                    "Layout orientation does not match viewport");
        }
    }

    private static List<GameMenuQuickBean> createKeyboard(
            int viewportWidth,
            int viewportHeight,
            int buttonWidth,
            int buttonHeight) {
        int width = Math.max(1, viewportWidth);
        int height = Math.max(1, viewportHeight);
        int unit = responsiveUnit(viewportWidth, buttonWidth, buttonHeight);
        int margin = Math.max(unit / 2, width / 50);
        int keyWidth = Math.max(unit, Math.min(unit * 2, width / 8));
        int keyHeight = Math.max(unit, buttonHeight);
        int gap = Math.max(1, unit / 5);
        int touchpadHeight = Math.min(
                keyHeight * 2 + gap,
                Math.max(keyHeight, height / 4));
        int dpadSize = Math.max(unit * 2, Math.min(unit * 3, width / 4));
        int controlRowHeight = Math.max(touchpadHeight, dpadSize);
        int row = Math.max(margin, height - margin - controlRowHeight);
        int secondRow = Math.max(margin, row - keyHeight - gap);
        int thirdRow = Math.max(margin, secondRow - keyHeight - gap);
        List<GameMenuQuickBean> controls = new ArrayList<>();

        int x = margin;
        controls.add(key("Esc", KeyEvent.KEYCODE_ESCAPE, "Esc", x, thirdRow,
                keyWidth, keyHeight));
        x += keyWidth + gap;
        controls.add(key("Tab", KeyEvent.KEYCODE_TAB, "Tab", x, thirdRow,
                keyWidth, keyHeight));
        x += keyWidth + gap;
        controls.add(key("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl", x,
                thirdRow, keyWidth, keyHeight));
        x += keyWidth + gap;
        controls.add(key("Shift", KeyEvent.KEYCODE_SHIFT_LEFT, "Shift", x,
                thirdRow, keyWidth, keyHeight));
        x += keyWidth + gap;
        controls.add(key("Alt", KeyEvent.KEYCODE_ALT_LEFT, "Alt", x,
                thirdRow, keyWidth, keyHeight));

        x = margin;
        controls.add(key("空格", KeyEvent.KEYCODE_SPACE, "Space", x, secondRow,
                keyWidth * 2 + gap, keyHeight));
        x += keyWidth * 2 + gap * 2;
        controls.add(key("Enter", KeyEvent.KEYCODE_ENTER, "Enter", x, secondRow,
                keyWidth * 2 + gap, keyHeight));
        x += keyWidth * 2 + gap * 2;
        controls.add(mouse("左键", 1, x, secondRow, keyWidth, keyHeight));
        x += keyWidth + gap;
        controls.add(mouse("右键", 3, x, secondRow, keyWidth, keyHeight));

        int touchpadWidth = Math.max(keyWidth * 3, Math.min(width - margin * 2,
                unit * 4));
        controls.add(touchpad(
                "触控板",
                margin,
                row + (controlRowHeight - touchpadHeight) / 2,
                touchpadWidth,
                touchpadHeight));
        controls.add(dpad(
                "方向键",
                width - margin - dpadSize,
                row + (controlRowHeight - dpadSize) / 2,
                dpadSize,
                dpadSize));
        return controls;
    }

    private static GameMenuQuickBean gamepadButton(
            String name,
            int code,
            int left,
            int top,
            int width,
            int height) {
        GameMenuQuickBean bean = new GameMenuQuickBean(
                name, code, name, BUTTON_TYPE_KEY, false);
        bean.setGamePad(true);
        return positioned(bean, left, top, width, height);
    }

    private static GameMenuQuickBean gamepadStick(
            String name,
            int code,
            int left,
            int top,
            int width,
            int height) {
        GameMenuQuickBean bean = new GameMenuQuickBean(
                name, code, "↑-←-↓-→", BUTTON_TYPE_JOYSTICK, false);
        bean.setGamePad(true);
        bean.setFreeStick(false);
        return positioned(bean, left, top, width, height);
    }

    private static GameMenuQuickBean gamepadDpad(
            String name,
            int code,
            int left,
            int top,
            int width,
            int height) {
        GameMenuQuickBean bean = new GameMenuQuickBean(
                name, code, "↑-←-↓-→", BUTTON_TYPE_DPAD, false);
        bean.setGamePad(true);
        return positioned(bean, left, top, width, height);
    }

    private static GameMenuQuickBean key(
            String name,
            int code,
            String description,
            int left,
            int top,
            int width,
            int height) {
        return positioned(
                new GameMenuQuickBean(
                        name,
                        Integer.toString(code),
                        description,
                        BUTTON_TYPE_KEY,
                        false)
                        .setShapeType(1),
                left,
                top,
                width,
                height);
    }

    private static GameMenuQuickBean mouse(
            String name,
            int code,
            int left,
            int top,
            int width,
            int height) {
        return positioned(
                new GameMenuQuickBean(
                        name, code, name, BUTTON_TYPE_MOUSE, false)
                        .setShapeType(1),
                left,
                top,
                width,
                height);
    }

    private static GameMenuQuickBean touchpad(
            String name,
            int left,
            int top,
            int width,
            int height) {
        return positioned(
                new GameMenuQuickBean(
                        name, 10, name, BUTTON_TYPE_TOUCHPAD, false)
                        .setShapeType(1),
                left,
                top,
                width,
                height);
    }

    private static GameMenuQuickBean dpad(
            String name,
            int left,
            int top,
            int width,
            int height) {
        return positioned(
                new GameMenuQuickBean(
                        name,
                        keyCodes(
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_DPAD_RIGHT),
                        "↑-←-↓-→",
                        BUTTON_TYPE_DPAD,
                        false),
                left,
                top,
                width,
                height);
    }

    private static GameMenuQuickBean positioned(
            GameMenuQuickBean bean,
            int left,
            int top,
            int width,
            int height) {
        bean.setId(VirtualControlElementIds.newId());
        bean.setmLeft(Math.max(0, left));
        bean.setmTop(Math.max(0, top));
        bean.setWidth(Math.max(1, width));
        bean.setHeight(Math.max(1, height));
        return bean;
    }

    private static int responsiveUnit(
            int viewportWidth,
            int buttonWidth,
            int buttonHeight) {
        int base = Math.max(1, Math.max(buttonWidth, buttonHeight));
        int widthLimit = Math.max(1, Math.max(1, viewportWidth) / 10);
        return Math.min(base, widthLimit);
    }

    private static String keyCodes(int... values) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append(values[index]);
        }
        return result.toString();
    }
}
