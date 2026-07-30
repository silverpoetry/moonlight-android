package com.limelight.ui.gamemenu;

import com.google.gson.Gson;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GameMenuQuickBeanJsonCompatibilityTest {
    private final Gson gson = new Gson();

    @Test
    public void readsLegacyShortcutAndPreservesFieldDefaults() {
        GameMenuQuickBean shortcut = gson.fromJson(
                "{\"name\":\"Ctrl+C\",\"datas\":[29,31],\"btnType\":4," +
                        "\"unknownFutureField\":true}",
                GameMenuQuickBean.class);

        assertEquals("Ctrl+C", shortcut.getName());
        assertArrayEquals(new short[] {29, 31}, shortcut.getDatas());
        assertEquals(4, shortcut.getBtnType());
        assertEquals(100, shortcut.getZoom());
        assertEquals(100, shortcut.getZoomW());
        assertEquals(100, shortcut.getZoomH());
        assertTrue(shortcut.isFreeeStickDrawNormal());
        assertFalse(shortcut.isSwitchMode());
    }

    @Test
    public void roundTripRetainsPersistedShortcutFields() {
        GameMenuQuickBean original =
                new GameMenuQuickBean("Mouse", "1,2,3", "description", 1, true);
        original.setId("shortcut-id");
        original.setWidth(320);
        original.setHeight(180);
        original.setmLeft(12);
        original.setmTop(34);
        original.setZoom(90);
        original.setZoomW(80);
        original.setZoomH(70);
        original.setShapeType(1)
                .setFreeStick(true)
                .setGamePad(true)
                .setFixedStrokeFreeStick(true)
                .setFreeeStickDrawNormal(false);

        GameMenuQuickBean restored =
                gson.fromJson(gson.toJson(original), GameMenuQuickBean.class);

        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getCodes(), restored.getCodes());
        assertEquals(original.getDesc(), restored.getDesc());
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getBtnType(), restored.getBtnType());
        assertEquals(original.getWidth(), restored.getWidth());
        assertEquals(original.getHeight(), restored.getHeight());
        assertEquals(original.getmLeft(), restored.getmLeft());
        assertEquals(original.getmTop(), restored.getmTop());
        assertEquals(original.getZoom(), restored.getZoom());
        assertEquals(original.getZoomW(), restored.getZoomW());
        assertEquals(original.getZoomH(), restored.getZoomH());
        assertEquals(original.getShapeType(), restored.getShapeType());
        assertEquals(original.isSwitchMode(), restored.isSwitchMode());
        assertEquals(original.isFreeStick(), restored.isFreeStick());
        assertEquals(original.isGamePad(), restored.isGamePad());
        assertEquals(original.isFixedStrokeFreeStick(), restored.isFixedStrokeFreeStick());
        assertEquals(original.isFreeeStickDrawNormal(), restored.isFreeeStickDrawNormal());
    }
}
