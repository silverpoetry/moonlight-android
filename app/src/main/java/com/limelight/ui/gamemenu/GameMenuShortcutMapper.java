package com.limelight.ui.gamemenu;

import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.shortcuts.GameMenuShortcutIds;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

/**
 * Explicit anti-corruption boundary between the immutable shortcut domain
 * and the legacy mutable row model still shared by virtual controls.
 */
final class GameMenuShortcutMapper {
    private GameMenuShortcutMapper() {
    }

    static GameMenuShortcut fromEditor(
            GameMenuQuickBean bean) {
        String stableId =
                GameMenuShortcutIds.custom(bean.getId());
        short[] moonlightKeys = bean.getDatas();
        if (moonlightKeys != null &&
                moonlightKeys.length != 0) {
            return GameMenuShortcut.moonlightChord(
                    stableId,
                    bean.getName(),
                    bean.getDesc(),
                    moonlightKeys,
                    true);
        }
        return GameMenuShortcut.androidChord(
                stableId,
                bean.getName(),
                bean.getDesc(),
                parseAndroidKeyCodes(bean.getCodes()),
                true);
    }

    static GameMenuQuickBean toRow(
            GameMenuShortcut shortcut) {
        GameMenuQuickBean row = new GameMenuQuickBean();
        row.setName(shortcut.getName());
        row.setDesc(shortcut.getDescription());
        if (shortcut.isEditable()) {
            row.setId(shortcut.getId());
        }
        if (shortcut.usesMoonlightKeyCodes()) {
            row.setDatas(shortcut.getMoonlightKeyCodes());
        }
        else {
            row.setCodes(encodeAndroidKeyCodes(
                    shortcut.getAndroidKeyCodes()));
        }
        return row;
    }

    private static String encodeAndroidKeyCodes(int[] keyCodes) {
        StringBuilder encoded = new StringBuilder();
        for (int index = 0; index < keyCodes.length; index++) {
            if (index != 0) {
                encoded.append(',');
            }
            encoded.append(keyCodes[index]);
        }
        return encoded.toString();
    }

    private static int[] parseAndroidKeyCodes(
            String encodedCodes) {
        if (encodedCodes == null ||
                encodedCodes.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Android shortcut chord is empty");
        }
        String[] values = encodedCodes.split(",");
        int[] keyCodes = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            try {
                keyCodes[index] = Integer.parseInt(
                        values[index].trim());
            }
            catch (NumberFormatException error) {
                throw new IllegalArgumentException(
                        "Invalid Android shortcut key code",
                        error);
            }
        }
        return keyCodes;
    }
}
