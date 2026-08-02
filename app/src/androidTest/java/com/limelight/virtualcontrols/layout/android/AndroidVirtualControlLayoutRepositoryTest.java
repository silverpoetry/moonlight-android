package com.limelight.virtualcontrols.layout.android;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.virtualcontrols.layout.VirtualControlLayoutDocument;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutReadResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AndroidVirtualControlLayoutRepositoryTest {
    private Context context;
    private AndroidVirtualControlLayoutRepository repository;
    private VirtualControlLayoutKey key;
    private File canonicalFile;
    private File legacyFile;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        repository =
                new AndroidVirtualControlLayoutRepository(context);
        key = VirtualControlLayoutKey.keyboard(
                "OSC_Keyboard_5",
                VirtualControlLayoutOrientation.LANDSCAPE);
        canonicalFile = new File(
                context.getFilesDir(),
                AndroidVirtualControlLayoutRepository
                        .canonicalFileName(key));
        legacyFile = new File(
                context.getFilesDir(),
                AndroidVirtualControlLayoutRepository
                        .legacyFileName(key));
        removeFixture(canonicalFile);
        removeFixture(legacyFile);
    }

    @After
    public void tearDown() {
        removeFixture(canonicalFile);
        removeFixture(legacyFile);
    }

    @Test
    public void missingSaveLoadAndReplacementAreDeterministic()
            throws Exception {
        assertFalse(repository.load(key).isFound());

        repository.save(
                key,
                VirtualControlLayoutDocument.fromJson(
                        "[{\"name\":\"按键一\"}]"));
        assertTrue(canonicalFile.isFile());
        assertFalse(legacyFile.exists());
        VirtualControlLayoutReadResult first = repository.load(key);
        assertTrue(first.isFound());
        assertEquals(
                "[{\"name\":\"按键一\"}]",
                first.getDocument().getJson());

        repository.save(
                key,
                VirtualControlLayoutDocument.fromJson("[]"));
        assertEquals(
                "[]",
                repository.load(key).getDocument().getJson());
    }

    @Test
    public void validatedLegacyLayoutMigratesOnce() throws Exception {
        try (FileOutputStream output =
                     new FileOutputStream(legacyFile)) {
            output.write(
                    "[{\"name\":\"旧布局\"}]"
                            .getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(
                "[{\"name\":\"旧布局\"}]",
                repository.load(key).getDocument().getJson());
        assertTrue(canonicalFile.isFile());
        assertFalse(legacyFile.exists());
    }

    private static void removeFixture(File file) {
        assertTrue(
                "Unable to remove isolated layout fixture",
                !file.exists() || file.delete());
    }
}
