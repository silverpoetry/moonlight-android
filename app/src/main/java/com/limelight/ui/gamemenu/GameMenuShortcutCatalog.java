package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.limelight.LimeLog;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the same shortcuts shown by {@link GameListQuickFragment}, together
 * with stable IDs suitable for storing references in the game menu layout.
 */
final class GameMenuShortcutCatalog {
    private static final String IMPORTED_SHORTCUT_PREFERENCES =
            "specialPrefs";
    private static final String IMPORTED_SHORTCUT_KEY = "special_key";

    static final class Entry {
        final String id;
        final GameMenuQuickBean shortcut;

        Entry(String id, GameMenuQuickBean shortcut) {
            this.id = id;
            this.shortcut = shortcut;
        }
    }

    private GameMenuShortcutCatalog() {
    }

    static List<Entry> load(Context context,
                            boolean includeBuiltInShortcuts) {
        LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
        if (includeBuiltInShortcuts) {
            addBuiltInShortcuts(entries);
        }
        addImportedShortcuts(context, entries);
        addSavedShortcuts(context, entries);
        return new ArrayList<>(entries.values());
    }

    static List<GameMenuQuickBean> loadBeans(
            Context context, boolean includeBuiltInShortcuts) {
        List<GameMenuQuickBean> shortcuts = new ArrayList<>();
        for (Entry entry : load(context, includeBuiltInShortcuts)) {
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
        add(destination, new Entry(
                "shortcut:builtin:" + id,
                new GameMenuQuickBean(name, keys)));
    }

    private static void addImportedShortcuts(
            Context context, Map<String, Entry> destination) {
        SharedPreferences preferences = context.getSharedPreferences(
                IMPORTED_SHORTCUT_PREFERENCES, Activity.MODE_PRIVATE);
        String value = preferences.getString(IMPORTED_SHORTCUT_KEY, "");
        if (TextUtils.isEmpty(value)) {
            return;
        }

        try {
            JSONArray array = new JSONObject(value).optJSONArray("data");
            if (array == null) {
                return;
            }
            for (int index = 0; index < array.length(); index++) {
                JSONObject shortcutObject = array.getJSONObject(index);
                String name = shortcutObject.optString("name");
                JSONArray keyArray = shortcutObject.getJSONArray("data");
                short[] keys = new short[keyArray.length()];
                for (int keyIndex = 0;
                     keyIndex < keyArray.length(); keyIndex++) {
                    String code = keyArray.getString(keyIndex);
                    keys[keyIndex] = (short) Integer.parseInt(
                            code.substring(2), 16);
                }
                GameMenuQuickBean shortcut =
                        new GameMenuQuickBean(name, keys);
                String fingerprint =
                        name + '\n' + Arrays.toString(keys);
                add(destination, new Entry(
                        "shortcut:imported:" + sha256(fingerprint),
                        shortcut));
            }
        } catch (Exception error) {
            LimeLog.warning(
                    "Ignoring invalid imported shortcut data: " +
                            error.getMessage());
        }
    }

    private static void addSavedShortcuts(
            Context context, Map<String, Entry> destination) {
        SharedPreferences preferences = context.getSharedPreferences(
                GameListQuickFragment.PREF_QUICK_LIST_NAME,
                Activity.MODE_PRIVATE);
        List<Map.Entry<String, ?>> storedEntries =
                new ArrayList<>(preferences.getAll().entrySet());
        Collections.sort(
                storedEntries,
                (left, right) ->
                        left.getKey().compareTo(right.getKey()));

        Gson gson = new Gson();
        for (Map.Entry<String, ?> storedEntry : storedEntries) {
            if (!(storedEntry.getValue() instanceof String)) {
                continue;
            }
            try {
                GameMenuQuickBean shortcut = gson.fromJson(
                        (String) storedEntry.getValue(),
                        GameMenuQuickBean.class);
                if (shortcut == null) {
                    continue;
                }
                String shortcutId = shortcut.getId();
                if (TextUtils.isEmpty(shortcutId)) {
                    shortcutId = storedEntry.getKey();
                    shortcut.setId(shortcutId);
                }
                add(destination, new Entry(
                        "shortcut:custom:" + shortcutId,
                        shortcut));
            } catch (RuntimeException error) {
                LimeLog.warning(
                        "Ignoring invalid saved shortcut " +
                                storedEntry.getKey() + ": " +
                                error.getMessage());
            }
        }
    }

    private static void add(
            Map<String, Entry> destination, Entry entry) {
        if (!TextUtils.isEmpty(entry.shortcut.getName())) {
            destination.put(entry.id, entry);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte valueByte : digest) {
                int unsignedByte = valueByte & 0xff;
                if (unsignedByte < 0x10) {
                    result.append('0');
                }
                result.append(Integer.toHexString(unsignedByte));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-256 is unavailable", error);
        }
    }
}
