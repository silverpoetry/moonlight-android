package com.limelight.ui.stream;

import android.app.Activity;

import androidx.annotation.MainThread;

import com.limelight.binding.video.DecoderCrashTracker;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.preferences.GlPreferences;
import com.limelight.settings.stream.StreamDecoderSettings;

import java.util.Objects;

/** Constructs the Android media runtime and freezes its startup capabilities. */
public final class AndroidStreamMediaRuntimeFactory {
    public static final class Result {
        private final StreamMediaResourceOwner resourceOwner;
        private final StreamDecoderCapabilities decoderCapabilities;

        private Result(
                StreamMediaResourceOwner resourceOwner,
                StreamDecoderCapabilities decoderCapabilities) {
            this.resourceOwner = Objects.requireNonNull(
                    resourceOwner,
                    "resourceOwner");
            this.decoderCapabilities = Objects.requireNonNull(
                    decoderCapabilities,
                    "decoderCapabilities");
        }

        public StreamMediaResourceOwner getResourceOwner() {
            return resourceOwner;
        }

        public StreamDecoderCapabilities getDecoderCapabilities() {
            return decoderCapabilities;
        }
    }

    private AndroidStreamMediaRuntimeFactory() {
    }

    @MainThread
    public static Result create(
            Activity activity,
            StreamDecoderSettings settings,
            DecoderCrashTracker crashTracker,
            boolean hdrRequested,
            PerfOverlayListener performanceListener,
            StreamMediaResourceOwner.AudioRendererFactory
                    audioRendererFactory) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(crashTracker, "crashTracker");
        Objects.requireNonNull(
                performanceListener,
                "performanceListener");
        Objects.requireNonNull(
                audioRendererFactory,
                "audioRendererFactory");

        GlPreferences glPreferences =
                GlPreferences.readPreferences(activity);
        MediaCodecHelper.initialize(
                activity,
                glPreferences.glRenderer);
        MediaCodecDecoderRenderer decoder =
                new MediaCodecDecoderRenderer(
                        activity,
                        settings,
                        crashTracker,
                        crashTracker.getInitialCrashCount(),
                        hdrRequested,
                        glPreferences.glRenderer,
                        performanceListener);
        StreamDecoderCapabilities capabilities =
                new StreamDecoderCapabilities(
                        decoder.isHevcSupported(),
                        hdrRequested &&
                                decoder
                                        .isHevcMain10Hdr10Supported(),
                        decoder.isAv1Supported(),
                        hdrRequested && decoder.isAv1Main10Supported(),
                        decoder.getPreferredColorSpace(),
                        decoder.getPreferredColorRange());
        return new Result(
                StreamMediaResourceOwner.create(
                        decoder,
                        audioRendererFactory),
                capabilities);
    }
}
