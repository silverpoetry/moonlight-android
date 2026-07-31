package com.limelight.shortcuts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decoded shortcuts plus the number of invalid entries isolated.
 */
public final class GameMenuShortcutDecodeResult {
    private final List<GameMenuShortcut> shortcuts;
    private final int rejectedEntryCount;

    public GameMenuShortcutDecodeResult(
            List<GameMenuShortcut> shortcuts,
            int rejectedEntryCount) {
        if (rejectedEntryCount < 0) {
            throw new IllegalArgumentException(
                    "rejectedEntryCount must not be negative");
        }
        this.shortcuts = Collections.unmodifiableList(
                new ArrayList<>(shortcuts));
        this.rejectedEntryCount = rejectedEntryCount;
    }

    public List<GameMenuShortcut> getShortcuts() {
        return shortcuts;
    }

    public int getRejectedEntryCount() {
        return rejectedEntryCount;
    }
}
