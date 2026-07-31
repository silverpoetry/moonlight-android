package com.limelight.nvstream.mic;

/**
 * Creates platform microphone sessions after checking negotiated support.
 */
public interface MicrophoneUplinkSessionFactory {
    boolean isSupported();

    MicrophoneUplinkSession create();
}
