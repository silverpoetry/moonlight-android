package com.limelight.nvstream.mic;

/**
 * Session-scoped microphone capability exposed to presentation controllers.
 *
 * <p>The endpoint owns transport and capture state. Callers must serialize
 * lifecycle operations off the Android main thread.</p>
 */
public interface MicrophoneUplinkEndpoint {
    boolean isMicUplinkSupported();

    boolean isMicUplinkActive();

    MicrophoneUplinkState getMicUplinkState();

    String getLastMicUplinkMessage();

    boolean startMicUplink();

    boolean stopMicUplink();
}
