package com.limelight.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class SettingsBackupCodecTest {
    private static final SettingKey<Boolean> BOOLEAN =
            SettingKey.booleanKey("test.boolean", false);
    private static final SettingKey<Integer> INTEGER =
            SettingKey.integerKey("test.integer", 4, 0, 10);
    private static final SettingKey<String> STRING =
            SettingKey.boundedStringKey("test.string", "", 20);
    private static final SettingKey<java.util.Set<String>> STRINGS =
            SettingKey.boundedStringCollectionKey(
                    "test.strings",
                    4,
                    20);

    @Test
    public void roundTripPreservesTypedValuesAndAppliesAtomically()
            throws Exception {
        MemoryRepository source = new MemoryRepository();
        source.edit()
                .put(BOOLEAN, true)
                .put(INTEGER, 9)
                .put(STRING, "Moonlight")
                .put(STRINGS, new HashSet<>(
                        Arrays.asList("second", "first")))
                .commit();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SettingsBackupCodec.write(
                source,
                Arrays.asList(BOOLEAN, INTEGER, STRING, STRINGS),
                output);

        MemoryRepository destination = new MemoryRepository();
        SettingsBackupCodec.Snapshot snapshot =
                SettingsBackupCodec.read(
                        Arrays.asList(BOOLEAN, INTEGER, STRING, STRINGS),
                        new ByteArrayInputStream(output.toByteArray()));

        assertTrue(snapshot.applyTo(destination));
        assertTrue(destination.get(BOOLEAN));
        assertEquals(9, (int) destination.get(INTEGER));
        assertEquals("Moonlight", destination.get(STRING));
        assertEquals(
                new HashSet<>(Arrays.asList("first", "second")),
                destination.get(STRINGS));
    }

    @Test
    public void unknownFutureSettingIsIgnored() throws Exception {
        String document = "{\n" +
                "  \"format\": \"" + SettingsBackupCodec.FORMAT + "\",\n" +
                "  \"version\": 1,\n" +
                "  \"settings\": [\n" +
                "    {\"key\":\"future.key\",\"type\":\"string\",\"value\":\"x\"},\n" +
                "    {\"key\":\"test.boolean\",\"type\":\"boolean\",\"value\":true}\n" +
                "  ]\n" +
                "}";

        SettingsBackupCodec.Snapshot snapshot =
                SettingsBackupCodec.read(
                        Collections.singletonList(BOOLEAN),
                        input(document));

        assertTrue(snapshot.contains(BOOLEAN));
        assertTrue(snapshot.get(BOOLEAN));
    }

    @Test
    public void knownTypeMismatchIsRejected() {
        String document = "{\"format\":\"" +
                SettingsBackupCodec.FORMAT +
                "\",\"version\":1,\"settings\":[" +
                "{\"key\":\"test.boolean\",\"type\":\"string\",\"value\":\"true\"}]}";

        assertThrows(
                java.io.IOException.class,
                () -> SettingsBackupCodec.read(
                        Collections.singletonList(BOOLEAN),
                        input(document)));
    }

    @Test
    public void failedCommitDoesNotPublishStagedValues() throws Exception {
        String document = "{\"format\":\"" +
                SettingsBackupCodec.FORMAT +
                "\",\"version\":1,\"settings\":[" +
                "{\"key\":\"test.boolean\",\"type\":\"boolean\",\"value\":true}]}";
        SettingsBackupCodec.Snapshot snapshot =
                SettingsBackupCodec.read(
                        Collections.singletonList(BOOLEAN),
                        input(document));
        MemoryRepository destination = new MemoryRepository();
        destination.failCommits = true;

        assertFalse(snapshot.applyTo(destination));
        assertFalse(destination.get(BOOLEAN));
    }

    private static ByteArrayInputStream input(String document) {
        return new ByteArrayInputStream(
                document.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MemoryRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();
        private boolean failCommits;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(values.getOrDefault(
                    key.getName(),
                    key.getDefaultValue()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending =
                        new HashMap<>(values);

                @Override
                public <T> Editor put(SettingKey<T> key, T value) {
                    pending.put(
                            key.getName(),
                            key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    pending.remove(key.getName());
                    return this;
                }

                @Override
                public void apply() {
                    commit();
                }

                @Override
                public boolean commit() {
                    if (failCommits) {
                        return false;
                    }
                    values.clear();
                    values.putAll(pending);
                    return true;
                }
            };
        }
    }
}
