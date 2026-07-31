package com.limelight.ui.gamemenu;

import com.limelight.shortcuts.GameMenuShortcut;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class GameMenuShortcutCatalogTest {
    @Test
    public void persistedSnapshotFollowsBuiltInsDeterministically() {
        GameMenuShortcut custom =
                GameMenuShortcut.androidChord(
                        "shortcut:custom:copy",
                        "Copy",
                        "",
                        new int[] {1},
                        true);

        List<GameMenuShortcutCatalog.Entry> entries =
                GameMenuShortcutCatalog.load(
                        Collections.singletonList(custom),
                        true);

        assertTrue(entries.size() > 1);
        assertEquals(
                "shortcut:builtin:escape",
                entries.get(0).id);
        assertEquals(
                custom.getId(),
                entries.get(entries.size() - 1).id);
    }

    @Test
    public void laterPersistedDuplicateReplacesPayloadWithoutReordering() {
        GameMenuShortcut first =
                GameMenuShortcut.androidChord(
                        "shortcut:custom:same",
                        "First",
                        "",
                        new int[] {1},
                        true);
        GameMenuShortcut replacement =
                GameMenuShortcut.androidChord(
                        "shortcut:custom:same",
                        "Replacement",
                        "",
                        new int[] {2},
                        true);

        List<GameMenuShortcutCatalog.Entry> entries =
                GameMenuShortcutCatalog.load(
                        Arrays.asList(first, replacement),
                        false);

        assertEquals(1, entries.size());
        assertEquals(
                "Replacement",
                entries.get(0).shortcut.getName());
    }
}
