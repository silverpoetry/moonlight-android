package com.limelight.ui.gamemenu;

import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.stream.CustomResolutionRepository;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;

/**
 * Lifecycle-bound settings capabilities required by the display dialog.
 *
 * <p>The dialog resolves this contract from its attached Activity so Android
 * can safely recreate it without restoring process-local repositories or
 * callbacks from instance fields.</p>
 */
public interface GameDisplayHost {
    StreamVideoSettings getStreamVideoSettings();

    void applyStreamVideoSettingsUpdate(
            StreamVideoSettingsUpdate update);

    CustomResolutionRepository
            getCustomResolutionRepository();

    StreamAudioSettings getStreamAudioSettings();

    void applyStreamAudioSettingsUpdate(
            StreamAudioSettingsUpdate update);

    void onDisplayConfigurationApplied();
}
