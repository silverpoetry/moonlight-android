/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.content.Context;
import android.view.KeyEvent;

import com.limelight.R;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.virtualcontrols.action.VirtualControlAction;

public class KeyBoardControllerConfigurationLoader {
    public static KeyboardDigitalPadButton createDiaitalPadButton(String elementId, int keyCodeLeft, int keyCodeRight, int keyCodeUp, int keyCodeDown, String[] textTipValues,final KeyBoardController controller, final Context context) {
        KeyboardDigitalPadButton button = new KeyboardDigitalPadButton(controller, context, elementId);
        button.setTextTipValues(textTipValues);
        button.addDigitalPadListener(new KeyboardDigitalPadButton.DigitalPadListener() {
            @Override
            public void onDirectionChange(int direction) {
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_LEFT) != 0) {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCodeLeft);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                } else {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, keyCodeLeft);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                }
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_RIGHT) != 0) {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCodeRight);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                } else {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, keyCodeRight);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                }
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_UP) != 0) {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCodeUp);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                } else {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, keyCodeUp);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                }
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_DOWN) != 0) {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCodeDown);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                } else {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, keyCodeDown);
                    event.setSource(3);
                    controller.sendKeyEvent(event);
                }
            }
        });
        return button;
    }

    //手柄十字键和abxy
    public static KeyboardDigitalPadButton createDiaitalPadButtonGamePad(
            String elementId,
            boolean isABXY,
            String[] textTipValues,
            final KeyBoardController controller,
            final Context context) {

        KeyboardDigitalPadButton digitalPad = new KeyboardDigitalPadButton(controller, context, elementId);
        digitalPad.setTextTipValues(textTipValues);
        digitalPad.addDigitalPadListener(new KeyboardDigitalPadButton.DigitalPadListener() {
            @Override
            public void onDirectionChange(int direction) {
                KeyBoardController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();

                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_LEFT) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.X_FLAG:ControllerPacket.LEFT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.X_FLAG:ControllerPacket.LEFT_FLAG);
                }
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_RIGHT) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.B_FLAG:ControllerPacket.RIGHT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.B_FLAG:ControllerPacket.RIGHT_FLAG);
                }
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_UP) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.Y_FLAG:ControllerPacket.UP_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.Y_FLAG:ControllerPacket.UP_FLAG);
                }
                if ((direction & KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_DOWN) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.A_FLAG:ControllerPacket.DOWN_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.A_FLAG:ControllerPacket.DOWN_FLAG);
                }

                controller.sendControllerInputContext();
            }
        });

        return digitalPad;
    }

    public static KeyBoardAnalogStickButton createKeyBoardAnalogStickButton(final KeyBoardController controller, String elementId, final Context context, int[] keylist,String[] textTipValues) {

        KeyBoardAnalogStickButton analogStick = new KeyBoardAnalogStickButton(controller, elementId, context, keylist,textTipValues);
        analogStick.setListener(new KeyBoardAnalogStickButton.KeyBoardAnalogStickListener() {
            @Override
            public void onkeyEvent(int code, boolean isPress) {
                KeyEvent keyEvent = new KeyEvent(isPress ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, code);
                keyEvent.setSource(2);
                controller.sendKeyEvent(keyEvent);
            }
        });

        return analogStick;

    }

    public static KeyBoardAnalogStickButtonFree createKeyBoardAnalogStickButton2(final KeyBoardController controller, String elementId, final Context context, int[] keylist,String[] textTipValues) {

        KeyBoardAnalogStickButtonFree analogStick = new KeyBoardAnalogStickButtonFree(controller, elementId, context, keylist,textTipValues);
        analogStick.setListener(new KeyBoardAnalogStickButtonFree.KeyBoardAnalogStickListener() {
            @Override
            public void onkeyEvent(int code, boolean isPress) {
                KeyEvent keyEvent = new KeyEvent(isPress ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, code);
                keyEvent.setSource(2);
                controller.sendKeyEvent(keyEvent);
            }
        });

        return analogStick;

    }


    public static KeyBoardDigitalButton createMouseButton(
            final String elementId,
            final int mouseButton,
            final int layer,
            final String text,
            final int icon,
            boolean switchMode,
            final KeyBoardController controller,
            final Context context) {
        KeyBoardDigitalButton button = createButton(
                elementId, layer, text, icon, switchMode, controller, context);
        switch (mouseButton) {
            case 1:
                button.setIcon(switchMode
                        ? R.drawable.ic_mouse_left_s
                        : R.drawable.ic_mouse_left);
                button.setIconPress(R.drawable.ic_mouse_left_s);
                break;
            case 2:
                button.setIcon(switchMode
                        ? R.drawable.ic_mouse_middle_s
                        : R.drawable.ic_mouse_middle);
                button.setIconPress(R.drawable.ic_mouse_middle_s);
                break;
            case 3:
                button.setIcon(switchMode
                        ? R.drawable.ic_mouse_right_s
                        : R.drawable.ic_mouse_right);
                button.setIconPress(R.drawable.ic_mouse_right_s);
                break;
            case 4:
            case 5:
                int wheelIcon = mouseButton == 4
                        ? R.drawable.ic_mouse_up
                        : R.drawable.ic_mouse_down;
                button.setIcon(wheelIcon);
                button.setIconPress(wheelIcon);
                break;
            default:
                break;
        }
        Runnable repeater = new Runnable() {
            @Override
            public void run() {
                controller.sendHighResolutionScroll(mouseButton == 4);
                button.postDelayed(this, 100);
            }
        };
        button.addDigitalButtonListener(new KeyBoardDigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                if (mouseButton == 4 || mouseButton == 5) {
                    button.post(repeater);
                    return;
                }
                KeyEvent keyEvent = new KeyEvent(
                        KeyEvent.ACTION_DOWN, mouseButton);
                keyEvent.setSource(1);
                controller.sendKeyEvent(keyEvent);
            }

            @Override
            public void onLongClick() {

            }

            @Override
            public void onRelease() {
                if (mouseButton == 4 || mouseButton == 5) {
                    button.removeCallbacks(repeater);
                    return;
                }
                KeyEvent keyEvent = new KeyEvent(
                        KeyEvent.ACTION_UP, mouseButton);
                keyEvent.setSource(1);
                controller.sendKeyEvent(keyEvent);
            }
        });

        return button;
    }

    public static KeyBoardDigitalButton createKeyChordButton(
            String elementId,
            String keyCodes,
            int layer,
            String text,
            int icon,
            boolean switchMode,
            KeyBoardController controller,
            Context context) {
        KeyBoardDigitalButton button = createButton(
                elementId, layer, text, icon, switchMode, controller, context);
        button.addDigitalButtonListener(
                new KeyBoardDigitalButton.DigitalButtonListener() {
                    @Override
                    public void onClick() {
                        controller.sendKeyChord(
                                keyCodes, KeyEvent.ACTION_DOWN);
                    }

                    @Override
                    public void onLongClick() {
                    }

                    @Override
                    public void onRelease() {
                        controller.sendKeyChord(
                                keyCodes, KeyEvent.ACTION_UP);
                    }
                });
        return button;
    }

    public static KeyBoardDigitalButton createLocalActionButton(
            String elementId,
            VirtualControlAction action,
            int layer,
            String text,
            int icon,
            KeyBoardController controller,
            Context context) {
        KeyBoardDigitalButton button = createButton(
                elementId, layer, text, icon, false, controller, context);
        button.addDigitalButtonListener(
                new KeyBoardDigitalButton.DigitalButtonListener() {
                    @Override
                    public void onClick() {
                        controller.sendLocalAction(
                                action, KeyEvent.ACTION_DOWN);
                    }

                    @Override
                    public void onLongClick() {
                    }

                    @Override
                    public void onRelease() {
                        controller.sendLocalAction(
                                action, KeyEvent.ACTION_UP);
                    }
                });
        return button;
    }

    private static KeyBoardDigitalButton createButton(
            String elementId,
            int layer,
            String text,
            int icon,
            boolean switchMode,
            KeyBoardController controller,
            Context context) {
        KeyBoardDigitalButton button = new KeyBoardDigitalButton(
                controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);
        button.setEnableSwitchDown(switchMode);
        return button;
    }


    //手柄普通按钮
    public static KeyBoardDigitalButton createDigitalButtonGamePad(
            final String elementId,
            final int keyShort,
            final int keyLong,
            final int layer,
            final String text,
            final int icon,
            boolean switchMode,
            final KeyBoardController controller,
            final Context context) {
        KeyBoardDigitalButton button = new KeyBoardDigitalButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);
        button.setEnableSwitchDown(switchMode);
        button.addDigitalButtonListener(new KeyBoardDigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                KeyBoardController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyShort;

                controller.sendControllerInputContext();
            }

            @Override
            public void onLongClick() {
                KeyBoardController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyLong;

                controller.sendControllerInputContext();
            }

            @Override
            public void onRelease() {
                KeyBoardController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap &= ~keyShort;
                inputContext.inputMap &= ~keyLong;

                controller.sendControllerInputContext();
            }
        });

        return button;
    }


    public static KeyBoardTouchPadButton createDigitalTouchButton(
            final String elementId,
            final int keyShort,
            final int type,
            final int layer,
            final String text,
            final int icon,
            final KeyBoardController controller,
            final Context context) {
        KeyBoardTouchPadButton button = new KeyBoardTouchPadButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);
        button.setCode(keyShort);
        button.addDigitalButtonListener(new KeyBoardTouchPadButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                if(keyShort==13){
                    return;
                }
                int code=1;
                switch (keyShort){
                    case 9:
                        code=3;
                        break;
                    case 12:
                        code=2;
                        break;
                }
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, code);
                keyEvent.setSource(type);
                controller.sendKeyEvent(keyEvent);
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onMove(int x, int y) {
                controller.sendMouseMove(x,y);
            }

            @Override
            public void onRelease() {
                if(keyShort==13){
                    return;
                }
                int code=1;
                switch (keyShort){
                    case 9:
                        code=3;
                        break;
                    case 12:
                        code=2;
                        break;
                }
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, code);
                keyEvent.setSource(type);
                controller.sendKeyEvent(keyEvent);

            }
        });

        return button;
    }

}
