package com.limelight.virtualcontrols.layout;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public final class VirtualControlLayoutDocumentTest {
    @Test
    public void preservesJsonExactly() {
        String json = "[\n  {\"name\":\"按键\"}\n]";

        assertEquals(
                json,
                VirtualControlLayoutDocument.fromJson(json)
                        .getJson());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOversizedUtf8Document() {
        char[] content =
                new char[
                        VirtualControlLayoutDocument.MAX_UTF8_BYTES +
                                1];
        Arrays.fill(content, 'x');

        VirtualControlLayoutDocument.fromJson(
                new String(content));
    }
}
