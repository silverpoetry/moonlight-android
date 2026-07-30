package com.limelight.ui.performance;

import com.limelight.binding.video.PerfOverlayStats;
import com.limelight.preferences.PreferenceConfiguration;

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

    public static final class Row {
        public final String label;
        public final String value;

        Row(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private final ByteCountFormatter byteCountFormatter;

    public PerformanceOverlayFormatter(
            ByteCountFormatter byteCountFormatter) {
        this.byteCountFormatter = Objects.requireNonNull(
                byteCountFormatter,
                "byteCountFormatter");
    }

    public String formatCompact(
            PerfOverlayStats stats,
            PreferenceConfiguration preferences,
            PerformanceOverlayRuntimeState runtime) {
        if (stats == null) {
            return "--";
        }

        StringBuilder builder = new StringBuilder();
        if (stats.networkRateKbps > 0) {
            builder.append("带宽：")
                    .append(formatThroughput(stats.networkRateKbps))
                    .append("  ");
        }
        if (preferences.enablePerfOverlayLiteExt) {
            builder.append(resolution(
                    stats,
                    preferences,
                    false));
            builder.append(" ");
            builder.append(nonEmpty(stats.codecName, "--"));
            builder.append("  ");
        }
        if (runtime.fsrEnabled) {
            builder.append("FSR ")
                    .append(nonEmpty(
                            runtime.fsrTargetDisplayName,
                            "--"))
                    .append("  ");
        }
        builder.append("延迟/解码：");
        builder.append(stats.networkLatencyMs)
                .append(" ms / ");
        builder.append(stats.decodeTimeMs > 0 ?
                String.format(
                        Locale.US,
                        "%.2f ms",
                        stats.decodeTimeMs) :
                "--");
        builder.append("  丢包率：")
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
            PreferenceConfiguration preferences,
            PerformanceOverlayRuntimeState runtime) {
        if (stats == null) {
            return Collections.singletonList(
                    new Row("状态", "--"));
        }

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(
                "分辨率",
                resolution(stats, preferences, true)));
        rows.add(new Row(
                "编码",
                nonEmpty(stats.codecName, "--")));
        rows.add(new Row(
                "目标码率",
                formatMbps(stats.targetBitrateKbps > 0 ?
                        stats.targetBitrateKbps :
                        preferences.bitrate)));
        rows.add(new Row(
                "目标帧率",
                (stats.targetFps > 0 ?
                        stats.targetFps :
                        preferences.fps) + " FPS"));
        rows.add(new Row(
                "实时帧率",
                formatFps(stats.totalFps)));
        rows.add(new Row(
                "视频码率",
                formatRate(stats.videoRateKbps)));
        rows.add(new Row(
                "音频码率",
                formatRate(stats.audioRateKbps)));
        rows.add(new Row(
                "累计视频流量",
                formatBytes(stats.videoBytes)));
        rows.add(new Row(
                "累计音频流量",
                formatBytes(stats.audioBytes)));
        rows.add(new Row(
                "渲染方式",
                runtime.fsrEnabled ?
                        "GLES渲染" :
                        "系统渲染"));
        rows.add(new Row(
                "超分状态",
                formatUpscaleStatus(runtime)));
        rows.add(new Row(
                "实际渲染链",
                formatRenderPipeline(runtime)));
        rows.add(new Row(
                "连接地址",
                nonEmpty(runtime.streamHost, "--")));
        rows.add(new Row(
                "本地时长",
                formatSessionDuration(runtime)));
        rows.add(new Row(
                "网络延迟",
                stats.networkLatencyMs > 0 ?
                        stats.networkLatencyMs +
                                " ms / 抖动 " +
                                stats.networkLatencyVarianceMs +
                                " ms" :
                        "--"));
        rows.add(new Row(
                "丢包率",
                String.format(
                        Locale.US,
                        "%.2f%%",
                        stats.packetLossPercent)));
        rows.add(new Row(
                "解码延迟",
                stats.decodeTimeMs > 0 ?
                        String.format(
                                Locale.US,
                                "%.2f ms",
                                stats.decodeTimeMs) :
                        "--"));
        rows.add(new Row(
                "主机延迟",
                stats.hostProcessingLatencyMs > 0 ?
                        String.format(
                                Locale.US,
                                "%.1f ms",
                                stats.hostProcessingLatencyMs) :
                        "--"));
        rows.add(new Row(
                "麦克风",
                runtime.micActive ?
                        "开启" :
                        "关闭"));
        rows.add(new Row(
                "音频震动",
                formatAudioHaptics(preferences)));
        rows.add(new Row(
                "USB手柄",
                formatUsbController(preferences, runtime)));
        return Collections.unmodifiableList(rows);
    }

    private String resolution(PerfOverlayStats stats,
                              PreferenceConfiguration preferences,
                              boolean includeHdr) {
        int width = stats.width > 0 ?
                stats.width :
                preferences.width;
        int height = stats.height > 0 ?
                stats.height :
                preferences.height;
        boolean hdr = stats.width > 0 && stats.height > 0 ?
                stats.hdr :
                preferences.enableHdr;
        return width + "x" + height +
                (includeHdr && hdr ? " HDR" : "");
    }

    private String formatUpscaleStatus(
            PerformanceOverlayRuntimeState runtime) {
        if (!runtime.fsrEnabled) {
            return "关闭";
        }
        return nonEmpty(runtime.fsrTargetDisplayName, "--") +
                " / " +
                nonEmpty(runtime.fsrSharpnessDisplayName, "--") +
                " / " +
                (runtime.fsrHdrOutput ? "HDR" : "SDR");
    }

    private String formatRenderPipeline(
            PerformanceOverlayRuntimeState runtime) {
        if (!runtime.fsrEnabled) {
            return "系统直出";
        }
        return runtime.fsrHdrOutput ?
                "GLES FSR HDR" :
                "GLES FSR SDR";
    }

    private String formatUsbController(
            PreferenceConfiguration preferences,
            PerformanceOverlayRuntimeState runtime) {
        if (!preferences.usbDriver) {
            return "关闭";
        }
        if (runtime.usbControllerActive) {
            String type = runtime.usbControllerTypeDisplayName;
            return type != null && !type.isEmpty() ?
                    "已接管 / " + type :
                    "已接管";
        }
        return runtime.usbServiceConnected ?
                "待机" :
                "未启动";
    }

    private String formatAudioHaptics(
            PreferenceConfiguration preferences) {
        if (!preferences.enableAudioHaptics) {
            return "关闭";
        }
        return "开 / " +
                ("controller".equals(
                        preferences.audioHapticsOutputTarget) ?
                        "手柄" :
                        "手机") +
                " / " +
                audioHapticsFilterName(
                        preferences.audioHapticsVoiceFilter) +
                " / " +
                preferences.audioHapticsStrength +
                "%";
    }

    private String audioHapticsFilterName(String filter) {
        if ("low".equals(filter)) {
            return "低";
        }
        if ("medium".equals(filter)) {
            return "中";
        }
        if ("high".equals(filter)) {
            return "高";
        }
        return "关";
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
}
