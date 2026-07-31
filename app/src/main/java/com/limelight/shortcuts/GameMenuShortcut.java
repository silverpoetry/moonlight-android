package com.limelight.shortcuts;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable keyboard shortcut exposed by the in-stream menu.
 *
 * <p>A shortcut carries exactly one input representation. Moonlight key
 * codes are sent through the native keyboard protocol, while Android key
 * codes are injected through the existing Android-key translation path.</p>
 */
public final class GameMenuShortcut {
    public static final int MAXIMUM_ID_LENGTH = 256;
    public static final int MAXIMUM_NAME_LENGTH = 128;
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 256;
    public static final int MAXIMUM_KEY_COUNT = 16;

    private final String id;
    private final String name;
    private final String description;
    private final short[] moonlightKeyCodes;
    private final int[] androidKeyCodes;
    private final boolean editable;

    private GameMenuShortcut(
            String id,
            String name,
            String description,
            short[] moonlightKeyCodes,
            int[] androidKeyCodes,
            boolean editable) {
        this.id = requireText(id, "id", MAXIMUM_ID_LENGTH);
        this.name = requireText(
                name, "name", MAXIMUM_NAME_LENGTH);
        this.description = optionalText(
                description, "description",
                MAXIMUM_DESCRIPTION_LENGTH);
        this.moonlightKeyCodes = copyKeys(moonlightKeyCodes);
        this.androidKeyCodes = copyKeys(androidKeyCodes);
        if ((this.moonlightKeyCodes.length == 0) ==
                (this.androidKeyCodes.length == 0)) {
            throw new IllegalArgumentException(
                    "Exactly one shortcut key representation is required");
        }
        this.editable = editable;
    }

    public static GameMenuShortcut moonlightChord(
            String id,
            String name,
            String description,
            short[] keyCodes,
            boolean editable) {
        return new GameMenuShortcut(
                id, name, description, keyCodes, null, editable);
    }

    public static GameMenuShortcut androidChord(
            String id,
            String name,
            String description,
            int[] keyCodes,
            boolean editable) {
        return new GameMenuShortcut(
                id, name, description, null, keyCodes, editable);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public short[] getMoonlightKeyCodes() {
        return Arrays.copyOf(
                moonlightKeyCodes, moonlightKeyCodes.length);
    }

    public int[] getAndroidKeyCodes() {
        return Arrays.copyOf(
                androidKeyCodes, androidKeyCodes.length);
    }

    public boolean usesMoonlightKeyCodes() {
        return moonlightKeyCodes.length != 0;
    }

    public boolean isEditable() {
        return editable;
    }

    private static short[] copyKeys(short[] source) {
        if (source == null) {
            return new short[0];
        }
        if (source.length == 0 ||
                source.length > MAXIMUM_KEY_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid Moonlight shortcut key count");
        }
        return Arrays.copyOf(source, source.length);
    }

    private static int[] copyKeys(int[] source) {
        if (source == null) {
            return new int[0];
        }
        if (source.length == 0 ||
                source.length > MAXIMUM_KEY_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid Android shortcut key count");
        }
        int[] copy = Arrays.copyOf(source, source.length);
        for (int keyCode : copy) {
            if (keyCode < 0 || keyCode > 0xffff) {
                throw new IllegalArgumentException(
                        "Android shortcut key code is out of range");
            }
        }
        return copy;
    }

    private static String requireText(
            String value, String fieldName, int maximumLength) {
        String required = Objects.requireNonNull(
                value, fieldName).trim();
        if (required.isEmpty() ||
                required.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "Invalid shortcut " + fieldName);
        }
        return required;
    }

    private static String optionalText(
            String value, String fieldName, int maximumLength) {
        if (value == null) {
            return "";
        }
        String optional = value.trim();
        if (optional.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "Invalid shortcut " + fieldName);
        }
        return optional;
    }
}
