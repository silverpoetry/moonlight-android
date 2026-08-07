package com.limelight.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Versioned, typed serialization for portable application settings.
 *
 * <p>The document contains only canonical schema keys supplied by the caller.
 * Unknown keys are ignored during import so a newer application can export a
 * document that an older application still partially understands. Known keys
 * must retain their declared storage type, and all values pass through the
 * canonical {@link SettingKey} normalizer before they reach persistence.</p>
 */
public final class SettingsBackupCodec {
    public static final String FORMAT =
            "com.silverpoetry.moonlight.app-settings";
    public static final int FORMAT_VERSION = 1;

    private static final int MAXIMUM_ENTRY_COUNT = 512;
    private static final int MAXIMUM_STRING_SET_ENTRIES = 4096;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private SettingsBackupCodec() {
    }

    public static void write(
            SettingsRepository repository,
            Collection<? extends SettingKey<?>> keys,
            OutputStream output)
            throws IOException {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(output, "output");

        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", FORMAT_VERSION);
        JsonArray entries = new JsonArray();
        HashSet<String> names = new HashSet<>();
        for (SettingKey<?> key : keys) {
            SettingKey<?> checkedKey = Objects.requireNonNull(key, "key");
            if (!names.add(checkedKey.getName())) {
                throw new IllegalArgumentException(
                        "Duplicate settings backup key: " +
                                checkedKey.getName());
            }
            entries.add(encodeEntry(repository, checkedKey));
        }
        root.add("settings", entries);

        OutputStreamWriter writer = new OutputStreamWriter(
                output,
                StandardCharsets.UTF_8);
        GSON.toJson(root, writer);
        writer.flush();
    }

    public static Snapshot read(
            Collection<? extends SettingKey<?>> keys,
            InputStream input)
            throws IOException {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(input, "input");

        LinkedHashMap<String, SettingKey<?>> catalog =
                new LinkedHashMap<>();
        for (SettingKey<?> key : keys) {
            SettingKey<?> checkedKey = Objects.requireNonNull(key, "key");
            if (catalog.put(checkedKey.getName(), checkedKey) != null) {
                throw new IllegalArgumentException(
                        "Duplicate settings backup key: " +
                                checkedKey.getName());
            }
        }

        JsonObject root;
        try {
            JsonElement document = JsonParser.parseReader(
                    new InputStreamReader(
                            input,
                            StandardCharsets.UTF_8));
            if (!document.isJsonObject()) {
                throw invalid("Settings document must be an object");
            }
            root = document.getAsJsonObject();
        }
        catch (JsonParseException | IllegalStateException error) {
            throw invalid("Invalid settings document", error);
        }

        if (!FORMAT.equals(requireString(root, "format"))) {
            throw invalid("Unsupported settings document format");
        }
        if (requireInteger(root, "version") != FORMAT_VERSION) {
            throw invalid("Unsupported settings document version");
        }
        JsonArray entries = requireArray(root, "settings");
        if (entries.size() > MAXIMUM_ENTRY_COUNT) {
            throw invalid("Settings document contains too many entries");
        }

        LinkedHashMap<SettingKey<?>, Object> values =
                new LinkedHashMap<>();
        HashSet<String> seenNames = new HashSet<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                throw invalid("Settings entry must be an object");
            }
            JsonObject entry = element.getAsJsonObject();
            String name = requireString(entry, "key");
            if (!seenNames.add(name)) {
                throw invalid("Duplicate settings entry: " + name);
            }
            SettingKey<?> key = catalog.get(name);
            if (key == null) {
                continue;
            }
            String type = requireString(entry, "type");
            if (!storageName(key).equals(type)) {
                throw invalid(
                        "Storage type mismatch for setting: " + name);
            }
            if (!entry.has("value")) {
                throw invalid("Missing value for setting: " + name);
            }
            values.put(
                    key,
                    decodeValue(key, entry.get("value")));
        }
        return new Snapshot(values);
    }

    private static <T> JsonObject encodeEntry(
            SettingsRepository repository,
            SettingKey<T> key) {
        JsonObject entry = new JsonObject();
        entry.addProperty("key", key.getName());
        entry.addProperty("type", storageName(key));
        entry.add("value", encodeValue(
                key,
                repository.get(key)));
        return entry;
    }

    private static JsonElement encodeValue(
            SettingKey<?> key,
            Object value) {
        switch (key.getStorageType()) {
            case BOOLEAN:
                return GSON.toJsonTree((Boolean) value);
            case INTEGER:
                return GSON.toJsonTree((Integer) value);
            case LONG:
                return GSON.toJsonTree((Long) value);
            case FLOAT:
                return GSON.toJsonTree((Float) value);
            case STRING:
                return GSON.toJsonTree((String) value);
            case STRING_SET:
                JsonArray values = new JsonArray();
                for (String entry : sortedStrings(value)) {
                    values.add(entry);
                }
                return values;
            default:
                throw new AssertionError(
                        "Unhandled settings storage type: " +
                                key.getStorageType());
        }
    }

    private static Object decodeValue(
            SettingKey<?> key,
            JsonElement value)
            throws IOException {
        try {
            switch (key.getStorageType()) {
                case BOOLEAN:
                    if (!value.isJsonPrimitive() ||
                            !value.getAsJsonPrimitive().isBoolean()) {
                        throw invalid("Expected a boolean value");
                    }
                    return key.normalizeStoredValue(value.getAsBoolean());
                case INTEGER:
                    long integer = requireIntegralNumber(value);
                    if (integer < Integer.MIN_VALUE ||
                            integer > Integer.MAX_VALUE) {
                        throw invalid("Integer setting is out of range");
                    }
                    return key.normalizeStoredValue((int) integer);
                case LONG:
                    return key.normalizeStoredValue(
                            requireIntegralNumber(value));
                case FLOAT:
                    if (!value.isJsonPrimitive() ||
                            !value.getAsJsonPrimitive().isNumber()) {
                        throw invalid("Expected a floating-point value");
                    }
                    float floating = value.getAsFloat();
                    if (!Float.isFinite(floating)) {
                        throw invalid("Floating-point setting is not finite");
                    }
                    return key.normalizeStoredValue(floating);
                case STRING:
                    if (!value.isJsonPrimitive() ||
                            !value.getAsJsonPrimitive().isString()) {
                        throw invalid("Expected a string value");
                    }
                    return key.normalizeStoredValue(value.getAsString());
                case STRING_SET:
                    if (!value.isJsonArray()) {
                        throw invalid("Expected a string-array value");
                    }
                    JsonArray array = value.getAsJsonArray();
                    if (array.size() > MAXIMUM_STRING_SET_ENTRIES) {
                        throw invalid("String set contains too many values");
                    }
                    LinkedHashSet<String> strings = new LinkedHashSet<>();
                    for (JsonElement element : array) {
                        if (!element.isJsonPrimitive() ||
                                !element.getAsJsonPrimitive().isString()) {
                            throw invalid(
                                    "String set contains a non-string value");
                        }
                        strings.add(element.getAsString());
                    }
                    return key.normalizeStoredValue(strings);
                default:
                    throw new AssertionError(
                            "Unhandled settings storage type: " +
                                    key.getStorageType());
            }
        }
        catch (NumberFormatException | ArithmeticException error) {
            throw invalid(
                    "Invalid value for setting: " + key.getName(),
                    error);
        }
    }

    private static long requireIntegralNumber(JsonElement value)
            throws IOException {
        if (!value.isJsonPrimitive() ||
                !value.getAsJsonPrimitive().isNumber()) {
            throw invalid("Expected an integer value");
        }
        String text = value.getAsString();
        if (text.indexOf('.') >= 0 ||
                text.indexOf('e') >= 0 ||
                text.indexOf('E') >= 0) {
            throw invalid("Expected an integer value");
        }
        try {
            return Long.parseLong(text);
        }
        catch (NumberFormatException error) {
            throw invalid("Integer value is out of range", error);
        }
    }

    private static Set<String> sortedStrings(Object value) {
        if (!(value instanceof Set<?>)) {
            throw new IllegalArgumentException(
                    "Expected a string set");
        }
        java.util.TreeSet<String> sorted = new java.util.TreeSet<>();
        for (Object entry : (Set<?>) value) {
            if (!(entry instanceof String)) {
                throw new IllegalArgumentException(
                        "String set contains a non-string value");
            }
            sorted.add((String) entry);
        }
        return sorted;
    }

    private static String storageName(SettingKey<?> key) {
        return key.getStorageType().name().toLowerCase(
                java.util.Locale.ROOT);
    }

    private static String requireString(JsonObject object, String name)
            throws IOException {
        JsonElement value = object.get(name);
        if (value == null ||
                !value.isJsonPrimitive() ||
                !value.getAsJsonPrimitive().isString()) {
            throw invalid("Missing or invalid string: " + name);
        }
        return value.getAsString();
    }

    private static int requireInteger(JsonObject object, String name)
            throws IOException {
        JsonElement value = object.get(name);
        long integer = requireIntegralNumber(value == null
                ? com.google.gson.JsonNull.INSTANCE
                : value);
        if (integer < Integer.MIN_VALUE || integer > Integer.MAX_VALUE) {
            throw invalid("Integer is out of range: " + name);
        }
        return (int) integer;
    }

    private static JsonArray requireArray(JsonObject object, String name)
            throws IOException {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonArray()) {
            throw invalid("Missing or invalid array: " + name);
        }
        return value.getAsJsonArray();
    }

    private static IOException invalid(String message) {
        return new IOException(message);
    }

    private static IOException invalid(String message, Throwable cause) {
        return new IOException(message, cause);
    }

    /** Validated settings values ready for one atomic repository commit. */
    public static final class Snapshot {
        private final Map<SettingKey<?>, Object> values;

        private Snapshot(Map<SettingKey<?>, Object> values) {
            this.values = Collections.unmodifiableMap(
                    new LinkedHashMap<>(values));
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        public boolean contains(SettingKey<?> key) {
            return values.containsKey(
                    Objects.requireNonNull(key, "key"));
        }

        public <T> T get(SettingKey<T> key) {
            Objects.requireNonNull(key, "key");
            Object value = values.get(key);
            if (value == null && !values.containsKey(key)) {
                return null;
            }
            return key.normalizeStoredValue(value);
        }

        public boolean applyTo(SettingsRepository repository) {
            Objects.requireNonNull(repository, "repository");
            SettingsRepository.Editor editor = repository.edit();
            for (Map.Entry<SettingKey<?>, Object> entry :
                    values.entrySet()) {
                put(editor, entry.getKey(), entry.getValue());
            }
            return editor.commit();
        }

        private static <T> void put(
                SettingsRepository.Editor editor,
                SettingKey<T> key,
                Object value) {
            editor.put(key, key.normalizeStoredValue(value));
        }
    }
}
