package com.limelight.ui.clipboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;

import androidx.core.content.IntentCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.nvstream.filetransfer.ClipboardFileDownloadResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public final class ClipboardFileShareIntentFactoryTest {
    @Test
    public void emptyDirectoryResultCannotCreateAMisleadingShare() {
        ClipboardFileDownloadResult result =
                new ClipboardFileDownloadResult(
                        1,
                        Collections.emptyList());

        assertThrows(
                IllegalArgumentException.class,
                () -> ClipboardFileShareIntentFactory.create(result));
    }

    @Test
    public void oneFileUsesReadOnlySingleShare() {
        Uri uri = Uri.parse("content://documents/image.png");
        ClipboardFileDownloadResult result =
                new ClipboardFileDownloadResult(
                        1,
                        Collections.singletonList(file(
                                uri,
                                "image.png",
                                "image/png")));

        Intent intent = ClipboardFileShareIntentFactory.create(result);

        assertEquals(Intent.ACTION_SEND, intent.getAction());
        assertEquals("image/png", intent.getType());
        assertEquals(
                uri,
                IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri.class));
        assertTrue((intent.getFlags() &
                Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertNotNull(intent.getClipData());
        assertEquals(1, intent.getClipData().getItemCount());
        assertEquals(uri, intent.getClipData().getItemAt(0).getUri());
    }

    @Test
    public void relatedFilesUseCategoryLevelMultipleShare() {
        Uri png = Uri.parse("content://documents/image.png");
        Uri jpeg = Uri.parse("content://documents/photo.jpg");
        ClipboardFileDownloadResult result =
                new ClipboardFileDownloadResult(
                        2,
                        Arrays.asList(
                                file(png, "image.png", "image/png"),
                                file(jpeg, "photo.jpg", "image/jpeg")));

        Intent intent = ClipboardFileShareIntentFactory.create(result);

        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.getAction());
        assertEquals("image/*", intent.getType());
        assertEquals(
                Arrays.asList(png, jpeg),
                IntentCompat.getParcelableArrayListExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri.class));
        assertNotNull(intent.getClipData());
        assertEquals(2, intent.getClipData().getItemCount());
    }

    @Test
    public void unrelatedFilesUseWildcardMultipleShare() {
        ClipboardFileDownloadResult result =
                new ClipboardFileDownloadResult(
                        2,
                        Arrays.asList(
                                file(
                                        Uri.parse(
                                                "content://documents/image.png"),
                                        "image.png",
                                        "image/png"),
                                file(
                                        Uri.parse(
                                                "content://documents/note.txt"),
                                        "note.txt",
                                        "text/plain")));

        Intent intent = ClipboardFileShareIntentFactory.create(result);

        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.getAction());
        assertEquals("*/*", intent.getType());
    }

    private static ClipboardFileDownloadResult.ShareableFile file(
            Uri uri,
            String name,
            String mimeType) {
        return new ClipboardFileDownloadResult.ShareableFile(
                uri,
                name,
                mimeType);
    }
}
