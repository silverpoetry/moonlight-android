package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class ControllerSlotAllocatorTest {
    @Test
    public void reservationMovesInitialSlotToCurrentOwnership() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0b111);

        assertEquals(0, allocator.reserveNext());
        assertEquals(
                0b111,
                allocator.getActiveMask(true, false));

        allocator.release((short) 0);

        assertEquals(
                0b110,
                allocator.getActiveMask(true, false));
    }

    @Test
    public void reservationsUseEveryProtocolSlotExactlyOnce() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);

        for (short slot = 0;
                slot < ControllerSlotAllocator.MAX_SLOTS;
                slot++) {
            assertEquals(slot, allocator.reserveNext());
        }
        assertEquals(
                ControllerSlotAllocator.NO_SLOT,
                allocator.reserveNext());
        assertEquals(
                (short) 0xFFFF,
                allocator.getActiveMask(true, false));
    }

    @Test
    public void releasedSlotIsReusedBeforeHigherSlots() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);
        assertEquals(0, allocator.reserveNext());
        assertEquals(1, allocator.reserveNext());

        allocator.release((short) 0);

        assertEquals(0, allocator.reserveNext());
    }

    @Test
    public void singleControllerModeAlwaysPublishesPlayerOne() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0b1100);

        assertEquals(
                1,
                allocator.getActiveMask(false, false));
    }

    @Test
    public void onscreenControllerAddsPlayerOneToMultiMask() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0b100);

        assertEquals(
                0b101,
                allocator.getActiveMask(true, true));
    }

    @Test
    public void invalidReleaseIsRejected() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.release((short) -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.release(
                        (short) ControllerSlotAllocator.MAX_SLOTS));
    }
}
