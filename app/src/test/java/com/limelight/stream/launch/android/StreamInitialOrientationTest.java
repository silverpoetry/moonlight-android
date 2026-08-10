package com.limelight.stream.launch.android;

import static org.junit.Assert.assertEquals;

import com.limelight.settings.stream.StreamVideoSettings;

import org.junit.Test;

public final class StreamInitialOrientationTest {
    @Test
    public void landscapeSettingsSelectLandscapeEntry() {
        assertEquals(
                StreamInitialOrientation.LANDSCAPE,
                StreamInitialOrientation.from(
                        StreamVideoSettings.builder()
                                .setPortrait(false)
                                .build()));
    }

    @Test
    public void portraitSettingsSelectPortraitEntry() {
        assertEquals(
                StreamInitialOrientation.PORTRAIT,
                StreamInitialOrientation.from(
                        StreamVideoSettings.builder()
                                .setPortrait(true)
                                .build()));
    }
}
