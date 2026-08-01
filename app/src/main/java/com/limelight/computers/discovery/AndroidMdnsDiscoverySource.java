package com.limelight.computers.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.discovery.DiscoveryService;
import com.limelight.nvstream.mdns.MdnsComputer;
import com.limelight.nvstream.mdns.MdnsDiscoveryListener;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Android service adapter for mDNS endpoint discovery. */
public final class AndroidMdnsDiscoverySource
        implements HostDiscoverySource, ServiceConnection {
    private final Context context;
    private final Listener listener;
    private final Object stateLock = new Object();

    private DiscoveryService.DiscoveryBinder binder;
    private boolean bound;
    private boolean closed;

    public AndroidMdnsDiscoverySource(
            Context context,
            Listener listener) {
        this.context = Objects.requireNonNull(
                        context,
                        "context")
                .getApplicationContext();
        this.listener = Objects.requireNonNull(listener, "listener");
        bound = this.context.bindService(
                new Intent(this.context, DiscoveryService.class),
                this,
                Context.BIND_AUTO_CREATE);
        if (!bound) {
            throw new IllegalStateException(
                    "Unable to bind host discovery service");
        }
    }

    @Override
    public void onServiceConnected(
            ComponentName componentName,
            IBinder service) {
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            binder = (DiscoveryService.DiscoveryBinder) service;
            binder.setListener(createListener());
            stateLock.notifyAll();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        synchronized (stateLock) {
            binder = null;
            stateLock.notifyAll();
        }
    }

    @Override
    public void awaitReady() throws InterruptedException {
        synchronized (stateLock) {
            while (binder == null && !closed) {
                stateLock.wait();
            }
            if (closed) {
                throw new IllegalStateException(
                        "Host discovery source is closed");
            }
        }
    }

    @Override
    public void start(int queryPeriodMs) {
        if (queryPeriodMs <= 0) {
            throw new IllegalArgumentException(
                    "Invalid discovery query period");
        }
        synchronized (stateLock) {
            requireBinder().startDiscovery(queryPeriodMs);
        }
    }

    @Override
    public void stop() {
        synchronized (stateLock) {
            if (binder != null) {
                binder.stopDiscovery();
            }
        }
    }

    @Override
    public void close() {
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
            if (binder != null) {
                binder.stopDiscovery();
                binder.setListener(null);
                binder = null;
            }
            stateLock.notifyAll();
        }
        if (bound) {
            context.unbindService(this);
            bound = false;
        }
    }

    private DiscoveryService.DiscoveryBinder requireBinder() {
        if (closed) {
            throw new IllegalStateException(
                    "Host discovery source is closed");
        }
        if (binder == null) {
            throw new IllegalStateException(
                    "Host discovery source is not ready");
        }
        return binder;
    }

    private MdnsDiscoveryListener createListener() {
        return new MdnsDiscoveryListener() {
            @Override
            public void notifyComputerAdded(MdnsComputer computer) {
                List<HostEndpoint> endpoints = new ArrayList<>(2);
                if (computer.getLocalAddress() != null) {
                    HostEndpoint.Kind kind =
                            computer.getLocalAddress() instanceof Inet4Address
                                    ? HostEndpoint.Kind.LOCAL_IPV4
                                    : HostEndpoint.Kind.LOCAL_IPV6;
                    endpoints.add(new HostEndpoint(
                            kind,
                            computer.getLocalAddress().getHostAddress(),
                            computer.getPort()));
                }
                if (computer.getIpv6Address() != null &&
                        !hasIpv6Endpoint(endpoints)) {
                    endpoints.add(new HostEndpoint(
                            HostEndpoint.Kind.LOCAL_IPV6,
                            computer.getIpv6Address().getHostAddress(),
                            computer.getPort()));
                }
                if (!endpoints.isEmpty()) {
                    listener.onHostDiscovered(
                            new HostDiscoveryCandidate(endpoints));
                }
            }

            @Override
            public void notifyDiscoveryFailure(Exception error) {
                listener.onDiscoveryFailure();
            }
        };
    }

    private static boolean hasIpv6Endpoint(
            List<HostEndpoint> endpoints) {
        for (HostEndpoint endpoint : endpoints) {
            if (endpoint.getKind() == HostEndpoint.Kind.LOCAL_IPV6) {
                return true;
            }
        }
        return false;
    }
}
