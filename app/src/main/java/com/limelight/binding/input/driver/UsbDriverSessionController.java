package com.limelight.binding.input.driver;

import java.util.Objects;

/**
 * Owns one Activity's binding and callback lease on the USB driver service.
 *
 * <p>All methods are expected to be called on the Activity main thread, just
 * like Android's {@code ServiceConnection} callbacks.</p>
 */
public final class UsbDriverSessionController {
    public interface ServiceBinding {
        boolean bind();

        void unbind();
    }

    /**
     * A connected service endpoint whose callbacks can be leased by this
     * session and explicitly revoked before the input owner is destroyed.
     */
    public interface Endpoint {
        void activate();

        void deactivate();
    }

    private final ServiceBinding serviceBinding;

    private Endpoint endpoint;
    private boolean bindingRequested;
    private boolean bindingActive;
    private boolean connected;
    private boolean destroyed;

    public UsbDriverSessionController(
            ServiceBinding serviceBinding) {
        this.serviceBinding = Objects.requireNonNull(
                serviceBinding,
                "serviceBinding");
    }

    /**
     * Requests the service binding at most once for this session.
     *
     * @return whether Android accepted the binding request
     */
    public boolean bind() {
        if (destroyed || bindingRequested) {
            return bindingActive;
        }

        bindingRequested = true;

        final boolean accepted;
        try {
            accepted = serviceBinding.bind();
        } catch (RuntimeException error) {
            bindingRequested = false;
            throw error;
        }

        if (destroyed) {
            if (accepted) {
                serviceBinding.unbind();
            }
            return false;
        }

        bindingActive = accepted;
        if (!accepted) {
            bindingRequested = false;
            deactivateEndpoint();
        }
        return accepted;
    }

    /**
     * Activates callbacks on a newly connected service endpoint.
     */
    public void onConnected(Endpoint connectedEndpoint) {
        Objects.requireNonNull(connectedEndpoint, "connectedEndpoint");
        if (destroyed || !bindingRequested) {
            connectedEndpoint.deactivate();
            return;
        }

        deactivateEndpoint();
        endpoint = connectedEndpoint;
        try {
            endpoint.activate();
            connected = true;
        } catch (RuntimeException error) {
            deactivateEndpoint();
            throw error;
        }
    }

    /**
     * Drops a dead endpoint while retaining the Android binding lease so a
     * later reconnect callback can activate a replacement endpoint.
     */
    public void onDisconnected() {
        endpoint = null;
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Revokes callbacks before unbinding. This is terminal and idempotent.
     */
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;

        deactivateEndpoint();

        if (bindingActive) {
            bindingActive = false;
            serviceBinding.unbind();
        }
        bindingRequested = false;
    }

    private void deactivateEndpoint() {
        Endpoint previousEndpoint = endpoint;
        endpoint = null;
        connected = false;
        if (previousEndpoint != null) {
            previousEndpoint.deactivate();
        }
    }
}
