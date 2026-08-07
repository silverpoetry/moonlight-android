package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigurationArchiveManifestTest {
    private static final String HASH =
            "0123456789abcdef0123456789abcdef" +
                    "0123456789abcdef0123456789abcdef";

    @Test
    public void roundTripPreservesComponentsAndIntegrityMetadata()
            throws Exception {
        ConfigurationArchiveManifest manifest = newManifest();
        StringWriter writer = new StringWriter();
        manifest.write(writer);

        ConfigurationArchiveManifest restored =
                ConfigurationArchiveManifest.read(
                        new StringReader(writer.toString()));

        assertEquals(2, restored.getComponents().size());
        assertTrue(restored.getComponents().contains(
                ConfigurationArchiveComponent.APP_SETTINGS));
        assertTrue(restored.getComponents().contains(
                ConfigurationArchiveComponent.PAIRING_DATA));
        assertEquals(
                42,
                restored.requireEntry(
                        ConfigurationArchiveComponent.APP_SETTINGS,
                        "app-settings.json")
                        .getSize());
        assertEquals(4, restored.getAllEntryNames().size());
    }

    @Test
    public void missingAtomicPairingFileIsRejected() {
        EnumMap<ConfigurationArchiveComponent,
                Map<String, ConfigurationArchiveManifest.Entry>> entries =
                entries();
        entries.get(ConfigurationArchiveComponent.PAIRING_DATA)
                .remove("client.key");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigurationArchiveManifest(
                        1,
                        "com.silverpoetry.moonlight",
                        "test",
                        entries));
    }

    @Test
    public void pathTraversalEntryIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigurationArchiveManifest.Entry(
                        "../client.key",
                        1,
                        HASH));
    }

    @Test
    public void unsupportedManifestVersionIsRejected() throws Exception {
        StringWriter writer = new StringWriter();
        newManifest().write(writer);
        String future = writer.toString().replace(
                "\"version\": 1",
                "\"version\": 2");

        assertThrows(
                java.io.IOException.class,
                () -> ConfigurationArchiveManifest.read(
                        new StringReader(future)));
    }

    private static ConfigurationArchiveManifest newManifest() {
        return new ConfigurationArchiveManifest(
                1,
                "com.silverpoetry.moonlight",
                "test",
                entries());
    }

    private static EnumMap<ConfigurationArchiveComponent,
            Map<String, ConfigurationArchiveManifest.Entry>> entries() {
        EnumMap<ConfigurationArchiveComponent,
                Map<String, ConfigurationArchiveManifest.Entry>> result =
                new EnumMap<>(ConfigurationArchiveComponent.class);
        LinkedHashMap<String, ConfigurationArchiveManifest.Entry> settings =
                new LinkedHashMap<>();
        settings.put(
                "app-settings.json",
                entry("app-settings.json", 42));
        result.put(ConfigurationArchiveComponent.APP_SETTINGS, settings);

        LinkedHashMap<String, ConfigurationArchiveManifest.Entry> pairing =
                new LinkedHashMap<>();
        pairing.put("paired-hosts.db", entry("paired-hosts.db", 100));
        pairing.put("client.crt", entry("client.crt", 200));
        pairing.put("client.key", entry("client.key", 300));
        result.put(ConfigurationArchiveComponent.PAIRING_DATA, pairing);
        return result;
    }

    private static ConfigurationArchiveManifest.Entry entry(
            String path,
            long size) {
        return new ConfigurationArchiveManifest.Entry(path, size, HASH);
    }
}
