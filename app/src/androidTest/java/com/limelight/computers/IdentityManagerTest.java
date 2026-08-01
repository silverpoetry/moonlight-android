package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public final class IdentityManagerTest {
    private File testDirectory;
    private Context isolatedContext;

    @Before
    public void setUp() {
        Context target = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        testDirectory = new File(
                target.getCacheDir(),
                "identity-manager-test");
        deleteRecursively(testDirectory);
        assertTrue(testDirectory.mkdirs());
        isolatedContext = new ContextWrapper(target) {
            @Override
            public File getFilesDir() {
                return testDirectory;
            }
        };
    }

    @After
    public void tearDown() {
        deleteRecursively(testDirectory);
    }

    @Test
    public void generatedIdentityIsStableAndStrictlyFormatted() {
        String first = new IdentityManager(isolatedContext).getUniqueId();
        String second = new IdentityManager(isolatedContext).getUniqueId();

        assertEquals(first, second);
        assertTrue(first.matches("[0-9a-f]{16}"));
    }

    @Test
    public void invalidIdentityIsReplacedWithoutLoggingItsValue()
            throws Exception {
        File identityFile = new File(testDirectory, "uniqueid");
        try (FileOutputStream output = new FileOutputStream(identityFile)) {
            output.write("NOT-A-VALID-ID!".getBytes(StandardCharsets.US_ASCII));
        }

        String identity = new IdentityManager(isolatedContext).getUniqueId();

        assertFalse("NOT-A-VALID-ID!".equals(identity));
        assertTrue(identity.matches("[0-9a-f]{16}"));
        assertEquals(
                identity,
                new IdentityManager(isolatedContext).getUniqueId());
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        assertTrue(file.delete());
    }
}
