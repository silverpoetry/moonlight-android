/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.content.Context;
import android.view.KeyEvent;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.virtual_controller.DigitalPad;
import com.limelight.nvstream.input.ControllerPacket;

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

                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_LEFT) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.X_FLAG:ControllerPacket.LEFT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.X_FLAG:ControllerPacket.LEFT_FLAG);
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_RIGHT) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.B_FLAG:ControllerPacket.RIGHT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.B_FLAG:ControllerPacket.RIGHT_FLAG);
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_UP) != 0) {
                    inputContext.inputMap |= isABXY?ControllerPacket.Y_FLAG:ControllerPacket.UP_FLAG;
                }
                else {
                    inputContext.inputMap &= ~(isABXY?ControllerPacket.Y_FLAG:ControllerPacket.UP_FLAG);
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_DOWN) != 0) {
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


    public static KeyBoardDigitalButton createDigitalButton(
            final String elementId,
            final Object keyShort,
            final int type,
            final int layer,
            final String text,
            final int icon,
            boolean switchMode,
            final KeyBoardController controller,
            final Context context) {
        KeyBoardDigitalButton button = new KeyBoardDigitalButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);
        if(type==1){
            switch ((Integer) keyShort){
                case 1://左
                    button.setIcon(R.drawable.ic_mouse_left);
                    if(switchMode){
                        button.setIcon(R.drawable.ic_mouse_left_s);
                    }
                    button.setIconPress(R.drawable.ic_mouse_left_s);
                    break;
                case 3://右
                    button.setIcon(R.drawable.ic_mouse_right);
                    if(switchMode){
                        button.setIcon(R.drawable.ic_mouse_right_s);
                    }
                    button.setIconPress(R.drawable.ic_mouse_right_s);
                    break;
                case 2://中
                    button.setIcon(R.drawable.ic_mouse_middle);
                    if(switchMode){
                        button.setIcon(R.drawable.ic_mouse_middle_s);
                    }
                    button.setIconPress(R.drawable.ic_mouse_middle_s);
                    break;
                case 4:
                case 5://滚轮上下
//                    button.setPadding(20,20,20,20);
                    button.setIcon((Integer) keyShort==4?R.drawable.ic_mouse_up:R.drawable.ic_mouse_down);
                    button.setIconPress((Integer) keyShort==4?R.drawable.ic_mouse_up:R.drawable.ic_mouse_down);
                    break;
            }
        }
        button.setEnableSwitchDown(switchMode);
        Runnable repeater = new Runnable() {
            @Override
            public void run() {
                if ((Integer) keyShort == 4) {
                    controller.sendHighResolutionScroll(true);
                } else {
                    controller.sendHighResolutionScroll(false);
                }
                button.postDelayed(this, 100);
            }
        };
        button.addDigitalButtonListener(new KeyBoardDigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                if(type==1){
                    switch ((Integer)keyShort){
                        case 4:
                        case 5:
                            button.post(repeater);
                            return;
                    }
                }
                if(type==4){
                    controller.sendAssembleKey((String) keyShort,KeyEvent.ACTION_DOWN);
                    return;
                }
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, (Integer) keyShort);
                keyEvent.setSource(type);
                controller.sendKeyEvent(keyEvent);
            }

            @Override
            public void onLongClick() {

            }

            @Override
            public void onRelease() {
                if(type==1){
                    switch ((Integer)keyShort){
                        case 4:
                        case 5:
                            button.removeCallbacks(repeater);
                            return;
                    }
                }
                if(type==4){
                    controller.sendAssembleKey((String) keyShort,KeyEvent.ACTION_UP);
                    return;
                }
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, (Integer) keyShort);
                keyEvent.setSource(type);
                controller.sendKeyEvent(keyEvent);

            }
        });

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
                LimeLog.info("axi->onclick:"+keyShort);
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
