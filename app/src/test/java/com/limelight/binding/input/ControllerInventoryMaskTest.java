package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerInventoryMaskTest {
    @Test
    public void noControllersProducesEmptyMask() {
        assertEquals(
                0,
                ControllerInventoryMask.fromAttachedDevices(
                        0,
                        false));
    }

    @Test
    public void attachedControllersReserveConsecutiveSlots() {
        assertEquals(
                0b1_1111,
                ControllerInventoryMask.fromAttachedDevices(
                        5,
                        false));
    }

    @Test
    public void onscreenControllerReservesPlayerOne() {
        assertEquals(
                1,
                ControllerInventoryMask.fromAttachedDevices(
                        0,
                        true));
        assertEquals(
                0b111,
                ControllerInventoryMask.fromAttachedDevices(
                        3,
                        true));
    }

    @Test
    public void countIsBoundedByProtocolCapacity() {
        assertEquals(
                (short) 0xffff,
                ControllerInventoryMask.fromAttachedDevices(
                        ControllerSlotAllocator.MAX_SLOTS,
                        false));
        assertEquals(
                (short) 0xffff,
                ControllerInventoryMask.fromAttachedDevices(
                        Integer.MAX_VALUE,
                        true));
    }

    @Test
    public void negativeCountIsTreatedAsEmpty() {
        assertEquals(
                0,
                ControllerInventoryMask.fromAttachedDevices(
                        -1,
                        false));
    }
}
