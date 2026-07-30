package com.limelight.utils;

/**
 * Pure inset policy shared by the Android window adapters.
 *
 * Keeping the decision separate from {@code WindowInsets} makes the stream geometry
 * deterministic and unit-testable. The returned insets are applied to the content
 * root, so video layout, touch coordinates, and local cursor coordinates all use the
 * same drawable rectangle.
 */
final class WindowInsetsPolicy {
    static final EdgeInsets NONE = new EdgeInsets(0, 0, 0, 0);

    private WindowInsetsPolicy() {
    }

    static EdgeInsets resolveStreamInsets(boolean inMultiWindowMode,
                                          boolean allowDisplayCutout,
                                          EdgeInsets systemBars,
                                          EdgeInsets displayCutout) {
        EdgeInsets result = inMultiWindowMode ? systemBars : NONE;
        if (!allowDisplayCutout) {
            result = max(result, displayCutout);
        }
        return result;
    }

    static EdgeInsets resolveSafeContentInsets(EdgeInsets systemBars,
                                               EdgeInsets displayCutout) {
        return max(systemBars, displayCutout);
    }

    static EdgeInsets max(EdgeInsets first, EdgeInsets second) {
        return new EdgeInsets(
                Math.max(first.left, second.left),
                Math.max(first.top, second.top),
                Math.max(first.right, second.right),
                Math.max(first.bottom, second.bottom));
    }

    static final class EdgeInsets {
        final int left;
        final int top;
        final int right;
        final int bottom;

        EdgeInsets(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EdgeInsets)) {
                return false;
            }
            EdgeInsets that = (EdgeInsets) other;
            return left == that.left &&
                    top == that.top &&
                    right == that.right &&
                    bottom == that.bottom;
        }

        @Override
        public int hashCode() {
            int result = left;
            result = 31 * result + top;
            result = 31 * result + right;
            result = 31 * result + bottom;
            return result;
        }

        @Override
        public String toString() {
            return "EdgeInsets{" +
                    "left=" + left +
                    ", top=" + top +
                    ", right=" + right +
                    ", bottom=" + bottom +
                    '}';
        }
    }
}
