package com.limelight.ui.performance;

import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.ui.StreamUiSettings;

import java.util.Objects;

/**
 * Immutable, consumer-specific projection used by the performance overlay.
 */
public final class PerformanceOverlayConfiguration {
    private final StreamUiSettings uiSettings;
    private final StreamDecoderSettings decoderSettings;
    private final StreamDisplaySettings displaySettings;
    private final StreamAudioSettings audioSettings;
    private final ControllerSettings controllerSettings;

    public PerformanceOverlayConfiguration(
            StreamUiSettings uiSettings,
            StreamDecoderSettings decoderSettings,
            StreamDisplaySettings displaySettings,
            StreamAudioSettings audioSettings,
            ControllerSettings controllerSettings) {
        this.uiSettings = Objects.requireNonNull(
                uiSettings,
                "uiSettings");
        this.decoderSettings = Objects.requireNonNull(
                decoderSettings,
                "decoderSettings");
        this.displaySettings = Objects.requireNonNull(
                displaySettings,
                "displaySettings");
        this.audioSettings = Objects.requireNonNull(
                audioSettings,
                "audioSettings");
        this.controllerSettings = Objects.requireNonNull(
                controllerSettings,
                "controllerSettings");
    }

    public StreamUiSettings getUiSettings() {
        return uiSettings;
    }

    public StreamDecoderSettings getDecoderSettings() {
        return decoderSettings;
    }

    public StreamDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    public StreamAudioSettings getAudioSettings() {
        return audioSettings;
    }

    public ControllerSettings getControllerSettings() {
        return controllerSettings;
    }
}
