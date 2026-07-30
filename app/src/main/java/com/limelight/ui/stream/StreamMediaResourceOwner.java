package com.limelight.ui.stream;

import android.view.Surface;

import androidx.annotation.MainThread;

import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;

import java.util.Objects;

/**
 * Owns the Activity-scoped references used by one media session.
 *
 * <p>The transport remains responsible for invoking renderer cleanup after a
 * successful start. This owner controls when render targets and the active
 * audio renderer become reachable from the UI, and it owns the FSR input
 * surface itself.</p>
 */
public final class StreamMediaResourceOwner {
    public interface AudioRendererFactory {
        @MainThread
        AndroidAudioRenderer create();
    }

    interface VideoResource {
        VideoDecoderRenderer getTransportRenderer();

        void setRenderTarget(Surface renderTarget);

        void prepareForStop();

        void notifyVideoBackground();

        void notifyVideoForeground();

        void setHdrMode(boolean enabled, byte[] hdrMetadata);

        boolean isAvcSupported();

        int getActiveVideoFormat();

        int getAverageEndToEndLatency();

        int getAverageDecoderLatency();
    }

    interface AudioResource {
        AudioRenderer getTransportRenderer();

        void updateAudioHapticsSettings(
                boolean enabled,
                int strength,
                String voiceFilter,
                String outputTarget);
    }

    interface AudioResourceFactory {
        AudioResource create();
    }

    interface OwnedSurface {
        Surface getSurface();

        void release();
    }

    interface RenderTarget {
        Surface getSurface();
    }

    public static final class StartResources {
        private final AudioRenderer audioRenderer;
        private final VideoDecoderRenderer videoRenderer;

        private StartResources(
                AudioRenderer audioRenderer,
                VideoDecoderRenderer videoRenderer) {
            this.audioRenderer = audioRenderer;
            this.videoRenderer = videoRenderer;
        }

        public AudioRenderer getAudioRenderer() {
            return audioRenderer;
        }

        public VideoDecoderRenderer getVideoRenderer() {
            return videoRenderer;
        }
    }

    private final VideoResource videoResource;
    private final AudioResourceFactory audioResourceFactory;

    private AudioResource activeAudioResource;
    private OwnedSurface fsrInputSurface;
    private boolean startPrepared;
    private boolean destroyed;

    @MainThread
    public static StreamMediaResourceOwner create(
            MediaCodecDecoderRenderer videoRenderer,
            AudioRendererFactory audioRendererFactory) {
        Objects.requireNonNull(videoRenderer, "videoRenderer");
        Objects.requireNonNull(
                audioRendererFactory,
                "audioRendererFactory");
        return new StreamMediaResourceOwner(
                new MediaCodecVideoResource(videoRenderer),
                () -> new AndroidAudioResource(
                        Objects.requireNonNull(
                                audioRendererFactory.create(),
                                "audioRenderer")));
    }

    StreamMediaResourceOwner(
            VideoResource videoResource,
            AudioResourceFactory audioResourceFactory) {
        this.videoResource = Objects.requireNonNull(
                videoResource,
                "videoResource");
        this.audioResourceFactory = Objects.requireNonNull(
                audioResourceFactory,
                "audioResourceFactory");
    }

    @MainThread
    public StartResources prepareStart(Surface renderTarget) {
        Surface validatedTarget =
                Objects.requireNonNull(renderTarget, "renderTarget");
        return prepareStart(() -> validatedTarget);
    }

    @MainThread
    StartResources prepareStart(RenderTarget renderTarget) {
        checkActive();
        if (startPrepared) {
            throw new IllegalStateException(
                    "Media resources were already acquired");
        }
        startPrepared = true;

        videoResource.setRenderTarget(
                Objects.requireNonNull(
                        renderTarget,
                        "renderTarget").getSurface());
        AudioResource newAudioResource = Objects.requireNonNull(
                audioResourceFactory.create(),
                "audioResource");
        activeAudioResource = newAudioResource;
        return new StartResources(
                newAudioResource.getTransportRenderer(),
                videoResource.getTransportRenderer());
    }

    @MainThread
    public void releaseStartResources() {
        activeAudioResource = null;
    }

    @MainThread
    public void replaceFsrInputSurface(Surface surface) {
        replaceFsrInputSurface(
                new AndroidOwnedSurface(
                        Objects.requireNonNull(surface, "surface")));
    }

    @MainThread
    void replaceFsrInputSurface(OwnedSurface surface) {
        Objects.requireNonNull(surface, "surface");
        if (destroyed) {
            surface.release();
            return;
        }
        releaseFsrInputSurface();
        fsrInputSurface = surface;
    }

    @MainThread
    public void releaseFsrInputSurface() {
        if (fsrInputSurface == null) {
            return;
        }
        fsrInputSurface.release();
        fsrInputSurface = null;
    }

    @MainThread
    public Surface getFsrInputSurface() {
        return fsrInputSurface == null
                ? null
                : fsrInputSurface.getSurface();
    }

    @MainThread
    public void setRenderTarget(Surface renderTarget) {
        checkActive();
        videoResource.setRenderTarget(
                Objects.requireNonNull(renderTarget, "renderTarget"));
    }

    @MainThread
    public void prepareVideoForStop() {
        if (!destroyed) {
            videoResource.prepareForStop();
        }
    }

    @MainThread
    public void notifyVideoBackground() {
        if (!destroyed) {
            videoResource.notifyVideoBackground();
        }
    }

    @MainThread
    public void notifyVideoForeground() {
        if (!destroyed) {
            videoResource.notifyVideoForeground();
        }
    }

    @MainThread
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        if (!destroyed) {
            videoResource.setHdrMode(enabled, hdrMetadata);
        }
    }

    @MainThread
    public boolean isAvcSupported() {
        return !destroyed && videoResource.isAvcSupported();
    }

    @MainThread
    public int getActiveVideoFormat() {
        return destroyed ? 0 : videoResource.getActiveVideoFormat();
    }

    @MainThread
    public int getAverageEndToEndLatency() {
        return destroyed
                ? 0
                : videoResource.getAverageEndToEndLatency();
    }

    @MainThread
    public int getAverageDecoderLatency() {
        return destroyed
                ? 0
                : videoResource.getAverageDecoderLatency();
    }

    @MainThread
    public void updateAudioHapticsSettings(
            boolean enabled,
            int strength,
            String voiceFilter,
            String outputTarget) {
        if (destroyed || activeAudioResource == null) {
            return;
        }
        activeAudioResource.updateAudioHapticsSettings(
                enabled,
                strength,
                voiceFilter,
                outputTarget);
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        activeAudioResource = null;
        releaseFsrInputSurface();
    }

    private void checkActive() {
        if (destroyed) {
            throw new IllegalStateException(
                    "Media resource owner is destroyed");
        }
    }

    private static final class MediaCodecVideoResource
            implements VideoResource {
        private final MediaCodecDecoderRenderer renderer;

        private MediaCodecVideoResource(
                MediaCodecDecoderRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public VideoDecoderRenderer getTransportRenderer() {
            return renderer;
        }

        @Override
        public void setRenderTarget(Surface renderTarget) {
            renderer.setRenderTarget(renderTarget);
        }

        @Override
        public void prepareForStop() {
            renderer.prepareForStop();
        }

        @Override
        public void notifyVideoBackground() {
            renderer.notifyVideoBackground();
        }

        @Override
        public void notifyVideoForeground() {
            renderer.notifyVideoForeground();
        }

        @Override
        public void setHdrMode(
                boolean enabled,
                byte[] hdrMetadata) {
            renderer.setHdrMode(enabled, hdrMetadata);
        }

        @Override
        public boolean isAvcSupported() {
            return renderer.isAvcSupported();
        }

        @Override
        public int getActiveVideoFormat() {
            return renderer.getActiveVideoFormat();
        }

        @Override
        public int getAverageEndToEndLatency() {
            return renderer.getAverageEndToEndLatency();
        }

        @Override
        public int getAverageDecoderLatency() {
            return renderer.getAverageDecoderLatency();
        }
    }

    private static final class AndroidAudioResource
            implements AudioResource {
        private final AndroidAudioRenderer renderer;

        private AndroidAudioResource(AndroidAudioRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public AudioRenderer getTransportRenderer() {
            return renderer;
        }

        @Override
        public void updateAudioHapticsSettings(
                boolean enabled,
                int strength,
                String voiceFilter,
                String outputTarget) {
            renderer.updateAudioHapticsSettings(
                    enabled,
                    strength,
                    voiceFilter,
                    outputTarget);
        }
    }

    private static final class AndroidOwnedSurface
            implements OwnedSurface {
        private final Surface surface;

        private AndroidOwnedSurface(Surface surface) {
            this.surface = surface;
        }

        @Override
        public Surface getSurface() {
            return surface;
        }

        @Override
        public void release() {
            surface.release();
        }
    }
}
