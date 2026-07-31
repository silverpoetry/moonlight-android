package com.limelight.ui.stream;

/** Pure decision policy for restoring immersive stream UI. */
public final class StreamSystemUiVisibilityPolicy {
    private StreamSystemUiVisibilityPolicy() {
    }

    public static boolean shouldRestoreImmersiveMode(
            boolean sessionConnected,
            boolean fullscreenVisible,
            boolean navigationHidden) {
        return sessionConnected &&
                (!fullscreenVisible || !navigationHidden);
    }
}
