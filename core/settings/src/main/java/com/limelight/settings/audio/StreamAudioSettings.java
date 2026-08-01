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

    public enum HapticsOutputTarget {
        PHONE,
        CONTROLLER
    }

    public enum VoiceFilter {
        OFF,
        LOW,
        MEDIUM,
        HIGH
    }

    private final ChannelConfiguration channelConfiguration;
    private final boolean playHostAudio;
    private final boolean audioEffectsEnabled;
    private final boolean muted;
    private final boolean audioHapticsEnabled;
    private final HapticsOutputTarget hapticsOutputTarget;
    private final int hapticsStrengthPercent;
    private final VoiceFilter voiceFilter;
    private final boolean keepControllerRumble;

    private StreamAudioSettings(Builder builder) {
        channelConfiguration =
                builder.channelConfiguration == null
                        ? ChannelConfiguration.STEREO
                        : builder.channelConfiguration;
        playHostAudio = builder.playHostAudio;
        audioEffectsEnabled = builder.audioEffectsEnabled;
        muted = builder.muted;
        audioHapticsEnabled = builder.audioHapticsEnabled;
        hapticsOutputTarget =
                builder.hapticsOutputTarget == null
                        ? HapticsOutputTarget.PHONE
                        : builder.hapticsOutputTarget;
        hapticsStrengthPercent = clamp(
                builder.hapticsStrengthPercent,
                25,
                200);
        voiceFilter =
                builder.voiceFilter == null
                        ? VoiceFilter.OFF
                        : builder.voiceFilter;
        keepControllerRumble = builder.keepControllerRumble;
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

    public boolean areAudioHapticsEnabled() {
        return audioHapticsEnabled;
    }

    HapticsOutputTarget getHapticsOutputTarget() {
        return hapticsOutputTarget;
    }

    public boolean isControllerHapticsTarget() {
        return hapticsOutputTarget ==
                HapticsOutputTarget.CONTROLLER;
    }

    public int getHapticsStrengthPercent() {
        return hapticsStrengthPercent;
    }

    public VoiceFilter getVoiceFilter() {
        return voiceFilter;
    }

    public boolean shouldKeepControllerRumble() {
        return keepControllerRumble;
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
        private boolean audioHapticsEnabled;
        private HapticsOutputTarget hapticsOutputTarget =
                HapticsOutputTarget.PHONE;
        private int hapticsStrengthPercent = 100;
        private VoiceFilter voiceFilter = VoiceFilter.OFF;
        private boolean keepControllerRumble;

        private Builder() {
        }

        private Builder(StreamAudioSettings settings) {
            channelConfiguration =
                    settings.channelConfiguration;
            playHostAudio = settings.playHostAudio;
            audioEffectsEnabled =
                    settings.audioEffectsEnabled;
            muted = settings.muted;
            audioHapticsEnabled =
                    settings.audioHapticsEnabled;
            hapticsOutputTarget =
                    settings.hapticsOutputTarget;
            hapticsStrengthPercent =
                    settings.hapticsStrengthPercent;
            voiceFilter = settings.voiceFilter;
            keepControllerRumble =
                    settings.keepControllerRumble;
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

        public Builder setAudioHaptics(
                boolean enabled,
                HapticsOutputTarget outputTarget,
                int strengthPercent,
                VoiceFilter filter,
                boolean keepRumble) {
            audioHapticsEnabled = enabled;
            hapticsOutputTarget = outputTarget;
            hapticsStrengthPercent = strengthPercent;
            voiceFilter = filter;
            keepControllerRumble = keepRumble;
            return this;
        }

        public Builder setAudioHapticsEnabled(boolean enabled) {
            audioHapticsEnabled = enabled;
            return this;
        }

        public Builder setHapticsOutputTarget(
                HapticsOutputTarget target) {
            hapticsOutputTarget = target;
            return this;
        }

        public Builder setHapticsStrengthPercent(int percent) {
            hapticsStrengthPercent = percent;
            return this;
        }

        public Builder setVoiceFilter(VoiceFilter filter) {
            voiceFilter = filter;
            return this;
        }

        public Builder setKeepControllerRumble(
                boolean keepRumble) {
            keepControllerRumble = keepRumble;
            return this;
        }

        public StreamAudioSettings build() {
            return new StreamAudioSettings(this);
        }
    }
}
