package com.limelight.ui.performance;

import com.limelight.binding.video.PerfOverlayStats;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDisplaySettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pure formatter for compact and expanded performance-overlay content.
 */
public final class PerformanceOverlayFormatter {
    public interface ByteCountFormatter {
        String format(long byteCount);
    }

    /** Supplies presentation strings while keeping metric calculations Android-free. */
    public interface TextProvider {
        String get(Text text);
    }

    public enum Text {
        BANDWIDTH,
        LATENCY_DECODE,
        PACKET_LOSS,
        STATUS,
        RESOLUTION,
        CODEC,
        TARGET_BITRATE,
        TARGET_FPS,
        ACTUAL_FPS,
        VIDEO_BITRATE,
        AUDIO_BITRATE,
        VIDEO_DATA,
        AUDIO_DATA,
        RENDERER,
        SYSTEM_RENDERER,
        CONNECTION_ADDRESS,
        LOCAL_DURATION,
        NETWORK_LATENCY,
        NETWORK_LATENCY_VALUE,
        DECODE_LATENCY,
        HOST_LATENCY,
        MICROPHONE,
        USB_CONTROLLER,
        ENABLED,
        DISABLED,
        USB_CLAIMED,
        USB_CLAIMED_WITH_TYPE,
        USB_STANDBY,
        USB_NOT_STARTED,
    }

    public static final class Row {
        public final String label;
        public final String value;

        Row(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private final ByteCountFormatter byteCountFormatter;
    private final TextProvider textProvider;

    public PerformanceOverlayFormatter(
            ByteCountFormatter byteCountFormatter) {
        this(byteCountFormatter, PerformanceOverlayFormatter::defaultText);
    }

    public PerformanceOverlayFormatter(
            ByteCountFormatter byteCountFormatter,
            TextProvider textProvider) {
        this.byteCountFormatter = Objects.requireNonNull(
                byteCountFormatter,
                "byteCountFormatter");
        this.textProvider = Objects.requireNonNull(
                textProvider,
                "textProvider");
    }

    public String formatCompact(
            PerfOverlayStats stats,
            PerformanceOverlayConfiguration configuration,
            PerformanceOverlayRuntimeState runtime) {
        if (stats == null) {
            return "--";
        }

        StringBuilder builder = new StringBuilder();
        if (stats.networkRateKbps > 0) {
            builder.append(text(Text.BANDWIDTH)).append(": ")
                    .append(formatThroughput(stats.networkRateKbps))
                    .append("  ");
        }
        if (configuration
                .getUiSettings()
                .areCompactPerformanceDetailsEnabled()) {
            builder.append(resolution(
                    stats,
                    configuration,
                    false));
            builder.append(" ");
            builder.append(nonEmpty(stats.codecName, "--"));
            builder.append("  ");
        }
        builder.append(text(Text.LATENCY_DECODE)).append(": ");
        if (stats.networkLatencyAvailable) {
            builder.append(stats.networkLatencyMs)
                    .append(" ms");
        }
        else {
            builder.append("--");
        }
        builder.append(" / ");
        builder.append(stats.decoderLatencyAvailable ?
                String.format(
                        Locale.US,
                        "%.2f ms",
                        stats.decodeTimeMs) :
                "--");
        builder.append("  ").append(text(Text.PACKET_LOSS)).append(": ")
                .append(String.format(
                        Locale.US,
                        "%.2f%%",
                        stats.packetLossPercent));
        builder.append("  FPS：")
                .append(String.format(
                        Locale.US,
                        "%.2f",
                        stats.totalFps));
        if (runtime.micActive) {
            builder.append(" Mic");
        }
        return builder.toString();
    }

    public List<Row> formatExpanded(
            PerfOverlayStats stats,
            PerformanceOverlayConfiguration configuration,
            PerformanceOverlayRuntimeState runtime) {
        if (stats == null) {
            return Collections.singletonList(
                    new Row(text(Text.STATUS), "--"));
        }

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(
                text(Text.RESOLUTION),
                resolution(stats, configuration, true)));
        rows.add(new Row(
                text(Text.CODEC),
                nonEmpty(stats.codecName, "--")));
        rows.add(new Row(
                text(Text.TARGET_BITRATE),
                formatMbps(stats.targetBitrateKbps > 0 ?
                        stats.targetBitrateKbps :
                        configuration
                                .getDecoderSettings()
                                .getBitrateKbps())));
        rows.add(new Row(
                text(Text.TARGET_FPS),
                (stats.targetFps > 0 ?
                        stats.targetFps :
                        configuration
                                .getDecoderSettings()
                                .getFps()) + " FPS"));
        rows.add(new Row(
                text(Text.ACTUAL_FPS),
                formatFps(stats.totalFps)));
        rows.add(new Row(
                text(Text.VIDEO_BITRATE),
                formatRate(stats.videoRateKbps)));
        rows.add(new Row(
                text(Text.AUDIO_BITRATE),
                formatRate(stats.audioRateKbps)));
        rows.add(new Row(
                text(Text.VIDEO_DATA),
                formatBytes(stats.videoBytes)));
        rows.add(new Row(
                text(Text.AUDIO_DATA),
                formatBytes(stats.audioBytes)));
        rows.add(new Row(
                text(Text.RENDERER),
                text(Text.SYSTEM_RENDERER)));
        rows.add(new Row(
                text(Text.CONNECTION_ADDRESS),
                nonEmpty(runtime.streamHost, "--")));
        rows.add(new Row(
                text(Text.LOCAL_DURATION),
                formatSessionDuration(runtime)));
        rows.add(new Row(
                text(Text.NETWORK_LATENCY),
                stats.networkLatencyAvailable ?
                        String.format(
                                Locale.US,
                                text(Text.NETWORK_LATENCY_VALUE),
                                stats.networkLatencyMs,
                                stats.networkLatencyVarianceMs) :
                        "--"));
        rows.add(new Row(
                text(Text.PACKET_LOSS),
                String.format(
                        Locale.US,
                        "%.2f%%",
                        stats.packetLossPercent)));
        rows.add(new Row(
                text(Text.DECODE_LATENCY),
                stats.decoderLatencyAvailable ?
                        String.format(
                                Locale.US,
                                "%.2f ms",
                                stats.decodeTimeMs) :
                        "--"));
        rows.add(new Row(
                text(Text.HOST_LATENCY),
                stats.hostProcessingLatencyMs > 0 ?
                        String.format(
                                Locale.US,
                                "%.1f ms",
                                stats.hostProcessingLatencyMs) :
                        "--"));
        rows.add(new Row(
                text(Text.MICROPHONE),
                runtime.micActive ?
                        text(Text.ENABLED) :
                        text(Text.DISABLED)));
        rows.add(new Row(
                text(Text.USB_CONTROLLER),
                formatUsbController(
                        configuration.getControllerSettings(),
                        runtime)));
        return Collections.unmodifiableList(rows);
    }

    private String resolution(PerfOverlayStats stats,
                              PerformanceOverlayConfiguration configuration,
                              boolean includeHdr) {
        StreamDecoderSettings decoder =
                configuration.getDecoderSettings();
        StreamDisplaySettings display =
                configuration.getDisplaySettings();
        int width = stats.width > 0 ?
                stats.width :
                decoder.getWidth();
        int height = stats.height > 0 ?
                stats.height :
                decoder.getHeight();
        boolean hdr = stats.width > 0 && stats.height > 0 ?
                stats.hdr :
                display.isHdrEnabled();
        return width + "x" + height +
                (includeHdr && hdr ? " HDR" : "");
    }

    private String formatUsbController(
            ControllerSettings settings,
            PerformanceOverlayRuntimeState runtime) {
        if (!settings.isUsbDriverEnabled()) {
            return text(Text.DISABLED);
        }
        if (runtime.usbControllerActive) {
            String type = runtime.usbControllerTypeDisplayName;
            return type != null && !type.isEmpty() ?
                    String.format(
                            Locale.US,
                            text(Text.USB_CLAIMED_WITH_TYPE),
                            type) :
                    text(Text.USB_CLAIMED);
        }
        return runtime.usbServiceConnected ?
                text(Text.USB_STANDBY) :
                text(Text.USB_NOT_STARTED);
    }

    private String formatSessionDuration(
            PerformanceOverlayRuntimeState runtime) {
        if (runtime.sessionStartElapsedMs <= 0) {
            return "--";
        }
        long totalSeconds = Math.max(
                0,
                (runtime.nowElapsedMs -
                        runtime.sessionStartElapsedMs) /
                        1000);
        long hours = totalSeconds / 3600;
        long minutes = totalSeconds % 3600 / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(
                    Locale.US,
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    seconds);
        }
        return String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds);
    }

    private String formatFps(float fps) {
        return fps > 0 ?
                String.format(Locale.US, "%.2f FPS", fps) :
                "--";
    }

    private String formatMbps(int kbps) {
        return kbps > 0 ?
                String.format(
                        Locale.US,
                        "%.0f Mbps",
                        kbps / 1000f) :
                "--";
    }

    private String formatRate(float kbps) {
        if (kbps <= 0) {
            return "--";
        }
        if (kbps >= 1000f) {
            return String.format(
                    Locale.US,
                    "%.2f Mbps",
                    kbps / 1000f);
        }
        return String.format(
                Locale.US,
                "%.0f Kbps",
                kbps);
    }

    private String formatThroughput(float kbps) {
        if (kbps <= 0) {
            return "--";
        }
        float kilobytesPerSecond = kbps / 8f;
        if (kilobytesPerSecond >= 1024f) {
            return String.format(
                    Locale.US,
                    "%.2fM/s",
                    kilobytesPerSecond / 1024f);
        }
        return String.format(
                Locale.US,
                "%.2fK/s",
                kilobytesPerSecond);
    }

    private String formatBytes(long bytes) {
        return bytes > 0 ?
                byteCountFormatter.format(bytes) :
                "--";
    }

    private static String nonEmpty(String value,
                                   String fallback) {
        return value == null || value.isEmpty() ?
                fallback :
                value;
    }

    private String text(Text text) {
        return textProvider.get(text);
    }

    private static String defaultText(Text text) {
        switch (text) {
            case BANDWIDTH: return "Bandwidth";
            case LATENCY_DECODE: return "Latency/Decode";
            case PACKET_LOSS: return "Packet loss";
            case STATUS: return "Status";
            case RESOLUTION: return "Resolution";
            case CODEC: return "Codec";
            case TARGET_BITRATE: return "Target bitrate";
            case TARGET_FPS: return "Target FPS";
            case ACTUAL_FPS: return "Actual FPS";
            case VIDEO_BITRATE: return "Video bitrate";
            case AUDIO_BITRATE: return "Audio bitrate";
            case VIDEO_DATA: return "Video data";
            case AUDIO_DATA: return "Audio data";
            case RENDERER: return "Renderer";
            case SYSTEM_RENDERER: return "System renderer";
            case CONNECTION_ADDRESS: return "Connection address";
            case LOCAL_DURATION: return "Local duration";
            case NETWORK_LATENCY: return "Network latency";
            case NETWORK_LATENCY_VALUE: return "%1$d ms / jitter %2$d ms";
            case DECODE_LATENCY: return "Decode latency";
            case HOST_LATENCY: return "Host latency";
            case MICROPHONE: return "Microphone";
            case USB_CONTROLLER: return "USB controller";
            case ENABLED: return "Enabled";
            case DISABLED: return "Disabled";
            case USB_CLAIMED: return "Claimed";
            case USB_CLAIMED_WITH_TYPE: return "Claimed / %1$s";
            case USB_STANDBY: return "Standby";
            case USB_NOT_STARTED: return "Not started";
            default: throw new IllegalArgumentException("Unknown performance text: " + text);
        }
    }
}
