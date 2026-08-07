package com.limelight.ui.gamemenu;

import android.content.Context;
import android.content.res.Resources;

import com.limelight.R;

import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.shortcuts.GameMenuShortcutIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure catalog policy that combines fixed shortcuts with one persisted
 * shortcut snapshot. Storage and migration belong to the Activity-owned
 * repository adapter.
 */
final class GameMenuShortcutCatalog {
    static final class Entry {
        final String id;
        final GameMenuShortcut shortcut;

        Entry(GameMenuShortcut shortcut) {
            this.id = shortcut.getId();
            this.shortcut = shortcut;
        }
    }

    private GameMenuShortcutCatalog() {
    }

    static List<Entry> load(
            List<GameMenuShortcut> persistedShortcuts,
            boolean includeBuiltInShortcuts) {
        return load(
                persistedShortcuts,
                includeBuiltInShortcuts,
                null);
    }

    static List<Entry> load(
            List<GameMenuShortcut> persistedShortcuts,
            boolean includeBuiltInShortcuts,
            Context context) {
        android.content.res.Resources resources = context == null ?
                null : context.getResources();
        LinkedHashMap<String, Entry> entries =
                new LinkedHashMap<>();
        if (includeBuiltInShortcuts) {
            addBuiltInShortcuts(entries, resources);
        }
        for (GameMenuShortcut shortcut :
                persistedShortcuts) {
            add(entries, new Entry(shortcut));
        }
        return new ArrayList<>(entries.values());
    }

    static List<GameMenuShortcut> loadShortcuts(
            List<GameMenuShortcut> persistedShortcuts,
            boolean includeBuiltInShortcuts) {
        return loadShortcuts(
                persistedShortcuts,
                includeBuiltInShortcuts,
                null);
    }

    static List<GameMenuShortcut> loadShortcuts(
            List<GameMenuShortcut> persistedShortcuts,
            boolean includeBuiltInShortcuts,
            Context context) {
        android.content.res.Resources resources = context == null ?
                null : context.getResources();
        List<GameMenuShortcut> shortcuts =
                new ArrayList<>();
        for (Entry entry : load(
                persistedShortcuts,
                includeBuiltInShortcuts,
                context)) {
            shortcuts.add(entry.shortcut);
        }
        return shortcuts;
    }

    private static void addBuiltInShortcuts(
            Map<String, Entry> destination,
            Resources resources) {
        addBuiltIn(
                destination,
                "escape",
                label(resources, R.string.shortcut_escape,
                        "ESC (Exit/menu)"),
                KeyboardTranslator.VK_ESCAPE);
        addBuiltIn(
                destination,
                "f11",
                label(resources, R.string.shortcut_f11,
                        "F11 (Web fullscreen)"),
                KeyboardTranslator.VK_F11);
        addBuiltIn(
                destination,
                "alt_f4",
                label(resources, R.string.shortcut_alt_f4,
                        "Alt + F4 (Close app)"),
                KeyboardTranslator.VK_LMENU,
                KeyboardTranslator.VK_F4);
        addBuiltIn(
                destination,
                "alt_enter",
                label(resources, R.string.shortcut_alt_enter,
                        "Alt + Enter (Window size)"),
                KeyboardTranslator.VK_LMENU,
                KeyboardTranslator.VK_RETURN);
        addBuiltIn(
                destination,
                "windows",
                label(resources, R.string.shortcut_windows,
                        "Win (Open Start menu)"),
                KeyboardTranslator.VK_LWIN);
        addBuiltIn(
                destination,
                "task_manager",
                label(resources, R.string.shortcut_task_manager,
                        "Ctrl+Shift+ESC (Task Manager)"),
                KeyboardTranslator.VK_LCONTROL,
                KeyboardTranslator.VK_LSHIFT,
                KeyboardTranslator.VK_ESCAPE);
        addBuiltIn(
                destination,
                "show_desktop",
                label(resources, R.string.shortcut_show_desktop,
                        "Win + D (Show desktop)"),
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_D);
        addBuiltIn(
                destination,
                "project",
                label(resources, R.string.shortcut_project,
                        "Win + P (Display mode)"),
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_P);
        addBuiltIn(
                destination,
                "game_bar",
                label(resources, R.string.shortcut_game_bar,
                        "Win + G (Open Xbox Game Bar)"),
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_G);
        addBuiltIn(
                destination,
                "steam_overlay",
                label(resources, R.string.shortcut_steam_overlay,
                        "Shift + Tab (Open Steam Overlay)"),
                KeyboardTranslator.VK_LSHIFT,
                KeyboardTranslator.VK_TAB);
        addBuiltIn(
                destination,
                "move_window_left",
                label(resources, R.string.shortcut_move_window_left,
                        "Win + Shift + left (Switch desktop)"),
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_LSHIFT,
                KeyboardTranslator.VK_LEFT);
    }

    private static String label(
            Resources resources,
            int resourceId,
            String fallback) {
        return resources == null ? fallback :
                resources.getString(resourceId);
    }

    private static void addBuiltIn(
            Map<String, Entry> destination,
            String id,
            String name,
            int... keyCodes) {
        short[] keys = new short[keyCodes.length];
        for (int index = 0; index < keyCodes.length; index++) {
            keys[index] = (short) keyCodes[index];
        }
        GameMenuShortcut shortcut =
                GameMenuShortcut.moonlightChord(
                        GameMenuShortcutIds.builtIn(id),
                        name,
                        "",
                        keys,
                        false);
        add(destination, new Entry(shortcut));
    }

    private static void add(
            Map<String, Entry> destination, Entry entry) {
        destination.put(entry.id, entry);
    }
}
