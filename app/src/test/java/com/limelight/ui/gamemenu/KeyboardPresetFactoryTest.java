package com.limelight.ui.gamemenu;

import org.junit.Test;

import com.limelight.virtualcontrols.action.VirtualControlAction;

import static org.junit.Assert.assertArrayEquals;

public class KeyboardPresetFactoryTest {
    @Test
    public void exposesEveryTypedLocalActionExactlyOnce() {
        assertArrayEquals(
                VirtualControlAction.values(),
                KeyboardPresetFactory.functionActions());
    }
}
