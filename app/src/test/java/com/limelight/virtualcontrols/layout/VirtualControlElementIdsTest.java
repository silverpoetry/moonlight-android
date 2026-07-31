package com.limelight.virtualcontrols.layout;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class VirtualControlElementIdsTest {
    @Test
    public void generatedIdsRetainPrefixAndDoNotCollide() {
        String first = VirtualControlElementIds.newId();
        String second = VirtualControlElementIds.newId();

        assertTrue(first.startsWith("assemble_key_"));
        assertTrue(second.startsWith("assemble_key_"));
        assertNotEquals(first, second);
    }
}
