package com.limelight.ui.gamemenu;

import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.virtualcontrols.action.VirtualControlAction;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class LegacyVirtualControlActionMigrationTest {
    @Test
    public void migratesEveryLegacyLocalAction() {
        List<GameMenuQuickBean> controls = Arrays.asList(
                legacyControl(0, 7),
                legacyControl(1, 8),
                legacyControl(2, 9),
                legacyControl(3, 10),
                legacyControl(4, 11),
                legacyControl(5, 12),
                legacyControl(6, 13));

        assertTrue(LegacyVirtualControlActionMigration.migrate(controls));

        assertEquals(
                Arrays.asList(VirtualControlAction.values()),
                Arrays.asList(
                        controls.get(0).getLocalAction(),
                        controls.get(1).getLocalAction(),
                        controls.get(2).getLocalAction(),
                        controls.get(3).getLocalAction(),
                        controls.get(4).getLocalAction(),
                        controls.get(5).getLocalAction(),
                        controls.get(6).getLocalAction()));
        for (GameMenuQuickBean control : controls) {
            assertNull(control.getCodes());
            assertFalse(control.isSwitchMode());
            assertEquals(control.getName(), control.getDesc());
        }
        assertFalse(LegacyVirtualControlActionMigration.migrate(controls));
    }

    @Test
    public void preservesAnIdenticalUserKeyChordWithoutLegacyDescription() {
        GameMenuQuickBean control = new GameMenuQuickBean(
                "Real chord",
                "29,52,37,52,7",
                "A-X-I-X-0",
                4,
                true);

        assertFalse(LegacyVirtualControlActionMigration.migrate(
                Collections.singletonList(control)));
        assertNull(control.getLocalAction());
        assertEquals("29,52,37,52,7", control.getCodes());
        assertTrue(control.isSwitchMode());
    }

    private static GameMenuQuickBean legacyControl(
            int suffix,
            int finalKeyCode) {
        return new GameMenuQuickBean(
                "Action " + suffix,
                "29,52,37,52," + finalKeyCode,
                "AXIX" + suffix,
                4,
                true);
    }
}
