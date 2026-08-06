package com.limelight.binding.input.virtual_controller.keyboard;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class VirtualControlDefaultLayoutFactoryTest {
    @Test
    public void createsUsableLandscapeGamepadLayout() {
        List<GameMenuQuickBean> controls =
                VirtualControlDefaultLayoutFactory.create(
                        VirtualControlLayoutKey.gamepad(
                                "gamePad",
                                VirtualControlLayoutOrientation.LANDSCAPE),
                        2400,
                        1080,
                        100,
                        100);

        assertFalse(controls.isEmpty());
        assertEquals(10, controls.size());
        assertTrue(containsCode(
                controls,
                ControllerPacket.PADDLE2_FLAG));
        assertTrue(containsCode(controls, ControllerPacket.PADDLE5_FLAG));
        assertTrue(containsType(controls, 5));
        assertAllPositioned(controls, 2400, 1080);
        assertEquals(54, controls.get(4).getmLeft());
        assertEquals(2400 - 54 - 108,
                controls.get(7).getmLeft());
    }

    @Test
    public void createsUsablePortraitGamepadLayout() {
        List<GameMenuQuickBean> controls =
                VirtualControlDefaultLayoutFactory.create(
                        VirtualControlLayoutKey.gamepad(
                                "gamePad",
                                VirtualControlLayoutOrientation.PORTRAIT),
                        1080,
                        2400,
                        100,
                        100);

        assertEquals(10, controls.size());
        assertTrue(containsCode(
                controls,
                ControllerPacket.PADDLE2_FLAG));
        assertTrue(containsCode(controls, ControllerPacket.PADDLE6_FLAG));
        assertAllPositioned(controls, 1080, 2400);
    }

    @Test
    public void createsUsablePortraitKeyboardLayout() {
        List<GameMenuQuickBean> controls =
                VirtualControlDefaultLayoutFactory.create(
                        VirtualControlLayoutKey.keyboard(
                                "OSC_Keyboard",
                                VirtualControlLayoutOrientation.PORTRAIT),
                        1080,
                        2400,
                        100,
                        100);

        assertFalse(controls.isEmpty());
        assertTrue(containsType(controls, 1));
        assertTrue(containsType(controls, 2));
        assertTrue(containsType(controls, 5));
        assertAllPositioned(controls, 1080, 2400);
    }

    @Test
    public void createsUsableLandscapeKeyboardLayout() {
        List<GameMenuQuickBean> controls =
                VirtualControlDefaultLayoutFactory.create(
                        VirtualControlLayoutKey.keyboard(
                                "OSC_Keyboard",
                                VirtualControlLayoutOrientation.LANDSCAPE),
                        2400,
                        1080,
                        100,
                        100);

        assertFalse(controls.isEmpty());
        assertTrue(containsType(controls, 1));
        assertTrue(containsType(controls, 2));
        assertTrue(containsType(controls, 5));
        assertAllPositioned(controls, 2400, 1080);
        assertKeyboardButtonsAreRectangular(controls);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGamepadViewportWithWrongOrientation() {
        VirtualControlDefaultLayoutFactory.create(
                VirtualControlLayoutKey.gamepad(
                        "gamePad",
                        VirtualControlLayoutOrientation.LANDSCAPE),
                1080,
                2400,
                100,
                100);
    }

    @Test
    public void doesNotPopulateCustomProfiles() {
        assertEquals(
                0,
                VirtualControlDefaultLayoutFactory.create(
                        VirtualControlLayoutKey.gamepad(
                                "gamePad_2",
                                VirtualControlLayoutOrientation.LANDSCAPE),
                        2400,
                        1080,
                        100,
                        100).size());
    }

    private static boolean containsCode(
            List<GameMenuQuickBean> controls,
            int code) {
        for (GameMenuQuickBean control : controls) {
            if (control.getCode() == code) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsType(
            List<GameMenuQuickBean> controls,
            int type) {
        for (GameMenuQuickBean control : controls) {
            if (control.getBtnType() == type) {
                return true;
            }
        }
        return false;
    }

    private static void assertAllPositioned(
            List<GameMenuQuickBean> controls,
            int width,
            int height) {
        for (GameMenuQuickBean control : controls) {
            assertNotNull(control.getId());
            assertTrue(control.getmLeft() >= 0);
            assertTrue(control.getmTop() >= 0);
            assertTrue(control.getWidth() > 0);
            assertTrue(control.getHeight() > 0);
            assertTrue(control.getmLeft() < width);
            assertTrue(control.getmTop() < height);
            assertTrue(control.getmLeft() + control.getWidth() <= width);
            assertTrue(control.getmTop() + control.getHeight() <= height);
        }
    }

    private static void assertKeyboardButtonsAreRectangular(
            List<GameMenuQuickBean> controls) {
        for (GameMenuQuickBean control : controls) {
            if (control.getBtnType() == 1 || control.getBtnType() == 4) {
                assertEquals(1, control.getShapeType());
            }
        }
    }
}
