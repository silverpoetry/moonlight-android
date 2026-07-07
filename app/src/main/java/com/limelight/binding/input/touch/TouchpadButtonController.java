package com.limelight.binding.input.touch;

import android.os.Handler;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;

// Owns mouse-button lifetime for touchpad gestures so the gesture recognizer can
// decide intent without duplicating delayed button-up and double-click timers.
final class TouchpadButtonController {
    interface CancellationProvider {
        boolean isCancelled();
    }

    private static final int SECOND_CLICK_DOWN_DELAY_MS = 30;
    private static final int SECOND_CLICK_UP_DELAY_MS = 80;
    private static final int TAP_CLICK_BUTTON_UP_DELAY_MS = 100;

    private final NvConnection conn;
    private final Handler handler;
    private final CancellationProvider cancellationProvider;
    private final boolean[] pendingButtonUp = new boolean[MouseButtonPacket.BUTTON_X2];

    private boolean primaryButtonDown;
    private boolean secondClickButtonDown;

    private final Runnable secondClickDownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!cancellationProvider.isCancelled() && !secondClickButtonDown) {
                conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                secondClickButtonDown = true;
            }
        }
    };

    private final Runnable secondClickUpRunnable = new Runnable() {
        @Override
        public void run() {
            releaseSecondClickButton();
        }
    };

    private final Runnable[] buttonUpRunnables = new Runnable[] {
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_LEFT);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_MIDDLE);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_X1);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    releasePendingButtonUp(MouseButtonPacket.BUTTON_X2);
                }
            }
    };

    TouchpadButtonController(NvConnection conn, Handler handler,
                             CancellationProvider cancellationProvider) {
        this.conn = conn;
        this.handler = handler;
        this.cancellationProvider = cancellationProvider;
    }

    boolean isPrimaryButtonDown() {
        return primaryButtonDown;
    }

    void pressPrimaryButton() {
        if (!primaryButtonDown) {
            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
            primaryButtonDown = true;
        }
    }

    void releasePrimaryButton() {
        if (primaryButtonDown) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            primaryButtonDown = false;
        }
    }

    void sendTapClick(byte buttonIndex) {
        completePendingTapClick(buttonIndex);

        conn.sendMouseButtonDown(buttonIndex);

        int index = getPendingButtonIndex(buttonIndex);
        pendingButtonUp[index] = true;
        handler.postDelayed(buttonUpRunnables[index], TAP_CLICK_BUTTON_UP_DELAY_MS);
    }

    void scheduleSecondPrimaryClick() {
        handler.postDelayed(secondClickDownRunnable, SECOND_CLICK_DOWN_DELAY_MS);
        handler.postDelayed(secondClickUpRunnable, SECOND_CLICK_UP_DELAY_MS);
    }

    void cancelSecondClick() {
        handler.removeCallbacks(secondClickDownRunnable);
        handler.removeCallbacks(secondClickUpRunnable);
        releaseSecondClickButton();
    }

    void cancelPendingTapClicks() {
        for (int i = 0; i < buttonUpRunnables.length; i++) {
            handler.removeCallbacks(buttonUpRunnables[i]);
            if (pendingButtonUp[i]) {
                pendingButtonUp[i] = false;
                conn.sendMouseButtonUp((byte) (i + 1));
            }
        }
    }

    private void completePendingTapClick(byte buttonIndex) {
        handler.removeCallbacks(buttonUpRunnables[getPendingButtonIndex(buttonIndex)]);
        releasePendingButtonUp(buttonIndex);
    }

    private boolean releasePendingButtonUp(byte buttonIndex) {
        int index = getPendingButtonIndex(buttonIndex);
        if (!pendingButtonUp[index]) {
            return false;
        }

        pendingButtonUp[index] = false;
        conn.sendMouseButtonUp(buttonIndex);
        return true;
    }

    private void releaseSecondClickButton() {
        if (secondClickButtonDown) {
            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
            secondClickButtonDown = false;
        }
    }

    private static int getPendingButtonIndex(byte buttonIndex) {
        return buttonIndex - 1;
    }
}
