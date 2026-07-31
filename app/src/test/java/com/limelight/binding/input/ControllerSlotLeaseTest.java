package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ControllerSlotLeaseTest {
    @Test
    public void fixedSelectionDoesNotOwnAllocatorReservation() {
        ControllerSlotLease lease = new ControllerSlotLease();
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);

        lease.selectFixed((short) 3);
        lease.completeAssignment();

        assertEquals(3, lease.getControllerNumber());
        assertTrue(lease.isAssigned());
        assertFalse(lease.isReserved());
        assertFalse(lease.releaseReservation(allocator));
    }

    @Test
    public void reservationOwnsAndReleasesAllocatedSlotOnce() {
        ControllerSlotLease lease = new ControllerSlotLease();
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);

        assertTrue(lease.reserveNext(allocator));
        lease.completeAssignment();
        assertTrue(lease.releaseReservation(allocator));
        assertFalse(lease.releaseReservation(allocator));

        assertEquals(0, allocator.reserveNext());
    }

    @Test
    public void exhaustedAllocatorFallsBackToUnreservedPlayerOne() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);
        for (int i = 0; i < ControllerSlotAllocator.MAX_SLOTS; i++) {
            allocator.reserveNext();
        }
        ControllerSlotLease lease = new ControllerSlotLease();

        assertFalse(lease.reserveNext(allocator));

        assertEquals(0, lease.getControllerNumber());
        assertFalse(lease.isReserved());
    }

    @Test
    public void snapshotMovesReservationOwnershipDuringContextMigration() {
        ControllerSlotAllocator allocator =
                new ControllerSlotAllocator((short) 0);
        ControllerSlotLease oldLease = new ControllerSlotLease();
        oldLease.reserveNext(allocator);
        oldLease.completeAssignment();
        ControllerSlotLease newLease = new ControllerSlotLease();

        short oldControllerNumber = oldLease.getControllerNumber();
        oldLease.transferTo(newLease);

        assertEquals(oldControllerNumber, newLease.getControllerNumber());
        assertTrue(newLease.isReserved());
        assertTrue(newLease.isAssigned());
        assertFalse(oldLease.isReserved());
        assertTrue(oldLease.isAssigned());
        assertEquals(
                oldControllerNumber,
                oldLease.getControllerNumber());
        assertTrue(newLease.releaseReservation(allocator));
    }

    @Test
    public void invalidFixedSlotIsRejected() {
        ControllerSlotLease lease = new ControllerSlotLease();

        assertThrows(
                IllegalArgumentException.class,
                () -> lease.selectFixed((short) -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> lease.selectFixed(
                        (short) ControllerSlotAllocator.MAX_SLOTS));
    }

    @Test
    public void completedSelectionCannotBeSilentlyReassigned() {
        ControllerSlotLease lease = new ControllerSlotLease();
        lease.selectFixed((short) 1);
        lease.completeAssignment();

        assertThrows(
                IllegalStateException.class,
                () -> lease.selectFixed((short) 2));
        assertThrows(
                IllegalStateException.class,
                () -> lease.reserveNext(
                        new ControllerSlotAllocator((short) 0)));
    }

    @Test
    public void assignmentCannotCompleteBeforeSelection() {
        ControllerSlotLease lease = new ControllerSlotLease();

        assertThrows(
                IllegalStateException.class,
                lease::completeAssignment);
    }
}
