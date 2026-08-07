package com.limelight.ui.gamemenu;

import android.content.Context;

import com.limelight.R;
import com.limelight.settings.ui.GameMenuCardIds;
import com.limelight.shortcuts.GameMenuShortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Unifies built-in game menu actions and user-selectable keyboard shortcuts.
 */
final class GameMenuCardCatalog {
    static final class Card {
        final String id;
        final String label;
        final String contentDescription;
        final int iconRes;
        final boolean defaultVisible;
        final GameMenuActionCatalog.Action action;
        final GameMenuShortcutCatalog.Entry shortcut;

        Card(
                String id,
                String label,
                String contentDescription,
                int iconRes,
                boolean defaultVisible,
                GameMenuActionCatalog.Action action,
                GameMenuShortcutCatalog.Entry shortcut) {
            this.id = id;
            this.label = label;
            this.contentDescription = contentDescription;
            this.iconRes = iconRes;
            this.defaultVisible = defaultVisible;
            this.action = action;
            this.shortcut = shortcut;
        }

        boolean requiresGamepad() {
            return action != null && action.requiresGamepad;
        }

        boolean dismissesMenuBeforeExecution() {
            return shortcut != null ||
                    (action != null &&
                            action.dismissesMenuBeforeExecution());
        }
    }

    private GameMenuCardCatalog() {
    }

    static List<Card> load(
            Context context,
            List<GameMenuShortcut> persistedShortcuts,
            boolean includeBuiltInShortcuts) {
        List<Card> cards = new ArrayList<>();
        for (GameMenuActionCatalog.Action action :
                GameMenuActionCatalog.all()) {
            cards.add(new Card(
                    actionCardId(action.id),
                    context.getString(action.labelRes),
                    context.getString(action.contentDescriptionRes),
                    action.iconRes,
                    true,
                    action,
                    null));
        }
        for (GameMenuShortcutCatalog.Entry shortcut :
                GameMenuShortcutCatalog.load(
                        persistedShortcuts,
                        includeBuiltInShortcuts,
                        context)) {
            cards.add(new Card(
                    shortcut.id,
                    compactShortcutLabel(
                            shortcut.shortcut.getName()),
                    shortcut.shortcut.getName(),
                    R.drawable.ic_m3_keyboard,
                    false,
                    null,
                    shortcut));
        }
        return cards;
    }

    static String actionCardId(String actionId) {
        return GameMenuCardIds.action(actionId);
    }

    private static String compactShortcutLabel(String name) {
        int descriptionStart = name.indexOf(" (");
        if (descriptionStart < 0) {
            descriptionStart = name.indexOf("（");
        }
        return descriptionStart > 0 ?
                name.substring(0, descriptionStart) : name;
    }
}
