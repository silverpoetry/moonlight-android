package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class UsbControllerLifecycleControllerTest {
    @Test
    public void newDeviceIsPublishedBeforePreparation() {
        Fixture fixture = new Fixture();
        Device device = new Device(7, "new");

        fixture.controller.onDeviceAdded(device);

        Context context = fixture.store.get(7);
        assertSame(device, context.device);
        assertEquals(
                list("create:new", "put:new", "prepare:new"),
                fixture.events);
    }

    @Test
    public void duplicateAddOfSameIdentityIsIdempotent() {
        Fixture fixture = new Fixture();
        Device device = new Device(7, "same");
        fixture.controller.onDeviceAdded(device);
        fixture.events.clear();

        fixture.controller.onDeviceAdded(device);

        assertTrue(fixture.events.isEmpty());
        assertSame(device, fixture.store.get(7).device);
    }

    @Test
    public void reusedIdRevokesAndDisposesOldContextFirst() {
        Fixture fixture = new Fixture();
        Device oldDevice = new Device(7, "old");
        Device replacement = new Device(7, "replacement");
        fixture.controller.onDeviceAdded(oldDevice);
        fixture.events.clear();

        fixture.controller.onDeviceAdded(replacement);

        assertEquals(
                list(
                        "remove:old",
                        "dispose:old:REPLACED",
                        "create:replacement",
                        "put:replacement",
                        "prepare:replacement"),
                fixture.events);
        assertSame(replacement, fixture.store.get(7).device);
    }

    @Test
    public void staleRemovalCannotDeleteReplacement() {
        Fixture fixture = new Fixture();
        Device oldDevice = new Device(7, "old");
        Device replacement = new Device(7, "replacement");
        fixture.controller.onDeviceAdded(oldDevice);
        fixture.controller.onDeviceAdded(replacement);
        fixture.events.clear();

        fixture.controller.onDeviceRemoved(oldDevice);

        assertTrue(fixture.events.isEmpty());
        assertSame(replacement, fixture.store.get(7).device);
    }

    @Test
    public void matchingRemovalRevokesRoutingBeforeDisposal() {
        Fixture fixture = new Fixture();
        Device device = new Device(7, "device");
        fixture.controller.onDeviceAdded(device);
        fixture.events.clear();

        fixture.controller.onDeviceRemoved(device);

        assertEquals(
                list(
                        "remove:device",
                        "dispose:device:REMOVED"),
                fixture.events);
        assertEquals(0, fixture.store.size());
    }

    @Test
    public void preparationFailureRollsBackPublishedContext() {
        Fixture fixture = new Fixture();
        fixture.delegate.failPreparation = true;

        try {
            fixture.controller.onDeviceAdded(
                    new Device(7, "broken"));
            fail("Expected preparation failure");
        }
        catch (IllegalStateException expected) {
            assertEquals("preparation failed", expected.getMessage());
        }

        assertEquals(
                list(
                        "create:broken",
                        "put:broken",
                        "prepare:broken",
                        "remove:broken",
                        "dispose:broken:PREPARATION_FAILED"),
                fixture.events);
        assertEquals(0, fixture.store.size());
    }

    @Test
    public void destroyRevokesAndDisposesEveryContextOnce() {
        Fixture fixture = new Fixture();
        fixture.controller.onDeviceAdded(new Device(1, "one"));
        fixture.controller.onDeviceAdded(new Device(2, "two"));
        fixture.events.clear();

        fixture.controller.destroy();
        fixture.controller.destroy();
        fixture.controller.onDeviceAdded(new Device(3, "late"));

        assertEquals(
                list(
                        "remove:two",
                        "dispose:two:SHUTDOWN",
                        "remove:one",
                        "dispose:one:SHUTDOWN"),
                fixture.events);
        assertEquals(0, fixture.store.size());
    }

    private static List<String> list(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private static final class Fixture {
        final List<String> events = new ArrayList<>();
        final FakeStore store = new FakeStore(events);
        final FakeDelegate delegate =
                new FakeDelegate(store, events);
        final UsbControllerLifecycleController<Device, Context>
                controller =
                new UsbControllerLifecycleController<>(
                        store,
                        delegate);
    }

    private static final class Device {
        final int id;
        final String name;

        Device(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class Context {
        final Device device;

        Context(Device device) {
            this.device = device;
        }
    }

    private static final class FakeStore
            implements UsbControllerLifecycleController.Store<Context> {
        private final Map<Integer, Context> contexts =
                new LinkedHashMap<>();
        private final List<String> events;

        FakeStore(List<String> events) {
            this.events = events;
        }

        @Override
        public Context get(int controllerId) {
            return contexts.get(controllerId);
        }

        @Override
        public void put(int controllerId, Context context) {
            contexts.put(controllerId, context);
            events.add("put:" + context.device.name);
        }

        @Override
        public void remove(int controllerId) {
            Context removed = contexts.remove(controllerId);
            events.add("remove:" + removed.device.name);
        }

        @Override
        public int size() {
            return contexts.size();
        }

        @Override
        public Context valueAt(int index) {
            return new ArrayList<>(contexts.values()).get(index);
        }
    }

    private static final class FakeDelegate
            implements UsbControllerLifecycleController.Delegate<
                    Device,
                    Context> {
        private final FakeStore store;
        private final List<String> events;
        boolean failPreparation;

        FakeDelegate(
                FakeStore store,
                List<String> events) {
            this.store = store;
            this.events = events;
        }

        @Override
        public int getControllerId(Device device) {
            return device.id;
        }

        @Override
        public Device getDevice(Context context) {
            return context.device;
        }

        @Override
        public Context createContext(Device device) {
            events.add("create:" + device.name);
            return new Context(device);
        }

        @Override
        public void preparePublishedDevice(Device device) {
            assertSame(device, store.get(device.id).device);
            events.add("prepare:" + device.name);
            if (failPreparation) {
                throw new IllegalStateException(
                        "preparation failed");
            }
        }

        @Override
        public void disposeContext(
                Context context,
                UsbControllerLifecycleController.RemovalReason reason) {
            assertTrue(store.get(context.device.id) != context);
            events.add(
                    "dispose:" + context.device.name + ":" + reason);
        }
    }
}
