package com.limelight.settings.audio;

/**
 * Immutable stream playback and audio-haptics policy.
 */
public final class StreamAudioSettings {
    public enum ChannelConfiguration {
        STEREO,
        SURROUND_5_1,
        SURROUND_7_1
    }

    private final ChannelConfiguration channelConfiguration;
    private final boolean playHostAudio;
    private final boolean audioEffectsEnabled;
    private final boolean muted;

    private StreamAudioSettings(Builder builder) {
        channelConfiguration =
                builder.channelConfiguration == null
                        ? ChannelConfiguration.STEREO
                        : builder.channelConfiguration;
        playHostAudio = builder.playHostAudio;
        audioEffectsEnabled = builder.audioEffectsEnabled;
        muted = builder.muted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public ChannelConfiguration getChannelConfiguration() {
        return channelConfiguration;
    }

    public boolean shouldPlayHostAudio() {
        return playHostAudio;
    }

    public boolean areAudioEffectsEnabled() {
        return audioEffectsEnabled;
    }

    public boolean isMuted() {
        return muted;
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Builder {
        private ChannelConfiguration channelConfiguration =
                ChannelConfiguration.STEREO;
        private boolean playHostAudio;
        private boolean audioEffectsEnabled;
        private boolean muted;

        private Builder() {
        }

        private Builder(StreamAudioSettings settings) {
            channelConfiguration =
                    settings.channelConfiguration;
            playHostAudio = settings.playHostAudio;
            audioEffectsEnabled =
                    settings.audioEffectsEnabled;
            muted = settings.muted;
        }

        public Builder setChannelConfiguration(
                ChannelConfiguration value) {
            channelConfiguration = value;
            return this;
        }

        public Builder setPlayHostAudio(boolean enabled) {
            playHostAudio = enabled;
            return this;
        }

        public Builder setAudioEffectsEnabled(boolean enabled) {
            audioEffectsEnabled = enabled;
            return this;
        }

        public Builder setMuted(boolean enabled) {
            muted = enabled;
            return this;
        }

        public StreamAudioSettings build() {
            return new StreamAudioSettings(this);
        }
    }
}
