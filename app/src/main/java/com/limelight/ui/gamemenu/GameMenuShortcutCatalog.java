package com.limelight.ui.gamemenu;

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
        LinkedHashMap<String, Entry> entries =
                new LinkedHashMap<>();
        if (includeBuiltInShortcuts) {
            addBuiltInShortcuts(entries);
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
        List<GameMenuShortcut> shortcuts =
                new ArrayList<>();
        for (Entry entry : load(
                persistedShortcuts,
                includeBuiltInShortcuts)) {
            shortcuts.add(entry.shortcut);
        }
        return shortcuts;
    }

    private static void addBuiltInShortcuts(
            Map<String, Entry> destination) {
        addBuiltIn(
                destination,
                "escape",
                "ESC (退出/菜单)",
                KeyboardTranslator.VK_ESCAPE);
        addBuiltIn(
                destination,
                "f11",
                "F11 (网页全屏)",
                KeyboardTranslator.VK_F11);
        addBuiltIn(
                destination,
                "alt_f4",
                "Alt + F4 (关闭应用)",
                KeyboardTranslator.VK_LMENU,
                KeyboardTranslator.VK_F4);
        addBuiltIn(
                destination,
                "alt_enter",
                "Alt + Enter (窗口大小)",
                KeyboardTranslator.VK_LMENU,
                KeyboardTranslator.VK_RETURN);
        addBuiltIn(
                destination,
                "windows",
                "Win (打开Windows开始菜单)",
                KeyboardTranslator.VK_LWIN);
        addBuiltIn(
                destination,
                "task_manager",
                "Ctrl+Shift+ESC (任务管理器)",
                KeyboardTranslator.VK_LCONTROL,
                KeyboardTranslator.VK_LSHIFT,
                KeyboardTranslator.VK_ESCAPE);
        addBuiltIn(
                destination,
                "show_desktop",
                "Win + D (返回桌面)",
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_D);
        addBuiltIn(
                destination,
                "project",
                "Win + P (显示器模式)",
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_P);
        addBuiltIn(
                destination,
                "game_bar",
                "Win + G (打开Xbox Game Bar)",
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_G);
        addBuiltIn(
                destination,
                "steam_overlay",
                "Shift + Tab (打开Steam Overlay)",
                KeyboardTranslator.VK_LSHIFT,
                KeyboardTranslator.VK_TAB);
        addBuiltIn(
                destination,
                "move_window_left",
                "Win + Shift + left (切换桌面)",
                KeyboardTranslator.VK_LWIN,
                KeyboardTranslator.VK_LSHIFT,
                KeyboardTranslator.VK_LEFT);
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
