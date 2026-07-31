package com.limelight.binding.audio.mic;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.mic.MicrophoneUplinkConfig;
import com.limelight.nvstream.mic.MicrophoneUplinkSession;
import com.limelight.nvstream.mic.MicrophoneUplinkSessionFactory;

import java.util.Objects;

/**
 * Android/common-c adapter for microphone capture sessions.
 */
public final class AndroidMicrophoneUplinkSessionFactory
        implements MicrophoneUplinkSessionFactory {
    private final MicrophoneUplinkConfig config;

    public AndroidMicrophoneUplinkSessionFactory(
            MicrophoneUplinkConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean isSupported() {
        return MoonBridge.isMicrophoneUplinkSupported();
    }

    @Override
    public MicrophoneUplinkSession create() {
        return new AndroidMicrophoneUplinkSession(config);
    }
}
