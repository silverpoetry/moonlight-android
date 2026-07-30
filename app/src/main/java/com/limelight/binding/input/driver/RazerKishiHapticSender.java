package com.limelight.binding.input.driver;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import com.limelight.DebugLog;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class RazerKishiHapticSender {
    private static final String TAG = "RazerKishiDebug";
    private static final int QUEUE_CAPACITY = 6;
    private static final int TRANSFER_TIMEOUT_MS = 100;

    private final UsbDeviceConnection connection;
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY, false);
    private volatile boolean running;
    private Thread worker;
    private UsbEndpoint endpoint;
    private int sentFrameCount;

    public RazerKishiHapticSender(UsbDeviceConnection connection) {
        this.connection = connection;
    }

    public synchronized void start(UsbEndpoint endpoint) {
        if (running) {
            DebugLog.debug(TAG, "Sender already running");
            return;
        }

        this.endpoint = endpoint;
        this.running = true;
        this.queue.clear();
        this.sentFrameCount = 0;
        DebugLog.info(TAG, "Starting Kishi sender endpoint=0x" +
                Integer.toHexString(endpoint.getAddress()) +
                " maxPacket=" + endpoint.getMaxPacketSize());

        worker = new Thread(() -> {
            while (running) {
                try {
                    byte[] frame = queue.take();
                    if (frame == null || frame.length == 0) {
                        continue;
                    }

                    int result = connection.bulkTransfer(endpoint, frame, frame.length, TRANSFER_TIMEOUT_MS);
                    sentFrameCount++;
                    if (DebugLog.isEnabled() &&
                            (sentFrameCount <= 5 || sentFrameCount % 50 == 0 || result < 0)) {
                        DebugLog.debug(TAG, "bulkTransfer #" + sentFrameCount +
                                " result=" + result + " size=" + frame.length +
                                " queueRemaining=" + queue.size());
                    }
                }
                catch (InterruptedException ignored) {
                    DebugLog.debug(TAG, "Sender interrupted");
                    break;
                }
            }

            queue.clear();
        }, "RazerKishi-Haptics");

        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        running = false;
        DebugLog.info(TAG, "Stopping Kishi sender");
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        queue.clear();
    }

    public boolean enqueue(byte[] frame) {
        if (!running || frame == null || frame.length == 0) {
            if (DebugLog.isEnabled()) {
                DebugLog.warning(TAG, "enqueue rejected: running=" + running +
                        " frameLength=" + (frame == null ? -1 : frame.length));
            }
            return false;
        }

        if (queue.offer(frame)) {
            if (DebugLog.isEnabled() && sentFrameCount < 5) {
                DebugLog.debug(TAG, "enqueue accepted, queueSize=" + queue.size());
            }
            return true;
        }

        if (DebugLog.isEnabled()) {
            DebugLog.warning(TAG, "enqueue full, dropping oldest frame");
        }
        queue.poll();
        return queue.offer(frame);
    }
}
