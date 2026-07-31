package com.limelight.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SettingKeyTest {
    @Test
    public void invalidStoredTypeUsesCanonicalDefault() {
        SettingKey<Integer> key =
                SettingKey.integerKey("count", 7, 0, 10);

        assertEquals(
                Integer.valueOf(7),
                key.normalizeStoredValue("7"));
    }

    @Test
    public void numericValuesAreClampedBySchema() {
        SettingKey<Integer> key =
                SettingKey.integerKey("count", 7, 0, 10);

        assertEquals(
                Integer.valueOf(0),
                key.normalizeStoredValue(-5));
        assertEquals(
                Integer.valueOf(10),
                key.normalizeStoredValue(50));
    }

    @Test
    public void invalidFloatUsesCanonicalDefault() {
        SettingKey<Float> key =
                SettingKey.floatKey("factor", 0.5f, 0f, 1f);

        assertEquals(
                0.5f,
                key.normalizeStoredValue(Float.NaN),
                0f);
    }

    @Test
    public void discreteIntegerRejectsValuesBetweenOptions() {
        SettingKey<Integer> key =
                SettingKey.integerSetKey(
                        "fingers",
                        0,
                        0,
                        3,
                        4,
                        5);

        assertEquals(
                Integer.valueOf(0),
                key.normalizeStoredValue(2));
        assertEquals(
                Integer.valueOf(4),
                key.normalizeStoredValue(4));
    }

    @Test
    public void discreteStringRejectsUnknownOption() {
        SettingKey<String> key =
                SettingKey.stringSetKey(
                        "mode",
                        "auto",
                        "auto",
                        "on",
                        "off");

        assertEquals("auto", key.normalizeStoredValue("future"));
        assertEquals("on", key.normalizeStoredValue("on"));
    }

    @Test
    public void boundedStringRejectsOversizedValue() {
        SettingKey<String> key =
                SettingKey.boundedStringKey(
                        "uri",
                        "",
                        4);

        assertEquals("abcd", key.normalizeStoredValue("abcd"));
        assertEquals("", key.normalizeStoredValue("abcde"));
    }
}
