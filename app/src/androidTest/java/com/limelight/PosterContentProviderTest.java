package com.limelight;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.grid.assets.DiskAssetLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class PosterContentProviderTest {
    private static final String COMPUTER_ID = "poster-provider-test";
    private static final int APP_ID = 42;

    private Context context;
    private File posterFile;

    @Before
    public void setUp() throws IOException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        posterFile = new DiskAssetLoader(context).getFile(COMPUTER_ID, APP_ID);
        File parent = posterFile.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Unable to create poster test directory");
        }
        try (FileOutputStream output = new FileOutputStream(posterFile)) {
            output.write(new byte[] {1, 2, 3, 4});
        }
    }

    @After
    public void tearDown() {
        posterFile.delete();
        File parent = posterFile.getParentFile();
        if (parent != null) {
            parent.delete();
        }
    }

    @Test
    public void validPosterUriIsReadOnly() throws Exception {
        Uri uri = PosterContentProvider.createBoxArtUri(
                context,
                COMPUTER_ID,
                Integer.toString(APP_ID));

        try (ParcelFileDescriptor descriptor =
                     context.getContentResolver().openFileDescriptor(uri, "r")) {
            assertNotNull(descriptor);
            assertEquals(4, descriptor.getStatSize());
        }
    }

    @Test
    public void unknownPathIsRejected() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(PosterContentProvider.getAuthority(context))
                .appendPath("not-boxart")
                .appendPath(COMPUTER_ID)
                .appendPath(Integer.toString(APP_ID))
                .build();

        assertFileNotFound(uri);
    }

    @Test
    public void unsafeComputerIdentifierIsRejected() {
        Uri uri = PosterContentProvider.createBoxArtUri(
                context,
                "..",
                Integer.toString(APP_ID));

        assertFileNotFound(uri);
    }

    private void assertFileNotFound(Uri uri) {
        try (ParcelFileDescriptor ignored =
                     context.getContentResolver().openFileDescriptor(uri, "r")) {
            fail("Expected provider to reject URI: " + uri);
        }
        catch (FileNotFoundException expected) {
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
