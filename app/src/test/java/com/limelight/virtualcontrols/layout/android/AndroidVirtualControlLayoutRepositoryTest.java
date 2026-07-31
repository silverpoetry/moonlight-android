package com.limelight.virtualcontrols.layout.android;

import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class AndroidVirtualControlLayoutRepositoryTest {
    @Test
    public void retainsLegacyLandscapeFileMapping() {
        assertEquals(
                "axi_OSC_Keyboard_2.txt",
                AndroidVirtualControlLayoutRepository.legacyFileName(
                        VirtualControlLayoutKey.keyboard(
                                "OSC_Keyboard_2",
                                VirtualControlLayoutOrientation.LANDSCAPE)));
    }

    @Test
    public void retainsLegacyPortraitFileMapping() {
        assertEquals(
                "axi_gamePad_4_1.txt",
                AndroidVirtualControlLayoutRepository.legacyFileName(
                        VirtualControlLayoutKey.gamepad(
                                "gamePad_4",
                        VirtualControlLayoutOrientation.PORTRAIT)));
    }

    @Test
    public void acceptsArrayLayoutDocument() throws Exception {
        assertEquals(
                "[{\"name\":\"A\"}]",
                AndroidVirtualControlLayoutRepository
                        .parseImportedDocument(
                                "[{\"name\":\"A\"}]")
                        .getJson());
    }

    @Test
    public void rejectsMalformedOrWrongRootImport() {
        assertThrows(
                java.io.IOException.class,
                () -> AndroidVirtualControlLayoutRepository
                        .parseImportedDocument("{\"name\":\"A\"}"));
        assertThrows(
                java.io.IOException.class,
                () -> AndroidVirtualControlLayoutRepository
                        .parseImportedDocument("["));
    }
}
