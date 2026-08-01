package com.limelight.binding.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.limelight.LimeLog;
import com.limelight.settings.audio.StreamAudioSettings.VoiceFilter;

public final class AudioHapticsController {
    private static final float LOWPASS_CUTOFF_HZ = 140.0f;
    private static final float VOICE_LOWPASS_CUTOFF_HZ = 1200.0f;
    private static final float NOISE_GATE = 0.012f;
    private static final float RELEASE_FLOOR = 0.0015f;
    private static final long UPDATE_INTERVAL_MS = 24L;
    private static final long EFFECT_DURATION_MS = 36L;
    private static final long LEGACY_INTERVAL_MS = 45L;
    private static final long LEGACY_PULSE_MS = 22L;
    private static final int LEGACY_MIN_AMPLITUDE = 42;
    private static final long LEGACY_MIN_PULSE_MS = 8L;
    private static final long LEGACY_MAX_INTERVAL_MS = 125L;

    private final Vibrator vibrator;
    private boolean enabled;
    private int strengthPercent;
    private VoiceFilter voiceFilter;

    private int channelCount;
    private int sampleRate;
    private float lowPassState;
    private float voiceLowPassState;
    private float smoothedLevel;
    private int lastAmplitude;
    private long lastVibrationTimeMs;

    public AudioHapticsController(Context context, boolean enabled, int strengthPercent,
                                  VoiceFilter voiceFilter) {
        this.vibrator = ContextCompat.getSystemService(
                context, Vibrator.class);
        this.enabled = enabled && this.vibrator != null && this.vibrator.hasVibrator();
        this.strengthPercent = Math.max(0, strengthPercent);
        this.voiceFilter = normalizeVoiceFilter(voiceFilter);
    }

    public synchronized void configure(int channelCount, int sampleRate) {
        this.channelCount = Math.max(1, channelCount);
        this.sampleRate = Math.max(1, sampleRate);
        this.lowPassState = 0.0f;
        this.voiceLowPassState = 0.0f;
        this.smoothedLevel = 0.0f;
        this.lastAmplitude = 0;
        this.lastVibrationTimeMs = 0L;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setSettings(
            boolean enabled,
            int strengthPercent,
            VoiceFilter voiceFilter) {
        boolean resolvedEnabled = enabled && vibrator != null && vibrator.hasVibrator();
        boolean wasEnabled = this.enabled;
        this.enabled = resolvedEnabled;
        this.strengthPercent = Math.max(0, strengthPercent);
        this.voiceFilter = normalizeVoiceFilter(voiceFilter);
        if (wasEnabled && !resolvedEnabled) {
            stop();
        }
    }

    public synchronized void onAudioFrame(short[] audioData) {
        if (!enabled || audioData == null || audioData.length < channelCount || channelCount <= 0) {
            return;
        }

        float alpha = getLowPassAlpha();
        float voiceAlpha = getVoiceLowPassAlpha();
        float envelope = 0.0f;
        float voiceEnvelope = 0.0f;
        int frames = audioData.length / channelCount;
        if (frames <= 0) {
            return;
        }

        for (int frameIndex = 0; frameIndex < frames; frameIndex++) {
            int base = frameIndex * channelCount;
            float monoSample = extractMonoSample(audioData, base);
            lowPassState += alpha * (monoSample - lowPassState);
            voiceLowPassState += voiceAlpha * (monoSample - voiceLowPassState);
            envelope += Math.abs(lowPassState);
            voiceEnvelope += Math.abs(voiceLowPassState - lowPassState);
        }

        float averageLevel = envelope / frames;
        float filteredLevel = applyVoiceFilter(averageLevel, voiceEnvelope / frames);
        float targetLevel = Math.max(0.0f, (filteredLevel - NOISE_GATE) / (0.20f - NOISE_GATE));
        float attack = targetLevel > smoothedLevel ? 0.40f : 0.10f;
        smoothedLevel += attack * (targetLevel - smoothedLevel);

        int amplitude = mapLevelToAmplitude(smoothedLevel);
        dispatch(amplitude);
    }

    public synchronized void stop() {
        if (vibrator == null) {
            return;
        }

        smoothedLevel = 0.0f;
        lowPassState = 0.0f;
        voiceLowPassState = 0.0f;
        lastAmplitude = 0;
        lastVibrationTimeMs = 0L;
        vibrator.cancel();
    }

    private float extractMonoSample(short[] audioData, int base) {
        if (channelCount == 1) {
            return audioData[base] / 32768.0f;
        }

        int mixed = 0;
        int mixedChannels = Math.min(channelCount, 2);
        for (int i = 0; i < mixedChannels; i++) {
            mixed += audioData[base + i];
        }

        return (mixed / (float) mixedChannels) / 32768.0f;
    }

    private float getLowPassAlpha() {
        float omega = (float) (2.0 * Math.PI * LOWPASS_CUTOFF_HZ / sampleRate);
        return Math.min(1.0f, Math.max(0.005f, omega));
    }

    private float getVoiceLowPassAlpha() {
        float omega = (float) (2.0 * Math.PI * VOICE_LOWPASS_CUTOFF_HZ / sampleRate);
        return Math.min(1.0f, Math.max(0.015f, omega));
    }

    private float applyVoiceFilter(float bassLevel, float voiceLevel) {
        float suppression = getVoiceSuppressionStrength();
        if (suppression <= 0.0f) {
            return bassLevel;
        }

        float filtered = Math.max(0.0f, bassLevel - (voiceLevel * suppression));

        if (voiceFilter == VoiceFilter.HIGH) {
            float voiceDominance = voiceLevel / Math.max(0.0025f, bassLevel + voiceLevel);
            if (voiceDominance > 0.42f) {
                float attenuation = Math.max(0.10f, 1.0f - ((voiceDominance - 0.42f) * 2.2f));
                filtered *= attenuation;
            }
        }

        return filtered;
    }

    private float getVoiceSuppressionStrength() {
        switch (voiceFilter) {
            case LOW:
                return 0.25f;
            case MEDIUM:
                return 0.50f;
            case HIGH:
                return 1.12f;
            case OFF:
            default:
                return 0.0f;
        }
    }

    private static VoiceFilter normalizeVoiceFilter(
            VoiceFilter filter) {
        return filter == null ? VoiceFilter.OFF : filter;
    }

    private int mapLevelToAmplitude(float level) {
        if (level < RELEASE_FLOOR || strengthPercent == 0) {
            return 0;
        }

        float curved = (float) Math.pow(Math.min(1.0f, level), 0.82f);
        float strengthScale = (float) Math.pow(strengthPercent / 100.0f, 2.35f);
        int amplitude = Math.round(curved * 255.0f * strengthScale);
        return Math.max(0, Math.min(255, amplitude));
    }

    private void dispatch(int amplitude) {
        long now = SystemClock.uptimeMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()) {
            if (amplitude == 0) {
                if (lastAmplitude != 0) {
                    vibrator.cancel();
                    lastAmplitude = 0;
                }
                return;
            }

            if (now - lastVibrationTimeMs < UPDATE_INTERVAL_MS &&
                    Math.abs(amplitude - lastAmplitude) < 12) {
                return;
            }

            VibrationEffect effect = VibrationEffect.createOneShot(EFFECT_DURATION_MS, amplitude);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                VibrationAttributes attrs = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_MEDIA)
                        .build();
                vibrator.vibrate(effect, attrs);
            }
            else {
                vibrateBeforeTiramisu(effect);
            }

            lastAmplitude = amplitude;
            lastVibrationTimeMs = now;
            return;
        }

        if (amplitude == 0) {
            return;
        }

        if (amplitude < LEGACY_MIN_AMPLITUDE) {
            return;
        }

        float legacyStrengthScale = amplitude / 255.0f;
        long legacyIntervalMs = Math.round(LEGACY_INTERVAL_MS +
                ((1.0f - legacyStrengthScale) * (LEGACY_MAX_INTERVAL_MS - LEGACY_INTERVAL_MS)));
        if (now - lastVibrationTimeMs < legacyIntervalMs) {
            return;
        }

        try {
            long legacyPulseMs = Math.max(LEGACY_MIN_PULSE_MS,
                    Math.round(LEGACY_PULSE_MS * Math.max(0.35f, legacyStrengthScale)));
            vibrateBeforeOreo(legacyPulseMs);
            lastVibrationTimeMs = now;
            lastAmplitude = amplitude;
        }
        catch (Exception e) {
            LimeLog.warning("Audio haptics legacy vibrate failed: " + e.getMessage());
        }
    }

    /** API 26-32 compatibility path; VibrationAttributes starts at API 33. */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressWarnings("deprecation")
    private void vibrateBeforeTiramisu(VibrationEffect effect) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        vibrator.vibrate(effect, attributes);
    }

    /** API 21-25 compatibility path; VibrationEffect starts at API 26. */
    @SuppressWarnings("deprecation")
    private void vibrateBeforeOreo(long durationMs) {
        vibrator.vibrate(durationMs);
    }
}
