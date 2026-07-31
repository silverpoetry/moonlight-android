package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class UsbControllerInputAdapterTest {
    @Test
    public void stateAppliesDeadzonesAndSendsOnce() {
        Fixture fixture = new Fixture();
        fixture.target.leftStickDeadzone = 0.20f;
        fixture.target.rightStickDeadzone = 0.10f;
        fixture.target.triggerDeadzone = 0.13f;

        fixture.adapter.handleState(
                fixture.target,
                0x1234,
                0.10f,
                0.10f,
                0.50f,
                -0.25f,
                0.13f,
                0.75f);

        ControllerInputState state = fixture.target.state;
        assertEquals(1, fixture.target.inputSendCount);
        assertEquals(0x1234, state.getInputMap());
        assertEquals(0, state.getLeftStickX());
        assertEquals(0, state.getLeftStickY());
        assertEquals((short) (0.50f * 0x7ffe),
                state.getRightStickX());
        assertEquals((short) (0.25f * 0x7ffe),
                state.getRightStickY());
        assertEquals(0, Byte.toUnsignedInt(state.getLeftTrigger()));
        assertEquals(
                (byte) (0.75f * 0xff),
                state.getRightTrigger());
    }

    @Test
    public void invalidAndOutOfRangeTriggersAreBounded() {
        Fixture fixture = new Fixture();

        fixture.adapter.handleState(
                fixture.target,
                0,
                Float.NaN,
                Float.NEGATIVE_INFINITY,
                2.0f,
                -2.0f,
                Float.NaN,
                2.0f);

        assertEquals(0, fixture.target.state.getLeftStickX());
        assertEquals(0, fixture.target.state.getLeftStickY());
        assertEquals(0x7ffe,
                fixture.target.state.getRightStickX());
        assertEquals(0x7ffe,
                fixture.target.state.getRightStickY());
        assertEquals(
                0,
                Byte.toUnsignedInt(
                        fixture.target.state.getLeftTrigger()));
        assertEquals(
                0xff,
                Byte.toUnsignedInt(
                        fixture.target.state.getRightTrigger()));
    }

    @Test
    public void motionAssignsSlotBeforeSending() {
        Fixture fixture = new Fixture();

        fixture.adapter.handleMotion(
                fixture.target,
                (byte) 2,
                1.5f,
                -2.5f,
                3.5f);

        assertEquals(
                Arrays.asList("assign", "motion"),
                fixture.events);
        assertEquals(4, fixture.output.controllerNumber);
        assertEquals(2, fixture.output.eventType);
        assertEquals(1.5f, fixture.output.x, 0);
        assertEquals(-2.5f, fixture.output.y, 0);
        assertEquals(3.5f, fixture.output.z, 0);
    }

    @Test
    public void touchAssignsSlotAndClampsEveryUnitCoordinate() {
        Fixture fixture = new Fixture();

        fixture.adapter.handleTouch(
                fixture.target,
                (byte) 3,
                17,
                -0.5f,
                1.5f,
                Float.POSITIVE_INFINITY);

        assertEquals(
                Arrays.asList("assign", "touch"),
                fixture.events);
        assertEquals(4, fixture.output.controllerNumber);
        assertEquals(3, fixture.output.eventType);
        assertEquals(17, fixture.output.pointerId);
        assertEquals(0, fixture.output.x, 0);
        assertEquals(1, fixture.output.y, 0);
        assertEquals(0, fixture.output.z, 0);
    }

    private static final class Fixture {
        final List<String> events = new ArrayList<>();
        final FakeTarget target = new FakeTarget(events);
        final CapturingOutput output = new CapturingOutput(events);
        final UsbControllerInputAdapter adapter =
                new UsbControllerInputAdapter(output);
    }

    private static final class FakeTarget
            implements UsbControllerInputAdapter.Target {
        final ControllerInputState state =
                new ControllerInputState();
        final List<String> events;
        float leftStickDeadzone;
        float rightStickDeadzone;
        float triggerDeadzone;
        int inputSendCount;

        FakeTarget(List<String> events) {
            this.events = events;
        }

        @Override
        public ControllerInputState getControllerInputState() {
            return state;
        }

        @Override
        public float getLeftStickDeadzoneRadius() {
            return leftStickDeadzone;
        }

        @Override
        public float getRightStickDeadzoneRadius() {
            return rightStickDeadzone;
        }

        @Override
        public float getTriggerDeadzone() {
            return triggerDeadzone;
        }

        @Override
        public byte ensureAssignedControllerNumber() {
            events.add("assign");
            return 4;
        }

        @Override
        public void sendControllerInput() {
            inputSendCount++;
        }
    }

    private static final class CapturingOutput
            implements UsbControllerInputAdapter.Output {
        private final List<String> events;
        byte controllerNumber;
        byte eventType;
        int pointerId;
        float x;
        float y;
        float z;

        CapturingOutput(List<String> events) {
            this.events = events;
        }

        @Override
        public void sendMotion(
                byte controllerNumber,
                byte motionType,
                float x,
                float y,
                float z) {
            events.add("motion");
            this.controllerNumber = controllerNumber;
            eventType = motionType;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void sendTouch(
                byte controllerNumber,
                byte eventType,
                int pointerId,
                float x,
                float y,
                float pressure) {
            events.add("touch");
            this.controllerNumber = controllerNumber;
            this.eventType = eventType;
            this.pointerId = pointerId;
            this.x = x;
            this.y = y;
            z = pressure;
        }
    }
}
