package com.limelight.binding.input;

import java.util.Objects;

/**
 * Owns identity-safe publication and teardown of USB controller contexts.
 *
 * <p>A controller ID may be reused. Removal therefore requires both the ID and
 * the exact device identity, preventing a late callback from an old device
 * from tearing down its replacement.</p>
 */
final class UsbControllerLifecycleController<DeviceT, ContextT> {
    enum RemovalReason {
        REPLACED,
        REMOVED,
        PREPARATION_FAILED,
        SHUTDOWN
    }

    interface Store<ContextT> {
        ContextT get(int controllerId);

        void put(int controllerId, ContextT context);

        void remove(int controllerId);

        int size();

        ContextT valueAt(int index);
    }

    interface Delegate<DeviceT, ContextT> {
        int getControllerId(DeviceT device);

        DeviceT getDevice(ContextT context);

        ContextT createContext(DeviceT device);

        void preparePublishedDevice(DeviceT device);

        void disposeContext(
                ContextT context,
                RemovalReason reason);
    }

    private final Store<ContextT> store;
    private final Delegate<DeviceT, ContextT> delegate;
    private boolean destroyed;

    UsbControllerLifecycleController(
            Store<ContextT> store,
            Delegate<DeviceT, ContextT> delegate) {
        this.store = Objects.requireNonNull(store, "store");
        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate");
    }

    void onDeviceAdded(DeviceT device) {
        Objects.requireNonNull(device, "device");
        if (destroyed) {
            return;
        }

        int controllerId = delegate.getControllerId(device);
        ContextT existingContext = store.get(controllerId);
        if (existingContext != null) {
            if (delegate.getDevice(existingContext) == device) {
                return;
            }
            // Revoke routing before teardown in case the old driver emits a
            // final synchronous callback while stopping.
            store.remove(controllerId);
            delegate.disposeContext(
                    existingContext,
                    RemovalReason.REPLACED);
        }

        ContextT context = Objects.requireNonNull(
                delegate.createContext(device),
                "created context");
        // Publish before optional preparation so synchronous callbacks from
        // preparation can already resolve their context.
        store.put(controllerId, context);
        try {
            delegate.preparePublishedDevice(device);
        }
        catch (RuntimeException | Error failure) {
            store.remove(controllerId);
            delegate.disposeContext(
                    context,
                    RemovalReason.PREPARATION_FAILED);
            throw failure;
        }
    }

    void onDeviceRemoved(DeviceT device) {
        Objects.requireNonNull(device, "device");
        if (destroyed) {
            return;
        }

        int controllerId = delegate.getControllerId(device);
        ContextT context = store.get(controllerId);
        if (context == null ||
                delegate.getDevice(context) != device) {
            return;
        }

        store.remove(controllerId);
        delegate.disposeContext(
                context,
                RemovalReason.REMOVED);
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;

        while (store.size() > 0) {
            ContextT context = store.valueAt(store.size() - 1);
            int controllerId = delegate.getControllerId(
                    delegate.getDevice(context));
            store.remove(controllerId);
            delegate.disposeContext(
                    context,
                    RemovalReason.SHUTDOWN);
        }
    }
}
