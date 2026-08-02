package org.cgutman.shieldcontrollerextensions;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.InputDevice;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class SceManager {
    private final Context context;

    private volatile IExposedControllerBinderWrapper binder;
    private boolean started;
    private boolean bound;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            IExposedControllerBinderWrapper connectedBinder =
                    new IExposedControllerBinderWrapper(iBinder);
            synchronized (SceManager.this) {
                if (!started) {
                    return;
                }
                binder = connectedBinder;
            }

            try {
                int registeredListenerId =
                        connectedBinder.registerListener(
                                controllerListener);
                synchronized (SceManager.this) {
                    if (started && binder == connectedBinder) {
                        listenerId = registeredListenerId;
                    } else {
                        connectedBinder.unregisterListener(
                                registeredListenerId);
                    }
                }
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to register SHIELD listener",
                        e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (SceManager.this) {
                binder = null;
                listenerId = 0;
            }

            clearDeviceState(null);
        }
    };

    // ConcurrentHashMap handles synchronization between the Binder thread adding/removing
    // entries and callers on arbitrary threads that are doing device lookups.
    //
    // Since these are separate maps, they can be temporarily inconsistent (only one-way
    // of the two-way mapping present). This is fine for our purposes here.
    private final ConcurrentHashMap<String, Integer> tokenToDeviceIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> deviceIdToTokenMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> activeRumbleTimerMap = new ConcurrentHashMap<>();

    private volatile int listenerId;
    private final IExposedControllerManagerListener.Stub controllerListener = new IExposedControllerManagerListener.Stub() {
        @Override
        public void onDeviceAdded(String controllerToken) {
            IExposedControllerBinderWrapper currentBinder = binder;
            if (currentBinder == null) {
                return;
            }
            try {
                int inputDeviceId = currentBinder.getInputDeviceId(
                        controllerToken);

                // If we don't have an input device ID yet, ignore this callback.
                // We will invoke it again in onDeviceChanged() when an ID is assigned.
                if (inputDeviceId < 0) {
                    return;
                }

                tokenToDeviceIdMap.put(controllerToken, inputDeviceId);
                deviceIdToTokenMap.put(inputDeviceId, controllerToken);
                SceDeviceListener currentListener = listener;
                if (currentListener != null) {
                    currentListener.onDeviceAdded(inputDeviceId);
                }
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to add SHIELD controller",
                        e);
            }
        }

        @Override
        public void onDeviceChanged(String controllerToken, int changeType) {
            IExposedControllerBinderWrapper currentBinder = binder;
            if (currentBinder == null) {
                return;
            }
            try {
                Integer inputDeviceId = tokenToDeviceIdMap.get(controllerToken);
                if (inputDeviceId == null) {
                    // If this is an input device changed, we will treat this as a device added
                    // when we're gaining an input device ID for the first time.
                    if (changeType == 7 &&
                            currentBinder.getInputDeviceId(
                                    controllerToken) >= 0) {
                        onDeviceAdded(controllerToken);
                    }
                    return;
                }

                // We handle input device ID changed internally to keep our mapping in sync.
                // NB: This can be called for a transition from a valid to invalid input device ID.
                int newInputDeviceId =
                        currentBinder.getInputDeviceId(controllerToken);
                if (changeType == 7) {
                    if (newInputDeviceId >= 0) {
                        deviceIdToTokenMap.remove(inputDeviceId);
                        tokenToDeviceIdMap.remove(controllerToken);
                        deviceIdToTokenMap.put(newInputDeviceId, controllerToken);
                        tokenToDeviceIdMap.put(controllerToken, newInputDeviceId);
                    }
                    else {
                        // Treat transition to -1 as a device removal. We don't notify this case
                        // as a normal input device ID change.
                        onDeviceRemoved(controllerToken);
                        return;
                    }
                }

                SceDeviceListener currentListener = listener;
                if (currentListener != null) {
                    switch (changeType) {
                        case 2:
                            currentListener.onBatteryPercentageChanged(inputDeviceId, currentBinder.getBatteryPercentage(controllerToken));
                            break;
                        case 3:
                            currentListener.onChargingStateChanged(inputDeviceId, currentBinder.getChargingState(controllerToken));
                            break;
                        case 4:
                            currentListener.onConnectionStateChanged(inputDeviceId, currentBinder.getConnectionState(controllerToken));
                            break;
                        case 5:
                            currentListener.onConnectionTypeChanged(inputDeviceId, currentBinder.getConnectionType(controllerToken));
                            break;
                        case 7:
                            currentListener.onInputDeviceIdChanged(inputDeviceId, newInputDeviceId);
                            break;
                        case 8:
                            currentListener.onNicknameChanged(inputDeviceId, currentBinder.getNickname(controllerToken));
                            break;
                        case 11:
                            currentListener.onHeadsetPresenceChanged(inputDeviceId, currentBinder.hasHeadset(controllerToken));
                            break;
                    }
                }
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to update SHIELD controller",
                        e);
            }
        }

        @Override
        public void onDeviceRemoved(String controllerToken) {
            Integer deviceId = tokenToDeviceIdMap.remove(controllerToken);
            if (deviceId != null) {
                deviceIdToTokenMap.remove(deviceId);

                Timer rumbleTimer = activeRumbleTimerMap.remove(controllerToken);
                if (rumbleTimer != null) {
                    rumbleTimer.cancel();
                }

                SceDeviceListener currentListener = listener;
                if (currentListener != null) {
                    currentListener.onDeviceRemoved(deviceId);
                }
            }
        }
    };

    private volatile SceDeviceListener listener;

    /**
     * Constructor for SceManager
     * @param context Context
     */
    public SceManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Sets or clears the optional device listener callbacks
     * @param listener Valid listener or null to clear
     */
    public void setDeviceListener(SceDeviceListener listener) {
        this.listener = listener;
    }

    /**
     * Determines if a given InputDevice can be used with SceManager APIs
     * @param device Input device to examine
     * @return true if the device is compatible with SceManager APIs, false otherwise
     */
    public boolean isRecognizedDevice(InputDevice device) {
        return getControllerToken(device) != null;
    }

    /**
     * Plays a rumble effect on the given InputDevice
     * @param device Input device to rumble
     * @param lowFreqMotor Value of the low frequency motor (0 - 65535)
     * @param highFreqMotor Value of the high frequency motor (0 - 65535)
     * @return true on success, false on failure
     */
    public boolean rumble(InputDevice device, final int lowFreqMotor, final int highFreqMotor) {
        final String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                Timer existingTimer = activeRumbleTimerMap.remove(controllerToken);
                if (existingTimer != null) {
                    existingTimer.cancel();
                }

                if (currentBinder.rumble(controllerToken, lowFreqMotor, highFreqMotor)) {
                    // NVIDIA only supports rumble for 1 second at a time, so we keep a timer to continue
                    // the rumbling until the caller explicitly stops it.
                    if (lowFreqMotor != 0 || highFreqMotor != 0) {
                        Timer timer = new Timer(true);
                        activeRumbleTimerMap.put(controllerToken, timer);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // If this timer was removed, return without rumbling
                                if (!activeRumbleTimerMap.containsKey(controllerToken)) {
                                    return;
                                }

                                try {
                                    IExposedControllerBinderWrapper timerBinder =
                                            binder;
                                    // Continue to rumble as long as it succeeds
                                    if (timerBinder != null &&
                                            timerBinder.rumble(
                                                    controllerToken,
                                                    lowFreqMotor,
                                                    highFreqMotor)) {
                                        return;
                                    }
                                } catch (RemoteException e) {
                                    ShieldControllerLog.warning(
                                            "Unable to continue SHIELD rumble",
                                            e);
                                }

                                // If we made it here, the rumble failed so cancel our timer.
                                Timer timer = activeRumbleTimerMap.remove(controllerToken);
                                if (timer != null) {
                                    timer.cancel();
                                }
                            }
                        }, 500, 500);
                    }

                    return true;
                }
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to rumble SHIELD controller",
                        e);
            }
        }
        return false;
    }

    /**
     * Gets the category (remote vs controller) of an InputDevice
     * @param device Input device to query
     * @return SceCategory
     */
    public SceCategory getCategory(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.getCategory(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to read SHIELD category",
                        e);
            }
        }
        return SceCategory.UNKNOWN;
    }

    /**
     * Gets the battery percentage of an InputDevice
     * @param device Input device to query
     * @return 0-100 or -1 on failure
     */
    public int getBatteryPercentage(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.getBatteryPercentage(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to read SHIELD battery",
                        e);
            }
        }
        return -1;
    }

    /**
     * Gets the current charging state (charging vs not charging) of an InputDevice
     * @param device Input device to query
     * @return SceChargingState
     */
    public SceChargingState getChargingState(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.getChargingState(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to read SHIELD charging state",
                        e);
            }
        }
        return SceChargingState.UNKNOWN;
    }

    /**
     * Gets the current connection state (connected vs disconnected) of an InputDevice
     * @param device Input device to query
     * @return SceConnectionState
     */
    public SceConnectionState getConnectionState(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.getConnectionState(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to read SHIELD connection state",
                        e);
            }
        }
        return SceConnectionState.UNKNOWN;
    }

    /**
     * Gets the current connection type (wired vs wireless) of an InputDevice
     * @param device Input device to query
     * @return SceConnectionType
     */
    public SceConnectionType getConnectionType(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.getConnectionType(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to read SHIELD connection type",
                        e);
            }
        }
        return SceConnectionType.UNKNOWN;
    }

    /**
     * Plays a haptic effect or other form of identification for an InputDevice
     * @param device Input device to identify
     * @return true on success, false otherwise
     */
    public boolean identify(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.identify(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to identify SHIELD controller",
                        e);
            }
        }
        return false;
    }

    /**
     * Queries if an InputDevice has a headset plugged in
     * @param device Input device to query
     * @return true if a headset is present, false otherwise
     */
    public boolean hasHeadset(InputDevice device) {
        String controllerToken = getControllerToken(device);
        IExposedControllerBinderWrapper currentBinder = binder;
        if (controllerToken != null && currentBinder != null) {
            try {
                return currentBinder.hasHeadset(controllerToken);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to read SHIELD headset state",
                        e);
            }
        }
        return false;
    }

    /**
     * Starts the SceManager. This must be called before functions other than setListener().
     *
     * Note: Starting is an asynchronous operation. It may not be possible to interact with
     * SceManager APIs until SceManager has bound to the SHIELD AccessoryService.
     *
     * If this function returns true, stop() must be called when you're finished.
     *
     * @return true if the SHIELD AccessoryService was available, false otherwise
     */
    public synchronized boolean start() {
        if (started) {
            return bound;
        }
        started = true;
        Intent intent = new Intent();
        intent.setClassName("com.nvidia.blakepairing", "com.nvidia.blakepairing.AccessoryService");
        try {
            bound = context.bindService(
                    intent,
                    serviceConnection,
                    Service.BIND_AUTO_CREATE);
            if (!bound) {
                started = false;
                return false;
            }

            return true;
        } catch (RuntimeException e) {
            started = false;
            bound = false;
            ShieldControllerLog.warning(
                    "Unable to bind SHIELD accessory service",
                    e);
            return false;
        }
    }

    private void clearDeviceState(
            IExposedControllerBinderWrapper activeBinder) {
        tokenToDeviceIdMap.clear();
        deviceIdToTokenMap.clear();

        for (Map.Entry<String, Timer> entry : activeRumbleTimerMap.entrySet()) {
            // Stop the repeating timer
            entry.getValue().cancel();

            // If our binder is still alive, send the rumble stop command
            if (activeBinder != null) {
                try {
                    activeBinder.rumble(entry.getKey(), 0, 0);
                } catch (RemoteException e) {
                    ShieldControllerLog.warning(
                            "Unable to stop SHIELD rumble",
                            e);
                }
            }
        }
        activeRumbleTimerMap.clear();
    }

    /**
     * Stops the SceManager. No further listener callbacks will be invoked.
     */
    public void stop() {
        IExposedControllerBinderWrapper currentBinder;
        int currentListenerId;
        boolean wasBound;
        synchronized (this) {
            if (!started && !bound) {
                return;
            }
            started = false;
            currentBinder = binder;
            currentListenerId = listenerId;
            wasBound = bound;
            listenerId = 0;
            binder = null;
            bound = false;
        }

        clearDeviceState(currentBinder);

        if (currentListenerId != 0 && currentBinder != null) {
            try {
                currentBinder.unregisterListener(currentListenerId);
            } catch (RemoteException e) {
                ShieldControllerLog.warning(
                        "Unable to unregister SHIELD listener",
                        e);
            }
        }

        if (wasBound) {
            try {
                context.unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                ShieldControllerLog.warning(
                        "SHIELD service was already unbound",
                        e);
            }
        }
    }

    private String getControllerToken(InputDevice device) {
        return deviceIdToTokenMap.get(device.getId());
    }

    public abstract static class SceDeviceListener {
        public void onDeviceAdded(int inputDeviceId) {}
        public void onDeviceRemoved(int inputDeviceId) {}

        public void onBatteryPercentageChanged(int inputDeviceId, int newBatteryPercentage) {}
        public void onChargingStateChanged(int inputDeviceId, SceChargingState newChargingState) {}
        public void onConnectionStateChanged(int inputDeviceId, SceConnectionState newConnectionState) {}
        public void onConnectionTypeChanged(int inputDeviceId, SceConnectionType newConnectionType) {}
        public void onInputDeviceIdChanged(int oldInputDeviceId, int newInputDeviceId) {}
        public void onNicknameChanged(int inputDeviceId, String newNickname) {}
        public void onHeadsetPresenceChanged(int inputDeviceId, boolean newHeadsetPresence) {}
    }
}
