package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerInputReportAggregatorTest {
    @Test
    public void aggregatesAssignedMatchingSourcesAndCrossModeDefault() {
        FakeSource primary = source(
                true,
                (short) 2,
                false,
                false,
                0x01,
                0x20,
                0x10,
                100,
                -200,
                300,
                -400);
        FakeSource companion = source(
                true,
                (short) 2,
                false,
                false,
                0x02,
                0xf0,
                0x08,
                -500,
                100,
                200,
                -800);
        FakeSource crossModeDefault = source(
                true,
                (short) 2,
                true,
                true,
                0x04,
                0x30,
                0x40,
                250,
                -300,
                -600,
                500);
        FakeSource wrongSlot = source(
                true,
                (short) 3,
                false,
                false,
                0x10,
                0xff,
                0xff,
                Short.MAX_VALUE,
                Short.MAX_VALUE,
                Short.MAX_VALUE,
                Short.MAX_VALUE);
        FakeSource unassigned = source(
                false,
                (short) 2,
                false,
                false,
                0x20,
                0xff,
                0xff,
                Short.MIN_VALUE,
                Short.MIN_VALUE,
                Short.MIN_VALUE,
                Short.MIN_VALUE);
        CapturingOutput output = new CapturingOutput();

        ControllerInputReportAggregator.aggregateAndSend(
                sources(
                        primary,
                        companion,
                        crossModeDefault,
                        wrongSlot,
                        unassigned),
                (short) 2,
                false,
                output);

        assertEquals(1, output.sendCount);
        assertEquals(0x07, output.inputMap);
        assertEquals(0xf0, Byte.toUnsignedInt(output.leftTrigger));
        assertEquals(0x40, Byte.toUnsignedInt(output.rightTrigger));
        assertEquals(-500, output.leftStickX);
        assertEquals(-300, output.leftStickY);
        assertEquals(-600, output.rightStickX);
        assertEquals(-800, output.rightStickY);
    }

    @Test
    public void mouseModeOnlyAggregatesMatchingSources() {
        FakeSource controller = source(
                true,
                (short) 0,
                true,
                false,
                0x01,
                1,
                2,
                3,
                4,
                5,
                6);
        FakeSource ordinaryController = source(
                true,
                (short) 0,
                false,
                false,
                0x02,
                10,
                20,
                30,
                40,
                50,
                60);
        CapturingOutput output = new CapturingOutput();

        ControllerInputReportAggregator.aggregateAndSend(
                sources(controller, ordinaryController),
                (short) 0,
                true,
                output);

        assertEquals(0x01, output.inputMap);
        assertEquals(1, Byte.toUnsignedInt(output.leftTrigger));
        assertEquals(2, Byte.toUnsignedInt(output.rightTrigger));
        assertEquals(3, output.leftStickX);
        assertEquals(4, output.leftStickY);
        assertEquals(5, output.rightStickX);
        assertEquals(6, output.rightStickY);
    }

    @Test
    public void emptyMatchStillSendsNeutralReport() {
        CapturingOutput output = new CapturingOutput();

        ControllerInputReportAggregator.aggregateAndSend(
                sources(),
                (short) 0,
                false,
                output);

        assertEquals(1, output.sendCount);
        assertEquals(0, output.inputMap);
        assertEquals(0, output.leftTrigger);
        assertEquals(0, output.rightTrigger);
        assertEquals(0, output.leftStickX);
        assertEquals(0, output.leftStickY);
        assertEquals(0, output.rightStickX);
        assertEquals(0, output.rightStickY);
    }

    private static FakeSource source(
            boolean assigned,
            short controllerNumber,
            boolean mouseEmulationActive,
            boolean includedAcrossMouseModes,
            int inputMap,
            int leftTrigger,
            int rightTrigger,
            int leftStickX,
            int leftStickY,
            int rightStickX,
            int rightStickY) {
        ControllerInputState state = new ControllerInputState();
        state.replace(
                inputMap,
                (byte) leftTrigger,
                (byte) rightTrigger,
                (short) leftStickX,
                (short) leftStickY,
                (short) rightStickX,
                (short) rightStickY);
        return new FakeSource(
                assigned,
                controllerNumber,
                mouseEmulationActive,
                includedAcrossMouseModes,
                state);
    }

    private static ControllerInputReportAggregator.Sources sources(
            FakeSource... sources) {
        return new ControllerInputReportAggregator.Sources() {
            @Override
            public int size() {
                return sources.length;
            }

            @Override
            public ControllerInputReportAggregator.Source sourceAt(
                    int index) {
                return sources[index];
            }
        };
    }

    private static final class FakeSource
            implements ControllerInputReportAggregator.Source {
        private final boolean assigned;
        private final short controllerNumber;
        private final boolean mouseEmulationActive;
        private final boolean includedAcrossMouseModes;
        private final ControllerInputState state;

        FakeSource(
                boolean assigned,
                short controllerNumber,
                boolean mouseEmulationActive,
                boolean includedAcrossMouseModes,
                ControllerInputState state) {
            this.assigned = assigned;
            this.controllerNumber = controllerNumber;
            this.mouseEmulationActive = mouseEmulationActive;
            this.includedAcrossMouseModes = includedAcrossMouseModes;
            this.state = state;
        }

        @Override
        public boolean isAssigned() {
            return assigned;
        }

        @Override
        public short getControllerNumber() {
            return controllerNumber;
        }

        @Override
        public boolean isMouseEmulationActive() {
            return mouseEmulationActive;
        }

        @Override
        public boolean isIncludedAcrossMouseModes() {
            return includedAcrossMouseModes;
        }

        @Override
        public ControllerInputState getControllerInputState() {
            return state;
        }
    }

    private static final class CapturingOutput
            implements ControllerInputReportAggregator.Output {
        int sendCount;
        int inputMap;
        byte leftTrigger;
        byte rightTrigger;
        short leftStickX;
        short leftStickY;
        short rightStickX;
        short rightStickY;

        @Override
        public void send(
                int inputMap,
                byte leftTrigger,
                byte rightTrigger,
                short leftStickX,
                short leftStickY,
                short rightStickX,
                short rightStickY) {
            sendCount++;
            this.inputMap = inputMap;
            this.leftTrigger = leftTrigger;
            this.rightTrigger = rightTrigger;
            this.leftStickX = leftStickX;
            this.leftStickY = leftStickY;
            this.rightStickX = rightStickX;
            this.rightStickY = rightStickY;
        }
    }
}
