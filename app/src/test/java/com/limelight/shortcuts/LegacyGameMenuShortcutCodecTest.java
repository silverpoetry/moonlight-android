package com.limelight.shortcuts;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class LegacyGameMenuShortcutCodecTest {
    private final LegacyGameMenuShortcutCodec codec =
            new LegacyGameMenuShortcutCodec();

    @Test
    public void importedDecoderKeepsValidRowsAndStableIds() {
        String json =
                "{\"data\":[" +
                        "{\"name\":\"Copy\",\"data\":[\"0x11\",\"0x43\"]}," +
                        "{\"name\":\"Broken\",\"data\":[\"43\"]}]}";

        GameMenuShortcutDecodeResult decoded =
                codec.decodeImported(json);

        assertEquals(1, decoded.getShortcuts().size());
        assertEquals(1, decoded.getRejectedEntryCount());
        GameMenuShortcut shortcut =
                decoded.getShortcuts().get(0);
        assertEquals(
                GameMenuShortcutIds.imported(
                        "Copy",
                        new short[] {0x11, 0x43}),
                shortcut.getId());
        assertArrayEquals(
                new short[] {0x11, 0x43},
                shortcut.getMoonlightKeyCodes());
        assertFalse(shortcut.isEditable());
    }

    @Test
    public void customDecoderReadsExistingBeanJson() {
        GameMenuShortcut shortcut = codec.decodeCustom(
                "quick_assemble_key_1",
                "{\"id\":\"quick_assemble_key_1\"," +
                        "\"name\":\"Paste\"," +
                        "\"codes\":\"113,50\"," +
                        "\"desc\":\"Ctrl + V\"," +
                        "\"unknown\":true}");

        assertEquals(
                "shortcut:custom:quick_assemble_key_1",
                shortcut.getId());
        assertArrayEquals(
                new int[] {113, 50},
                shortcut.getAndroidKeyCodes());
        assertTrue(shortcut.isEditable());
    }
}
