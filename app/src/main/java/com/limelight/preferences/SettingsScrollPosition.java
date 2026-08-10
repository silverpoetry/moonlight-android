package com.limelight.preferences;

import java.util.Objects;

/** Immutable position within a lazily rendered settings list. */
public final class SettingsScrollPosition {
    public static final SettingsScrollPosition START =
            new SettingsScrollPosition(0, 0);

    private final int itemIndex;
    private final int itemOffset;

    private SettingsScrollPosition(int itemIndex, int itemOffset) {
        this.itemIndex = itemIndex;
        this.itemOffset = itemOffset;
    }

    public static SettingsScrollPosition of(
            int itemIndex,
            int itemOffset) {
        int normalizedIndex = Math.max(0, itemIndex);
        int normalizedOffset = Math.max(0, itemOffset);
        if (normalizedIndex == 0 && normalizedOffset == 0) {
            return START;
        }
        return new SettingsScrollPosition(
                normalizedIndex,
                normalizedOffset);
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public int getItemOffset() {
        return itemOffset;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SettingsScrollPosition)) {
            return false;
        }
        SettingsScrollPosition position =
                (SettingsScrollPosition) other;
        return itemIndex == position.itemIndex &&
                itemOffset == position.itemOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemIndex, itemOffset);
    }
}
