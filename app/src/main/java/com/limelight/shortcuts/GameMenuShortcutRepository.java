package com.limelight.shortcuts;

import java.util.List;

/**
 * Persistence port for user-visible stream-menu shortcuts.
 */
public interface GameMenuShortcutRepository {
    List<GameMenuShortcut> load();

    boolean save(GameMenuShortcut shortcut);

    boolean delete(String shortcutId);
}
