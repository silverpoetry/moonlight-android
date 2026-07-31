package com.limelight.settings.stream;

import java.util.Locale;
import java.util.Objects;

/**
 * Validated user-authored stream resolution.
 */
public final class CustomResolution
        implements Comparable<CustomResolution> {
    public static final int MIN_DIMENSION = 1;
    public static final int MAX_DIMENSION = 99_999;

    private final int width;
    private final int height;

    public CustomResolution(int width, int height) {
        if (width < MIN_DIMENSION ||
                width > MAX_DIMENSION ||
                height < MIN_DIMENSION ||
                height > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    "Resolution dimensions are out of range");
        }
        this.width = width;
        this.height = height;
    }

    public static CustomResolution parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('×', 'x');
        int separator = normalized.indexOf('x');
        if (separator <= 0 ||
                separator != normalized.lastIndexOf('x') ||
                separator == normalized.length() - 1) {
            return null;
        }
        Integer width = parseDimension(
                normalized.substring(0, separator));
        Integer height = parseDimension(
                normalized.substring(separator + 1));
        return width == null || height == null
                ? null
                : new CustomResolution(width, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String toStorageValue() {
        return width + "x" + height;
    }

    @Override
    public int compareTo(CustomResolution other) {
        return toStorageValue().compareTo(
                Objects.requireNonNull(other, "other")
                        .toStorageValue());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CustomResolution)) {
            return false;
        }
        CustomResolution that = (CustomResolution) other;
        return width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public String toString() {
        return toStorageValue();
    }

    private static Integer parseDimension(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= MIN_DIMENSION &&
                    parsed <= MAX_DIMENSION
                    ? parsed
                    : null;
        }
        catch (NumberFormatException invalidValue) {
            return null;
        }
    }
}
