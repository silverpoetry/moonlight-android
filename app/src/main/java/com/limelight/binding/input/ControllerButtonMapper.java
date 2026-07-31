package com.limelight.binding.input;

import android.annotation.SuppressLint;
import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;

import java.util.Map;

/**
 * Immutable controller key-layout policy.
 *
 * <p>The mapper owns device-specific key corrections. Per-controller learning
 * is supplied explicitly through {@link ControllerButtonMappingState}. It
 * performs no I/O and allocates nothing while handling an event.</p>
 */
final class ControllerButtonMapper {
    static final int IGNORE = -1;
    static final int CONSUME = -2;

    private static final int ANDROID_10_API_LEVEL = 29;

    @SuppressLint("InlinedApi")
    private static final Map<Integer, Integer> PROTOCOL_BUTTON_MAPPINGS =
            Map.ofEntries(
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_A,
                            ControllerPacket.A_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_B,
                            ControllerPacket.B_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_X,
                            ControllerPacket.X_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_Y,
                            ControllerPacket.Y_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_UP,
                            ControllerPacket.UP_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            ControllerPacket.DOWN_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            ControllerPacket.LEFT_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            ControllerPacket.RIGHT_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_UP_LEFT,
                            ControllerPacket.UP_FLAG |
                                    ControllerPacket.LEFT_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_UP_RIGHT,
                            ControllerPacket.UP_FLAG |
                                    ControllerPacket.RIGHT_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_DOWN_LEFT,
                            ControllerPacket.DOWN_FLAG |
                                    ControllerPacket.LEFT_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_DPAD_DOWN_RIGHT,
                            ControllerPacket.DOWN_FLAG |
                                    ControllerPacket.RIGHT_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_L1,
                            ControllerPacket.LB_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_R1,
                            ControllerPacket.RB_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_THUMBL,
                            ControllerPacket.LS_CLK_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_THUMBR,
                            ControllerPacket.RS_CLK_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_START,
                            ControllerPacket.PLAY_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_MENU,
                            ControllerPacket.PLAY_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_SELECT,
                            ControllerPacket.BACK_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BACK,
                            ControllerPacket.BACK_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_MODE,
                            ControllerPacket.SPECIAL_BUTTON_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_MEDIA_RECORD,
                            ControllerPacket.MISC_FLAG),
                    Map.entry(
                            KeyEvent.KEYCODE_BUTTON_1,
                            ControllerPacket.TOUCHPAD_FLAG));

    private final int vendorId;
    private final int productId;
    private final int sdkInt;
    private final boolean ignoreBack;
    private final boolean hasShare;
    private final boolean dualShockStandaloneTouchpad;
    private final boolean linuxStandardFaceButtons;
    private final boolean nonStandardDualShock4;
    private final boolean serval;
    private final boolean nonStandardXboxBluetooth;
    private final boolean searchIsMode;
    private final boolean hasHatAxes;

    private ControllerButtonMapper(Builder builder) {
        vendorId = builder.vendorId;
        productId = builder.productId;
        sdkInt = builder.sdkInt;
        ignoreBack = builder.ignoreBack;
        hasShare = builder.hasShare;
        dualShockStandaloneTouchpad =
                builder.dualShockStandaloneTouchpad;
        linuxStandardFaceButtons =
                builder.linuxStandardFaceButtons;
        nonStandardDualShock4 =
                builder.nonStandardDualShock4;
        serval = builder.serval;
        nonStandardXboxBluetooth =
                builder.nonStandardXboxBluetooth;
        searchIsMode = builder.searchIsMode;
        hasHatAxes = builder.hasHatAxes;
    }

    static Builder builder(
            int vendorId,
            int productId,
            int sdkInt) {
        return new Builder(vendorId, productId, sdkInt);
    }

    static Map<Integer, Integer> getProtocolButtonMappings() {
        return PROTOCOL_BUTTON_MAPPINGS;
    }

    int remap(
            ControllerButtonMappingState state,
            int keyCode,
            int scanCode,
            int eventFlags,
            boolean hasNoModifiers,
            boolean joyConFixEnabled) {
        if (ignoreBack && keyCode == KeyEvent.KEYCODE_BACK) {
            return IGNORE;
        }

        if (hasShare &&
                keyCode == KeyEvent.KEYCODE_UNKNOWN &&
                scanCode == 167) {
            return KeyEvent.KEYCODE_MEDIA_RECORD;
        }

        if (vendorId == 0x054c &&
                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT &&
                (scanCode == 317 ||
                        dualShockStandaloneTouchpad)) {
            return KeyEvent.KEYCODE_BUTTON_1;
        }

        if (vendorId == 0x2dc8 && scanCode == 306) {
            return KeyEvent.KEYCODE_BUTTON_MODE;
        }

        if ((vendorId == 0x057e &&
                productId == 0x2009 &&
                sdkInt < ANDROID_10_API_LEVEL) ||
                (vendorId == 0x0f0d &&
                        productId == 0x00c1)) {
            int mappedKeyCode = mapNintendoScanCode(scanCode);
            if (mappedKeyCode != IGNORE) {
                return mappedKeyCode;
            }
        }

        if (joyConFixEnabled &&
                vendorId == 0x057e &&
                productId == 0x2006) {
            int mappedKeyCode = mapLeftJoyConScanCode(scanCode);
            if (mappedKeyCode != IGNORE) {
                return mappedKeyCode;
            }
        }

        if (joyConFixEnabled &&
                vendorId == 0x057e &&
                productId == 0x2007) {
            int mappedKeyCode = mapRightJoyConScanCode(scanCode);
            if (mappedKeyCode != IGNORE) {
                return mappedKeyCode;
            }
        }

        if (linuxStandardFaceButtons) {
            int mappedKeyCode = mapLinuxFaceButtonScanCode(scanCode);
            if (mappedKeyCode != IGNORE) {
                return mappedKeyCode;
            }
        }

        if (nonStandardDualShock4) {
            return mapNonStandardDualShock4ScanCode(scanCode);
        }
        else if (serval && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            if (scanCode == 314) {
                return KeyEvent.KEYCODE_BUTTON_SELECT;
            }
            if (scanCode == 315) {
                return KeyEvent.KEYCODE_BUTTON_START;
            }
        }
        else if (nonStandardXboxBluetooth) {
            int mappedKeyCode =
                    mapNonStandardXboxScanCode(scanCode);
            if (mappedKeyCode != IGNORE) {
                return mappedKeyCode;
            }
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                return KeyEvent.KEYCODE_BUTTON_MODE;
            }
        }
        else if (vendorId == 0x0b05 &&
                (productId == 0x7900 ||
                        productId == 0x7902)) {
            if (scanCode == 264 || scanCode == 266) {
                return KeyEvent.KEYCODE_BUTTON_START;
            }
            if (scanCode == 265 || scanCode == 267) {
                return KeyEvent.KEYCODE_BUTTON_SELECT;
            }
        }

        if (!hasHatAxes &&
                keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            int mappedKeyCode = mapRawDpadScanCode(scanCode);
            if (mappedKeyCode != IGNORE) {
                return mappedKeyCode;
            }
        }

        int mappedKeyCode = keyCode;
        if (mappedKeyCode == KeyEvent.KEYCODE_BACK &&
                !hasNoModifiers &&
                (eventFlags & KeyEvent.FLAG_SOFT_KEYBOARD) != 0) {
            mappedKeyCode = KeyEvent.KEYCODE_BUTTON_B;
        }

        if (mappedKeyCode == KeyEvent.KEYCODE_BUTTON_START ||
                mappedKeyCode == KeyEvent.KEYCODE_MENU) {
            state.observeStartButton();
        }
        else if (mappedKeyCode ==
                KeyEvent.KEYCODE_BUTTON_SELECT) {
            state.observeSelectButton();
        }
        else if (state.shouldMapBackToStart() &&
                mappedKeyCode == KeyEvent.KEYCODE_BACK) {
            return KeyEvent.KEYCODE_BUTTON_START;
        }
        else if (state.shouldMapModeToSelect() &&
                mappedKeyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
            return KeyEvent.KEYCODE_BUTTON_SELECT;
        }
        else if (searchIsMode &&
                mappedKeyCode == KeyEvent.KEYCODE_SEARCH) {
            return KeyEvent.KEYCODE_BUTTON_MODE;
        }

        return mappedKeyCode;
    }

    static int flipFaceButtons(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                return KeyEvent.KEYCODE_BUTTON_B;
            case KeyEvent.KEYCODE_BUTTON_B:
                return KeyEvent.KEYCODE_BUTTON_A;
            case KeyEvent.KEYCODE_BUTTON_X:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return KeyEvent.KEYCODE_BUTTON_X;
            default:
                return keyCode;
        }
    }

    private static int mapNintendoScanCode(int scanCode) {
        switch (scanCode) {
            case 0x130:
                return KeyEvent.KEYCODE_BUTTON_A;
            case 0x131:
                return KeyEvent.KEYCODE_BUTTON_B;
            case 0x132:
                return KeyEvent.KEYCODE_BUTTON_X;
            case 0x133:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case 0x134:
                return KeyEvent.KEYCODE_BUTTON_L1;
            case 0x135:
                return KeyEvent.KEYCODE_BUTTON_R1;
            case 0x136:
                return KeyEvent.KEYCODE_BUTTON_L2;
            case 0x137:
                return KeyEvent.KEYCODE_BUTTON_R2;
            case 0x138:
                return KeyEvent.KEYCODE_BUTTON_SELECT;
            case 0x139:
                return KeyEvent.KEYCODE_BUTTON_START;
            case 0x13a:
                return KeyEvent.KEYCODE_BUTTON_THUMBL;
            case 0x13b:
                return KeyEvent.KEYCODE_BUTTON_THUMBR;
            case 0x13d:
                return KeyEvent.KEYCODE_BUTTON_MODE;
            default:
                return IGNORE;
        }
    }

    private static int mapLeftJoyConScanCode(int scanCode) {
        switch (scanCode) {
            case 546:
                return KeyEvent.KEYCODE_DPAD_LEFT;
            case 547:
                return KeyEvent.KEYCODE_DPAD_RIGHT;
            case 544:
                return KeyEvent.KEYCODE_DPAD_UP;
            case 545:
                return KeyEvent.KEYCODE_DPAD_DOWN;
            case 309:
                return KeyEvent.KEYCODE_BUTTON_MODE;
            case 310:
                return KeyEvent.KEYCODE_BUTTON_L1;
            case 312:
                return KeyEvent.KEYCODE_BUTTON_L2;
            case 314:
                return KeyEvent.KEYCODE_BUTTON_SELECT;
            case 317:
                return KeyEvent.KEYCODE_BUTTON_THUMBL;
            default:
                return IGNORE;
        }
    }

    private static int mapRightJoyConScanCode(int scanCode) {
        switch (scanCode) {
            case 307:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case 308:
                return KeyEvent.KEYCODE_BUTTON_X;
            case 304:
                return KeyEvent.KEYCODE_BUTTON_A;
            case 305:
                return KeyEvent.KEYCODE_BUTTON_B;
            case 311:
                return KeyEvent.KEYCODE_BUTTON_R1;
            case 313:
                return KeyEvent.KEYCODE_BUTTON_R2;
            case 315:
                return KeyEvent.KEYCODE_BUTTON_START;
            case 316:
                return KeyEvent.KEYCODE_BUTTON_MODE;
            case 318:
                return KeyEvent.KEYCODE_BUTTON_THUMBR;
            default:
                return IGNORE;
        }
    }

    private static int mapLinuxFaceButtonScanCode(
            int scanCode) {
        switch (scanCode) {
            case 304:
                return KeyEvent.KEYCODE_BUTTON_A;
            case 305:
                return KeyEvent.KEYCODE_BUTTON_B;
            case 307:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case 308:
                return KeyEvent.KEYCODE_BUTTON_X;
            default:
                return IGNORE;
        }
    }

    private static int mapNonStandardDualShock4ScanCode(
            int scanCode) {
        switch (scanCode) {
            case 304:
                return KeyEvent.KEYCODE_BUTTON_X;
            case 305:
                return KeyEvent.KEYCODE_BUTTON_A;
            case 306:
                return KeyEvent.KEYCODE_BUTTON_B;
            case 307:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case 308:
                return KeyEvent.KEYCODE_BUTTON_L1;
            case 309:
                return KeyEvent.KEYCODE_BUTTON_R1;
            case 312:
                return KeyEvent.KEYCODE_BUTTON_SELECT;
            case 313:
                return KeyEvent.KEYCODE_BUTTON_START;
            case 314:
                return KeyEvent.KEYCODE_BUTTON_THUMBL;
            case 315:
                return KeyEvent.KEYCODE_BUTTON_THUMBR;
            case 316:
                return KeyEvent.KEYCODE_BUTTON_MODE;
            default:
                return CONSUME;
        }
    }

    private static int mapNonStandardXboxScanCode(
            int scanCode) {
        switch (scanCode) {
            case 306:
                return KeyEvent.KEYCODE_BUTTON_X;
            case 307:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case 308:
                return KeyEvent.KEYCODE_BUTTON_L1;
            case 309:
                return KeyEvent.KEYCODE_BUTTON_R1;
            case 310:
                return KeyEvent.KEYCODE_BUTTON_SELECT;
            case 311:
                return KeyEvent.KEYCODE_BUTTON_START;
            case 312:
                return KeyEvent.KEYCODE_BUTTON_THUMBL;
            case 313:
                return KeyEvent.KEYCODE_BUTTON_THUMBR;
            case 139:
                return KeyEvent.KEYCODE_BUTTON_MODE;
            default:
                return IGNORE;
        }
    }

    private static int mapRawDpadScanCode(int scanCode) {
        switch (scanCode) {
            case 704:
                return KeyEvent.KEYCODE_DPAD_LEFT;
            case 705:
                return KeyEvent.KEYCODE_DPAD_RIGHT;
            case 706:
                return KeyEvent.KEYCODE_DPAD_UP;
            case 707:
                return KeyEvent.KEYCODE_DPAD_DOWN;
            default:
                return IGNORE;
        }
    }

    static final class Builder {
        private final int vendorId;
        private final int productId;
        private final int sdkInt;

        private boolean ignoreBack;
        private boolean hasShare;
        private boolean dualShockStandaloneTouchpad;
        private boolean linuxStandardFaceButtons;
        private boolean nonStandardDualShock4;
        private boolean serval;
        private boolean nonStandardXboxBluetooth;
        private boolean searchIsMode;
        private boolean hasHatAxes;

        private Builder(
                int vendorId,
                int productId,
                int sdkInt) {
            this.vendorId = vendorId;
            this.productId = productId;
            this.sdkInt = sdkInt;
        }

        Builder ignoreBack(boolean value) {
            ignoreBack = value;
            return this;
        }

        Builder hasShare(boolean value) {
            hasShare = value;
            return this;
        }

        Builder dualShockStandaloneTouchpad(boolean value) {
            dualShockStandaloneTouchpad = value;
            return this;
        }

        Builder linuxStandardFaceButtons(boolean value) {
            linuxStandardFaceButtons = value;
            return this;
        }

        Builder nonStandardDualShock4(boolean value) {
            nonStandardDualShock4 = value;
            return this;
        }

        Builder serval(boolean value) {
            serval = value;
            return this;
        }

        Builder nonStandardXboxBluetooth(boolean value) {
            nonStandardXboxBluetooth = value;
            return this;
        }

        Builder searchIsMode(boolean value) {
            searchIsMode = value;
            return this;
        }

        Builder hasHatAxes(boolean value) {
            hasHatAxes = value;
            return this;
        }

        ControllerButtonMapper build() {
            return new ControllerButtonMapper(this);
        }
    }
}
