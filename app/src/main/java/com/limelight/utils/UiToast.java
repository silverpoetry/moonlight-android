package com.limelight.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * App-wide transient message controller.
 *
 * <p>Android Toast instances can otherwise queue behind each other and keep
 * appearing after the operation that produced them has finished. This class
 * keeps at most one Toast active and suppresses rapid duplicate messages.</p>
 */
public final class UiToast {
    public static final int LENGTH_SHORT =
            android.widget.Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG =
            android.widget.Toast.LENGTH_LONG;

    private static final long DUPLICATE_WINDOW_MILLIS = 2_000;
    private static final Object LOCK = new Object();
    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static android.widget.Toast activeToast;
    private static CharSequence lastMessage;
    private static long lastShownAt;

    private final Context context;
    private final CharSequence message;
    private final int duration;

    private UiToast(
            Context context, CharSequence message, int duration) {
        this.context = context.getApplicationContext();
        this.message = message;
        this.duration = duration;
    }

    public static UiToast makeText(
            Context context, CharSequence message, int duration) {
        return new UiToast(context, message, duration);
    }

    public static UiToast makeText(
            Context context, int messageRes, int duration) {
        return new UiToast(
                context, context.getText(messageRes), duration);
    }

    public void show() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showOnMainThread();
        } else {
            MAIN_HANDLER.post(this::showOnMainThread);
        }
    }

    private void showOnMainThread() {
        long now = SystemClock.elapsedRealtime();
        synchronized (LOCK) {
            if (messageEquals(lastMessage, message) &&
                    now - lastShownAt < DUPLICATE_WINDOW_MILLIS) {
                return;
            }
            if (activeToast != null) {
                activeToast.cancel();
            }
            activeToast = android.widget.Toast.makeText(
                    context, message, duration);
            activeToast.show();
            lastMessage = message;
            lastShownAt = now;
        }
    }

    private static boolean messageEquals(
            CharSequence left, CharSequence right) {
        return left == right ||
                (left != null && right != null &&
                        left.toString().contentEquals(right));
    }
}
