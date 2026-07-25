package com.limelight.nvstream;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Process;

import com.limelight.LimeLog;
import com.limelight.nvstream.jni.MoonBridge;

import java.util.concurrent.CountDownLatch;

/**
 * Captures 48 kHz mono PCM and forwards it to moonlight-common-c. Opus
 * encoding, SRTP protection, queueing, and transport all live in the native
 * protocol layer so microphone audio shares the established audio UDP socket.
 */
public final class MicUplinkConnection {
    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_SAMPLES = 960;
    private static final int OPUS_BITRATE = 40000;

    private volatile boolean running;
    private volatile boolean stopRequested;
    private volatile String lastErrorMessage;
    private Thread workerThread;
    private volatile AudioRecord audioRecord;
    private final AudioTimestamp captureTimestamp = new AudioTimestamp();
    private long capturedFramePosition;

    public static boolean isSupported() {
        return MoonBridge.isMicrophoneUplinkSupported();
    }

    public boolean start() {
        if (!isSupported()) {
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

    public String getLastErrorMessage() {
        return lastErrorMessage != null ? lastErrorMessage : "麦克风上行失败";
    }

    public boolean isRunning() {
        return running;
    }

    private void runWorker(CountDownLatch startupLatch) {
        short[] pcmFrame = new short[FRAME_SAMPLES];

        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minBufferSize <= 0) {
                throw new IllegalStateException("无法确定麦克风缓冲区大小");
            }

            audioRecord = createStartedAudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    Math.max(minBufferSize, FRAME_SAMPLES * 2 * 4));
            if (audioRecord == null) {
                audioRecord = createStartedAudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        Math.max(minBufferSize, FRAME_SAMPLES * 2 * 4));
            }
            if (audioRecord == null) {
                throw new IllegalStateException("无法初始化麦克风采集");
            }

            int result = MoonBridge.startMicrophoneUplink(OPUS_BITRATE);
            if (result != 0) {
                throw new IllegalStateException("协议启动失败 (" + result + ")");
            }

            LimeLog.info("Microphone uplink capture started: Opus 48 kHz mono, 20 ms, 40 kbps");
            startupLatch.countDown();

            while (!stopRequested) {
                if (!readFrame(pcmFrame)) {
                    break;
                }

                capturedFramePosition += FRAME_SAMPLES;
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
                long deltaNanos = (frameDelta / SAMPLE_RATE) * 1_000_000_000L +
                        (frameDelta % SAMPLE_RATE) * 1_000_000_000L / SAMPLE_RATE;
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
                    SAMPLE_RATE,
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
