package com.limelight.binding.audio.mic;

import android.content.Context;

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
    private final Context appContext;

    public AndroidMicrophoneUplinkSessionFactory(
            Context context,
            MicrophoneUplinkConfig config) {
        appContext = context.getApplicationContext();
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean isSupported() {
        return MoonBridge.isMicrophoneUplinkSupported();
    }

    @Override
    public MicrophoneUplinkSession create() {
        return new AndroidMicrophoneUplinkSession(appContext, config);
    }
}
