package com.limelight.nvstream.filetransfer;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class FileManifestTest {
    @Test
    public void commonCFixtureDecodesAndReencodesByteExactly()
            throws Exception {
        byte[] encoded = loadCanonicalFixture();

        FileManifest manifest = FileManifest.decode(encoded);

        assertEquals(3, manifest.entries.size());
        assertEquals(2, manifest.fileCount);
        assertEquals(15, manifest.totalFileBytes);
        assertEntry(
                manifest.entries.get(0),
                FileManifest.TYPE_DIRECTORY,
                "folder",
                0);
        assertEntry(
                manifest.entries.get(1),
                FileManifest.TYPE_REGULAR,
                "folder/one.txt",
                10);
        assertEntry(
                manifest.entries.get(2),
                FileManifest.TYPE_REGULAR,
                "two.bin",
                5);
        assertArrayEquals(encoded, FileManifest.encode(manifest.entries));
    }

    @Test
    public void rejectsSameUnsafePathMatrixAsCommonC() {
        List<String> unsafePaths = Arrays.asList(
                "../secret.txt",
                "folder\\file.txt",
                "C:/file.txt",
                "folder//file.txt",
                "CON.txt",
                "file. ");

        for (String unsafePath : unsafePaths) {
            assertThrows(
                    unsafePath,
                    IOException.class,
                    () -> FileManifest.encode(Arrays.asList(
                            new FileManifest.Entry(
                                    FileManifest.TYPE_REGULAR,
                                    unsafePath,
                                    1,
                                    0))));
        }
    }

    @Test
    public void rejectsOutOfOrderAndCaseFoldedDuplicatePaths() {
        assertThrows(
                IOException.class,
                () -> FileManifest.encode(Arrays.asList(
                        new FileManifest.Entry(
                                FileManifest.TYPE_REGULAR,
                                "folder/file.txt",
                                1,
                                0),
                        new FileManifest.Entry(
                                FileManifest.TYPE_DIRECTORY,
                                "folder",
                                0,
                                0))));
        assertThrows(
                IOException.class,
                () -> FileManifest.encode(Arrays.asList(
                        new FileManifest.Entry(
                                FileManifest.TYPE_REGULAR,
                                "Readme.txt",
                                1,
                                0),
                        new FileManifest.Entry(
                                FileManifest.TYPE_REGULAR,
                                "README.TXT",
                                1,
                                0))));
    }

    @Test
    public void rejectsCorruptedSharedFixture() throws Exception {
        byte[] invalidVersion = loadCanonicalFixture();
        invalidVersion[5] = 1;
        assertThrows(
                IOException.class,
                () -> FileManifest.decode(invalidVersion));

        byte[] unsafe = loadCanonicalFixture();
        unsafe[unsafe.length - "two.bin".length()] = '/';
        assertThrows(IOException.class, () -> FileManifest.decode(unsafe));
    }

    private static void assertEntry(
            FileManifest.Entry entry,
            byte type,
            String path,
            long size) {
        assertEquals(type, entry.type);
        assertEquals(path, entry.path);
        assertEquals(size, entry.size);
        assertEquals(1_720_000_000_000L, entry.modifiedTimeMs);
    }

    private static byte[] loadCanonicalFixture() throws IOException {
        try (InputStream input = FileManifestTest.class.getResourceAsStream(
                "/ClipboardManifestV1.inc")) {
            if (input == null) {
                throw new IOException("Shared clipboard fixture is missing");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String source = new String(
                    output.toByteArray(),
                    StandardCharsets.US_ASCII).trim();
            if (source.length() < 2 || source.charAt(0) != '"' ||
                    source.charAt(source.length() - 1) != '"') {
                throw new IOException("Shared clipboard fixture is malformed");
            }
            return decodeHex(source.substring(1, source.length() - 1));
        }
    }

    private static byte[] decodeHex(String source) throws IOException {
        if ((source.length() & 1) != 0) {
            throw new IOException("Odd-length clipboard fixture");
        }
        byte[] decoded = new byte[source.length() / 2];
        for (int index = 0; index < decoded.length; index++) {
            int high = Character.digit(source.charAt(index * 2), 16);
            int low = Character.digit(source.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IOException("Invalid clipboard fixture hex");
            }
            decoded[index] = (byte)((high << 4) | low);
        }
        return decoded;
    }
}
