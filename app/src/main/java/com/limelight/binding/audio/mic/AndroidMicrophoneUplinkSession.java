package com.limelight.binding.audio.mic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Process;

import com.limelight.LimeLog;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.mic.MicrophoneUplinkConfig;
import com.limelight.nvstream.mic.MicrophoneUplinkSession;

import java.util.concurrent.CountDownLatch;
import java.util.Objects;

/**
 * Captures 48 kHz mono PCM and forwards it to moonlight-common-c. Opus
 * encoding, SRTP protection, queueing, and transport all live in the native
 * protocol layer so microphone audio shares the established audio UDP socket.
 */
public final class AndroidMicrophoneUplinkSession
        implements MicrophoneUplinkSession {
    private final MicrophoneUplinkConfig config;
    private volatile boolean running;
    private volatile boolean stopRequested;
    private volatile String lastErrorMessage;
    private Thread workerThread;
    private volatile AudioRecord audioRecord;
    private final AudioTimestamp captureTimestamp = new AudioTimestamp();
    private long capturedFramePosition;

    public AndroidMicrophoneUplinkSession(
            MicrophoneUplinkConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean start() {
        if (!MoonBridge.isMicrophoneUplinkSupported()) {
            lastErrorMessage = "主机不支持麦克风上行";
            return false;
        }

        CountDownLatch startupLatch = new CountDownLatch(1);
        capturedFramePosition = 0;
        stopRequested = false;
        running = true;
        workerThread = new Thread(() -> runWorker(startupLatch), "MicUplinkCapture");
        workerThread.start();

        try {
            startupLatch.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            lastErrorMessage = "启动麦克风时被中断";
            stop();
            return false;
        }

        return running && !stopRequested;
    }

    @Override
    public boolean stop() {
        stopRequested = true;

        AudioRecord recorder = audioRecord;
        if (recorder != null) {
            try {
                recorder.stop();
            }
            catch (IllegalStateException ignored) {
            }
        }

        Thread thread = workerThread;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            try {
                thread.join(2000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastErrorMessage = "停止麦克风时被中断";
                return false;
            }
        }

        if (thread != null && thread.isAlive()) {
            lastErrorMessage = "麦克风采集线程未能及时停止";
            return false;
        }

        workerThread = null;
        running = false;
        return true;
    }

    @Override
    public String getLastErrorMessage() {
        return lastErrorMessage != null ? lastErrorMessage : "麦克风上行失败";
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void runWorker(CountDownLatch startupLatch) {
        short[] pcmFrame =
                new short[config.getSamplesPerFrame()];

        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            int minBufferSize = AudioRecord.getMinBufferSize(
                    config.getSampleRateHz(),
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minBufferSize <= 0) {
                throw new IllegalStateException("无法确定麦克风缓冲区大小");
            }

            audioRecord = createStartedAudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    Math.max(
                            minBufferSize,
                            config.getCaptureBufferSizeBytes()));
            if (audioRecord == null) {
                audioRecord = createStartedAudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        Math.max(
                                minBufferSize,
                                config.getCaptureBufferSizeBytes()));
            }
            if (audioRecord == null) {
                throw new IllegalStateException("无法初始化麦克风采集");
            }

            int result = MoonBridge.startMicrophoneUplink(
                    config.getOpusBitrateBps());
            if (result != 0) {
                throw new IllegalStateException("协议启动失败 (" + result + ")");
            }

            LimeLog.info(
                    "Microphone uplink capture started: Opus " +
                            config.getSampleRateHz() +
                            " Hz, " +
                            config.getChannelCount() +
                            " channel, " +
                            config.getFrameDurationMillis() +
                            " ms, " +
                            config.getOpusBitrateBps() +
                            " bps");
            startupLatch.countDown();

            while (!stopRequested) {
                if (!readFrame(pcmFrame)) {
                    break;
                }

                capturedFramePosition +=
                        config.getSamplesPerFrame();
                long captureTimeUs = getCaptureTimeUs();
                result = MoonBridge.sendMicrophonePcm(pcmFrame, captureTimeUs);
                if (result != 0) {
                    throw new IllegalStateException("发送麦克风帧失败 (" + result + ")");
                }
            }
        }
        catch (Exception e) {
            lastErrorMessage = "麦克风不可用：" + e.getMessage();
            LimeLog.warning(lastErrorMessage);
            stopRequested = true;
        }
        finally {
            startupLatch.countDown();
            cleanup();
        }
    }

    private long getCaptureTimeUs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (audioRecord.getTimestamp(captureTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC) ==
                    AudioRecord.SUCCESS) {
                // Project the device's monotonic timestamp to the end of the
                // frame just read. This preserves real capture gaps without
                // turning Java thread scheduling stalls into artificial gaps.
                long frameDelta = capturedFramePosition - captureTimestamp.framePosition;
                int sampleRateHz = config.getSampleRateHz();
                long deltaNanos =
                        (frameDelta / sampleRateHz) *
                                1_000_000_000L +
                        (frameDelta % sampleRateHz) *
                                1_000_000_000L /
                                sampleRateHz;
                return (captureTimestamp.nanoTime + deltaNanos) / 1000L;
            }
        }
        return System.nanoTime() / 1000L;
    }

    private AudioRecord createStartedAudioRecord(int source, int bufferSize) {
        AudioRecord recorder = null;
        try {
            recorder = new AudioRecord(
                    source,
                    config.getSampleRateHz(),
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording();
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    return recorder;
                }
            }
        }
        catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            LimeLog.warning("Unable to start audio source " + source + ": " + e.getMessage());
        }

        if (recorder != null) {
            try {
                recorder.stop();
            }
            catch (IllegalStateException ignored) {
            }
            recorder.release();
        }
        return null;
    }

    private boolean readFrame(short[] frame) {
        int offset = 0;
        while (!stopRequested && offset < frame.length) {
            int samplesRead = audioRecord.read(frame, offset, frame.length - offset);
            if (samplesRead <= 0) {
                if (!stopRequested) {
                    lastErrorMessage = "麦克风采集失败 (" + samplesRead + ")";
                }
                return false;
            }
            offset += samplesRead;
        }
        return !stopRequested;
    }

    private void cleanup() {
        AudioRecord recorder = audioRecord;
        audioRecord = null;
        if (recorder != null) {
            try {
                recorder.stop();
            }
            catch (IllegalStateException ignored) {
            }
            recorder.release();
        }

        MoonBridge.stopMicrophoneUplink();
        running = false;
        LimeLog.info("Microphone uplink capture stopped");
    }
}
