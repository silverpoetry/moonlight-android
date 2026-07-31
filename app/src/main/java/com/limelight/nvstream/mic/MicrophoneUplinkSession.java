package com.limelight.nvstream.mic;

/**
 * One platform capture session connected to the microphone protocol uplink.
 */
public interface MicrophoneUplinkSession {
    boolean start();

    boolean stop();

    boolean isRunning();

    String getLastErrorMessage();
}
