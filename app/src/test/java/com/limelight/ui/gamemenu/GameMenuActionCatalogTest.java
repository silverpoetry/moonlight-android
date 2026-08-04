package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public final class GameMenuActionCatalogTest {
    @Test
    public void builtInActionsUseDistinctSemanticIcons() {
        Set<Integer> icons = new HashSet<>();
        for (GameMenuActionCatalog.Action action :
                GameMenuActionCatalog.all()) {
            icons.add(action.iconRes);
        }

        assertEquals(GameMenuActionCatalog.all().size(), icons.size());
    }
}
