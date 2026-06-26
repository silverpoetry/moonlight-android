package com.limelight.nvstream;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.limelight.LimeLog;
import com.limelight.nvstream.jni.MoonBridge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class ClipboardSyncController implements ClipboardManager.OnPrimaryClipChangedListener,
        MoonBridge.ClipboardTextListener {
    private static final int MAX_TEXT_BYTES = 1024 * 1024;

    private final Context context;
    private final ClipboardManager clipboardManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private byte[] lastLocalText;
    private byte[] lastRemoteText;
    private boolean started;
    private boolean ready;
    private boolean applyingRemoteText;

    ClipboardSyncController(Context context) {
        this.context = context.getApplicationContext();
        clipboardManager = (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    synchronized void start() {
        if (started || clipboardManager == null) {
            return;
        }

        started = true;
        ready = false;
        MoonBridge.setClipboardTextListener(this);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                clipboardManager.addPrimaryClipChangedListener(ClipboardSyncController.this);
            }
        });
    }

    synchronized void stop() {
        if (!started) {
            return;
        }

        started = false;
        ready = false;
        MoonBridge.setClipboardTextListener(null);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (clipboardManager != null) {
                    clipboardManager.removePrimaryClipChangedListener(ClipboardSyncController.this);
                }
            }
        });
    }

    @Override
    public void onPrimaryClipChanged() {
        if (applyingRemoteText || !ready) {
            return;
        }

        sendCurrentClipboard();
    }

    @Override
    public void onClipboardReady() {
        ready = true;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                sendCurrentClipboard();
            }
        });
    }

    @Override
    public void onClipboardText(final byte[] text) {
        if (text == null || text.length > MAX_TEXT_BYTES || sameBytes(text, lastLocalText)) {
            return;
        }

        lastRemoteText = Arrays.copyOf(text, text.length);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!started || clipboardManager == null) {
                    return;
                }

                String value = new String(text, StandardCharsets.UTF_8);
                applyingRemoteText = true;
                try {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", value));
                    lastLocalText = Arrays.copyOf(text, text.length);
                }
                finally {
                    applyingRemoteText = false;
                }
            }
        });
    }

    private void sendCurrentClipboard() {
        if (!started || !ready || clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
            return;
        }

        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            return;
        }

        CharSequence text = clipData.getItemAt(0).coerceToText(context);
        if (text == null) {
            return;
        }

        byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_BYTES || sameBytes(bytes, lastLocalText) || sameBytes(bytes, lastRemoteText)) {
            return;
        }

        if (MoonBridge.sendClipboardText(bytes) == 0) {
            lastLocalText = Arrays.copyOf(bytes, bytes.length);
        }
        else {
            LimeLog.warning("Failed to send clipboard text to host");
        }
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        return a != null && b != null && Arrays.equals(a, b);
    }
}
