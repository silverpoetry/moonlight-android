package com.limelight.ui.performance;

import com.limelight.binding.video.PerfOverlayStats;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PerformanceOverlayFormatterTest {
    private final PerformanceOverlayFormatter formatter =
            new PerformanceOverlayFormatter(
                    bytes -> bytes + " B");

    @Test
    public void compactOverlayPreservesStreamingSignals() {
        PreferenceConfiguration preferences = preferences();
        PerfOverlayStats stats = stats();
        PerformanceOverlayRuntimeState runtime = runtime();

        String text = formatter.formatCompact(
                stats,
                preferences,
                runtime);

        assertTrue(text.contains("带宽：100.00K/s"));
        assertTrue(text.contains("2560x1440 HEVC"));
        assertTrue(text.contains("FSR 原始高度"));
        assertTrue(text.contains("延迟/解码：6 ms / 1.25 ms"));
        assertTrue(text.contains("丢包率：0.50%"));
        assertTrue(text.contains("FPS：119.88"));
        assertTrue(text.endsWith(" Mic"));
    }

    @Test
    public void expandedOverlayUsesRuntimeAndPreferenceFallbacks() {
        PreferenceConfiguration preferences = preferences();
        PerfOverlayStats stats = stats();
        stats.targetBitrateKbps = 0;
        stats.targetFps = 0;
        PerformanceOverlayRuntimeState runtime = runtime();

        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        stats,
                        preferences,
                        runtime);

        assertEquals(
                "2560x1440 HDR",
                value(rows, "分辨率"));
        assertEquals(
                "80 Mbps",
                value(rows, "目标码率"));
        assertEquals(
                "120 FPS",
                value(rows, "目标帧率"));
        assertEquals(
                "原始高度 / 75% / HDR",
                value(rows, "超分状态"));
        assertEquals(
                "GLES FSR HDR",
                value(rows, "实际渲染链"));
        assertEquals(
                "1:02:03",
                value(rows, "本地时长"));
        assertEquals(
                "开 / 手柄 / 中 / 65%",
                value(rows, "音频震动"));
        assertEquals(
                "已接管 / DualSense",
                value(rows, "USB手柄"));
    }

    @Test
    public void unavailableStatsHaveStableFallback() {
        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        null,
                        preferences(),
                        runtime());

        assertEquals(1, rows.size());
        assertEquals("状态", rows.get(0).label);
        assertEquals("--", rows.get(0).value);
        assertEquals(
                "--",
                formatter.formatCompact(
                        null,
                        preferences(),
                        runtime()));
    }

    private static PreferenceConfiguration preferences() {
        PreferenceConfiguration preferences =
                new PreferenceConfiguration();
        preferences.width = 1920;
        preferences.height = 1080;
        preferences.bitrate = 80000;
        preferences.fps = 120;
        preferences.enableHdr = true;
        preferences.enablePerfOverlayLiteExt = true;
        preferences.usbDriver = true;
        preferences.enableAudioHaptics = true;
        preferences.audioHapticsOutputTarget = "controller";
        preferences.audioHapticsVoiceFilter = "medium";
        preferences.audioHapticsStrength = 65;
        return preferences;
    }

    private static PerfOverlayStats stats() {
        PerfOverlayStats stats = new PerfOverlayStats();
        stats.width = 2560;
        stats.height = 1440;
        stats.hdr = true;
        stats.codecName = "HEVC";
        stats.targetBitrateKbps = 80000;
        stats.targetFps = 120;
        stats.totalFps = 119.88f;
        stats.networkRateKbps = 800f;
        stats.networkLatencyMs = 6;
        stats.networkLatencyVarianceMs = 2;
        stats.decodeTimeMs = 1.25f;
        stats.packetLossPercent = 0.5f;
        stats.videoRateKbps = 72000f;
        stats.audioRateKbps = 384f;
        stats.videoBytes = 1000;
        stats.audioBytes = 200;
        stats.hostProcessingLatencyMs = 3.5f;
        return stats;
    }

    private static PerformanceOverlayRuntimeState runtime() {
        return new PerformanceOverlayRuntimeState(
                true,
                "原始高度",
                "75%",
                true,
                true,
                "192.168.3.7",
                1000,
                3724000,
                true,
                "DualSense",
                true);
    }

    private static String value(
            List<PerformanceOverlayFormatter.Row> rows,
            String label) {
        for (PerformanceOverlayFormatter.Row row : rows) {
            if (label.equals(row.label)) {
                return row.value;
            }
        }
        throw new AssertionError("Missing row: " + label);
    }
}
