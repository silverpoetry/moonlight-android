package com.limelight.ui;

/**
 * Pure geometry for sizing the decoded stream and post-processing surfaces.
 *
 * <p>This class has no Android dependencies so the same rules can be exercised
 * as deterministic local unit tests. Integer conversion deliberately matches
 * Android view measurement: aspect-fit dimensions truncate, while requested
 * render-surface dimensions round to the nearest pixel and then become even.</p>
 */
public final class StreamLayoutGeometry {
    private static final double DEFAULT_ASPECT_RATIO = 16.0 / 9.0;

    private StreamLayoutGeometry() {
    }

    public static double getAspectRatio(int width, int height) {
        if (width <= 0 || height <= 0) {
            return 0.0;
        }
        return width / (double) height;
    }

    public static boolean hasCompatibleAspectRatio(
            int firstWidth,
            int firstHeight,
            int secondWidth,
            int secondHeight,
            double tolerance) {
        if (firstWidth <= 0 ||
                firstHeight <= 0 ||
                secondWidth <= 0 ||
                secondHeight <= 0 ||
                !Double.isFinite(tolerance) ||
                tolerance < 0) {
            return false;
        }

        // Keep the legacy display-mode comparison exactly equivalent. It uses
        // height/width because that is how the pre-Marshmallow path was
        // historically evaluated.
        double firstRatio = firstHeight / (double) firstWidth;
        double secondRatio = secondHeight / (double) secondWidth;
        return Math.abs(firstRatio - secondRatio) < tolerance;
    }

    public static Size fitWithin(
            int availableWidth,
            int availableHeight,
            double aspectRatio) {
        if (availableWidth < 0 || availableHeight < 0) {
            throw new IllegalArgumentException(
                    "Available dimensions must be non-negative");
        }
        if (!Double.isFinite(aspectRatio) || aspectRatio <= 0) {
            throw new IllegalArgumentException(
                    "Aspect ratio must be finite and positive");
        }

        if (availableWidth > availableHeight * aspectRatio) {
            return new Size(
                    (int) (availableHeight * aspectRatio),
                    availableHeight);
        }
        return new Size(
                availableWidth,
                (int) (availableWidth / aspectRatio));
    }

    public static Size getEvenOutputSize(
            int sourceWidth,
            int sourceHeight,
            int targetHeight,
            int minimumTargetWidth) {
        if (targetHeight <= 0 || minimumTargetWidth < 0) {
            throw new IllegalArgumentException(
                    "Target height must be positive and minimum width non-negative");
        }

        double aspectRatio = getAspectRatio(sourceWidth, sourceHeight);
        if (aspectRatio == 0.0) {
            aspectRatio = DEFAULT_ASPECT_RATIO;
        }

        int targetWidth = Math.max(
                (int) Math.round(targetHeight * aspectRatio),
                minimumTargetWidth);
        return new Size(makeEven(targetWidth), makeEven(targetHeight));
    }

    private static int makeEven(int value) {
        return value & ~1;
    }

    public static final class Size {
        public final int width;
        public final int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Size)) {
                return false;
            }
            Size that = (Size) other;
            return width == that.width && height == that.height;
        }

        @Override
        public int hashCode() {
            return 31 * width + height;
        }

        @Override
        public String toString() {
            return "Size{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
