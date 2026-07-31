package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerMotionRegistrationsTest {
    @Test
    public void registrationUsesCurrentManagerAndSamplingPeriod() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);
        registrations.setManager("device");

        boolean wasActive = registrations.replace(
                (short) 3,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 200);

        assertFalse(wasActive);
        assertTrue(backend.events.contains(
                "listener:3:gyro:correct"));
        assertTrue(backend.events.contains(
                "register:device:gyroscope:5000"));
    }

    @Test
    public void replacementUnregistersOldManagerBeforeNewRegistration() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);
        registrations.setManager("controller");
        registrations.replace(
                (short) 1,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 50);
        registrations.setManager("device");

        boolean wasActive = registrations.replace(
                (short) 1,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 100);

        assertTrue(wasActive);
        assertBefore(
                backend.events,
                "unregister:controller:listener-1",
                "register:device:accelerometer:10000");
    }

    @Test
    public void zeroRateClearsRegistrationWithoutCreatingListener() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);
        registrations.setManager("controller");
        registrations.replace(
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 100);
        int eventCount = backend.events.size();

        boolean wasActive = registrations.replace(
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 0);

        assertTrue(wasActive);
        assertEquals(eventCount + 1, backend.events.size());
        assertTrue(backend.events.get(eventCount)
                .startsWith("unregister:"));
    }

    @Test
    public void missingSensorAndFailedRegistrationLeaveNoActiveLease() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);
        registrations.setManager("missing");
        registrations.replace(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 60);
        assertFalse(registrations.replace(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 0));

        registrations.setManager("reject");
        registrations.replace(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 60);
        assertFalse(registrations.replace(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 0));
    }

    @Test
    public void capabilitiesAndManagerIdentityFollowCurrentBinding() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);

        assertFalse(registrations.hasManager());
        assertFalse(registrations.hasSensor(
                ControllerMotionRegistrations.SensorKind.ACCELEROMETER));
        String manager = new String("device");
        registrations.setManager(manager);

        assertTrue(registrations.hasManager());
        assertTrue(registrations.usesManager(manager));
        assertFalse(registrations.usesManager(new String("device")));
        assertTrue(registrations.hasSensor(
                ControllerMotionRegistrations.SensorKind.GYROSCOPE));
    }

    @Test
    public void unsupportedMotionTypeDoesNotDisturbActiveRegistration() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);
        registrations.setManager("controller");
        registrations.replace(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 100);
        int eventCount = backend.events.size();

        assertFalse(registrations.replace(
                (short) 0,
                (byte) 99,
                (short) 0));
        assertEquals(eventCount, backend.events.size());
        assertTrue(registrations.replace(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 0));
    }

    @Test
    public void neutralGyroscopeUsesBackendSink() {
        RecordingBackend backend = new RecordingBackend();
        ControllerMotionRegistrations<String, String, String> registrations =
                new ControllerMotionRegistrations<>(backend);

        registrations.sendNeutralGyroscope((short) 5);

        assertTrue(backend.events.contains("neutral:5"));
    }

    private static void assertBefore(
            List<String> events,
            String first,
            String second) {
        assertTrue(events.indexOf(first) >= 0);
        assertTrue(events.indexOf(second) >= 0);
        assertTrue(events.indexOf(first) < events.indexOf(second));
    }

    private static final class RecordingBackend implements
            ControllerMotionRegistrations.Backend<
                    String,
                    String,
                    String> {
        final List<String> events = new ArrayList<>();
        int listenerNumber;

        @Override
        public String findSensor(
                String manager,
                ControllerMotionRegistrations.SensorKind kind) {
            if ("missing".equals(manager)) {
                return null;
            }
            return kind ==
                    ControllerMotionRegistrations.SensorKind.ACCELEROMETER
                    ? "accelerometer"
                    : "gyroscope";
        }

        @Override
        public String createListener(
                short controllerNumber,
                byte motionType,
                boolean needsDeviceOrientationCorrection) {
            String listener = "listener-" + (++listenerNumber);
            events.add(
                    "listener:" + controllerNumber + ":" +
                            (motionType == MoonBridge.LI_MOTION_TYPE_GYRO
                                    ? "gyro"
                                    : "accel") + ":" +
                            (needsDeviceOrientationCorrection
                                    ? "correct"
                                    : "raw"));
            return listener;
        }

        @Override
        public boolean register(
                String manager,
                String listener,
                String sensor,
                int samplingPeriodUs) {
            events.add(
                    "register:" + manager + ":" + sensor + ":" +
                            samplingPeriodUs);
            return !"reject".equals(manager);
        }

        @Override
        public void unregister(String manager, String listener) {
            events.add("unregister:" + manager + ":" + listener);
        }

        @Override
        public boolean needsDeviceOrientationCorrection(String manager) {
            return "device".equals(manager);
        }

        @Override
        public void sendNeutralGyroscope(short controllerNumber) {
            events.add("neutral:" + controllerNumber);
        }
    }
}
