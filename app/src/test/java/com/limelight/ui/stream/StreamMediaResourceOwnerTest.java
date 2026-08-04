package com.limelight.ui.stream;

import android.view.Surface;

import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class StreamMediaResourceOwnerTest {
    @Test
    public void startResourcesAreSingleUseAndReleasedExplicitly() {
        FakeVideoResource video = new FakeVideoResource();
        FakeAudioResource audio = new FakeAudioResource();
        StreamMediaResourceOwner owner =
                new StreamMediaResourceOwner(video, () -> audio);

        StreamMediaResourceOwner.StartResources resources =
                owner.prepareStart(() -> null);

        assertSame(audio.renderer, resources.getAudioRenderer());
        assertSame(video.renderer, resources.getVideoRenderer());
        assertEquals(1, video.renderTargetUpdates);
        assertThrows(
                IllegalStateException.class,
                () -> owner.prepareStart(() -> null));

        owner.releaseStartResources();

        assertThrows(
                IllegalStateException.class,
                () -> owner.prepareStart(() -> null));
        assertEquals(1, video.renderTargetUpdates);
    }

    @Test
    public void destroyRejectsNewResources() {
        StreamMediaResourceOwner owner =
                new StreamMediaResourceOwner(
                        new FakeVideoResource(),
                        FakeAudioResource::new);

        owner.destroy();
        owner.destroy();

        assertThrows(
                IllegalStateException.class,
                () -> owner.prepareStart(() -> null));
    }

    @Test
    public void videoLifecycleAndStatisticsUseSinglePort() {
        FakeVideoResource video = new FakeVideoResource();
        video.avcSupported = true;
        video.activeVideoFormat = 5;
        video.endToEndLatency = 6;
        video.decoderLatency = 7;
        StreamMediaResourceOwner owner =
                new StreamMediaResourceOwner(
                        video,
                        FakeAudioResource::new);

        owner.notifyVideoBackground();
        owner.notifyVideoForeground();
        owner.prepareVideoForStop();
        owner.setHdrMode(true, new byte[] {1});

        assertTrue(owner.isAvcSupported());
        assertEquals(5, owner.getActiveVideoFormat());
        assertEquals(6, owner.getAverageEndToEndLatency());
        assertEquals(7, owner.getAverageDecoderLatency());
        assertEquals(1, video.backgroundCount);
        assertEquals(1, video.foregroundCount);
        assertEquals(1, video.prepareForStopCount);
        assertTrue(video.hdrEnabled);

        owner.destroy();

        assertFalse(owner.isAvcSupported());
        assertEquals(0, owner.getActiveVideoFormat());
    }

    private static final class FakeVideoResource
            implements StreamMediaResourceOwner.VideoResource {
        private final VideoDecoderRenderer renderer =
                new FakeVideoRenderer();
        private int renderTargetUpdates;
        private int prepareForStopCount;
        private int backgroundCount;
        private int foregroundCount;
        private boolean hdrEnabled;
        private boolean avcSupported;
        private int activeVideoFormat;
        private int endToEndLatency;
        private int decoderLatency;

        @Override
        public VideoDecoderRenderer getTransportRenderer() {
            return renderer;
        }

        @Override
        public void setRenderTarget(Surface renderTarget) {
            renderTargetUpdates++;
        }

        @Override
        public void prepareForStop() {
            prepareForStopCount++;
        }

        @Override
        public void notifyVideoBackground() {
            backgroundCount++;
        }

        @Override
        public void notifyVideoForeground() {
            foregroundCount++;
        }

        @Override
        public void setHdrMode(
                boolean enabled,
                byte[] hdrMetadata) {
            hdrEnabled = enabled;
        }

        @Override
        public boolean isAvcSupported() {
            return avcSupported;
        }

        @Override
        public int getActiveVideoFormat() {
            return activeVideoFormat;
        }

        @Override
        public int getAverageEndToEndLatency() {
            return endToEndLatency;
        }

        @Override
        public int getAverageDecoderLatency() {
            return decoderLatency;
        }
    }

    private static final class FakeAudioResource
            implements StreamMediaResourceOwner.AudioResource {
        private final AudioRenderer renderer = new FakeAudioRenderer();

        @Override
        public AudioRenderer getTransportRenderer() {
            return renderer;
        }

    }

    private static final class FakeAudioRenderer
            implements AudioRenderer {
        @Override
        public int setup(
                MoonBridge.AudioConfiguration audioConfiguration,
                int sampleRate,
                int samplesPerFrame) {
            return 0;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void playDecodedAudio(short[] audioData) {
        }

        @Override
        public void cleanup() {
        }
    }

    private static final class FakeVideoRenderer
            extends VideoDecoderRenderer {
        @Override
        public int setup(
                int format,
                int width,
                int height,
                int redrawRate) {
            return 0;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public int submitDecodeUnit(
                byte[] decodeUnitData,
                int decodeUnitLength,
                int decodeUnitType,
                int frameNumber,
                int frameType,
                char frameHostProcessingLatency,
                long receiveTimeMs,
                long enqueueTimeMs) {
            return 0;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public int getCapabilities() {
            return 0;
        }

        @Override
        public void setHdrMode(
                boolean enabled,
                byte[] hdrMetadata) {
        }
    }
}
