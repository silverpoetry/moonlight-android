package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public final class ConfigurationArchiveManagerTest {
    private Context context;
    private SettingsRepository repository;
    private String originalTheme;
    private int originalBitrate;
    private File archive;

    @Before
    public void setUp() {
        context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        repository = AndroidSettingsRepository.create(context);
        originalTheme = repository.get(
                AppPresentationSettingKeys.THEME_MODE);
        originalBitrate = repository.get(
                StreamVideoSettingKeys.BITRATE_KBPS);
    }

    @After
    public void tearDown() {
        repository.edit()
                .put(AppPresentationSettingKeys.THEME_MODE, originalTheme)
                .put(StreamVideoSettingKeys.BITRATE_KBPS, originalBitrate)
                .commit();
        if (archive != null && archive.exists()) {
            assertTrue(archive.delete());
        }
    }

    @Test
    public void exportAndSelectiveSettingsImportRoundTrip()
            throws Exception {
        repository.edit()
                .put(
                        AppPresentationSettingKeys.THEME_MODE,
                        AppPresentationSettingKeys.THEME_MODE_DARK)
                .put(StreamVideoSettingKeys.BITRATE_KBPS, 42000)
                .commit();
        ConfigurationArchiveManager manager =
                new ConfigurationArchiveManager(context, repository);

        archive = manager.createExportArchive();
        assertTrue(archive.isFile());
        repository.edit()
                .put(
                        AppPresentationSettingKeys.THEME_MODE,
                        AppPresentationSettingKeys.THEME_MODE_LIGHT)
                .put(StreamVideoSettingKeys.BITRATE_KBPS, 8000)
                .commit();

        ConfigurationArchiveManager.PreparedImport prepared =
                manager.prepareImport(Uri.fromFile(archive));
        assertTrue(prepared.getComponents().contains(
                ConfigurationArchiveComponent.APP_SETTINGS));
        assertTrue(prepared.getComponents().contains(
                ConfigurationArchiveComponent.PAIRING_DATA));
        manager.importSelected(
                prepared,
                Collections.singleton(
                        ConfigurationArchiveComponent.APP_SETTINGS));

        assertEquals(
                AppPresentationSettingKeys.THEME_MODE_DARK,
                repository.get(AppPresentationSettingKeys.THEME_MODE));
        assertEquals(
                42000,
                (int) repository.get(
                        StreamVideoSettingKeys.BITRATE_KBPS));
    }
}
