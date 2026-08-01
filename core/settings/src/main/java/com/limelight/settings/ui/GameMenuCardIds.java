package com.limelight.settings.ui;

import java.util.Objects;

/**
 * Stable identity namespace shared by card catalogs and settings migration.
 */
public final class GameMenuCardIds {
    private static final String ACTION_PREFIX = "action:";

    private GameMenuCardIds() {
    }

    public static String action(String actionId) {
        Objects.requireNonNull(actionId, "actionId");
        if (actionId.isEmpty() ||
                ACTION_PREFIX.length() + actionId.length() >
                        GameMenuCardLayout
                                .MAXIMUM_CARD_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "Action ID is invalid");
        }
        return ACTION_PREFIX + actionId;
    }
}
