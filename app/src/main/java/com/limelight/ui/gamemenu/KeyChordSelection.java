package com.limelight.ui.gamemenu;

import java.util.ArrayList;
import java.util.List;

final class KeyChordSelection {
    private final int maximumSize;
    private final List<String> keyCodes = new ArrayList<>();
    private final List<String> keyNames = new ArrayList<>();

    KeyChordSelection(int maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException(
                    "maximumSize must be positive");
        }
        this.maximumSize = maximumSize;
    }

    boolean add(String keyCode, String keyName) {
        if (keyCode == null || keyName == null) {
            throw new IllegalArgumentException(
                    "keyCode and keyName must be non-null");
        }
        if (keyCodes.size() >= maximumSize) {
            return false;
        }
        keyCodes.add(keyCode);
        keyNames.add(keyName);
        return true;
    }

    boolean isEmpty() {
        return keyCodes.isEmpty();
    }

    String getEncodedKeyCodes() {
        return join(",", keyCodes);
    }

    String getDisplayName() {
        return join("+", keyNames);
    }

    void clear() {
        keyCodes.clear();
        keyNames.clear();
    }

    private static String join(
            String separator, List<String> values) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                result.append(separator);
            }
            result.append(values.get(index));
        }
        return result.toString();
    }
}
