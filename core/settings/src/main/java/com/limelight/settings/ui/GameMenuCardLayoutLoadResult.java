package com.limelight.settings.ui;

import java.util.Objects;

/**
 * API-21-safe result that distinguishes an absent layout from an explicitly
 * configured empty layout.
 */
public final class GameMenuCardLayoutLoadResult {
    private static final GameMenuCardLayoutLoadResult ABSENT =
            new GameMenuCardLayoutLoadResult(null);

    private final GameMenuCardLayout layout;

    private GameMenuCardLayoutLoadResult(
            GameMenuCardLayout layout) {
        this.layout = layout;
    }

    public static GameMenuCardLayoutLoadResult absent() {
        return ABSENT;
    }

    public static GameMenuCardLayoutLoadResult present(
            GameMenuCardLayout layout) {
        return new GameMenuCardLayoutLoadResult(
                Objects.requireNonNull(layout, "layout"));
    }

    public boolean hasLayout() {
        return layout != null;
    }

    public GameMenuCardLayout getLayout() {
        if (layout == null) {
            throw new IllegalStateException(
                    "No card layout is present");
        }
        return layout;
    }
}
