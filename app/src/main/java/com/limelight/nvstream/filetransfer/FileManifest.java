package com.limelight.nvstream.filetransfer;

import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileManifest {
    public static final byte TYPE_REGULAR = 1;
    public static final byte TYPE_DIRECTORY = 2;
    public static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    public static final int MAX_ENTRIES = 4096;
    public static final int MAX_PATH_BYTES = 1024;
    public static final int MAX_CHUNK_BYTES = 4 * 1024 * 1024;
    public static final long MAX_FILE_BYTES = 64L * 1024L * 1024L * 1024L;
    public static final long MAX_TRANSFER_BYTES = 256L * 1024L * 1024L * 1024L;

    private static final int HEADER_BYTES = 24;
    private static final int ENTRY_HEADER_BYTES = 24;

    public static final class Entry {
        public final byte type;
        public final String path;
        public final long size;
        public final long modifiedTimeMs;
        public final Uri sourceUri;

        public Entry(byte type, String path, long size, long modifiedTimeMs) {
            this(type, path, size, modifiedTimeMs, null);
        }

        public Entry(byte type, String path, long size, long modifiedTimeMs,
                     Uri sourceUri) {
            this.type = type;
            this.path = path;
            this.size = size;
            this.modifiedTimeMs = modifiedTimeMs;
            this.sourceUri = sourceUri;
        }
    }

    public final List<Entry> entries;
    public final int fileCount;
    public final long totalFileBytes;

    private FileManifest(List<Entry> entries, int fileCount, long totalFileBytes) {
        this.entries = entries;
        this.fileCount = fileCount;
        this.totalFileBytes = totalFileBytes;
    }

    public static FileManifest decode(byte[] encoded) throws IOException {
        if (encoded == null || encoded.length < HEADER_BYTES ||
                encoded.length > MAX_MANIFEST_BYTES) {
            throw new IOException("Invalid file manifest size");
        }

        ByteBuffer input = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (input.get() != 'M' || input.get() != 'L' ||
                input.get() != 'F' || input.get() != 'M' ||
                input.get() != 1 || input.get() != 0 ||
                input.get() != 0 || input.get() != 0) {
            throw new IOException("Invalid file manifest header");
        }

        int entryCount = input.getInt();
        int expectedFileCount = input.getInt();
        long expectedTotalBytes = input.getLong();
        if (entryCount <= 0 || entryCount > MAX_ENTRIES ||
                expectedFileCount < 0 || expectedFileCount > entryCount ||
                expectedTotalBytes < 0 || expectedTotalBytes > MAX_TRANSFER_BYTES) {
            throw new IOException("Invalid file manifest limits");
        }

        List<Entry> entries = new ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) {
            if (input.remaining() < ENTRY_HEADER_BYTES) {
                throw new IOException("Truncated file manifest entry");
            }
            byte type = input.get();
            if (input.get() != 0 || input.get() != 0 || input.get() != 0) {
                throw new IOException("Invalid file manifest entry flags");
            }
            int pathLength = input.getInt();
            long size = input.getLong();
            long modifiedTimeMs = input.getLong();
            if (pathLength <= 0 || pathLength > MAX_PATH_BYTES ||
                    input.remaining() < pathLength ||
                    size < 0 || size > MAX_FILE_BYTES ||
                    (type != TYPE_REGULAR && type != TYPE_DIRECTORY) ||
                    (type == TYPE_DIRECTORY && size != 0)) {
                throw new IOException("Invalid file manifest entry");
            }
            byte[] pathBytes = new byte[pathLength];
            input.get(pathBytes);
            String path = decodeUtf8(pathBytes);
            entries.add(new Entry(type, path, size, modifiedTimeMs));
        }
        if (input.hasRemaining()) {
            throw new IOException("Trailing file manifest data");
        }

        FileManifest manifest = validate(entries);
        if (manifest.fileCount != expectedFileCount ||
                manifest.totalFileBytes != expectedTotalBytes) {
            throw new IOException("File manifest totals do not match");
        }
        return manifest;
    }

    public static byte[] encode(List<Entry> entries) throws IOException {
        FileManifest manifest = validate(entries);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        header.put((byte)'M').put((byte)'L').put((byte)'F').put((byte)'M');
        header.put((byte)1).put((byte)0).put((byte)0).put((byte)0);
        header.putInt(manifest.entries.size());
        header.putInt(manifest.fileCount);
        header.putLong(manifest.totalFileBytes);
        output.write(header.array(), 0, header.array().length);

        for (Entry entry : manifest.entries) {
            byte[] path = entry.path.getBytes(StandardCharsets.UTF_8);
            ByteBuffer entryHeader = ByteBuffer.allocate(ENTRY_HEADER_BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN);
            entryHeader.put(entry.type).put((byte)0).put((byte)0).put((byte)0);
            entryHeader.putInt(path.length);
            entryHeader.putLong(entry.size);
            entryHeader.putLong(entry.modifiedTimeMs);
            output.write(entryHeader.array(), 0, entryHeader.array().length);
            output.write(path, 0, path.length);
            if (output.size() > MAX_MANIFEST_BYTES) {
                throw new IOException("File manifest exceeds limit");
            }
        }
        return output.toByteArray();
    }

    public static FileManifest validate(List<Entry> sourceEntries) throws IOException {
        if (sourceEntries == null || sourceEntries.isEmpty() ||
                sourceEntries.size() > MAX_ENTRIES) {
            throw new IOException("Invalid file entry count");
        }

        List<Entry> entries = new ArrayList<>(sourceEntries);
        Map<String, Entry> previous = new HashMap<>();
        Set<String> topLevel = new HashSet<>();
        int fileCount = 0;
        long totalBytes = 0;
        int encodedBytes = HEADER_BYTES;

        for (Entry entry : entries) {
            if (entry == null || !isSafePath(entry.path) ||
                    (entry.type != TYPE_REGULAR && entry.type != TYPE_DIRECTORY) ||
                    entry.size < 0 || entry.size > MAX_FILE_BYTES ||
                    (entry.type == TYPE_DIRECTORY && entry.size != 0)) {
                throw new IOException("Invalid file entry");
            }

            byte[] pathBytes = entry.path.getBytes(StandardCharsets.UTF_8);
            if (pathBytes.length > MAX_PATH_BYTES) {
                throw new IOException("File path exceeds limit");
            }
            encodedBytes += ENTRY_HEADER_BYTES + pathBytes.length;
            if (encodedBytes > MAX_MANIFEST_BYTES) {
                throw new IOException("File manifest exceeds limit");
            }

            String foldedPath = asciiFold(entry.path);
            if (previous.containsKey(foldedPath)) {
                throw new IOException("Duplicate file path");
            }
            int separator = entry.path.lastIndexOf('/');
            if (separator < 0) {
                topLevel.add(foldedPath);
            }
            else {
                String parent = asciiFold(entry.path.substring(0, separator));
                Entry parentEntry = previous.get(parent);
                if (parentEntry == null || parentEntry.type != TYPE_DIRECTORY) {
                    throw new IOException("File entry appears before its parent");
                }
            }
            previous.put(foldedPath, entry);

            if (entry.type == TYPE_REGULAR) {
                if (totalBytes > MAX_TRANSFER_BYTES - entry.size) {
                    throw new IOException("File transfer exceeds limit");
                }
                totalBytes += entry.size;
                fileCount++;
            }
        }
        if (topLevel.isEmpty()) {
            throw new IOException("File manifest has no top-level entry");
        }
        return new FileManifest(entries, fileCount, totalBytes);
    }

    public static String sanitizeName(String value) {
        String input = value == null ? "" : value.trim();
        StringBuilder output = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character < 0x20 || character == 0x7F ||
                    character == '/' || character == '\\' || character == ':' ||
                    character == '*' || character == '?' || character == '"' ||
                    character == '<' || character == '>' || character == '|') {
                output.append('_');
            }
            else {
                output.append(character);
            }
        }
        while (output.length() != 0) {
            char last = output.charAt(output.length() - 1);
            if (last != '.' && last != ' ') {
                break;
            }
            output.deleteCharAt(output.length() - 1);
        }
        if (output.length() == 0) {
            output.append("unnamed");
        }
        if (isReservedWindowsSegment(output.toString())) {
            output.insert(0, '_');
        }
        return output.toString();
    }

    public static String appendCollisionSuffix(String name, int suffix,
                                               boolean directory) {
        if (directory) {
            return name + " (" + suffix + ')';
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return name + " (" + suffix + ')';
        }
        return name.substring(0, dot) + " (" + suffix + ')' + name.substring(dot);
    }

    private static String decodeUtf8(byte[] value) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value))
                    .toString();
        } catch (CharacterCodingException error) {
            throw new IOException("Invalid UTF-8 file path", error);
        }
    }

    private static boolean isSafePath(String path) {
        if (path == null || path.isEmpty() ||
                path.startsWith("/") || path.endsWith("/")) {
            return false;
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment) ||
                    segment.endsWith(".") || segment.endsWith(" ") ||
                    isReservedWindowsSegment(segment)) {
                return false;
            }
            for (int index = 0; index < segment.length(); index++) {
                char character = segment.charAt(index);
                if (character < 0x20 || character == 0x7F ||
                        character == '\\' || character == ':' ||
                        character == '*' || character == '?' ||
                        character == '"' || character == '<' ||
                        character == '>' || character == '|') {
                    return false;
                }
            }
        }
        return !path.contains("\u0000");
    }

    private static boolean isReservedWindowsSegment(String segment) {
        String base = segment;
        int dot = base.indexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        String folded = asciiFold(base);
        if ("con".equals(folded) || "prn".equals(folded) ||
                "aux".equals(folded) || "nul".equals(folded)) {
            return true;
        }
        return folded.length() == 4 &&
                (folded.startsWith("com") || folded.startsWith("lpt")) &&
                folded.charAt(3) >= '1' && folded.charAt(3) <= '9';
    }

    private static String asciiFold(String value) {
        StringBuilder folded = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            folded.append(character >= 'A' && character <= 'Z' ?
                    (char)(character + ('a' - 'A')) :
                    character);
        }
        return folded.toString();
    }
}
