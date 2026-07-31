package com.limelight.binding.input;

/**
 * Owns the sixteen controller slots represented by the protocol mask.
 *
 * <p>Initially enumerated devices remain in {@code initialMask} until their
 * first input reserves a concrete slot. A reservation atomically moves that
 * bit into {@code currentMask}, so releasing the device later also removes it
 * from the host-visible mask.</p>
 */
final class ControllerSlotAllocator {
    static final int MAX_SLOTS = Short.SIZE;
    static final short NO_SLOT = -1;
    private static final int ALL_SLOTS_MASK = 0xFFFF;

    private int currentMask;
    private int initialMask;

    ControllerSlotAllocator(short initialMask) {
        this.initialMask = initialMask & ALL_SLOTS_MASK;
    }

    short reserveNext() {
        for (short slot = 0;
                slot < MAX_SLOTS;
                slot++) {
            int bit = 1 << slot;
            if ((currentMask & bit) != 0) {
                continue;
            }
            currentMask |= bit;
            initialMask &= ~bit;
            return slot;
        }
        return NO_SLOT;
    }

    void release(short slot) {
        checkSlot(slot);
        currentMask &= ~(1 << slot);
    }

    short getActiveMask(
            boolean multiControllerEnabled,
            boolean onscreenControllerEnabled) {
        if (!multiControllerEnabled) {
            return 1;
        }
        int activeMask = currentMask | initialMask;
        if (onscreenControllerEnabled) {
            activeMask |= 1;
        }
        return (short) activeMask;
    }

    private static void checkSlot(short slot) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            throw new IllegalArgumentException(
                    "Controller slot out of range: " + slot);
        }
    }
}
