package com.limelight.nvstream.mdns;

import androidx.annotation.RequiresApi;
import android.content.Context;
import android.net.nsd.DiscoveryRequest;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import com.limelight.LimeLog;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class NsdManagerDiscoveryAgent extends MdnsDiscoveryAgent {
    private static final String SERVICE_TYPE = "_nvstream._tcp";
    private static final int CALLBACK_QUEUE_CAPACITY = 64;
    private static final long CALLBACK_THREAD_IDLE_SECONDS = 30;
    private final NsdManager nsdManager;
    private final Object listenerLock = new Object();
    private NsdManager.DiscoveryListener pendingListener;
    private NsdManager.DiscoveryListener activeListener;
    private final HashMap<String, NsdManager.ServiceInfoCallback> serviceCallbacks = new HashMap<>();
    private final ThreadPoolExecutor callbackExecutor =
            createCallbackExecutor();
    private long discoveryGeneration;

    private NsdManager.DiscoveryListener createDiscoveryListener(
            long generation) {
        return new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                LimeLog.severe("NSD: Service discovery start failed: " + errorCode);

                // This listener is no longer pending after this failure
                synchronized (listenerLock) {
                    if (pendingListener != this) {
                        return;
                    }

                    pendingListener = null;
                }

                listener.notifyDiscoveryFailure(new RuntimeException("onStartDiscoveryFailed(): " + errorCode));
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                LimeLog.severe("NSD: Service discovery stop failed: " + errorCode);

                // This listener is no longer active after this failure
                synchronized (listenerLock) {
                    if (activeListener != this) {
                        return;
                    }

                    activeListener = null;
                }
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                LimeLog.info("NSD: Service discovery started");

                synchronized (listenerLock) {
                    if (pendingListener != this) {
                        // If we registered another discovery listener in the meantime, stop this one
                        nsdManager.stopServiceDiscovery(this);
                        return;
                    }

                    pendingListener = null;
                    activeListener = this;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                LimeLog.info("NSD: Service discovery stopped");

                synchronized (listenerLock) {
                    if (activeListener != this) {
                        return;
                    }

                    activeListener = null;
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                // Protect against racing stopDiscovery() call
                synchronized (listenerLock) {
                    // Ignore callbacks if we're not the active listener
                    if (activeListener != this) {
                        return;
                    }

                    String serviceName = nsdServiceInfo.getServiceName();
                    LimeLog.info("NSD: Machine appeared: " + serviceName);

                    NsdManager.ServiceInfoCallback serviceInfoCallback = new NsdManager.ServiceInfoCallback() {
                        @Override
                        public void onServiceInfoCallbackRegistrationFailed(int errorCode) {
                            if (!removeCurrentServiceCallback(
                                    serviceName,
                                    this,
                                    generation)) {
                                return;
                            }
                            LimeLog.severe("NSD: Service info callback registration failed: " + errorCode);
                            listener.notifyDiscoveryFailure(new RuntimeException("onServiceInfoCallbackRegistrationFailed(): " + errorCode));
                        }

                        @Override
                        public void onServiceUpdated(NsdServiceInfo nsdServiceInfo) {
                            if (!isCurrentServiceCallback(
                                    serviceName,
                                    this,
                                    generation)) {
                                return;
                            }
                            LimeLog.info("NSD: Machine resolved: " + nsdServiceInfo.getServiceName());
                            reportNewComputer(nsdServiceInfo.getServiceName(), nsdServiceInfo.getPort(),
                                    getV4Addrs(nsdServiceInfo.getHostAddresses()),
                                    getV6Addrs(nsdServiceInfo.getHostAddresses()));
                        }

                        @Override
                        public void onServiceLost() {
                        }

                        @Override
                        public void onServiceInfoCallbackUnregistered() {
                        }
                    };

                    NsdManager.ServiceInfoCallback previousCallback =
                            serviceCallbacks.put(
                                    serviceName,
                                    serviceInfoCallback);
                    try {
                        nsdManager.registerServiceInfoCallback(
                                nsdServiceInfo,
                                callbackExecutor,
                                serviceInfoCallback);
                    }
                    catch (RuntimeException error) {
                        if (previousCallback == null) {
                            serviceCallbacks.remove(
                                    serviceName,
                                    serviceInfoCallback);
                        }
                        else {
                            serviceCallbacks.put(
                                    serviceName,
                                    previousCallback);
                        }
                        throw error;
                    }
                    if (previousCallback != null) {
                        nsdManager.unregisterServiceInfoCallback(
                                previousCallback);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                // Protect against racing stopDiscovery() call
                synchronized (listenerLock) {
                    // Ignore callbacks if we're not the active listener
                    if (activeListener != this) {
                        return;
                    }

                    LimeLog.info("NSD: Machine lost: " + nsdServiceInfo.getServiceName());

                    NsdManager.ServiceInfoCallback serviceInfoCallback = serviceCallbacks.remove(nsdServiceInfo.getServiceName());
                    if (serviceInfoCallback != null) {
                        nsdManager.unregisterServiceInfoCallback(serviceInfoCallback);
                    }
                }
            }
        };
    }

    public NsdManagerDiscoveryAgent(Context context, MdnsDiscoveryListener listener) {
        super(listener);
        this.nsdManager = context.getSystemService(NsdManager.class);
    }

    @Override
    public void startDiscovery(int discoveryIntervalMs) {
        synchronized (listenerLock) {
            // Register a new service discovery listener if there's not already one starting or running
            if (pendingListener == null && activeListener == null) {
                long generation = ++discoveryGeneration;
                pendingListener = createDiscoveryListener(generation);
                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.CINNAMON_BUN) {
                    DiscoveryRequest request =
                            new DiscoveryRequest.Builder(SERVICE_TYPE)
                                    .setFlags(
                                            DiscoveryRequest
                                                    .FLAG_NO_PICKER)
                                    .build();
                    nsdManager.discoverServices(
                            request,
                            callbackExecutor,
                            pendingListener);
                }
                else {
                    nsdManager.discoverServices(
                            SERVICE_TYPE,
                            NsdManager.PROTOCOL_DNS_SD,
                            pendingListener);
                }
            }
        }
    }

    @Override
    public void stopDiscovery() {
        // Protect against racing ServiceInfoCallback and DiscoveryListener callbacks
        synchronized (listenerLock) {
            discoveryGeneration++;

            // Clear any pending listener to ensure the discoverStarted() callback
            // will realize it's gone and stop itself.
            pendingListener = null;

            // Unregister the service discovery listener
            if (activeListener != null) {
                nsdManager.stopServiceDiscovery(activeListener);

                // Even though listener stoppage is asynchronous, the listener is gone as far as
                // we're concerned. We null this right now to ensure pending callbacks know it's
                // stopped and startDiscovery() can immediately create a new listener. If we left
                // it until onDiscoveryStopped() was called, startDiscovery() would get confused
                // and assume a listener was already running, even though it's stopping.
                activeListener = null;
            }

            // Unregister all service info callbacks
            for (NsdManager.ServiceInfoCallback callback : serviceCallbacks.values()) {
                nsdManager.unregisterServiceInfoCallback(callback);
            }
            serviceCallbacks.clear();
        }
    }

    private boolean isCurrentServiceCallback(
            String serviceName,
            NsdManager.ServiceInfoCallback callback,
            long generation) {
        synchronized (listenerLock) {
            return generation == discoveryGeneration &&
                    activeListener != null &&
                    serviceCallbacks.get(serviceName) == callback;
        }
    }

    private boolean removeCurrentServiceCallback(
            String serviceName,
            NsdManager.ServiceInfoCallback callback,
            long generation) {
        synchronized (listenerLock) {
            if (!isCurrentServiceCallback(
                    serviceName,
                    callback,
                    generation)) {
                return false;
            }
            serviceCallbacks.remove(serviceName);
            return true;
        }
    }

    private static ThreadPoolExecutor createCallbackExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                CALLBACK_THREAD_IDLE_SECONDS,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(CALLBACK_QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "NsdServiceCallbacks");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static Inet4Address[] getV4Addrs(List<InetAddress> addrs) {
        int matchCount = 0;
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet4Address) {
                matchCount++;
            }
        }

        Inet4Address[] matching = new Inet4Address[matchCount];

        int i = 0;
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet4Address) {
                matching[i++] = (Inet4Address) addr;
            }
        }

        return matching;
    }

    private static Inet6Address[] getV6Addrs(List<InetAddress> addrs) {
        int matchCount = 0;
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet6Address) {
                matchCount++;
            }
        }

        Inet6Address[] matching = new Inet6Address[matchCount];

        int i = 0;
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet6Address) {
                matching[i++] = (Inet6Address) addr;
            }
        }

        return matching;
    }
}
