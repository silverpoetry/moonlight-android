package com.limelight.binding.input;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges Android components that cannot receive constructor dependencies to
 * the input gateway of the currently started stream Activity.
 *
 * <p>The registry never owns the Activity: gateways are weakly referenced and
 * registrations are explicitly closed with the Activity lifecycle. A stale
 * registration cannot clear a newer one.</p>
 */
public final class StreamInputGatewayRegistry {
    private static final StreamInputGatewayRegistry INSTANCE =
            new StreamInputGatewayRegistry();

    private final AtomicReference<Entry> activeEntry = new AtomicReference<>();

    StreamInputGatewayRegistry() {
    }

    public static StreamInputGatewayRegistry getInstance() {
        return INSTANCE;
    }

    public Registration register(StreamInputGateway gateway) {
        Entry entry = new Entry(Objects.requireNonNull(gateway, "gateway"));
        activeEntry.set(entry);
        return () -> activeEntry.compareAndSet(entry, null);
    }

    public StreamInputGateway getActiveGateway() {
        Entry entry = activeEntry.get();
        if (entry == null) {
            return null;
        }

        StreamInputGateway gateway = entry.gatewayReference.get();
        if (gateway == null) {
            activeEntry.compareAndSet(entry, null);
        }
        return gateway;
    }

    private static final class Entry {
        private final WeakReference<StreamInputGateway> gatewayReference;

        private Entry(StreamInputGateway gateway) {
            gatewayReference = new WeakReference<>(gateway);
        }
    }

    public interface Registration {
        void unregister();
    }
}
