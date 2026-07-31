package com.limelight.shortcuts;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class GameMenuShortcutDocumentCodecTest {
    private final GameMenuShortcutDocumentCodec codec =
            new GameMenuShortcutDocumentCodec();

    @Test
    public void roundTripPreservesOrderAndRepresentations() {
        GameMenuShortcut moonlight =
                GameMenuShortcut.moonlightChord(
                        "shortcut:imported:one",
                        "Escape",
                        "",
                        new short[] {27},
                        false);
        GameMenuShortcut android =
                GameMenuShortcut.androidChord(
                        "shortcut:custom:two",
                        "Copy",
                        "Ctrl + C",
                        new int[] {113, 31},
                        true);

        GameMenuShortcutDecodeResult decoded =
                codec.decode(codec.encode(
                        Arrays.asList(moonlight, android)));

        assertEquals(0, decoded.getRejectedEntryCount());
        assertEquals(2, decoded.getShortcuts().size());
        assertEquals(
                moonlight.getId(),
                decoded.getShortcuts().get(0).getId());
        assertArrayEquals(
                new int[] {113, 31},
                decoded.getShortcuts().get(1)
                        .getAndroidKeyCodes());
        assertTrue(decoded.getShortcuts().get(1)
                .isEditable());
        assertFalse(decoded.getShortcuts().get(0)
                .isEditable());
    }

    @Test
    public void isolatesMalformedAndDuplicateEntries() {
        String json =
                "{\"version\":1,\"shortcuts\":[" +
                        "{\"id\":\"shortcut:custom:ok\"," +
                        "\"name\":\"OK\",\"androidKeyCodes\":[1]," +
                        "\"editable\":true}," +
                        "{\"id\":\"shortcut:custom:bad\"," +
                        "\"name\":\"Bad\",\"androidKeyCodes\":[]}," +
                        "{\"id\":\"shortcut:custom:ok\"," +
                        "\"name\":\"Duplicate\"," +
                        "\"androidKeyCodes\":[2]}]}";

        GameMenuShortcutDecodeResult decoded =
                codec.decode(json);

        assertEquals(1, decoded.getShortcuts().size());
        assertEquals(2, decoded.getRejectedEntryCount());
        assertEquals(
                "OK",
                decoded.getShortcuts().get(0).getName());
    }

    @Test
    public void rejectsUnsupportedDocumentVersion() {
        try {
            codec.decode("{\"version\":2,\"shortcuts\":[]}");
            fail("Expected unsupported version");
        }
        catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void wrapsWrongJsonTypesAsInvalidDocuments() {
        try {
            codec.decode(
                    "{\"version\":\"wrong\"," +
                            "\"shortcuts\":{}}");
            fail("Expected invalid document");
        }
        catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void refusesToEncodeRuntimeBuiltIns() {
        try {
            codec.encode(Arrays.asList(
                    GameMenuShortcut.moonlightChord(
                            "shortcut:builtin:escape",
                            "Escape",
                            "",
                            new short[] {27},
                            false)));
            fail("Expected non-persisted identity rejection");
        }
        catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
