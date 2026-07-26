package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.limelight.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public final class DesktopFileUploaderTest {
    @Test
    public void testGenericContentUri() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        Uri uri = Uri.parse(
                "content://" + BuildConfig.APPLICATION_ID +
                        ".desktopfiletest/item");

        List<FileManifest.Entry> entries = DesktopFileUploader.enumerate(
                context, Collections.singletonList(uri));

        assertEquals(1, entries.size());
        FileManifest.Entry entry = entries.get(0);
        assertEquals(FileManifest.TYPE_REGULAR, entry.type);
        assertEquals(GenericContentProvider.FILE_NAME, entry.path);
        assertEquals(GenericContentProvider.FILE_CONTENT.length, entry.size);
        assertEquals(uri, entry.sourceUri);

        try (InputStream input =
                     context.getContentResolver().openInputStream(uri)) {
            assertNotNull(input);
            for (byte expected : GenericContentProvider.FILE_CONTENT) {
                assertEquals(expected & 0xFF, input.read());
            }
            assertEquals(-1, input.read());
        }
    }

    @Test
    public void testGenericContentUriWithoutMetadataQuery() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        Uri uri = Uri.parse(
                "content://" + BuildConfig.APPLICATION_ID +
                        ".desktopfiletest/without-metadata");

        List<FileManifest.Entry> entries = DesktopFileUploader.enumerate(
                context, Collections.singletonList(uri));

        assertEquals(1, entries.size());
        FileManifest.Entry entry = entries.get(0);
        assertEquals(FileManifest.TYPE_REGULAR, entry.type);
        assertEquals("without-metadata", entry.path);
        assertEquals(GenericContentProvider.FILE_CONTENT.length, entry.size);
        assertEquals(uri, entry.sourceUri);
    }
}
