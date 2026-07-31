package com.limelight.binding.input;

import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerDigitalButtonMappingTest {
    @Test
    public void systemButtonsMapToProtocolTargets() {
        assertTarget(KeyEvent.KEYCODE_BUTTON_MODE,
                ControllerDigitalButtonMapping.Target.SPECIAL);
        assertTarget(KeyEvent.KEYCODE_BUTTON_START,
                ControllerDigitalButtonMapping.Target.PLAY);
        assertTarget(KeyEvent.KEYCODE_MENU,
                ControllerDigitalButtonMapping.Target.PLAY);
        assertTarget(KeyEvent.KEYCODE_BACK,
                ControllerDigitalButtonMapping.Target.BACK);
        assertTarget(KeyEvent.KEYCODE_BUTTON_SELECT,
                ControllerDigitalButtonMapping.Target.BACK);
        assertTarget(KeyEvent.KEYCODE_MEDIA_RECORD,
                ControllerDigitalButtonMapping.Target.MISC);
        assertTarget(KeyEvent.KEYCODE_BUTTON_1,
                ControllerDigitalButtonMapping.Target.TOUCHPAD);
    }

    @Test
    public void faceStickBumperAndTriggerButtonsMap() {
        assertTarget(KeyEvent.KEYCODE_DPAD_CENTER,
                ControllerDigitalButtonMapping.Target.A);
        assertTarget(KeyEvent.KEYCODE_BUTTON_A,
                ControllerDigitalButtonMapping.Target.A);
        assertTarget(KeyEvent.KEYCODE_BUTTON_B,
                ControllerDigitalButtonMapping.Target.B);
        assertTarget(KeyEvent.KEYCODE_BUTTON_X,
                ControllerDigitalButtonMapping.Target.X);
        assertTarget(KeyEvent.KEYCODE_BUTTON_Y,
                ControllerDigitalButtonMapping.Target.Y);
        assertTarget(KeyEvent.KEYCODE_BUTTON_L1,
                ControllerDigitalButtonMapping.Target.LEFT_BUMPER);
        assertTarget(KeyEvent.KEYCODE_BUTTON_R1,
                ControllerDigitalButtonMapping.Target.RIGHT_BUMPER);
        assertTarget(KeyEvent.KEYCODE_BUTTON_THUMBL,
                ControllerDigitalButtonMapping.Target.LEFT_STICK);
        assertTarget(KeyEvent.KEYCODE_BUTTON_THUMBR,
                ControllerDigitalButtonMapping.Target.RIGHT_STICK);
        assertTarget(KeyEvent.KEYCODE_BUTTON_L2,
                ControllerDigitalButtonMapping.Target.LEFT_TRIGGER);
        assertTarget(KeyEvent.KEYCODE_BUTTON_R2,
                ControllerDigitalButtonMapping.Target.RIGHT_TRIGGER);
    }

    @Test
    public void cardinalAndDiagonalDpadButtonsMap() {
        assertTarget(KeyEvent.KEYCODE_DPAD_LEFT,
                ControllerDigitalButtonMapping.Target.DPAD_LEFT);
        assertTarget(KeyEvent.KEYCODE_DPAD_RIGHT,
                ControllerDigitalButtonMapping.Target.DPAD_RIGHT);
        assertTarget(KeyEvent.KEYCODE_DPAD_UP,
                ControllerDigitalButtonMapping.Target.DPAD_UP);
        assertTarget(KeyEvent.KEYCODE_DPAD_DOWN,
                ControllerDigitalButtonMapping.Target.DPAD_DOWN);
        assertTarget(KeyEvent.KEYCODE_DPAD_UP_LEFT,
                ControllerDigitalButtonMapping.Target.DPAD_UP_LEFT);
        assertTarget(KeyEvent.KEYCODE_DPAD_UP_RIGHT,
                ControllerDigitalButtonMapping.Target.DPAD_UP_RIGHT);
        assertTarget(KeyEvent.KEYCODE_DPAD_DOWN_LEFT,
                ControllerDigitalButtonMapping.Target.DPAD_DOWN_LEFT);
        assertTarget(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT,
                ControllerDigitalButtonMapping.Target.DPAD_DOWN_RIGHT);
    }

    @Test
    public void paddleScanCodesRequireAdvertisedCapability() {
        assertEquals(
                ControllerDigitalButtonMapping.Target.UNHANDLED,
                resolve(KeyEvent.KEYCODE_UNKNOWN, 0x2c4, false));
        assertEquals(
                ControllerDigitalButtonMapping.Target.PADDLE_1,
                resolve(KeyEvent.KEYCODE_UNKNOWN, 0x2c4, true));
        assertEquals(
                ControllerDigitalButtonMapping.Target.PADDLE_2,
                resolve(KeyEvent.KEYCODE_UNKNOWN, 0x2c5, true));
        assertEquals(
                ControllerDigitalButtonMapping.Target.PADDLE_3,
                resolve(KeyEvent.KEYCODE_UNKNOWN, 0x2c6, true));
        assertEquals(
                ControllerDigitalButtonMapping.Target.PADDLE_4,
                resolve(KeyEvent.KEYCODE_UNKNOWN, 0x2c7, true));
        assertEquals(
                ControllerDigitalButtonMapping.Target.UNHANDLED,
                resolve(KeyEvent.KEYCODE_UNKNOWN, 0x2c8, true));
    }

    @Test
    public void unsupportedKeysRemainUnhandled() {
        assertEquals(
                ControllerDigitalButtonMapping.Target.UNHANDLED,
                resolve(KeyEvent.KEYCODE_SPACE, 0, true));
    }

    @Test
    public void targetsCarryExactProtocolMasks() {
        assertMask(
                ControllerDigitalButtonMapping.Target.SPECIAL,
                ControllerPacket.SPECIAL_BUTTON_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.PLAY,
                ControllerPacket.PLAY_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.BACK,
                ControllerPacket.BACK_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.DPAD_UP_LEFT,
                ControllerPacket.UP_FLAG |
                        ControllerPacket.LEFT_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.A,
                ControllerPacket.A_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.LEFT_BUMPER,
                ControllerPacket.LB_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.MISC,
                ControllerPacket.MISC_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.TOUCHPAD,
                ControllerPacket.TOUCHPAD_FLAG);
        assertMask(
                ControllerDigitalButtonMapping.Target.PADDLE_4,
                ControllerPacket.PADDLE4_FLAG);
    }

    @Test
    public void hatSuppressionMatchesAxisOwnership() {
        assertTrue(suppressed(
                ControllerDigitalButtonMapping.Target.DPAD_LEFT,
                true,
                false));
        assertFalse(suppressed(
                ControllerDigitalButtonMapping.Target.DPAD_LEFT,
                false,
                true));
        assertTrue(suppressed(
                ControllerDigitalButtonMapping.Target.DPAD_UP,
                false,
                true));
        assertTrue(suppressed(
                ControllerDigitalButtonMapping.Target.DPAD_UP_LEFT,
                true,
                true));
        assertFalse(suppressed(
                ControllerDigitalButtonMapping.Target.DPAD_UP_LEFT,
                true,
                false));
        assertFalse(suppressed(
                ControllerDigitalButtonMapping.Target.A,
                true,
                true));
    }

    private static void assertTarget(
            int keyCode,
            ControllerDigitalButtonMapping.Target expected) {
        assertEquals(expected, resolve(keyCode, 0, false));
    }

    private static ControllerDigitalButtonMapping.Target resolve(
            int keyCode,
            int scanCode,
            boolean hasPaddles) {
        return ControllerDigitalButtonMapping.resolve(
                keyCode,
                scanCode,
                hasPaddles);
    }

    private static boolean suppressed(
            ControllerDigitalButtonMapping.Target target,
            boolean horizontalHatUsed,
            boolean verticalHatUsed) {
        return ControllerDigitalButtonMapping.isSuppressedByHat(
                target,
                horizontalHatUsed,
                verticalHatUsed);
    }

    private static void assertMask(
            ControllerDigitalButtonMapping.Target target,
            int expectedMask) {
        assertEquals(expectedMask, target.getInputMask());
    }
}
