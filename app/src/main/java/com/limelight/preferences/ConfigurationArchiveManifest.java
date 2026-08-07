package com.limelight.preferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Versioned manifest and integrity metadata for a Moonlight configuration ZIP. */
final class ConfigurationArchiveManifest {
    static final String ENTRY_NAME = "manifest.json";
    static final String FORMAT =
            "com.silverpoetry.moonlight.configuration";
    static final int FORMAT_VERSION = 1;

    private static final int MAXIMUM_COMPONENT_COUNT = 16;
    private static final int SHA_256_HEX_LENGTH = 64;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final long createdAtEpochMillis;
    private final String applicationId;
    private final String applicationVersion;
    private final Map<ConfigurationArchiveComponent, Map<String, Entry>>
            components;

    ConfigurationArchiveManifest(
            long createdAtEpochMillis,
            String applicationId,
            String applicationVersion,
            Map<ConfigurationArchiveComponent, Map<String, Entry>>
                    components) {
        this.createdAtEpochMillis = createdAtEpochMillis;
        this.applicationId = requireText(
                applicationId,
                "applicationId");
        this.applicationVersion = requireText(
                applicationVersion,
                "applicationVersion");
        this.components = immutableComponents(components);
        if (this.components.isEmpty()) {
            throw new IllegalArgumentException(
                    "Configuration archive cannot be empty");
        }
    }

    Set<ConfigurationArchiveComponent> getComponents() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(components.keySet()));
    }

    Entry requireEntry(
            ConfigurationArchiveComponent component,
            String name) {
        Map<String, Entry> entries = components.get(component);
        Entry entry = entries == null ? null : entries.get(name);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Missing archive entry metadata: " + name);
        }
        return entry;
    }

    Set<String> getAllEntryNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Map<String, Entry> entries : components.values()) {
            names.addAll(entries.keySet());
        }
        return Collections.unmodifiableSet(names);
    }

    void write(Writer writer) throws IOException {
        Objects.requireNonNull(writer, "writer");
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", FORMAT_VERSION);
        root.addProperty("createdAtEpochMillis", createdAtEpochMillis);
        root.addProperty("applicationId", applicationId);
        root.addProperty("applicationVersion", applicationVersion);
        JsonArray componentArray = new JsonArray();
        for (Map.Entry<ConfigurationArchiveComponent, Map<String, Entry>>
                componentEntry : components.entrySet()) {
            JsonObject component = new JsonObject();
            component.addProperty(
                    "id",
                    componentEntry.getKey().getId());
            JsonArray files = new JsonArray();
            for (Entry entry : componentEntry.getValue().values()) {
                JsonObject file = new JsonObject();
                file.addProperty("path", entry.getPath());
                file.addProperty("size", entry.getSize());
                file.addProperty("sha256", entry.getSha256());
                files.add(file);
            }
            component.add("files", files);
            componentArray.add(component);
        }
        root.add("components", componentArray);
        GSON.toJson(root, writer);
        writer.flush();
    }

    static ConfigurationArchiveManifest read(Reader reader)
            throws IOException {
        Objects.requireNonNull(reader, "reader");
        JsonObject root;
        try {
            JsonElement document = JsonParser.parseReader(reader);
            if (!document.isJsonObject()) {
                throw invalid("Archive manifest must be an object");
            }
            root = document.getAsJsonObject();
        }
        catch (JsonParseException | IllegalStateException error) {
            throw invalid("Invalid archive manifest", error);
        }

        if (!FORMAT.equals(requireString(root, "format"))) {
            throw invalid("Unsupported configuration archive format");
        }
        if (requireInteger(root, "version") != FORMAT_VERSION) {
            throw invalid("Unsupported configuration archive version");
        }
        long createdAt = requireLong(root, "createdAtEpochMillis");
        String applicationId = requireString(root, "applicationId");
        String applicationVersion = requireString(
                root,
                "applicationVersion");
        JsonArray componentArray = requireArray(root, "components");
        if (componentArray.size() == 0 ||
                componentArray.size() > MAXIMUM_COMPONENT_COUNT) {
            throw invalid("Invalid configuration component count");
        }

        EnumMap<ConfigurationArchiveComponent, Map<String, Entry>>
                components = new EnumMap<>(
                        ConfigurationArchiveComponent.class);
        HashSet<String> allPaths = new HashSet<>();
        HashSet<String> allIds = new HashSet<>();
        for (JsonElement element : componentArray) {
            if (!element.isJsonObject()) {
                throw invalid("Archive component must be an object");
            }
            JsonObject componentObject = element.getAsJsonObject();
            String id = requireString(componentObject, "id");
            if (!allIds.add(id)) {
                throw invalid("Duplicate configuration component: " + id);
            }
            ConfigurationArchiveComponent component =
                    ConfigurationArchiveComponent.fromId(id);
            if (component == null) {
                throw invalid(
                        "Unsupported configuration component: " + id);
            }
            JsonArray files = requireArray(componentObject, "files");
            LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
            for (JsonElement fileElement : files) {
                if (!fileElement.isJsonObject()) {
                    throw invalid("Archive file must be an object");
                }
                JsonObject file = fileElement.getAsJsonObject();
                String path = requireString(file, "path");
                long size = requireLong(file, "size");
                String sha256 = requireString(file, "sha256");
                Entry entry;
                try {
                    entry = new Entry(path, size, sha256);
                }
                catch (IllegalArgumentException error) {
                    throw invalid("Invalid archive file metadata", error);
                }
                if (entries.put(path, entry) != null ||
                        !allPaths.add(path)) {
                    throw invalid("Duplicate archive path: " + path);
                }
            }
            Set<String> expected = new LinkedHashSet<>(
                    component.getEntryNames());
            if (!expected.equals(entries.keySet())) {
                throw invalid(
                        "Configuration component has unexpected files: " +
                                id);
            }
            components.put(component, entries);
        }

        try {
            return new ConfigurationArchiveManifest(
                    createdAt,
                    applicationId,
                    applicationVersion,
                    components);
        }
        catch (IllegalArgumentException error) {
            throw invalid("Invalid archive manifest", error);
        }
    }

    private static Map<ConfigurationArchiveComponent, Map<String, Entry>>
            immutableComponents(
                    Map<ConfigurationArchiveComponent, Map<String, Entry>>
                            source) {
        Objects.requireNonNull(source, "components");
        EnumMap<ConfigurationArchiveComponent, Map<String, Entry>> copy =
                new EnumMap<>(ConfigurationArchiveComponent.class);
        HashSet<String> allPaths = new HashSet<>();
        for (Map.Entry<ConfigurationArchiveComponent, Map<String, Entry>>
                componentEntry : source.entrySet()) {
            ConfigurationArchiveComponent component = Objects.requireNonNull(
                    componentEntry.getKey(),
                    "component");
            Map<String, Entry> sourceEntries = Objects.requireNonNull(
                    componentEntry.getValue(),
                    "component entries");
            LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
            for (String name : component.getEntryNames()) {
                Entry entry = sourceEntries.get(name);
                if (entry == null) {
                    throw new IllegalArgumentException(
                            "Missing archive entry: " + name);
                }
                if (!name.equals(entry.getPath()) ||
                        entries.put(name, entry) != null ||
                        !allPaths.add(name)) {
                    throw new IllegalArgumentException(
                            "Invalid or duplicate archive entry: " + name);
                }
            }
            if (entries.size() != sourceEntries.size()) {
                throw new IllegalArgumentException(
                        "Unexpected archive entries for " +
                                component.getId());
            }
            copy.put(
                    component,
                    Collections.unmodifiableMap(entries));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return value;
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
        long value = requireLong(object, name);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw invalid("Integer is out of range: " + name);
        }
        return (int) value;
    }

    private static long requireLong(JsonObject object, String name)
            throws IOException {
        JsonElement value = object.get(name);
        if (value == null ||
                !value.isJsonPrimitive() ||
                !value.getAsJsonPrimitive().isNumber()) {
            throw invalid("Missing or invalid integer: " + name);
        }
        String text = value.getAsString();
        if (text.indexOf('.') >= 0 ||
                text.indexOf('e') >= 0 ||
                text.indexOf('E') >= 0) {
            throw invalid("Expected an integer: " + name);
        }
        try {
            return Long.parseLong(text);
        }
        catch (NumberFormatException error) {
            throw invalid("Integer is out of range: " + name, error);
        }
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

    static final class Entry {
        private final String path;
        private final long size;
        private final String sha256;

        Entry(String path, long size, String sha256) {
            if (path == null ||
                    path.isEmpty() ||
                    path.contains("/") ||
                    path.contains("\\") ||
                    path.equals(".") ||
                    path.equals("..")) {
                throw new IllegalArgumentException(
                        "Archive entry must be a root-level file");
            }
            if (size < 0) {
                throw new IllegalArgumentException(
                        "Archive entry size cannot be negative");
            }
            if (sha256 == null ||
                    sha256.length() != SHA_256_HEX_LENGTH ||
                    !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "Archive entry SHA-256 is invalid");
            }
            this.path = path;
            this.size = size;
            this.sha256 = sha256;
        }

        String getPath() {
            return path;
        }

        long getSize() {
            return size;
        }

        String getSha256() {
            return sha256;
        }
    }
}
