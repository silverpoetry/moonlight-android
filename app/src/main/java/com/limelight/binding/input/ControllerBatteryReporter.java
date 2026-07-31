package com.limelight.binding.input;

import java.util.Objects;

/**
 * Samples and emits battery changes for one controller.
 */
final class ControllerBatteryReporter {
    interface Source {
        ControllerBatterySample sample();
    }

    interface Sink {
        void send(byte protocolState, byte percentage);
    }

    private static final Source UNAVAILABLE_SOURCE = () -> null;

    private final Source source;
    private final Sink sink;
    private int lastReportedStatus;
    private float lastReportedCapacity;

    ControllerBatteryReporter(Source source, Sink sink) {
        this.source = Objects.requireNonNull(source, "source");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    static Source unavailableSource() {
        return UNAVAILABLE_SOURCE;
    }

    synchronized void report() {
        ControllerBatterySample sample = source.sample();
        if (sample == null) {
            return;
        }

        ControllerBatteryReport report =
                ControllerBatteryReport.fromAndroidSample(
                        sample.getStatus(),
                        sample.getCapacity());
        if (report == null ||
                !report.differsFrom(
                        lastReportedStatus,
                        lastReportedCapacity)) {
            return;
        }

        sink.send(
                report.getProtocolState(),
                report.getPercentage());
        lastReportedStatus = report.getAndroidStatus();
        lastReportedCapacity = report.getCapacity();
    }
}
