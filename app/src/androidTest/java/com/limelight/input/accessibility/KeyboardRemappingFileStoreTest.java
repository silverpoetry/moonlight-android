package com.limelight.input.accessibility;

import android.content.Context;
import android.view.KeyEvent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class KeyboardRemappingFileStoreTest {
    private Context context;
    private File canonicalFile;
    private File legacyFile;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        canonicalFile = new File(
                context.getFilesDir(),
                KeyboardRemappingFileStore.FILE_NAME);
        legacyFile = new File(
                context.getFilesDir(),
                KeyboardRemappingFileStore.LEGACY_FILE_NAME);
        removeFixture(canonicalFile);
        removeFixture(legacyFile);
    }

    @After
    public void tearDown() {
        removeFixture(canonicalFile);
        removeFixture(legacyFile);
    }

    @Test
    public void migratesLegacyDocumentAndLoadsImmutableSnapshot()
            throws Exception {
        write(
                legacyFile,
                "{\"data\":[{\"scancode\":30,\"code\":" +
                        KeyEvent.KEYCODE_A + "}]}");

        Map<Integer, Integer> mappings =
                KeyboardRemappingFileStore.load(context);

        assertEquals(
                Integer.valueOf(KeyEvent.KEYCODE_A),
                mappings.get(30));
        assertThrows(
                UnsupportedOperationException.class,
                () -> mappings.put(31, KeyEvent.KEYCODE_B));
        assertTrue(canonicalFile.isFile());
        assertFalse(legacyFile.exists());
        assertEquals(
                "{\"data\":[{\"scancode\":30,\"code\":" +
                        KeyEvent.KEYCODE_A + "}]}",
                read(canonicalFile));
    }

    @Test
    public void firstDuplicateMappingRetainsHistoricalPrecedence()
            throws Exception {
        write(
                canonicalFile,
                "{\"data\":[" +
                        "{\"scancode\":30,\"code\":" +
                        KeyEvent.KEYCODE_A + "}," +
                        "{\"scancode\":30,\"code\":" +
                        KeyEvent.KEYCODE_B + "}]}");

        assertEquals(
                Integer.valueOf(KeyEvent.KEYCODE_A),
                KeyboardRemappingFileStore
                        .load(context)
                        .get(30));
    }

    private static void write(File file, String contents)
            throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(contents.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int count = input.read(bytes, offset, bytes.length - offset);
                if (count == -1) {
                    break;
                }
                offset += count;
            }
            assertEquals(bytes.length, offset);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void removeFixture(File file) {
        assertTrue(
                "Unable to remove key-remapping fixture",
                !file.exists() || file.delete());
    }
}
