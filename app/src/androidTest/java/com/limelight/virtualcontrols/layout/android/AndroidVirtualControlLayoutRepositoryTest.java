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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AndroidVirtualControlLayoutRepositoryTest {
    private Context context;
    private AndroidVirtualControlLayoutRepository repository;
    private VirtualControlLayoutKey key;
    private File persistedFile;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        repository =
                new AndroidVirtualControlLayoutRepository(context);
        key = VirtualControlLayoutKey.keyboard(
                "OSC_Keyboard_5",
                VirtualControlLayoutOrientation.LANDSCAPE);
        persistedFile = new File(
                context.getFilesDir(),
                "axi_OSC_Keyboard_5.txt");
        assertTrue(
                "Unable to prepare isolated layout fixture",
                !persistedFile.exists() || persistedFile.delete());
    }

    @After
    public void tearDown() {
        assertTrue(
                "Unable to remove isolated layout fixture",
                !persistedFile.exists() || persistedFile.delete());
    }

    @Test
    public void missingSaveLoadAndReplacementAreDeterministic()
            throws Exception {
        assertFalse(repository.load(key).isFound());

        repository.save(
                key,
                VirtualControlLayoutDocument.fromJson(
                        "[{\"name\":\"按键一\"}]"));
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
}
