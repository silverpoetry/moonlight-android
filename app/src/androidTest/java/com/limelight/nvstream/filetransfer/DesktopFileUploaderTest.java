package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.BuildConfig;
import com.limelight.transfer.FileManifest;

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
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri uri = Uri.parse(
                "content://" + BuildConfig.APPLICATION_ID +
                        ".desktopfiletest/item");

        List<DesktopFileUploadSource> sources =
                DesktopFileUploadSourceEnumerator.enumerate(
                        context,
                        Collections.singletonList(uri));

        assertEquals(1, sources.size());
        DesktopFileUploadSource source = sources.get(0);
        FileManifest.Entry entry = source.getManifestEntry();
        assertEquals(FileManifest.TYPE_REGULAR, entry.type);
        assertEquals(GenericContentProvider.FILE_NAME, entry.path);
        assertEquals(GenericContentProvider.FILE_CONTENT.length, entry.size);
        assertEquals(uri, source.getSourceUri());

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
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri uri = Uri.parse(
                "content://" + BuildConfig.APPLICATION_ID +
                        ".desktopfiletest/without-metadata");

        List<DesktopFileUploadSource> sources =
                DesktopFileUploadSourceEnumerator.enumerate(
                        context,
                        Collections.singletonList(uri));

        assertEquals(1, sources.size());
        DesktopFileUploadSource source = sources.get(0);
        FileManifest.Entry entry = source.getManifestEntry();
        assertEquals(FileManifest.TYPE_REGULAR, entry.type);
        assertEquals("without-metadata", entry.path);
        assertEquals(GenericContentProvider.FILE_CONTENT.length, entry.size);
        assertEquals(uri, source.getSourceUri());
    }
}
