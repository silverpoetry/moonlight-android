package com.limelight.binding.input;

import android.os.BatteryManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerBatteryReporterTest {
    @Test
    public void unavailableSourceDoesNotEmit() {
        RecordingSink sink = new RecordingSink();
        ControllerBatteryReporter reporter = new ControllerBatteryReporter(
                ControllerBatteryReporter.unavailableSource(),
                sink);

        reporter.report();

        assertEquals(0, sink.count);
    }

    @Test
    public void unchangedSamplesAreSuppressed() {
        MutableSource source = new MutableSource();
        source.sample = new ControllerBatterySample(
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                0.6f);
        RecordingSink sink = new RecordingSink();
        ControllerBatteryReporter reporter =
                new ControllerBatteryReporter(source, sink);

        reporter.report();
        reporter.report();

        assertEquals(1, sink.count);
    }

    @Test
    public void changedCapacityEmitsAgain() {
        MutableSource source = new MutableSource();
        source.sample = new ControllerBatterySample(
                BatteryManager.BATTERY_STATUS_CHARGING,
                0.4f);
        RecordingSink sink = new RecordingSink();
        ControllerBatteryReporter reporter =
                new ControllerBatteryReporter(source, sink);
        reporter.report();

        source.sample = new ControllerBatterySample(
                BatteryManager.BATTERY_STATUS_CHARGING,
                0.5f);
        reporter.report();

        assertEquals(2, sink.count);
        assertEquals(50, sink.percentage & 0xFF);
    }

    @Test
    public void invalidStatusDoesNotPoisonDuplicateState() {
        MutableSource source = new MutableSource();
        RecordingSink sink = new RecordingSink();
        ControllerBatteryReporter reporter =
                new ControllerBatteryReporter(source, sink);
        source.sample = new ControllerBatterySample(
                Integer.MAX_VALUE,
                0.2f);
        reporter.report();

        source.sample = new ControllerBatterySample(
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                0.2f);
        reporter.report();

        assertEquals(1, sink.count);
    }

    private static final class MutableSource
            implements ControllerBatteryReporter.Source {
        ControllerBatterySample sample;

        @Override
        public ControllerBatterySample sample() {
            return sample;
        }
    }

    private static final class RecordingSink
            implements ControllerBatteryReporter.Sink {
        int count;
        byte percentage;

        @Override
        public void send(byte protocolState, byte percentage) {
            count++;
            this.percentage = percentage;
        }
    }
}
