package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerInputStateTest {
    @Test
    public void stickConversionAppliesRadialDeadzoneWithoutRenormalizing() {
        ControllerInputState state = new ControllerInputState();

        state.updateLeftStick(0.5f, 0, 0.5f);
        assertEquals(0, state.getLeftStickX());
        assertEquals(0, state.getLeftStickY());

        state.updateLeftStick(0.6f, 0.25f, 0.5f);
        assertEquals(19659, state.getLeftStickX());
        assertEquals(-8191, state.getLeftStickY());
    }

    @Test
    public void leftAndRightSticksUseTheSameInvertedYMapping() {
        ControllerInputState state = new ControllerInputState();

        state.updateLeftStick(-0.5f, -0.5f, 0);
        state.updateRightStick(-0.5f, -0.5f, 0);

        assertEquals(-16383, state.getLeftStickX());
        assertEquals(16383, state.getLeftStickY());
        assertEquals(state.getLeftStickX(), state.getRightStickX());
        assertEquals(state.getLeftStickY(), state.getRightStickY());
    }

    @Test
    public void buttonMasksComposeAndReleaseIndependently() {
        ControllerInputState state = new ControllerInputState();

        state.setButtonMask(ControllerPacket.A_FLAG, true);
        state.setButtonMask(ControllerPacket.B_FLAG, true);
        state.setButtonMask(ControllerPacket.A_FLAG, false);

        assertEquals(ControllerPacket.B_FLAG, state.getInputMap());
    }

    @Test
    public void hatUpdatesOnlyDirectionalBitsAndRemembersUsedAxes() {
        ControllerInputState state = new ControllerInputState();
        state.setButtonMask(ControllerPacket.X_FLAG, true);

        state.updateHat(-1.0f, 1.0f);

        assertEquals(
                ControllerPacket.X_FLAG |
                        ControllerPacket.LEFT_FLAG |
                        ControllerPacket.DOWN_FLAG,
                state.getInputMap());
        assertTrue(state.isHorizontalHatUsed());
        assertTrue(state.isVerticalHatUsed());

        state.updateHat(0, 0);
        assertEquals(ControllerPacket.X_FLAG, state.getInputMap());
        assertTrue(state.isHorizontalHatUsed());
        assertTrue(state.isVerticalHatUsed());
    }

    @Test
    public void negativeIdleTriggersNormalizeOnlyAfterMovement() {
        ControllerInputState state = new ControllerInputState();

        state.updateTriggerAxes(0, 0, true, 0.13f);
        assertEquals(0, Byte.toUnsignedInt(state.getLeftTrigger()));
        assertEquals(0, Byte.toUnsignedInt(state.getRightTrigger()));

        state.updateTriggerAxes(-0.5f, 1.0f, true, 0.13f);
        assertEquals(63, Byte.toUnsignedInt(state.getLeftTrigger()));
        assertEquals(255, Byte.toUnsignedInt(state.getRightTrigger()));
        assertTrue(state.isLeftTriggerAxisUsed());
        assertTrue(state.isRightTriggerAxisUsed());
    }

    @Test
    public void analogTriggerUseSuppressesDigitalTriggerMutation() {
        ControllerInputState state = new ControllerInputState();

        assertTrue(state.setDigitalTrigger(true, true));
        assertEquals(255, Byte.toUnsignedInt(state.getLeftTrigger()));
        assertTrue(state.setDigitalTrigger(true, false));

        state.updateTriggerAxes(0.5f, 0, false, 0.13f);

        assertFalse(state.setDigitalTrigger(true, false));
        assertEquals(127, Byte.toUnsignedInt(state.getLeftTrigger()));
        assertTrue(state.setDigitalTrigger(false, true));
        assertEquals(255, Byte.toUnsignedInt(state.getRightTrigger()));
    }

    @Test
    public void replacementSetsOneCompleteProtocolSnapshot() {
        ControllerInputState state = new ControllerInputState();

        state.replace(
                123,
                (byte) 45,
                (byte) 67,
                (short) 100,
                (short) -200,
                (short) 300,
                (short) -400);

        assertEquals(123, state.getInputMap());
        assertEquals(45, state.getLeftTrigger());
        assertEquals(67, state.getRightTrigger());
        assertEquals(100, state.getLeftStickX());
        assertEquals(-200, state.getLeftStickY());
        assertEquals(300, state.getRightStickX());
        assertEquals(-400, state.getRightStickY());
    }

    @Test
    public void restorationPreservesProtocolAndAxisOwnershipState() {
        ControllerInputState previous = new ControllerInputState();
        previous.replace(
                ControllerPacket.Y_FLAG,
                (byte) 12,
                (byte) 34,
                (short) 100,
                (short) 200,
                (short) 300,
                (short) 400);
        previous.updateTriggerAxes(0.5f, 0.75f, false, 0.13f);
        previous.updateHat(-1, 0);

        ControllerInputState restored = new ControllerInputState();
        restored.restoreFrom(previous);

        assertEquals(previous.getInputMap(), restored.getInputMap());
        assertEquals(previous.getLeftTrigger(), restored.getLeftTrigger());
        assertEquals(previous.getRightTrigger(), restored.getRightTrigger());
        assertEquals(previous.getLeftStickX(), restored.getLeftStickX());
        assertEquals(previous.getLeftStickY(), restored.getLeftStickY());
        assertEquals(previous.getRightStickX(), restored.getRightStickX());
        assertEquals(previous.getRightStickY(), restored.getRightStickY());
        assertTrue(restored.isHorizontalHatUsed());
        assertFalse(restored.setDigitalTrigger(true, false));
        assertFalse(restored.setDigitalTrigger(false, false));
    }
}
