package com.limelight.settings;

import java.util.Objects;

/**
 * A typed, validated key in the application settings schema.
 *
 * <p>The key and its default live together so persistence adapters, runtime
 * snapshots, and settings UI cannot silently choose different defaults.</p>
 */
public final class SettingKey<T> {
    public enum StorageType {
        BOOLEAN,
        INTEGER,
        LONG,
        FLOAT,
        STRING
    }

    interface Normalizer<T> {
        T normalize(T value);
    }

    private final String name;
    private final StorageType storageType;
    private final Class<T> valueClass;
    private final T defaultValue;
    private final Normalizer<T> normalizer;

    private SettingKey(
            String name,
            StorageType storageType,
            Class<T> valueClass,
            T defaultValue,
            Normalizer<T> normalizer) {
        this.name = requireName(name);
        this.storageType = Objects.requireNonNull(
                storageType,
                "storageType");
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
        this.defaultValue = normalizeTyped(
                Objects.requireNonNull(defaultValue, "defaultValue"));
    }

    public static SettingKey<Boolean> booleanKey(
            String name,
            boolean defaultValue) {
        return new SettingKey<>(
                name,
                StorageType.BOOLEAN,
                Boolean.class,
                defaultValue,
                value -> value);
    }

    public static SettingKey<Integer> integerKey(
            String name,
            int defaultValue,
            int minimum,
            int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "Minimum cannot exceed maximum");
        }
        return new SettingKey<>(
                name,
                StorageType.INTEGER,
                Integer.class,
                defaultValue,
                value -> Math.max(minimum, Math.min(maximum, value)));
    }

    public static SettingKey<Integer> integerSetKey(
            String name,
            int defaultValue,
            int... allowedValues) {
        int[] allowed = requireAllowedValues(
                defaultValue,
                allowedValues);
        return new SettingKey<>(
                name,
                StorageType.INTEGER,
                Integer.class,
                defaultValue,
                value -> contains(allowed, value)
                        ? value
                        : defaultValue);
    }

    public static SettingKey<Long> longKey(
            String name,
            long defaultValue,
            long minimum,
            long maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "Minimum cannot exceed maximum");
        }
        return new SettingKey<>(
                name,
                StorageType.LONG,
                Long.class,
                defaultValue,
                value -> Math.max(minimum, Math.min(maximum, value)));
    }

    public static SettingKey<Float> floatKey(
            String name,
            float defaultValue,
            float minimum,
            float maximum) {
        if (!Float.isFinite(minimum) ||
                !Float.isFinite(maximum) ||
                minimum > maximum) {
            throw new IllegalArgumentException("Invalid float range");
        }
        return new SettingKey<>(
                name,
                StorageType.FLOAT,
                Float.class,
                defaultValue,
                value -> Float.isFinite(value)
                        ? Math.max(minimum, Math.min(maximum, value))
                        : defaultValue);
    }

    public static SettingKey<String> stringKey(
            String name,
            String defaultValue) {
        return new SettingKey<>(
                name,
                StorageType.STRING,
                String.class,
                defaultValue,
                value -> value);
    }

    public static SettingKey<String> stringSetKey(
            String name,
            String defaultValue,
            String... allowedValues) {
        String[] allowed = requireAllowedValues(
                defaultValue,
                allowedValues);
        return new SettingKey<>(
                name,
                StorageType.STRING,
                String.class,
                defaultValue,
                value -> contains(allowed, value)
                        ? value
                        : defaultValue);
    }

    public String getName() {
        return name;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns a safe value for data loaded from an untyped persistence layer.
     */
    public T normalizeStoredValue(Object value) {
        if (!valueClass.isInstance(value)) {
            return defaultValue;
        }
        return normalizeTyped(valueClass.cast(value));
    }

    public T normalizeValue(T value) {
        return value == null ? defaultValue : normalizeTyped(value);
    }

    private T normalizeTyped(T value) {
        T normalized = normalizer.normalize(value);
        return Objects.requireNonNull(
                normalized,
                "Setting normalizer returned null");
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Setting name cannot be empty");
        }
        return name;
    }

    private static int[] requireAllowedValues(
            int defaultValue,
            int[] allowedValues) {
        Objects.requireNonNull(allowedValues, "allowedValues");
        int[] copy = allowedValues.clone();
        if (copy.length == 0 || !contains(copy, defaultValue)) {
            throw new IllegalArgumentException(
                    "Allowed values must contain the default");
        }
        return copy;
    }

    private static String[] requireAllowedValues(
            String defaultValue,
            String[] allowedValues) {
        Objects.requireNonNull(allowedValues, "allowedValues");
        String[] copy = allowedValues.clone();
        for (String value : copy) {
            Objects.requireNonNull(value, "allowedValue");
        }
        if (copy.length == 0 || !contains(copy, defaultValue)) {
            throw new IllegalArgumentException(
                    "Allowed values must contain the default");
        }
        return copy;
    }

    private static boolean contains(int[] values, int candidate) {
        for (int value : values) {
            if (value == candidate) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(
            String[] values,
            String candidate) {
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
