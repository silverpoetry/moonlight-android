package com.limelight.virtualcontrols.action;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class VirtualControlActionTest {
    @Test
    public void resolvesEveryStablePersistentId() {
        for (VirtualControlAction action : VirtualControlAction.values()) {
            assertEquals(
                    action,
                    VirtualControlAction.fromPersistentId(
                            action.getPersistentId()));
        }
    }

    @Test
    public void rejectsMissingAndUnknownPersistentIds() {
        assertNull(VirtualControlAction.fromPersistentId(null));
        assertNull(VirtualControlAction.fromPersistentId("unknown_action"));
    }
}
