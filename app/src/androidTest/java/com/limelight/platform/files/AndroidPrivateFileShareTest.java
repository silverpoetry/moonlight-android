package com.limelight.platform.files;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AndroidPrivateFileShareTest {
    private static final byte[] CONTENT =
            "private-share-fixture".getBytes(StandardCharsets.UTF_8);

    private Context context;
    private File privateSource;
    private File clipboardSource;
    private File cacheSibling;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        privateSource = new File(
                context.getFilesDir(),
                "private-share-fixture.txt");
        File clipboardDirectory = new File(
                context.getCacheDir(),
                AndroidPrivateFileShare.CLIPBOARD_CACHE_DIRECTORY);
        assertTrue(
                clipboardDirectory.isDirectory() ||
                        clipboardDirectory.mkdirs());
        clipboardSource = new File(
                clipboardDirectory,
                "clipboard-share-fixture.png");
        cacheSibling = new File(
                context.getCacheDir(),
                "non-clipboard-share-fixture.png");
        write(privateSource, CONTENT);
        write(clipboardSource, CONTENT);
        write(cacheSibling, CONTENT);
    }

    @After
    public void tearDown() {
        assertTrue(!privateSource.exists() || privateSource.delete());
        assertTrue(!clipboardSource.exists() || clipboardSource.delete());
        assertTrue(!cacheSibling.exists() || cacheSibling.delete());
        deleteFixtureSessions();
    }

    @Test
    public void persistentFileIsStagedAndNeverExposedDirectly()
            throws Exception {
        Uri staged = AndroidPrivateFileShare.stageReadOnly(
                context,
                privateSource);

        assertArrayEquals(CONTENT, read(staged));
        assertThrows(
                IllegalArgumentException.class,
                () -> FileProvider.getUriForFile(
                        context,
                        authority(),
                        privateSource));
    }

    @Test
    public void clipboardShareRejectsEverySiblingCachePath()
            throws Exception {
        Uri clipboard = AndroidPrivateFileShare.exposeClipboardFile(
                context,
                clipboardSource);

        assertArrayEquals(CONTENT, read(clipboard));
        assertThrows(
                FileNotFoundException.class,
                () -> AndroidPrivateFileShare.exposeClipboardFile(
                        context,
                        cacheSibling));
        assertThrows(
                IllegalArgumentException.class,
                () -> FileProvider.getUriForFile(
                        context,
                        authority(),
                        cacheSibling));
    }

    @Test
    public void stagingRejectsFilesOutsideInternalOwnership()
            throws Exception {
        File externalRoot = context.getExternalCacheDir();
        if (externalRoot == null) {
            return;
        }
        File external = new File(
                externalRoot,
                "external-share-fixture.txt");
        write(external, CONTENT);
        try {
            assertThrows(
                    FileNotFoundException.class,
                    () -> AndroidPrivateFileShare.stageReadOnly(
                            context,
                            external));
        }
        finally {
            assertTrue(!external.exists() || external.delete());
        }
    }

    private byte[] read(Uri uri) throws Exception {
        try (InputStream input = context
                .getContentResolver()
                .openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private String authority() {
        return context.getPackageName() + ".fileprovider";
    }

    private static void write(File file, byte[] content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content);
        }
    }

    private static void deleteRecursively(File target) {
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (target.exists()) {
            assertTrue(target.delete());
        }
    }

    private void deleteFixtureSessions() {
        File outboundRoot = new File(
                context.getCacheDir(),
                AndroidPrivateFileShare.OUTBOUND_CACHE_DIRECTORY);
        File[] sessions = outboundRoot.listFiles();
        if (sessions == null) {
            return;
        }
        for (File session : sessions) {
            if (new File(session, privateSource.getName()).isFile()) {
                deleteRecursively(session);
            }
        }
    }
}
