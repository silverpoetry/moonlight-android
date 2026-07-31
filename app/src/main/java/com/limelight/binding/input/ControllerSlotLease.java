package com.limelight.binding.input;

import java.util.Objects;

/**
 * Owns one controller context's selected protocol slot, reservation ownership,
 * and host-announcement state.
 */
final class ControllerSlotLease {
    private volatile short controllerNumber;
    private volatile boolean selected;
    private volatile boolean reserved;
    private volatile boolean assigned;

    short getControllerNumber() {
        return controllerNumber;
    }

    boolean isReserved() {
        return reserved;
    }

    boolean isAssigned() {
        return assigned;
    }

    void selectFixed(short controllerNumber) {
        ensureSelectionMutable();
        validateControllerNumber(controllerNumber);
        this.controllerNumber = controllerNumber;
        selected = true;
        reserved = false;
    }

    boolean reserveNext(ControllerSlotAllocator allocator) {
        Objects.requireNonNull(allocator, "allocator");
        ensureSelectionMutable();
        short reservedSlot = allocator.reserveNext();
        if (reservedSlot == ControllerSlotAllocator.NO_SLOT) {
            selectFixed((short) 0);
            return false;
        }
        controllerNumber = reservedSlot;
        selected = true;
        reserved = true;
        return true;
    }

    void completeAssignment() {
        if (!selected) {
            throw new IllegalStateException(
                    "Controller slot has not been selected");
        }
        assigned = true;
    }

    boolean releaseReservation(ControllerSlotAllocator allocator) {
        Objects.requireNonNull(allocator, "allocator");
        if (!reserved) {
            return false;
        }
        allocator.release(controllerNumber);
        reserved = false;
        return true;
    }

    void transferTo(ControllerSlotLease replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (replacement == this) {
            throw new IllegalArgumentException(
                    "Cannot transfer a controller slot lease to itself");
        }
        if (replacement.selected || replacement.assigned ||
                replacement.reserved) {
            throw new IllegalStateException(
                    "Replacement controller slot lease is already owned");
        }

        replacement.controllerNumber = controllerNumber;
        replacement.selected = selected;
        replacement.reserved = reserved;
        replacement.assigned = assigned;
        // Keep the old routing snapshot for a callback that was already in
        // flight when its device context was destroyed. Only reservation
        // ownership moves to the replacement.
        reserved = false;
    }

    private static void validateControllerNumber(short controllerNumber) {
        if (controllerNumber < 0 ||
                controllerNumber >= ControllerSlotAllocator.MAX_SLOTS) {
            throw new IllegalArgumentException(
                    "Controller number out of range: " +
                            controllerNumber);
        }
    }

    private void ensureSelectionMutable() {
        if (selected || assigned || reserved) {
            throw new IllegalStateException(
                    "Controller slot selection is already owned");
        }
    }
}
