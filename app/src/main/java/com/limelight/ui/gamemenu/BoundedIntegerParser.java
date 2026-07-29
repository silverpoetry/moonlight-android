package com.limelight.ui.gamemenu;

/**
 * Parses decimal user input without allowing overflow or values outside the
 * caller's accepted range.
 */
final class BoundedIntegerParser {
    private BoundedIntegerParser() {
    }

    static Integer parse(String text, int minimum, int maximum) {
        if (text == null || minimum > maximum) {
            return null;
        }

        try {
            int value = Integer.parseInt(text.trim());
            return value >= minimum && value <= maximum ? value : null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
