package com.limelight.ui.performance;

import com.limelight.binding.video.PerfOverlayStats;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.ui.StreamUiSettings;

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
        PerformanceOverlayConfiguration configuration =
                configuration();
        PerfOverlayStats stats = stats();
        PerformanceOverlayRuntimeState runtime = runtime();

        String text = formatter.formatCompact(
                stats,
                configuration,
                runtime);

        assertTrue(text.contains("Bandwidth: 100.00K/s"));
        assertTrue(text.contains("2560x1440 HEVC"));
        assertTrue(text.contains("Latency/Decode: 6 ms / 1.25 ms"));
        assertTrue(text.contains("Packet loss: 0.50%"));
        assertTrue(text.contains("FPS：119.88"));
        assertTrue(text.endsWith(" Mic"));
    }

    @Test
    public void expandedOverlayUsesRuntimeAndPreferenceFallbacks() {
        PerformanceOverlayConfiguration configuration =
                configuration();
        PerfOverlayStats stats = stats();
        stats.targetBitrateKbps = 0;
        stats.targetFps = 0;
        PerformanceOverlayRuntimeState runtime = runtime();

        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        stats,
                        configuration,
                        runtime);

        assertEquals(
                "2560x1440 HDR",
                value(rows, "Resolution"));
        assertEquals(
                "80 Mbps",
                value(rows, "Target bitrate"));
        assertEquals(
                "120 FPS",
                value(rows, "Target FPS"));
        assertEquals(
                "System renderer",
                value(rows, "Renderer"));
        assertEquals(
                "1:02:03",
                value(rows, "Local duration"));
        assertEquals(
                "Claimed / DualSense",
                value(rows, "USB controller"));
    }

    @Test
    public void unavailableStatsHaveStableFallback() {
        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        null,
                        configuration(),
                        runtime());

        assertEquals(1, rows.size());
        assertEquals("Status", rows.get(0).label);
        assertEquals("--", rows.get(0).value);
        assertEquals(
                "--",
                formatter.formatCompact(
                        null,
                        configuration(),
                runtime()));
    }

    @Test
    public void zeroValueSamplesRemainDistinctFromUnavailableMetrics() {
        PerfOverlayStats stats = stats();
        stats.networkLatencyAvailable = false;
        stats.decoderLatencyAvailable = true;
        stats.decodeTimeMs = 0f;

        assertTrue(formatter.formatCompact(
                stats,
                configuration(),
                runtime()).contains("Latency/Decode: -- / 0.00 ms"));

        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        stats,
                        configuration(),
                        runtime());
        assertEquals("--", value(rows, "Network latency"));
        assertEquals("0.00 ms", value(rows, "Decode latency"));
    }

    private static PerformanceOverlayConfiguration
            configuration() {
        StreamUiSettings uiSettings =
                StreamUiSettings.builder()
                        .setCompactPerformanceDetails(true)
                        .build();
        StreamDecoderSettings decoderSettings =
                new StreamDecoderSettings(
                        1920,
                        1080,
                        120,
                        80000,
                        StreamDecoderSettings.VideoFormat.AUTO,
                        StreamDecoderSettings.FramePacing.BALANCED,
                        false,
                        false,
                        true,
                        2);
        StreamDisplaySettings displaySettings =
                new StreamDisplaySettings(
                        1920,
                        1080,
                        false,
                        false,
                        false,
                        false,
                        true,
                        StreamDisplaySettings.Gravity.DEFAULT);
        ControllerSettings controllerSettings =
                ControllerSettings.builder()
                        .setUsbDriverEnabled(true)
                        .build();
        return new PerformanceOverlayConfiguration(
                uiSettings,
                decoderSettings,
                displaySettings,
                controllerSettings);
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
        stats.networkLatencyAvailable = true;
        stats.decodeTimeMs = 1.25f;
        stats.decoderLatencyAvailable = true;
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
