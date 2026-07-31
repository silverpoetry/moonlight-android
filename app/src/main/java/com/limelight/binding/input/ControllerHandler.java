package com.limelight.binding.input;

import androidx.annotation.RequiresApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibratorManager;
import com.limelight.DebugLog;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.limelight.utils.UiToast;

import com.google.gson.Gson;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.driver.AbstractController;
import com.limelight.binding.input.driver.DualSenseController;
import com.limelight.binding.input.driver.RazerKishiHapticsDevice;
import com.limelight.binding.input.driver.UsbDriverListener;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.protocol.NvConnectionKeyboardInputSink;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.ui.GameGestures;
import com.limelight.utils.Vector2d;

import org.cgutman.shieldcontrollerextensions.SceChargingState;
import org.cgutman.shieldcontrollerextensions.SceConnectionType;
import org.cgutman.shieldcontrollerextensions.SceManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class ControllerHandler implements InputManager.InputDeviceListener,
        UsbDriverListener, GamepadInputHandler,
        StreamInputLifecycleController.ControllerDevices {
    private static final String KISHI_LOG_TAG = "RazerKishiDebug";

    private static final int MAXIMUM_BUMPER_UP_DELAY_MS = 100;

    private static final int START_DOWN_TIME_MOUSE_MODE_MS = 750;

    private static final int MINIMUM_BUTTON_DOWN_TIME_MS = 25;

    private static final int EMULATING_SPECIAL = 0x1;
    private static final int EMULATING_SELECT = 0x2;
    private static final int EMULATING_TOUCHPAD = 0x4;

    private static final int BATTERY_RECHECK_INTERVAL_MS = 120 * 1000;

    private final Vector2d inputVector = new Vector2d();

    private final SparseArray<InputDeviceContext> inputDeviceContexts = new SparseArray<>();
    private final SparseArray<UsbDeviceContext> usbDeviceContexts = new SparseArray<>();
    private final SparseArray<RazerKishiHapticsDevice> razerKishiHapticsDevices = new SparseArray<>();
    private final AtomicIntegerArray controllerLeftTriggerStates =
            new AtomicIntegerArray(ControllerSlotAllocator.MAX_SLOTS);

    private final NvConnection conn;
    private final KeyboardInputSink keyboardInputSink;
    private final ControllerMouseEmulationTranslator.Output
            mouseEmulationOutput;
    private final Activity activityContext;
    private final double stickDeadzone;
    private final InputDeviceContext defaultContext;
    private final GameGestures gestures;
    private final InputManager inputManager;
    private final UsbManager usbManager;
    private final ControllerVibrationRenderer vibrationRenderer;
    private final SensorManager deviceSensorManager;
    private final SceManager sceManager;
    private final Handler mainThreadHandler;
    private final ControllerMouseEmulationSession.Scheduler
            mouseEmulationScheduler;
    private final ControllerMotionSession.Scheduler
            motionSensorScheduler;
    private final HandlerThread backgroundHandlerThread;
    private final Handler backgroundThreadHandler;
    private long lastRazerKishiRefreshTimeMs;
    private boolean hasGameController;
    private boolean stopped = false;

    private final ControllerSettingsState settingsState;
    private final StreamAudioSettingsState audioSettingsState;
    private final ControllerSlotAllocator slotAllocator;

    private boolean shouldUseControllerAudioHaptics() {
        return ControllerHapticsPolicy
                .shouldUseAudioHaptics(
                        audioSettingsState.get());
    }

    private boolean shouldSuppressControllerRumble() {
        return ControllerHapticsPolicy
                .shouldSuppressStandardRumble(
                        audioSettingsState.get(),
                        false,
                        false);
    }

    private boolean shouldSuppressInputDeviceRumble(InputDeviceContext context) {
        StreamAudioSettings settings = audioSettingsState.get();
        boolean selectiveSuppression =
                RazerKishiHapticsDevice.isFeatureEnabled();
        boolean deviceReceivesAudioHaptics =
                selectiveSuppression &&
                        RazerKishiHapticsDevice.canUseDevice(
                                context.vendorId,
                                context.productId,
                                context.name);
        return ControllerHapticsPolicy
                .shouldSuppressStandardRumble(
                        settings,
                        selectiveSuppression,
                        deviceReceivesAudioHaptics);
    }

    private void maybeRefreshRazerKishiHapticsState() {
        if (!RazerKishiHapticsDevice.isFeatureEnabled()) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now - lastRazerKishiRefreshTimeMs < 1500) {
            return;
        }

        refreshRazerKishiHapticsState();
    }

    private void refreshRazerKishiHapticsState() {
        if (!RazerKishiHapticsDevice.isFeatureEnabled()) {
            stopRazerKishiHapticsDevices();
            return;
        }

        lastRazerKishiRefreshTimeMs = SystemClock.uptimeMillis();

        if (usbManager == null) {
            stopRazerKishiHapticsDevices();
            return;
        }

        boolean enable = shouldUseControllerAudioHaptics();
        ArrayList<Integer> activeIds = new ArrayList<>();

        if (enable) {
            for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
                if (!RazerKishiHapticsDevice.canUseDevice(usbDevice)) {
                    continue;
                }

                DebugLog.debug(KISHI_LOG_TAG, "refreshRazerKishiHapticsState matched device: vid=0x" +
                        Integer.toHexString(usbDevice.getVendorId()) + " pid=0x" +
                        Integer.toHexString(usbDevice.getProductId()) + " name=" +
                        usbDevice.getProductName() + " permission=" + usbManager.hasPermission(usbDevice));

                int deviceId = usbDevice.getDeviceId();
                activeIds.add(deviceId);

                RazerKishiHapticsDevice existing = razerKishiHapticsDevices.get(deviceId);
                if (existing != null && existing.isStarted()) {
                    DebugLog.debug(KISHI_LOG_TAG, "Kishi haptics already active for deviceId=" + deviceId);
                    continue;
                }

                if (!usbManager.hasPermission(usbDevice)) {
                    DebugLog.warning(KISHI_LOG_TAG, "Kishi device missing USB permission: deviceId=" + deviceId);
                    continue;
                }

                android.hardware.usb.UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
                if (connection == null) {
                    DebugLog.error(KISHI_LOG_TAG, "openDevice failed for Kishi deviceId=" + deviceId);
                    continue;
                }

                RazerKishiHapticsDevice hapticsDevice = new RazerKishiHapticsDevice(usbDevice, connection);
                if (hapticsDevice.start()) {
                    DebugLog.info(KISHI_LOG_TAG, "Kishi haptics sidecar started for deviceId=" + deviceId);
                    razerKishiHapticsDevices.put(deviceId, hapticsDevice);
                }
                else {
                    DebugLog.error(KISHI_LOG_TAG, "Kishi haptics sidecar failed to start for deviceId=" + deviceId);
                    hapticsDevice.stop();
                }
            }
        }

        for (int i = razerKishiHapticsDevices.size() - 1; i >= 0; i--) {
            int deviceId = razerKishiHapticsDevices.keyAt(i);
            if (!enable || !activeIds.contains(deviceId)) {
                DebugLog.info(KISHI_LOG_TAG, "Removing Kishi haptics sidecar for deviceId=" + deviceId);
                RazerKishiHapticsDevice device = razerKishiHapticsDevices.valueAt(i);
                device.stop();
                razerKishiHapticsDevices.removeAt(i);
            }
        }
    }

    private void stopRazerKishiHapticsDevices() {
        for (int i = razerKishiHapticsDevices.size() - 1; i >= 0; i--) {
            razerKishiHapticsDevices.valueAt(i).stop();
            razerKishiHapticsDevices.removeAt(i);
        }
    }

    private boolean rumbleInputDeviceContext(InputDeviceContext deviceContext,
                                              short lowFreqMotor, short highFreqMotor) {
        return vibrationRenderer.rumble(
                deviceContext.vibrationTarget,
                deviceContext.inputDevice,
                lowFreqMotor,
                highFreqMotor);
    }

    public boolean handleStandardControllerAudioHaptics(short lowFreqMotor, short highFreqMotor) {
        if (stopped || !shouldUseControllerAudioHaptics()) {
            return false;
        }

        boolean vibrated = false;

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);
            vibrated |= rumbleInputDeviceContext(deviceContext, lowFreqMotor, highFreqMotor);
        }

        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);
            if (!deviceContext.device.isAdvancedAudioHapticsActive()) {
                deviceContext.device.rumble(lowFreqMotor, highFreqMotor);
                vibrated = true;
            }
        }

        return vibrated;
    }

    public boolean handleControllerAdvancedAudioHapticsFrame(byte[] frame, float intensityGain) {
        if (stopped || !shouldUseControllerAudioHaptics() || frame == null || frame.length == 0) {
            return false;
        }

        boolean submitted = false;
        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);
            if (deviceContext.device.isAdvancedAudioHapticsActive()) {
                submitted |= deviceContext.device.submitAdvancedAudioHapticsFrame(frame, intensityGain);
            }
        }

        return submitted;
    }

    public boolean handleRazerKishiAudioHapticsFrame(byte[] frame, float intensityGain) {
        if (!RazerKishiHapticsDevice.isFeatureEnabled()) {
            return false;
        }

        if (stopped || !shouldUseControllerAudioHaptics() || frame == null || frame.length == 0) {
            if (DebugLog.isEnabled()) {
                DebugLog.debug(KISHI_LOG_TAG,
                        "handleRazerKishiAudioHapticsFrame skipped: stopped=" + stopped +
                                " useControllerAudio=" + shouldUseControllerAudioHaptics() +
                                " frameLength=" + (frame == null ? -1 : frame.length));
            }
            return false;
        }

        if (razerKishiHapticsDevices.size() == 0) {
            if (DebugLog.isEnabled()) {
                DebugLog.debug(KISHI_LOG_TAG, "No active Kishi sidecars, refreshing before submit");
            }
            maybeRefreshRazerKishiHapticsState();
        }

        boolean submitted = false;
        for (int i = 0; i < razerKishiHapticsDevices.size(); i++) {
            submitted |= razerKishiHapticsDevices.valueAt(i).submitFrame(frame, intensityGain);
        }

        if (!submitted) {
            if (DebugLog.isEnabled()) {
                DebugLog.warning(KISHI_LOG_TAG,
                        "Kishi audio haptics frame was not submitted to any sidecar");
            }
        }

        return submitted;
    }

    public void refreshAudioHapticsState() {
        if (stopped) {
            return;
        }

        boolean enableControllerAudioHaptics = shouldUseControllerAudioHaptics();
        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);
            AbstractController controller = deviceContext.device;
            if (!controller.hasAdvancedAudioHapticsSupport()) {
                continue;
            }

            if (enableControllerAudioHaptics) {
                controller.startAdvancedAudioHaptics();
            }
            else {
                controller.stopAdvancedAudioHaptics();
            }
        }

        refreshRazerKishiHapticsState();
    }

    public ControllerHandler(
            Activity activityContext,
            NvConnection conn,
            GameGestures gestures,
            ControllerSettingsState settingsState,
            StreamAudioSettingsState audioSettingsState) {
        this.activityContext = activityContext;
        this.conn = conn;
        this.keyboardInputSink =
                new NvConnectionKeyboardInputSink(conn);
        mouseEmulationOutput =
                new ControllerMouseEmulationTranslator.Output() {
                    @Override
                    public void sendMouseButton(
                            byte button,
                            boolean down) {
                        if (down) {
                            conn.sendMouseButtonDown(button);
                        }
                        else {
                            conn.sendMouseButtonUp(button);
                        }
                    }

                    @Override
                    public void sendKey(
                            int keyCode,
                            byte action) {
                        keyboardInputSink.sendKey(
                                (short) keyCode,
                                action,
                                (byte) 0,
                                (byte) 0);
                    }

                    @Override
                    public void sendChord(short[] keyCodes) {
                        KeyboardChordSender.send(
                                keyboardInputSink,
                                keyCodes);
                    }

                    @Override
                    public void sendMouseMove(
                            short deltaX,
                            short deltaY) {
                        conn.sendMouseMove(deltaX, deltaY);
                    }

                    @Override
                    public void sendHighResolutionScroll(
                            short verticalAmount,
                            short horizontalAmount) {
                        conn.sendMouseHighResScroll(
                                verticalAmount);
                        conn.sendMouseHighResHScroll(
                                horizontalAmount);
                    }

                    @Override
                    public void sendDiscreteScroll(byte amount) {
                        conn.sendMouseScroll(amount);
                    }
                };
        this.gestures = gestures;
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
        this.audioSettingsState = Objects.requireNonNull(
                audioSettingsState,
                "audioSettingsState");
        this.usbManager = (UsbManager) activityContext.getSystemService(Context.USB_SERVICE);
        Vibrator deviceVibrator = (Vibrator) activityContext
                .getSystemService(Context.VIBRATOR_SERVICE);
        this.deviceSensorManager = (SensorManager) activityContext.getSystemService(Context.SENSOR_SERVICE);
        this.inputManager = (InputManager) activityContext.getSystemService(Context.INPUT_SERVICE);
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
        this.mouseEmulationScheduler =
                new ControllerMouseEmulationSession.Scheduler() {
                    @Override
                    public void schedule(
                            Runnable runnable,
                            long delayMs) {
                        mainThreadHandler.postDelayed(
                                runnable,
                                delayMs);
                    }

                    @Override
                    public void cancel(Runnable runnable) {
                        mainThreadHandler.removeCallbacks(
                                runnable);
                    }
                };
        // Create a HandlerThread to process battery state updates. These can be slow enough
        // that they lead to ANRs if we do them on the main thread.
        this.backgroundHandlerThread = new HandlerThread("ControllerHandler");
        this.backgroundHandlerThread.start();
        this.backgroundThreadHandler = new Handler(backgroundHandlerThread.getLooper());
        this.motionSensorScheduler =
                new ControllerMotionSession.Scheduler() {
                    @Override
                    public void schedule(
                            Runnable runnable,
                            long delayMs) {
                        backgroundThreadHandler.postDelayed(
                                runnable,
                                delayMs);
                    }

                    @Override
                    public void cancel(Runnable runnable) {
                        backgroundThreadHandler.removeCallbacks(
                                runnable);
                    }
                };
        VibratorManager deviceVibratorManager =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? (VibratorManager) activityContext
                                .getSystemService(
                                        Context.VIBRATOR_MANAGER_SERVICE)
                        : null;

        this.sceManager = new SceManager(activityContext);
        this.sceManager.start();
        this.vibrationRenderer = new ControllerVibrationRenderer(
                settingsState,
                sceManager,
                deviceVibrator,
                deviceVibratorManager);
        this.defaultContext = new InputDeviceContext();
        this.defaultContext.vibrationTarget =
                vibrationRenderer.emptyTarget();

        int deadzonePercentage =
                settingsState.get().getStickDeadzonePercent();

        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev == null) {
                // This device was removed during enumeration
                continue;
            }
            if ((dev.getSources() & InputDevice.SOURCE_JOYSTICK) != 0 ||
                    (dev.getSources() & InputDevice.SOURCE_GAMEPAD) != 0) {
                // This looks like a gamepad, but we'll check X and Y to be sure
                if (getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_X) != null &&
                    getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_Y) != null) {
                    // This is a gamepad
                    hasGameController = true;
                }
            }
        }

        // 1% is the lowest possible deadzone we support
        if (deadzonePercentage <= 0) {
            deadzonePercentage = 0;
        }

        this.stickDeadzone = (double)deadzonePercentage / 100.0;

        // Initialize the default context for events with no device
        defaultContext.leftStickXAxis = MotionEvent.AXIS_X;
        defaultContext.leftStickYAxis = MotionEvent.AXIS_Y;
        defaultContext.leftStickDeadzoneRadius = (float) stickDeadzone;
        defaultContext.rightStickXAxis = MotionEvent.AXIS_Z;
        defaultContext.rightStickYAxis = MotionEvent.AXIS_RZ;
        defaultContext.rightStickDeadzoneRadius = (float) stickDeadzone;
        defaultContext.leftTriggerAxis = MotionEvent.AXIS_BRAKE;
        defaultContext.rightTriggerAxis = MotionEvent.AXIS_GAS;
        defaultContext.hatXAxis = MotionEvent.AXIS_HAT_X;
        defaultContext.hatYAxis = MotionEvent.AXIS_HAT_Y;
        defaultContext.controllerNumber = (short) 0;
        defaultContext.assignedControllerNumber = true;
        defaultContext.external = false;

        // Some devices (GPD XD) have a back button which sends input events
        // with device ID == 0. This hits the default context which would normally
        // consume these. Instead, let's ignore them since that's probably the
        // most likely case.
        defaultContext.buttonMapper =
                ControllerButtonMapper.builder(
                                0,
                                0,
                                Build.VERSION.SDK_INT)
                        .ignoreBack(true)
                        .hasHatAxes(true)
                        .build();

        // Get the initially attached set of gamepads. As each gamepad receives
        // its initial InputEvent, we will move these from this set onto the
        // active reservation set, which allows them to unplug cleanly
        // if they are removed.
        slotAllocator = new ControllerSlotAllocator(
                getAttachedControllerMask(
                        activityContext,
                        settingsState.get()));

        // Register ourselves for input device notifications
        inputManager.registerInputDeviceListener(this, null);
    }

    private static InputDevice.MotionRange getMotionRangeForJoystickAxis(InputDevice dev, int axis) {
        InputDevice.MotionRange range;

        // First get the axis for SOURCE_JOYSTICK
        range = dev.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK);
        if (range == null) {
            // Now try the axis for SOURCE_GAMEPAD
            range = dev.getMotionRange(axis, InputDevice.SOURCE_GAMEPAD);
        }

        return range;
    }

    private static boolean hasJoystickAxisPair(
            InputDevice device,
            int firstAxis,
            int secondAxis) {
        return getMotionRangeForJoystickAxis(device, firstAxis) != null &&
                getMotionRangeForJoystickAxis(device, secondAxis) != null;
    }

    private static int toAndroidAxis(
            ControllerAxisProfile.Axis axis) {
        switch (axis) {
            case X:
                return MotionEvent.AXIS_X;
            case Y:
                return MotionEvent.AXIS_Y;
            case Z:
                return MotionEvent.AXIS_Z;
            case RZ:
                return MotionEvent.AXIS_RZ;
            case RX:
                return MotionEvent.AXIS_RX;
            case RY:
                return MotionEvent.AXIS_RY;
            case LEFT_TRIGGER:
                return MotionEvent.AXIS_LTRIGGER;
            case RIGHT_TRIGGER:
                return MotionEvent.AXIS_RTRIGGER;
            case BRAKE:
                return MotionEvent.AXIS_BRAKE;
            case GAS:
                return MotionEvent.AXIS_GAS;
            case THROTTLE:
                return MotionEvent.AXIS_THROTTLE;
            case HAT_X:
                return MotionEvent.AXIS_HAT_X;
            case HAT_Y:
                return MotionEvent.AXIS_HAT_Y;
            case NONE:
            default:
                return -1;
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        // Nothing happening here yet
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        InputDeviceContext context = inputDeviceContexts.get(deviceId);
        if (context != null) {
            LimeLog.info("Removed controller: "+context.name+" ("+deviceId+")");
            releaseControllerNumber(context);
            context.destroy();
            inputDeviceContexts.remove(deviceId);
        }
    }

    // This can happen when gaining/losing input focus with some devices.
    // Input devices that have a trackpad may gain/lose AXIS_RELATIVE_X/Y.
    @Override
    public void onInputDeviceChanged(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if (device == null) {
            return;
        }

        // If we don't have a context for this device, we don't need to update anything
        InputDeviceContext existingContext = inputDeviceContexts.get(deviceId);
        if (existingContext == null) {
            return;
        }

        LimeLog.info("Device changed: "+existingContext.name+" ("+deviceId+")");
        // Migrate the existing context into this new one by moving any stateful elements
        InputDeviceContext newContext = createInputDeviceContextForDevice(device);
        newContext.migrateContext(existingContext);
        inputDeviceContexts.put(deviceId, newContext);
    }

    @Override
    public void stop() {
        if (stopped) {
            return;
        }

        // Stop new device contexts from being created or used
        stopped = true;

        // Unregister our input device callbacks
        inputManager.unregisterInputDeviceListener(this);

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);
            deviceContext.destroy();
        }
        defaultContext.destroy();

        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);
            deviceContext.destroy();
        }

        stopRazerKishiHapticsDevices();
        vibrationRenderer.cancelDevice();
    }

    @Override
    public void destroy() {
        if (!stopped) {
            stop();
        }

        sceManager.stop();
        backgroundHandlerThread.quit();
    }

    public void disableSensors() {
        defaultContext.disableSensors();
        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);
            deviceContext.disableSensors();
        }
    }

    public void enableSensors() {
        if (stopped) {
            return;
        }

        defaultContext.enableSensors();
        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);
            deviceContext.enableSensors();
        }
    }

    private static boolean hasJoystickAxes(InputDevice device) {
        return (device.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                getMotionRangeForJoystickAxis(device, MotionEvent.AXIS_X) != null &&
                getMotionRangeForJoystickAxis(device, MotionEvent.AXIS_Y) != null;
    }

    private static boolean hasGamepadButtons(InputDevice device) {
        return (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
    }

    @Override
    public boolean isGameControllerDevice(InputDevice device) {
        if (device == null) {
            return true;
        }

        if (hasJoystickAxes(device) || hasGamepadButtons(device)) {
            // Has real joystick axes or gamepad buttons
            return true;
        }

        // HACK for https://issuetracker.google.com/issues/163120692
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            if (device.getId() == -1) {
                // This "virtual" device could be input from any of the attached devices.
                // Look to see if any gamepads are connected.
                int[] ids = InputDevice.getDeviceIds();
                for (int id : ids) {
                    InputDevice dev = InputDevice.getDevice(id);
                    if (dev == null) {
                        // This device was removed during enumeration
                        continue;
                    }

                    // If there are any gamepad devices connected, we'll
                    // report that this virtual device is a gamepad.
                    if (hasJoystickAxes(dev) || hasGamepadButtons(dev)) {
                        return true;
                    }
                }
            }
        }

        // Otherwise, we'll try anything that claims to be a non-alphabetic keyboard
        return device.getKeyboardType() != InputDevice.KEYBOARD_TYPE_ALPHABETIC;
    }

    public static short getAttachedControllerMask(
            Context context,
            ControllerSettings settings) {
        int count = 0;
        short mask = 0;

        // Count all input devices that are gamepads
        InputManager im = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        for (int id : im.getInputDeviceIds()) {
            InputDevice dev = im.getInputDevice(id);
            if (dev == null) {
                continue;
            }

            if (hasJoystickAxes(dev)) {
                LimeLog.info("Counting InputDevice: "+dev.getName());
                mask |= 1 << count++;
            }
        }

        // Count all USB devices that match our drivers
        if (settings.isUsbDriverEnabled()) {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager != null) {
                for (UsbDevice dev : usbManager.getDeviceList().values()) {
                    // We explicitly check not to claim devices that appear as InputDevices
                    // otherwise we will double count them.
                    if (UsbDriverService.shouldClaimDevice(dev, false) &&
                            !UsbDriverService.isRecognizedInputDevice(dev)) {
                        LimeLog.info("Counting UsbDevice: "+dev.getDeviceName());
                        mask |= 1 << count++;
                    }
                }
            }
        }

        if (settings.isOnscreenControllerEnabled()) {
            LimeLog.info("Counting OSC gamepad");
            mask |= 1;
        }

        LimeLog.info("Enumerated "+count+" gamepads");
        return mask;
    }

    private void releaseControllerNumber(GenericControllerContext context) {
        // If we reserved a controller number, remove that reservation
        if (context.reservedControllerNumber) {
            LimeLog.info("Controller number "+context.controllerNumber+" is now available");
            slotAllocator.release(context.controllerNumber);
        }

        // If this device sent data as a gamepad, zero the values before removing.
        // We must do this after releasing the slot so this
        // causes the device to be removed on the server PC.
        if (context.assignedControllerNumber) {
            conn.sendControllerInput(context.controllerNumber, getActiveControllerMask(),
                    (short) 0,
                    (byte) 0, (byte) 0,
                    (short) 0, (short) 0,
                    (short) 0, (short) 0);
        }
    }

    private boolean isAssociatedJoystick(InputDevice originalDevice, InputDevice possibleAssociatedJoystick) {
        if (possibleAssociatedJoystick == null) {
            return false;
        }

        // This can't be an associated joystick if it's not a joystick
        if ((possibleAssociatedJoystick.getSources() & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) {
            return false;
        }

        // Make sure the device names *don't* match in order to prevent us from accidentally matching
        // on another of the exact same device.
        if (possibleAssociatedJoystick.getName().equals(originalDevice.getName())) {
            return false;
        }

        // Make sure the descriptor matches. This can match in cases where two of the exact same
        // input device are connected, so we perform the name check to exclude that case.
        if (!possibleAssociatedJoystick.getDescriptor().equals(originalDevice.getDescriptor())) {
            return false;
        }

        return true;
    }

    private void reserveControllerNumber(
            GenericControllerContext context) {
        short reservedSlot = slotAllocator.reserveNext();
        if (reservedSlot == ControllerSlotAllocator.NO_SLOT) {
            context.controllerNumber = 0;
            context.reservedControllerNumber = false;
            LimeLog.warning(
                    "No controller number is available; " +
                            "falling back to controller 0");
            return;
        }
        context.controllerNumber = reservedSlot;
        context.reservedControllerNumber = true;
    }

    // Called before sending input but after we've determined that this
    // is definitely a controller (not a keyboard, mouse, or something else)
    private void assignControllerNumberIfNeeded(GenericControllerContext context) {
        if (context.assignedControllerNumber) {
            return;
        }

        ControllerSettings settings = settingsState.get();
        if (context instanceof InputDeviceContext) {
            InputDeviceContext devContext = (InputDeviceContext) context;

            LimeLog.info(devContext.name+" ("+context.id+") needs a controller number assigned");
            if (!devContext.external) {
                LimeLog.info("Built-in buttons hardcoded as controller 0");
                context.controllerNumber = 0;
            }
            else if (settings.isMultiControllerEnabled() &&
                    devContext.hasJoystickAxes) {
                LimeLog.info("Reserving the next available controller number");
                reserveControllerNumber(context);
            }
            else if (!devContext.hasJoystickAxes) {
                // If this device doesn't have joystick axes, it may be an input device associated
                // with another joystick (like a PS4 touchpad). We'll propagate that joystick's
                // controller number to this associated device.

                context.controllerNumber = 0;

                // For the DS4 case, the associated joystick is the next device after the touchpad.
                // We'll try the opposite case too, just to be a little future-proof.
                InputDevice associatedDevice = InputDevice.getDevice(devContext.id + 1);
                if (!isAssociatedJoystick(devContext.inputDevice, associatedDevice)) {
                    associatedDevice = InputDevice.getDevice(devContext.id - 1);
                    if (!isAssociatedJoystick(devContext.inputDevice, associatedDevice)) {
                        LimeLog.info("No associated joystick device found");
                        associatedDevice = null;
                    }
                }

                if (associatedDevice != null) {
                    InputDeviceContext associatedDeviceContext = inputDeviceContexts.get(associatedDevice.getId());

                    // Create a new context for the associated device if one doesn't exist
                    if (associatedDeviceContext == null) {
                        associatedDeviceContext = createInputDeviceContextForDevice(associatedDevice);
                        inputDeviceContexts.put(associatedDevice.getId(), associatedDeviceContext);
                    }

                    // Assign a controller number for the associated device if one isn't assigned
                    if (!associatedDeviceContext.assignedControllerNumber) {
                        assignControllerNumberIfNeeded(associatedDeviceContext);
                    }

                    // Propagate the associated controller number
                    context.controllerNumber = associatedDeviceContext.controllerNumber;

                    LimeLog.info("Propagated controller number from "+associatedDeviceContext.name);
                }
            }
            else {
                LimeLog.info("Not reserving a controller number");
                context.controllerNumber = 0;
            }

            // If the gamepad doesn't have motion sensors, use the on-device sensors as a fallback for player 1
            if (settings
                    .isMotionSensorsFallbackToDeviceEnabled() &&
                    context.controllerNumber == 0 &&
                    devContext.sensorManager == null) {
                devContext.sensorManager = deviceSensorManager;
            }
        }
        else {
            if (settings.isMultiControllerEnabled()) {
                LimeLog.info("Reserving the next available controller number");
                reserveControllerNumber(context);
            }
            else {
                LimeLog.info("Not reserving a controller number");
                context.controllerNumber = 0;
            }
        }

        LimeLog.info("Assigned as controller "+context.controllerNumber);
        context.assignedControllerNumber = true;

        // Report attributes of this new controller to the host
        context.sendControllerArrival();
    }

    private UsbDeviceContext createUsbDeviceContextForDevice(AbstractController device) {
        UsbDeviceContext context = new UsbDeviceContext();

        context.id = device.getControllerId();
        context.device = device;
        context.external = true;

        context.vendorId = device.getVendorId();
        context.productId = device.getProductId();

        context.leftStickDeadzoneRadius = (float) stickDeadzone;
        context.rightStickDeadzoneRadius = (float) stickDeadzone;
        context.triggerDeadzone = 0.13f;
        if (settingsState.get().isTriggerDeadzoneDisabled()) {
            context.triggerDeadzone = 0.0f;
        }
        return context;
    }

    private static boolean hasButtonUnderTouchpad(InputDevice dev, byte type) {
        // It has to have a touchpad to have a button under it
        if ((dev.getSources() & InputDevice.SOURCE_TOUCHPAD) != InputDevice.SOURCE_TOUCHPAD) {
            return false;
        }

        // Landroid/view/InputDevice;->hasButtonUnderPad()Z is blocked after O
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            try {
                return (Boolean) dev.getClass().getMethod("hasButtonUnderPad").invoke(dev);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }

        // We can't use the platform API, so we'll have to just guess based on the gamepad type.
        // If this is a PlayStation controller with a touchpad, we know it has a clickpad.
        return type == MoonBridge.LI_CTYPE_PS;
    }

    private static boolean isExternal(InputDevice dev) {
        // The ASUS Tinker Board inaccurately reports Bluetooth gamepads as internal,
        // causing shouldIgnoreBack() to believe it should pass through back as a
        // navigation event for any attached gamepads.
        if (Build.MODEL.equals("Tinker Board")) {
            return true;
        }

        String deviceName = dev.getName();
        if (deviceName.contains("gpio") || // This is the back button on Shield portable consoles
                deviceName.contains("joy_key") || // These are the gamepad buttons on the Archos Gamepad 2
                deviceName.contains("keypad") || // These are gamepad buttons on the XPERIA Play
                deviceName.equalsIgnoreCase("NVIDIA Corporation NVIDIA Controller v01.01") || // Gamepad on Shield Portable
                deviceName.equalsIgnoreCase("NVIDIA Corporation NVIDIA Controller v01.02") || // Gamepad on Shield Portable (?)
                deviceName.equalsIgnoreCase("GR0006") // Gamepad on Logitech G Cloud
        )
        {
            LimeLog.info(dev.getName()+" is internal by hardcoded mapping");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Landroid/view/InputDevice;->isExternal()Z is officially public on Android Q
            return dev.isExternal();
        }
        else {
            try {
                // Landroid/view/InputDevice;->isExternal()Z is on the light graylist in Android P
                return (Boolean)dev.getClass().getMethod("isExternal").invoke(dev);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }

        // Answer true if we don't know
        return true;
    }

    private boolean shouldIgnoreBack(InputDevice dev) {
        String devName = dev.getName();

        // The Serval has a Select button but the framework doesn't
        // know about that because it uses a non-standard scancode.
        if (devName.contains("Razer Serval")) {
            return true;
        }

        // Classify this device as a remote by name if it has no joystick axes
        if (!hasJoystickAxes(dev) &&
                devName.toLowerCase(Locale.ROOT).contains("remote")) {
            return true;
        }

        // Otherwise, dynamically try to determine whether we should allow this
        // back button to function for navigation.
        //
        // First, check if this is an internal device we're being called on.
        if (!isExternal(dev)) {
            InputManager im = (InputManager) activityContext.getSystemService(Context.INPUT_SERVICE);

            boolean foundInternalGamepad = false;
            boolean foundInternalSelect = false;
            for (int id : im.getInputDeviceIds()) {
                InputDevice currentDev = im.getInputDevice(id);

                // Ignore external devices
                if (currentDev == null || isExternal(currentDev)) {
                    continue;
                }

                // Note that we are explicitly NOT excluding the current device we're examining here,
                // since the other gamepad buttons may be on our current device and that's fine.
                if (currentDev.hasKeys(KeyEvent.KEYCODE_BUTTON_SELECT)[0]) {
                    foundInternalSelect = true;
                }

                // We don't check KEYCODE_BUTTON_A here, since the Shield Android TV has a
                // virtual mouse device that claims to have KEYCODE_BUTTON_A. Instead, we rely
                // on the SOURCE_GAMEPAD flag to be set on gamepad devices.
                if (hasGamepadButtons(currentDev)) {
                    foundInternalGamepad = true;
                }
            }

            // Allow the back button to function for navigation if we either:
            // a) have no internal gamepad (most phones)
            // b) have an internal gamepad but also have an internal select button (GPD XD)
            // but not:
            // c) have an internal gamepad but no internal select button (NVIDIA SHIELD Portable)
            return !foundInternalGamepad || foundInternalSelect;
        }
        else {
            // For external devices, we want to pass through the back button if the device
            // has no gamepad axes or gamepad buttons.
            return !hasJoystickAxes(dev) && !hasGamepadButtons(dev);
        }
    }

    private InputDeviceContext createInputDeviceContextForDevice(InputDevice dev) {
        InputDeviceContext context = new InputDeviceContext();
        String devName = dev.getName();

        LimeLog.info("Creating controller context for device: "+devName);
        LimeLog.info("Vendor ID: " + dev.getVendorId());
        LimeLog.info("Product ID: "+dev.getProductId());
        LimeLog.info(dev.toString());

        context.inputDevice = dev;
        context.name = devName;
        context.id = dev.getId();
        context.external = isExternal(dev);

        context.vendorId = dev.getVendorId();
        context.productId = dev.getProductId();

        // These aren't always present in the Android key layout files, so they won't show up
        // in our normal InputDevice.hasKeys() probing.
        context.hasPaddles = MoonBridge.guessControllerHasPaddles(context.vendorId, context.productId);
        context.hasShare = MoonBridge.guessControllerHasShareButton(context.vendorId, context.productId);

        context.vibrationTarget = vibrationRenderer.selectTarget(
                dev,
                context.external);
        // On Android 12, we can try to use the InputDevice's sensors. This may not work if the
        // Linux kernel version doesn't have motion sensor support, which is common for third-party
        // gamepads.
        //
        // Android 12 has a bug that causes InputDeviceSensorManager to cause a NPE on a background
        // thread due to bad error checking in InputListener callbacks. InputDeviceSensorManager is
        // created upon the first call to InputDevice.getSensorManager(), so we avoid calling this
        // on Android 12 unless we have a gamepad that could plausibly have motion sensors.
        // https://cs.android.com/android/_/android/platform/frameworks/base/+/8970010a5e9f3dc5c069f56b4147552accfcbbeb
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                (Build.VERSION.SDK_INT == Build.VERSION_CODES.S &&
                        (context.vendorId == 0x054c || context.vendorId == 0x057e))) && // Sony or Nintendo
                settingsState.get().areMotionSensorsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dev.getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null || dev.getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                    context.sensorManager = dev.getSensorManager();
                }
            }
        }

        // Check if this device has a usable RGB LED and cache that result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (Light light : dev.getLightsManager().getLights()) {
                if (light.hasRgbControl()) {
                    context.hasRgbLed = true;
                    break;
                }
            }
        }

        // Detect if the gamepad has Mode and Select buttons according to the Android key layouts.
        // We do this first because other codepaths below may override these defaults.
        boolean[] buttons = dev.hasKeys(KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK, 0);
        context.hasMode = buttons[0];
        context.hasSelect = buttons[1] || buttons[2];

        context.touchpadXRange = dev.getMotionRange(MotionEvent.AXIS_X, InputDevice.SOURCE_TOUCHPAD);
        context.touchpadYRange = dev.getMotionRange(MotionEvent.AXIS_Y, InputDevice.SOURCE_TOUCHPAD);
        context.touchpadPressureRange = dev.getMotionRange(MotionEvent.AXIS_PRESSURE, InputDevice.SOURCE_TOUCHPAD);

        // This is hack to deal with the Nvidia Shield's modifications that causes the DS4 clickpad
        // to work as a duplicate Select button instead of a unique button we can handle separately.
        boolean dualShockStandaloneTouchpad =
                context.vendorId == 0x054c && // Sony
                        (devName.endsWith(" Touchpad")||devName.startsWith("DualSense")) &&
                dev.getSources() == (InputDevice.SOURCE_KEYBOARD | InputDevice.SOURCE_MOUSE);

        InputDevice.MotionRange gasRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_GAS);
        boolean hasRxAndRy =
                hasJoystickAxisPair(
                        dev,
                        MotionEvent.AXIS_RX,
                        MotionEvent.AXIS_RY);
        boolean hasSonyButtonC =
                context.vendorId == 0x054c &&
                        devName != null &&
                        hasRxAndRy &&
                        dev.hasKeys(KeyEvent.KEYCODE_BUTTON_C)[0];
        ControllerAxisProfile axisProfile =
                ControllerAxisProfile.resolve(
                        context.vendorId,
                        devName != null,
                        hasSonyButtonC,
                        ControllerAxisProfile.Capabilities.builder()
                                .xAndY(
                                        hasJoystickAxisPair(
                                                dev,
                                                MotionEvent.AXIS_X,
                                                MotionEvent.AXIS_Y))
                                .leftTriggerAndRightTrigger(
                                        hasJoystickAxisPair(
                                                dev,
                                                MotionEvent.AXIS_LTRIGGER,
                                                MotionEvent.AXIS_RTRIGGER))
                                .brakeAndGas(
                                        hasJoystickAxisPair(
                                                dev,
                                                MotionEvent.AXIS_BRAKE,
                                                MotionEvent.AXIS_GAS))
                                .brakeAndThrottle(
                                        hasJoystickAxisPair(
                                                dev,
                                                MotionEvent.AXIS_BRAKE,
                                                MotionEvent.AXIS_THROTTLE))
                                .rxAndRy(hasRxAndRy)
                                .zAndRz(
                                        hasJoystickAxisPair(
                                                dev,
                                                MotionEvent.AXIS_Z,
                                                MotionEvent.AXIS_RZ))
                                .hatXAndHatY(
                                        hasJoystickAxisPair(
                                                dev,
                                                MotionEvent.AXIS_HAT_X,
                                                MotionEvent.AXIS_HAT_Y))
                                .build());
        context.leftStickXAxis =
                toAndroidAxis(axisProfile.getLeftStickX());
        context.leftStickYAxis =
                toAndroidAxis(axisProfile.getLeftStickY());
        context.rightStickXAxis =
                toAndroidAxis(axisProfile.getRightStickX());
        context.rightStickYAxis =
                toAndroidAxis(axisProfile.getRightStickY());
        context.leftTriggerAxis =
                toAndroidAxis(axisProfile.getLeftTrigger());
        context.rightTriggerAxis =
                toAndroidAxis(axisProfile.getRightTrigger());
        context.hatXAxis =
                toAndroidAxis(axisProfile.getHatX());
        context.hatYAxis =
                toAndroidAxis(axisProfile.getHatY());
        context.triggersIdleNegative =
                axisProfile.areTriggersIdleNegative();
        context.hasJoystickAxes = axisProfile.hasLeftStick();
        if (context.hasJoystickAxes) {
            hasGameController = true;
        }

        boolean nonStandardDualShock4 =
                axisProfile.isNonStandardDualShock4();
        boolean linuxStandardFaceButtons =
                axisProfile.hasLinuxStandardFaceButtons();
        if (nonStandardDualShock4) {
            LimeLog.info("Detected non-standard DualShock 4 mapping");
            context.hasSelect = true;
            context.hasMode = true;
        }
        else if (linuxStandardFaceButtons) {
            LimeLog.info("Detected DualShock 4 (Linux standard mapping)");
        }

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            context.leftStickDeadzoneRadius = (float) stickDeadzone;
        }

        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            context.rightStickDeadzoneRadius = (float) stickDeadzone;
        }
        //todo --trigger
        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            InputDevice.MotionRange ltRange = getMotionRangeForJoystickAxis(dev, context.leftTriggerAxis);
            InputDevice.MotionRange rtRange = getMotionRangeForJoystickAxis(dev, context.rightTriggerAxis);

            // It's important to have a valid deadzone so controller packet batching works properly
            context.triggerDeadzone = Math.max(Math.abs(ltRange.getFlat()), Math.abs(rtRange.getFlat()));

            if (!settingsState.get().isTriggerDeadzoneDisabled()) {
                // For triggers without (valid) deadzones, we'll use 13% (around XInput's default)
                if (context.triggerDeadzone < 0.13f ||
                        context.triggerDeadzone > 0.30f)
                {
                    context.triggerDeadzone = 0.13f;
                }
            }
        }

        boolean ignoreBack = shouldIgnoreBack(dev);
        boolean hasStartOrMenu = false;
        if (devName != null &&
                devName.contains("ASUS Gamepad")) {
            boolean[] startAndMenu =
                    dev.hasKeys(
                            KeyEvent.KEYCODE_BUTTON_START,
                            KeyEvent.KEYCODE_MENU,
                            0);
            hasStartOrMenu =
                    startAndMenu[0] || startAndMenu[1];
        }
        ControllerDeviceQuirks deviceQuirks =
                ControllerDeviceQuirks.resolve(
                        ControllerDeviceQuirks.Facts.builder(
                                        context.vendorId,
                                        context.productId)
                                .deviceName(devName)
                                .hasMode(context.hasMode)
                                .hasSelect(context.hasSelect)
                                .hasStartOrMenu(hasStartOrMenu)
                                .hasGasAxis(gasRange != null)
                                .triggerDeadzone(
                                        context.triggerDeadzone)
                                .build());
        context.hasMode = deviceQuirks.hasMode();
        context.hasSelect = deviceQuirks.hasSelect();
        context.triggerDeadzone =
                deviceQuirks.getTriggerDeadzone();

        LimeLog.info("Analog stick deadzone: "+context.leftStickDeadzoneRadius+" "+context.rightStickDeadzoneRadius);
        LimeLog.info("Trigger deadzone: "+context.triggerDeadzone);

        context.buttonMapper =
                ControllerButtonMapper.builder(
                                context.vendorId,
                                context.productId,
                                Build.VERSION.SDK_INT)
                        .ignoreBack(ignoreBack)
                        .hasShare(context.hasShare)
                        .dualShockStandaloneTouchpad(
                                dualShockStandaloneTouchpad)
                        .linuxStandardFaceButtons(
                                linuxStandardFaceButtons)
                        .nonStandardDualShock4(
                                nonStandardDualShock4)
                        .serval(deviceQuirks.isServal())
                        .nonStandardXboxBluetooth(
                                deviceQuirks
                                        .isNonStandardXboxBluetooth())
                        .backIsStart(deviceQuirks.isBackStart())
                        .modeIsSelect(deviceQuirks.isModeSelect())
                        .searchIsMode(deviceQuirks.isSearchMode())
                        .hasHatAxes(
                                context.hatXAxis != -1 ||
                                        context.hatYAxis != -1)
                        .build();

        return context;
    }

    private InputDeviceContext getContextForEvent(InputEvent event) {
        // Don't return a context if we're stopped
        if (stopped) {
            return null;
        }
        else if (event.getDeviceId() == 0) {
            // Unknown devices use the default context
            return defaultContext;
        }
        else if (event.getDevice() == null) {
            // During device removal, sometimes we can get events after the
            // input device has been destroyed. In this case we'll see a
            // != 0 device ID but no device attached.
            return null;
        }

        // HACK for https://issuetracker.google.com/issues/163120692
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            if (event.getDeviceId() == -1) {
                return defaultContext;
            }
        }

        // Return the existing context if it exists
        InputDeviceContext context = inputDeviceContexts.get(event.getDeviceId());
        if (context != null) {
            return context;
        }

        // Otherwise create a new context
        context = createInputDeviceContextForDevice(event.getDevice());
        inputDeviceContexts.put(event.getDeviceId(), context);

        return context;
    }

    private short getActiveControllerMask() {
        ControllerSettings settings = settingsState.get();
        return slotAllocator.getActiveMask(
                settings.isMultiControllerEnabled(),
                settings.isOnscreenControllerEnabled());
    }

    // This must not be called on the main thread due to risk of ANRs!
    private void sendControllerBatteryPacket(InputDeviceContext context) {
        int currentBatteryStatus;
        float currentBatteryCapacity;

        // Use the BatteryState object introduced in Android S, if it's available and present.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.inputDevice.getBatteryState().isPresent()) {
            currentBatteryStatus = context.inputDevice.getBatteryState().getStatus();
            currentBatteryCapacity = context.inputDevice.getBatteryState().getCapacity();
        }
        else if (sceManager.isRecognizedDevice(context.inputDevice)) {
            // On the SHIELD Android TV, we can use a proprietary API to access battery/charge state.
            // We will convert it to the same form used by BatteryState to share code.
            int batteryPercentage = sceManager.getBatteryPercentage(context.inputDevice);
            if (batteryPercentage < 0) {
                currentBatteryCapacity = Float.NaN;
            }
            else {
                currentBatteryCapacity = batteryPercentage / 100.f;
            }

            SceConnectionType connectionType = sceManager.getConnectionType(context.inputDevice);
            SceChargingState chargingState = sceManager.getChargingState(context.inputDevice);

            // We can make some assumptions about charge state based on the connection type
            if (connectionType == SceConnectionType.WIRED || connectionType == SceConnectionType.BOTH) {
                if (batteryPercentage == 100) {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_FULL;
                }
                else if (chargingState == SceChargingState.NOT_CHARGING) {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_NOT_CHARGING;
                }
                else {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_CHARGING;
                }
            }
            else if (connectionType == SceConnectionType.WIRELESS) {
                if (chargingState == SceChargingState.CHARGING) {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_CHARGING;
                }
                else {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_DISCHARGING;
                }
            }
            else {
                // If connection type is unknown, just use the charge state
                if (batteryPercentage == 100) {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_FULL;
                }
                else if (chargingState == SceChargingState.NOT_CHARGING) {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_DISCHARGING;
                }
                else if (chargingState == SceChargingState.CHARGING) {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_CHARGING;
                }
                else {
                    currentBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
                }
            }
        }
        else {
            return;
        }

        ControllerBatteryReport report =
                ControllerBatteryReport.fromAndroidSample(
                        currentBatteryStatus,
                        currentBatteryCapacity);
        if (report != null &&
                report.differsFrom(
                        context.lastReportedBatteryStatus,
                        context.lastReportedBatteryCapacity)) {
            conn.sendControllerBatteryEvent(
                    (byte) context.controllerNumber,
                    report.getProtocolState(),
                    report.getPercentage());

            context.lastReportedBatteryStatus =
                    report.getAndroidStatus();
            context.lastReportedBatteryCapacity =
                    report.getCapacity();
        }
    }

    private void sendControllerInputPacket(GenericControllerContext originalContext) {
        assignControllerNumberIfNeeded(originalContext);

        // Take the context's controller number and fuse all inputs with the same number
        short controllerNumber = originalContext.controllerNumber;
        int inputMap = 0;
        byte leftTrigger = 0;
        byte rightTrigger = 0;
        short leftStickX = 0;
        short leftStickY = 0;
        short rightStickX = 0;
        short rightStickY = 0;

        // In order to properly handle controllers that are split into multiple devices,
        // we must aggregate all controllers with the same controller number into a single
        // device before we send it.
        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            GenericControllerContext context = inputDeviceContexts.valueAt(i);
            if (context.assignedControllerNumber &&
                    context.controllerNumber == controllerNumber &&
                    context.isMouseEmulationActive() ==
                            originalContext.isMouseEmulationActive()) {
                inputMap |= context.inputMap;
                leftTrigger =
                        ControllerAnalogInputCombiner.combineTrigger(
                                leftTrigger,
                                context.leftTrigger);
                rightTrigger =
                        ControllerAnalogInputCombiner.combineTrigger(
                                rightTrigger,
                                context.rightTrigger);
                leftStickX =
                        ControllerAnalogInputCombiner.combineAxis(
                                leftStickX,
                                context.leftStickX);
                leftStickY =
                        ControllerAnalogInputCombiner.combineAxis(
                                leftStickY,
                                context.leftStickY);
                rightStickX =
                        ControllerAnalogInputCombiner.combineAxis(
                                rightStickX,
                                context.rightStickX);
                rightStickY =
                        ControllerAnalogInputCombiner.combineAxis(
                                rightStickY,
                                context.rightStickY);
            }
        }
        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            GenericControllerContext context = usbDeviceContexts.valueAt(i);
            if (context.assignedControllerNumber &&
                    context.controllerNumber == controllerNumber &&
                    context.isMouseEmulationActive() ==
                            originalContext.isMouseEmulationActive()) {
                inputMap |= context.inputMap;
                leftTrigger =
                        ControllerAnalogInputCombiner.combineTrigger(
                                leftTrigger,
                                context.leftTrigger);
                rightTrigger =
                        ControllerAnalogInputCombiner.combineTrigger(
                                rightTrigger,
                                context.rightTrigger);
                leftStickX =
                        ControllerAnalogInputCombiner.combineAxis(
                                leftStickX,
                                context.leftStickX);
                leftStickY =
                        ControllerAnalogInputCombiner.combineAxis(
                                leftStickY,
                                context.leftStickY);
                rightStickX =
                        ControllerAnalogInputCombiner.combineAxis(
                                rightStickX,
                                context.rightStickX);
                rightStickY =
                        ControllerAnalogInputCombiner.combineAxis(
                                rightStickY,
                                context.rightStickY);
            }
        }
        if (defaultContext.controllerNumber == controllerNumber) {
            inputMap |= defaultContext.inputMap;
            leftTrigger =
                    ControllerAnalogInputCombiner.combineTrigger(
                            leftTrigger,
                            defaultContext.leftTrigger);
            rightTrigger =
                    ControllerAnalogInputCombiner.combineTrigger(
                            rightTrigger,
                            defaultContext.rightTrigger);
            leftStickX =
                    ControllerAnalogInputCombiner.combineAxis(
                            leftStickX,
                            defaultContext.leftStickX);
            leftStickY =
                    ControllerAnalogInputCombiner.combineAxis(
                            leftStickY,
                            defaultContext.leftStickY);
            rightStickX =
                    ControllerAnalogInputCombiner.combineAxis(
                            rightStickX,
                            defaultContext.rightStickX);
            rightStickY =
                    ControllerAnalogInputCombiner.combineAxis(
                            rightStickY,
                            defaultContext.rightStickY);
        }

        if (originalContext.isMouseEmulationActive()) {
            originalContext.mouseEmulationTranslator.translate(
                    inputMap,
                    mouseEmulationOutput);

            conn.sendControllerInput(controllerNumber, getActiveControllerMask(),
                    (short)0, (byte)0, (byte)0, (short)0, (short)0, (short)0, (short)0);
        }
        else {
            //强制体感模拟右摇杆
            if (settingsState.get().isForceGyroEnabled()) {
                setControllerLeftTriggerState(
                        controllerNumber,
                        leftTrigger);
            }
            conn.sendControllerInput(controllerNumber, getActiveControllerMask(),
                    inputMap,
                    leftTrigger, rightTrigger,
                    leftStickX, leftStickY,
                    rightStickX, rightStickY);
        }
    }

    private void setControllerLeftTriggerState(
            short controllerNumber,
            byte leftTrigger) {
        if (controllerNumber >= 0 &&
                controllerNumber < ControllerSlotAllocator.MAX_SLOTS) {
            controllerLeftTriggerStates.set(
                    controllerNumber,
                    Byte.toUnsignedInt(leftTrigger));
        }
    }

    private int getControllerLeftTriggerState(
            short controllerNumber) {
        if (controllerNumber < 0 ||
                controllerNumber >= ControllerSlotAllocator.MAX_SLOTS) {
            return 0;
        }
        return controllerLeftTriggerStates.get(controllerNumber);
    }

    private int handleRemapping(InputDeviceContext context, KeyEvent event) {
        return context.buttonMapper.remap(
                event.getKeyCode(),
                event.getScanCode(),
                event.getFlags(),
                event.hasNoModifiers(),
                settingsState.get().isJoyConFixEnabled());
    }

    private Vector2d populateCachedVector(float x, float y) {
        // Reinitialize our cached Vector2d object
        inputVector.initialize(x, y);
        return inputVector;
    }

    private void handleDeadZone(Vector2d stickVector, float deadzoneRadius) {
        if (stickVector.getMagnitude() <= deadzoneRadius) {
            // Deadzone
            stickVector.initialize(0, 0);
        }

        // We're not normalizing here because we let the computer handle the deadzones.
        // Normalizing can make the deadzones larger than they should be after the computer also
        // evaluates the deadzone.
    }

    private void handleAxisSet(InputDeviceContext context, float lsX, float lsY, float rsX,
                               float rsY, float lt, float rt, float hatX, float hatY) {

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            Vector2d leftStickVector = populateCachedVector(lsX, lsY);

            handleDeadZone(leftStickVector, context.leftStickDeadzoneRadius);

            context.leftStickX = (short) (leftStickVector.getX() * 0x7FFE);
            context.leftStickY = (short) (-leftStickVector.getY() * 0x7FFE);
        }

        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            Vector2d rightStickVector = populateCachedVector(rsX, rsY);

            handleDeadZone(rightStickVector, context.rightStickDeadzoneRadius);

            context.rightStickX = (short) (rightStickVector.getX() * 0x7FFE);
            context.rightStickY = (short) (-rightStickVector.getY() * 0x7FFE);
        }

        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            // Android sends an initial 0 value for trigger axes even if the trigger
            // should be negative when idle. After the first touch, the axes will go back
            // to normal behavior, so ignore triggersIdleNegative for each trigger until
            // first touch.
            if (lt != 0) {
                context.leftTriggerAxisUsed = true;
            }
            if (rt != 0) {
                context.rightTriggerAxisUsed = true;
            }
            if (context.triggersIdleNegative) {
                if (context.leftTriggerAxisUsed) {
                    lt = (lt + 1) / 2;
                }
                if (context.rightTriggerAxisUsed) {
                    rt = (rt + 1) / 2;
                }
            }

            if (lt <= context.triggerDeadzone) {
                lt = 0;
            }
            if (rt <= context.triggerDeadzone) {
                rt = 0;
            }

            context.leftTrigger = (byte)(lt * 0xFF);
            context.rightTrigger = (byte)(rt * 0xFF);
        }

        if (context.hatXAxis != -1 && context.hatYAxis != -1) {
            context.inputMap &= ~(ControllerPacket.LEFT_FLAG | ControllerPacket.RIGHT_FLAG);
            if (hatX < -0.5) {
                context.inputMap |= ControllerPacket.LEFT_FLAG;
                context.hatXAxisUsed = true;
            }
            else if (hatX > 0.5) {
                context.inputMap |= ControllerPacket.RIGHT_FLAG;
                context.hatXAxisUsed = true;
            }

            context.inputMap &= ~(ControllerPacket.UP_FLAG | ControllerPacket.DOWN_FLAG);
            if (hatY < -0.5) {
                context.inputMap |= ControllerPacket.UP_FLAG;
                context.hatYAxisUsed = true;
            }
            else if (hatY > 0.5) {
                context.inputMap |= ControllerPacket.DOWN_FLAG;
                context.hatYAxisUsed = true;
            }
        }

        sendControllerInputPacket(context);
    }

    // Normalize the given raw float value into a 0.0-1.0f range
    private float normalizeRawValueWithRange(float value, InputDevice.MotionRange range) {
        value = Math.max(value, range.getMin());
        value = Math.min(value, range.getMax());

        value -= range.getMin();

        return value / range.getRange();
    }

    private boolean sendTouchpadEventForPointer(InputDeviceContext context, MotionEvent event, byte touchType, int pointerIndex) {
        float normalizedX = normalizeRawValueWithRange(event.getX(pointerIndex), context.touchpadXRange);
        float normalizedY = normalizeRawValueWithRange(event.getY(pointerIndex), context.touchpadYRange);
        float normalizedPressure = context.touchpadPressureRange != null ?
                normalizeRawValueWithRange(event.getPressure(pointerIndex), context.touchpadPressureRange)
                : 0;

        return conn.sendControllerTouchEvent((byte)context.controllerNumber, touchType,
                event.getPointerId(pointerIndex),
                normalizedX, normalizedY, normalizedPressure) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    @Override
    public boolean tryHandleTouchpadEvent(MotionEvent event) {
        // Bail if this is not a touchpad or mouse event
        if (event.getSource() != InputDevice.SOURCE_TOUCHPAD &&
                event.getSource() != InputDevice.SOURCE_MOUSE) {
            return false;
        }

        // Only get a context if one already exists. We want to ensure we don't report non-gamepads.
        InputDeviceContext context = inputDeviceContexts.get(event.getDeviceId());
        if (context == null) {
            return false;
        }
        boolean touchpadAsMouse =
                settingsState.get().isTouchpadAsMouse();

        // When we're working with a mouse source instead of a touchpad, we're quite limited in
        // what useful input we can provide via the controller API. The ABS_X/ABS_Y values are
        // screen coordinates rather than touchpad coordinates. For now, we will just support
        // the clickpad button and nothing else.
        if (event.getSource() == InputDevice.SOURCE_MOUSE) {
            // Unlike the touchpad where down and up refer to individual touches on the touchpad,
            // down and up on a mouse indicates the state of the left mouse button.
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    break;
                default:
                    break;
            }

            return !touchpadAsMouse;
        }

        byte touchType;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                touchType = MoonBridge.LI_TOUCH_EVENT_DOWN;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if ((event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                    touchType = MoonBridge.LI_TOUCH_EVENT_CANCEL;
                }
                else {
                    touchType = MoonBridge.LI_TOUCH_EVENT_UP;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                touchType = MoonBridge.LI_TOUCH_EVENT_MOVE;
                break;

            case MotionEvent.ACTION_CANCEL:
                // ACTION_CANCEL applies to *all* pointers in the gesture, so it maps to CANCEL_ALL
                // rather than CANCEL. For a single pointer cancellation, that's indicated via
                // FLAG_CANCELED on a ACTION_POINTER_UP.
                // https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi
                touchType = MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL;
                break;

            case MotionEvent.ACTION_BUTTON_PRESS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && event.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                    context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    return !touchpadAsMouse; // Report as unhandled event to trigger mouse handling
                }
                return false;

            case MotionEvent.ACTION_BUTTON_RELEASE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && event.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                    context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    return !touchpadAsMouse; // Report as unhandled event to trigger mouse handling
                }
                return false;

            default:
                return false;
        }

        // Bail if the user wants gamepad touchpads to control the mouse
        //
        // NB: We do this after processing ACTION_BUTTON_PRESS and ACTION_BUTTON_RELEASE
        // because we want to still send the touchpad button via the gamepad even when
        // configured to use the touchpad for mouse control.
        if (touchpadAsMouse) {
            return false;
        }

        // If we don't have X and Y ranges, we can't process this event
        if (context.touchpadXRange == null || context.touchpadYRange == null) {
            return false;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            // Move events may impact all active pointers
            for (int i = 0; i < event.getPointerCount(); i++) {
                if (!sendTouchpadEventForPointer(context, event, touchType, i)) {
                    // Controller touch events are not supported by the host
                    return false;
                }
            }
            return true;
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            // Cancel impacts all active pointers
            return conn.sendControllerTouchEvent((byte)context.controllerNumber, MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                    0, 0, 0, 0) != MoonBridge.LI_ERR_UNSUPPORTED;
        }
        else {
            // Down and Up events impact the action index pointer
            return sendTouchpadEventForPointer(context, event, touchType, event.getActionIndex());
        }
    }

    @Override
    public boolean handleMotionEvent(MotionEvent event) {
        InputDeviceContext context = getContextForEvent(event);
        if (context == null) {
            return true;
        }

        float lsX = 0, lsY = 0, rsX = 0, rsY = 0, rt = 0, lt = 0, hatX = 0, hatY = 0;

        // We purposefully ignore the historical values in the motion event as it makes
        // the controller feel sluggish for some users.

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            lsX = event.getAxisValue(context.leftStickXAxis);
            lsY = event.getAxisValue(context.leftStickYAxis);
        }

        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            rsX = event.getAxisValue(context.rightStickXAxis);
            rsY = event.getAxisValue(context.rightStickYAxis);
        }

        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            lt = event.getAxisValue(context.leftTriggerAxis);
            rt = event.getAxisValue(context.rightTriggerAxis);
        }

        if (context.hatXAxis != -1 && context.hatYAxis != -1) {
            hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        }

        handleAxisSet(context, lsX, lsY, rsX, rsY, lt, rt, hatX, hatY);

        return true;
    }

    public void handleRumble(short controllerNumber, short lowFreqMotor, short highFreqMotor) {
        boolean foundMatchingDevice = false;
        boolean vibrated = false;

        if (stopped) {
            return;
        }

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);

            if (deviceContext.controllerNumber == controllerNumber) {
                foundMatchingDevice = true;

                if (shouldSuppressInputDeviceRumble(deviceContext)) {
                    continue;
                }

                vibrated |= rumbleInputDeviceContext(deviceContext, lowFreqMotor, highFreqMotor);
            }
        }

        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);

            if (deviceContext.controllerNumber == controllerNumber) {
                foundMatchingDevice = vibrated = true;
                if (!shouldSuppressControllerRumble()) {
                    deviceContext.device.rumble(lowFreqMotor, highFreqMotor);
                }
            }
        }

        // We may decide to rumble the device for player 1
        if (controllerNumber == 0) {
            ControllerSettings settings = settingsState.get();
            // If we didn't find a matching device, it must be the on-screen
            // controls that triggered the rumble. Vibrate the device if
            // the user has requested that behavior.
            if (!foundMatchingDevice &&
                    settings.isOnscreenControllerEnabled() &&
                    !settings.isOnlyL3R3Enabled() &&
                    settings.isOnscreenRumbleEnabled()) {
                vibrationRenderer.rumbleDevice(
                        lowFreqMotor,
                        highFreqMotor);
            }
            else if (foundMatchingDevice &&
                    !vibrated &&
                    settings.isFallbackDeviceRumbleEnabled()) {
                // We found a device to vibrate but it didn't have rumble support. The user
                // has requested us to vibrate the device in this case.

                short lowFreqMotorAdjusted =
                        ControllerRumbleAmplitudes
                                .scaleProtocolMotor(
                                        lowFreqMotor,
                                        settings
                                                .getFallbackDeviceRumbleStrengthPercent());
                short highFreqMotorAdjusted =
                        ControllerRumbleAmplitudes
                                .scaleProtocolMotor(
                                        highFreqMotor,
                                        settings
                                                .getFallbackDeviceRumbleStrengthPercent());

                vibrationRenderer.rumbleDevice(
                        lowFreqMotorAdjusted,
                        highFreqMotorAdjusted);
            }
        }
    }

    public void handleRumbleTriggers(short controllerNumber, short leftTrigger, short rightTrigger) {
        if (stopped) {
            return;
        }

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);

            if (deviceContext.controllerNumber == controllerNumber) {
                if (shouldSuppressInputDeviceRumble(deviceContext)) {
                    continue;
                }

                vibrationRenderer.rumbleTriggers(
                        deviceContext.vibrationTarget,
                        leftTrigger,
                        rightTrigger);
            }
        }

        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);

            if (deviceContext.controllerNumber == controllerNumber) {
                if (!shouldSuppressControllerRumble()) {
                    deviceContext.device.rumbleTriggers(leftTrigger, rightTrigger);
                }
            }
        }
    }

    private SensorEventListener createSensorListener(
            final GenericControllerContext context,
            final short controllerNumber,
            final byte motionType,
            final boolean needsDeviceOrientationCorrection) {
        return new SensorEventListener() {
            private final ControllerMotionSampleTransformer sampleTransformer =
                    new ControllerMotionSampleTransformer();

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                int deviceRotation =
                        needsDeviceOrientationCorrection
                                ? activityContext
                                        .getWindowManager()
                                        .getDefaultDisplay()
                                        .getRotation()
                                : ControllerMotionSampleTransformer.ROTATION_0;
                boolean gyroscope =
                        motionType == MoonBridge.LI_MOTION_TYPE_GYRO;
                if (!sampleTransformer.update(
                        sensorEvent.values[0],
                        sensorEvent.values[1],
                        sensorEvent.values[2],
                        deviceRotation,
                        needsDeviceOrientationCorrection,
                        gyroscope)) {
                    return;
                }

                ControllerSettings settings = settingsState.get();
                if (settings.isForceGyroEnabled()) {
                    if (settings.isForceGyroLeftTriggerRequired() &&
                            getControllerLeftTriggerState(
                                    controllerNumber) < 200) {
                        context.gyroStickTranslator.reset();
                        context.rightStickX =
                                context.gyroStickTranslator
                                        .getRightStickX();
                        context.rightStickY =
                                context.gyroStickTranslator
                                        .getRightStickY();
                        sendControllerInputPacket(context);
                        return;
                    }
                    if (gyroscope) {
                        if (!needsDeviceOrientationCorrection) {
                            deviceRotation =
                                    activityContext
                                            .getWindowManager()
                                            .getDefaultDisplay()
                                            .getRotation();
                        }
                        context.gyroStickTranslator.update(
                                sampleTransformer.getRawX(),
                                sampleTransformer.getRawY(),
                                deviceRotation,
                                settings.areForceGyroAxesSwapped(),
                                settings.getForceGyroSensitivityPercent());
                        context.rightStickX =
                                context.gyroStickTranslator
                                        .getRightStickX();
                        context.rightStickY =
                                context.gyroStickTranslator
                                        .getRightStickY();
                        sendControllerInputPacket(context);
                    }
                    return;
                }

                conn.sendControllerMotionEvent(
                        (byte) controllerNumber,
                        motionType,
                        sampleTransformer.getTransformedX(),
                        sampleTransformer.getTransformedY(),
                        sampleTransformer.getTransformedZ());
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    }

    public void handleSetMotionEventState(
            final short controllerNumber,
            final byte motionType,
            short reportRateHz) {
        if (stopped) {
            return;
        }

        // The on-device sensors may supplement a virtual controller when requested by the user.
        if (settingsState.get()
                .isVirtualControllerMotionEnabled()) {
            defaultContext.sensorManager = deviceSensorManager;
            defaultContext.motionSession.setReportRate(
                    controllerNumber,
                    motionType,
                    reportRateHz);
        }

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);

            if (deviceContext.controllerNumber == controllerNumber) {
                deviceContext.motionSession.setReportRate(
                        controllerNumber,
                        motionType,
                        reportRateHz);
                break;
            }
        }
    }

    public void handleSetControllerLED(short controllerNumber, byte r, byte g, byte b) {
        if (stopped) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (int i = 0; i < inputDeviceContexts.size(); i++) {
                InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);

                // Ignore input devices without an RGB LED
                if (deviceContext.controllerNumber == controllerNumber && deviceContext.hasRgbLed) {
                    // Create a new light session if one doesn't already exist
                    if (deviceContext.lightsSession == null) {
                        deviceContext.lightsSession = deviceContext.inputDevice.getLightsManager().openSession();
                    }

                    // Convert the RGB components into the integer value that LightState uses
                    int argbValue = 0xFF000000 | ((r << 16) & 0xFF0000) | ((g << 8) & 0xFF00) | (b & 0xFF);
                    LightState lightState = new LightState.Builder().setColor(argbValue).build();

                    // Set the RGB value for each RGB-controllable LED on the device
                    LightsRequest.Builder lightsRequestBuilder = new LightsRequest.Builder();
                    for (Light light : deviceContext.inputDevice.getLightsManager().getLights()) {
                        if (light.hasRgbControl()) {
                            lightsRequestBuilder.addLight(light, lightState);
                        }
                    }

                    // Apply the LED changes
                    deviceContext.lightsSession.requestLights(lightsRequestBuilder.build());
                }
            }
        }
    }

    @Override
    public boolean handleButtonUp(KeyEvent event) {
        InputDeviceContext context = getContextForEvent(event);
        if (context == null) {
            return true;
        }
        ControllerSettings settings = settingsState.get();

        int keyCode = handleRemapping(context, event);
        if (keyCode < 0) {
            return keyCode == ControllerButtonMapper.CONSUME;
        }

        if (settings.areFaceButtonsFlipped()) {
            keyCode =
                    ControllerButtonMapper.flipFaceButtons(
                            keyCode);
        }

        // If the button hasn't been down long enough, sleep for a bit before sending the up event
        // This allows "instant" button presses (like OUYA's virtual menu button) to work. This
        // path should not be triggered during normal usage.
        int buttonDownTime = (int)(event.getEventTime() - event.getDownTime());
        if (buttonDownTime < ControllerHandler.MINIMUM_BUTTON_DOWN_TIME_MS)
        {
            // Since our sleep time is so short (<= 25 ms), it shouldn't cause a problem doing this
            // in the UI thread.
            try {
                Thread.sleep(ControllerHandler.MINIMUM_BUTTON_DOWN_TIME_MS - buttonDownTime);
            } catch (InterruptedException e) {
                e.printStackTrace();

                // InterruptedException clears the thread's interrupt status. Since we can't
                // handle that here, we will re-interrupt the thread to set the interrupt
                // status back to true.
                Thread.currentThread().interrupt();
            }
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BUTTON_MODE:
            if (settings.isMouseEmulationEnabled() &&
                    settings.getMouseEmulationButton() == 1) {
                if ((context.inputMap & ControllerPacket.SPECIAL_BUTTON_FLAG) != 0) {
                    if (settings.doesMouseEmulationOpenGameMenu()) {
                        //todo 展示快捷菜单
                        gestures.showGameMenu(context);
                    }else{
                        context.toggleMouseEmulation();
                    }
                }
            }
            context.inputMap &= ~ControllerPacket.SPECIAL_BUTTON_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_START:
        case KeyEvent.KEYCODE_MENU:
            // Sometimes we'll get a spurious key up event on controller disconnect.
            // Make sure it's real by checking that the key is actually down before taking
            // any action.
            if (settings.isMouseEmulationEnabled() &&
                    settings.getMouseEmulationButton() == 0) {
                if ((context.inputMap & ControllerPacket.PLAY_FLAG) != 0 &&
                        event.getEventTime() - context.startDownTime > ControllerHandler.START_DOWN_TIME_MOUSE_MODE_MS) {
                    if (settings.doesMouseEmulationOpenGameMenu()) {
                        //todo 展示快捷菜单
                        gestures.showGameMenu(context);
                    }else{
                        context.toggleMouseEmulation();
                    }
                }
            }
            context.inputMap &= ~ControllerPacket.PLAY_FLAG;
            break;
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_BUTTON_SELECT:
            if (settings.isMouseEmulationEnabled() &&
                    settings.getMouseEmulationButton() == 2) {
                if ((context.inputMap & ControllerPacket.BACK_FLAG) != 0 &&
                        event.getEventTime() - context.startDownTime > ControllerHandler.START_DOWN_TIME_MOUSE_MODE_MS) {
                    if (settings.doesMouseEmulationOpenGameMenu()) {
                        //todo 展示快捷菜单
                        gestures.showGameMenu(context);
                    }else{
                        context.toggleMouseEmulation();
                    }
                }
            }
            context.inputMap &= ~ControllerPacket.BACK_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.UP_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.DOWN_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.UP_FLAG | ControllerPacket.LEFT_FLAG);
            break;
        case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.UP_FLAG | ControllerPacket.RIGHT_FLAG);
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.DOWN_FLAG | ControllerPacket.LEFT_FLAG);
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.DOWN_FLAG | ControllerPacket.RIGHT_FLAG);
            break;
        case KeyEvent.KEYCODE_BUTTON_B:
            context.inputMap &= ~ControllerPacket.B_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_BUTTON_A:
            context.inputMap &= ~ControllerPacket.A_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_X:
            context.inputMap &= ~ControllerPacket.X_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_Y:
            context.inputMap &= ~ControllerPacket.Y_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L1:
            context.inputMap &= ~ControllerPacket.LB_FLAG;
            context.lastLbUpTime = event.getEventTime();
            break;
        case KeyEvent.KEYCODE_BUTTON_R1:
            context.inputMap &= ~ControllerPacket.RB_FLAG;
            context.lastRbUpTime = event.getEventTime();
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBL:
            context.inputMap &= ~ControllerPacket.LS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBR:
            context.inputMap &= ~ControllerPacket.RS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_MEDIA_RECORD: // Xbox Series X Share button
            context.inputMap &= ~ControllerPacket.MISC_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_1: // PS4/PS5 touchpad button (prior to 4.10)
            context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L2:
            if (context.leftTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.leftTrigger = 0;
            break;
        case KeyEvent.KEYCODE_BUTTON_R2:
            if (context.rightTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.rightTrigger = 0;
            break;
        case KeyEvent.KEYCODE_UNKNOWN:
            // Paddles aren't mapped in any of the Android key layout files,
            // so we need to handle the evdev key codes directly.
            if (context.hasPaddles) {
                switch (event.getScanCode()) {
                    case 0x2c4: // BTN_TRIGGER_HAPPY5
                        context.inputMap &= ~ControllerPacket.PADDLE1_FLAG;
                        break;
                    case 0x2c5: // BTN_TRIGGER_HAPPY6
                        context.inputMap &= ~ControllerPacket.PADDLE2_FLAG;
                        break;
                    case 0x2c6: // BTN_TRIGGER_HAPPY7
                        context.inputMap &= ~ControllerPacket.PADDLE3_FLAG;
                        break;
                    case 0x2c7: // BTN_TRIGGER_HAPPY8
                        context.inputMap &= ~ControllerPacket.PADDLE4_FLAG;
                        break;
                    default:
                        return false;
                }
            }
            else {
                return false;
            }
            break;
        default:
            return false;
        }

        // Check if we're emulating the select button
        if ((context.emulatingButtonFlags & ControllerHandler.EMULATING_SELECT) != 0)
        {
            // If either start or LB is up, select comes up too
            if ((context.inputMap & ControllerPacket.PLAY_FLAG) == 0 ||
                (context.inputMap & ControllerPacket.LB_FLAG) == 0)
            {
                context.inputMap &= ~ControllerPacket.BACK_FLAG;

                context.emulatingButtonFlags &= ~ControllerHandler.EMULATING_SELECT;
            }
        }

        // Check if we're emulating the special button
        if ((context.emulatingButtonFlags & ControllerHandler.EMULATING_SPECIAL) != 0)
        {
            // If either start or select and RB is up, the special button comes up too
            if ((context.inputMap & ControllerPacket.PLAY_FLAG) == 0 ||
                ((context.inputMap & ControllerPacket.BACK_FLAG) == 0 &&
                 (context.inputMap & ControllerPacket.RB_FLAG) == 0))
            {
                context.inputMap &= ~ControllerPacket.SPECIAL_BUTTON_FLAG;

                context.emulatingButtonFlags &= ~ControllerHandler.EMULATING_SPECIAL;
            }
        }

        // Check if we're emulating the touchpad button
        if ((context.emulatingButtonFlags & ControllerHandler.EMULATING_TOUCHPAD) != 0)
        {
            // If either select or LB is up, touchpad comes up too
            if ((context.inputMap & ControllerPacket.BACK_FLAG) == 0 ||
                    (context.inputMap & ControllerPacket.LB_FLAG) == 0)
            {
                context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;

                context.emulatingButtonFlags &= ~ControllerHandler.EMULATING_TOUCHPAD;
            }
        }

        sendControllerInputPacket(context);

        if (context.pendingExit && context.inputMap == 0) {
            // All buttons from the quit combo are lifted. Finish the activity now.
            activityContext.finish();
        }

        return true;
    }

    @Override
    public boolean handleButtonDown(KeyEvent event) {
        InputDeviceContext context = getContextForEvent(event);
        if (context == null) {
            return true;
        }

        int keyCode = handleRemapping(context, event);
        if (keyCode < 0) {
            return keyCode == ControllerButtonMapper.CONSUME;
        }

        if (settingsState.get().areFaceButtonsFlipped()) {
            keyCode =
                    ControllerButtonMapper.flipFaceButtons(
                            keyCode);
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BUTTON_MODE:
            context.hasMode = true;
            context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_START:
        case KeyEvent.KEYCODE_MENU:
            if (event.getRepeatCount() == 0) {
                context.startDownTime = event.getEventTime();
            }
            context.inputMap |= ControllerPacket.PLAY_FLAG;
            break;
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_BUTTON_SELECT:
            context.hasSelect = true;
            context.inputMap |= ControllerPacket.BACK_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.UP_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.DOWN_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.UP_FLAG | ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.UP_FLAG | ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.DOWN_FLAG | ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.DOWN_FLAG | ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_B:
            context.inputMap |= ControllerPacket.B_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_BUTTON_A:
            context.inputMap |= ControllerPacket.A_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_X:
            context.inputMap |= ControllerPacket.X_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_Y:
            context.inputMap |= ControllerPacket.Y_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L1:
            context.inputMap |= ControllerPacket.LB_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_R1:
            context.inputMap |= ControllerPacket.RB_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBL:
            context.inputMap |= ControllerPacket.LS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBR:
            context.inputMap |= ControllerPacket.RS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_MEDIA_RECORD: // Xbox Series X Share button
            context.inputMap |= ControllerPacket.MISC_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_1: // PS4/PS5 touchpad button (prior to 4.10)
            context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L2:
            if (context.leftTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.leftTrigger = (byte)0xFF;
            break;
        case KeyEvent.KEYCODE_BUTTON_R2:
            if (context.rightTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.rightTrigger = (byte)0xFF;
            break;
        case KeyEvent.KEYCODE_UNKNOWN:
            // Paddles aren't mapped in any of the Android key layout files,
            // so we need to handle the evdev key codes directly.
            if (context.hasPaddles) {
                switch (event.getScanCode()) {
                    case 0x2c4: // BTN_TRIGGER_HAPPY5
                        context.inputMap |= ControllerPacket.PADDLE1_FLAG;
                        break;
                    case 0x2c5: // BTN_TRIGGER_HAPPY6
                        context.inputMap |= ControllerPacket.PADDLE2_FLAG;
                        break;
                    case 0x2c6: // BTN_TRIGGER_HAPPY7
                        context.inputMap |= ControllerPacket.PADDLE3_FLAG;
                        break;
                    case 0x2c7: // BTN_TRIGGER_HAPPY8
                        context.inputMap |= ControllerPacket.PADDLE4_FLAG;
                        break;
                    default:
                        return false;
                }
            }
            else {
                return false;
            }
            break;
        default:
            return false;
        }

        // Start+Back+LB+RB is the quit combo
        if (context.inputMap == (ControllerPacket.BACK_FLAG | ControllerPacket.PLAY_FLAG |
                                 ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG)) {
            // Wait for the combo to lift and then finish the activity
            context.pendingExit = true;
        }

        // Start+LB acts like select for controllers with one button
        if (!context.hasSelect) {
            if (context.inputMap == (ControllerPacket.PLAY_FLAG | ControllerPacket.LB_FLAG) ||
                    (context.inputMap == ControllerPacket.PLAY_FLAG &&
                            event.getEventTime() - context.lastLbUpTime <= MAXIMUM_BUMPER_UP_DELAY_MS))
            {
                context.inputMap &= ~(ControllerPacket.PLAY_FLAG | ControllerPacket.LB_FLAG);
                context.inputMap |= ControllerPacket.BACK_FLAG;

                context.emulatingButtonFlags |= ControllerHandler.EMULATING_SELECT;
            }
        }
        else if (context.needsClickpadEmulation) {
            // Select+LB acts like the clickpad when we're faking a PS4 controller for motion support
            if (context.inputMap == (ControllerPacket.BACK_FLAG | ControllerPacket.LB_FLAG) ||
                    (context.inputMap == ControllerPacket.BACK_FLAG &&
                            event.getEventTime() - context.lastLbUpTime <= MAXIMUM_BUMPER_UP_DELAY_MS))
            {
                context.inputMap &= ~(ControllerPacket.BACK_FLAG | ControllerPacket.LB_FLAG);
                context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;

                context.emulatingButtonFlags |= ControllerHandler.EMULATING_TOUCHPAD;
            }
        }

        // If there is a physical select button, we'll use Start+Select as the special button combo
        // otherwise we'll use Start+RB.
        if (!context.hasMode) {
            if (context.hasSelect) {
                if (context.inputMap == (ControllerPacket.PLAY_FLAG | ControllerPacket.BACK_FLAG)) {
                    context.inputMap &= ~(ControllerPacket.PLAY_FLAG | ControllerPacket.BACK_FLAG);
                    context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;

                    context.emulatingButtonFlags |= ControllerHandler.EMULATING_SPECIAL;
                }
            }
            else {
                if (context.inputMap == (ControllerPacket.PLAY_FLAG | ControllerPacket.RB_FLAG) ||
                        (context.inputMap == ControllerPacket.PLAY_FLAG &&
                                event.getEventTime() - context.lastRbUpTime <= MAXIMUM_BUMPER_UP_DELAY_MS))
                {
                    context.inputMap &= ~(ControllerPacket.PLAY_FLAG | ControllerPacket.RB_FLAG);
                    context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;

                    context.emulatingButtonFlags |= ControllerHandler.EMULATING_SPECIAL;
                }
            }
        }

        // We don't need to send repeat key down events, but the platform
        // sends us events that claim to be repeats but they're from different
        // devices, so we just send them all and deal with some duplicates.
        sendControllerInputPacket(context);
        return true;
    }

    public void reportOscState(int buttonFlags,
                               short leftStickX, short leftStickY,
                               short rightStickX, short rightStickY,
                               byte leftTrigger, byte rightTrigger) {
        defaultContext.leftStickX = leftStickX;
        defaultContext.leftStickY = leftStickY;

        defaultContext.rightStickX = rightStickX;
        defaultContext.rightStickY = rightStickY;

        defaultContext.leftTrigger = leftTrigger;
        defaultContext.rightTrigger = rightTrigger;

        defaultContext.inputMap = buttonFlags;

        sendControllerInputPacket(defaultContext);
    }

    @Override
    public void reportControllerState(int controllerId, int buttonFlags,
                                      float leftStickX, float leftStickY,
                                      float rightStickX, float rightStickY,
                                      float leftTrigger, float rightTrigger) {
        GenericControllerContext context = usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }

        Vector2d leftStickVector = populateCachedVector(leftStickX, leftStickY);

        handleDeadZone(leftStickVector, context.leftStickDeadzoneRadius);

        context.leftStickX = (short) (leftStickVector.getX() * 0x7FFE);
        context.leftStickY = (short) (-leftStickVector.getY() * 0x7FFE);

        Vector2d rightStickVector = populateCachedVector(rightStickX, rightStickY);

        handleDeadZone(rightStickVector, context.rightStickDeadzoneRadius);

        context.rightStickX = (short) (rightStickVector.getX() * 0x7FFE);
        context.rightStickY = (short) (-rightStickVector.getY() * 0x7FFE);

        if (leftTrigger <= context.triggerDeadzone) {
            leftTrigger = 0;
        }
        if (rightTrigger <= context.triggerDeadzone) {
            rightTrigger = 0;
        }

        context.leftTrigger = (byte)(leftTrigger * 0xFF);
        context.rightTrigger = (byte)(rightTrigger * 0xFF);

        context.inputMap = buttonFlags;

        sendControllerInputPacket(context);
    }

    private float clampUnitRange(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0f;
        }
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    @Override
    public void reportControllerMotion(int controllerId, byte motionType, float motionX, float motionY, float motionZ) {
        GenericControllerContext context = usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }
        if (!settingsState.get()
                .isUsbGyroscopeReportingEnabled()) {
            return;
        }
        conn.sendControllerMotionEvent((byte)context.controllerNumber, motionType, motionX, motionY, motionZ);
    }

    @Override
    public void reportControllerTouchpadEvent(int controllerId, byte eventType, int pointerId,
                                              float x, float y, float pressure) {
        UsbDeviceContext context = usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }

        assignControllerNumberIfNeeded(context);

        conn.sendControllerTouchEvent((byte) context.controllerNumber, eventType, pointerId,
                clampUnitRange(x), clampUnitRange(y), clampUnitRange(pressure));
    }

    @Override
    public void deviceRemoved(AbstractController controller) {
        UsbDeviceContext context = usbDeviceContexts.get(controller.getControllerId());
        if (context != null) {
            controller.stopAdvancedAudioHaptics();
            releaseControllerNumber(context);
            context.destroy();
            usbDeviceContexts.remove(controller.getControllerId());
        }
    }

    @Override
    public void deviceAdded(AbstractController controller) {
        if (stopped) {
            return;
        }

        if (shouldUseControllerAudioHaptics() && controller.hasAdvancedAudioHapticsSupport()) {
            controller.startAdvancedAudioHaptics();
        }

        UsbDeviceContext context = createUsbDeviceContextForDevice(controller);
        usbDeviceContexts.put(controller.getControllerId(), context);
    }

    public boolean hasActiveUsbController() {
        return usbDeviceContexts.size() > 0;
    }

    public String getActiveUsbControllerTypeDisplayName() {
        if (usbDeviceContexts.size() <= 0) {
            return "";
        }

        UsbDeviceContext context = usbDeviceContexts.valueAt(0);
        if (context == null || context.device == null) {
            return "";
        }

        String controllerType;
        byte type = context.device.getType();
        if (type == MoonBridge.LI_CTYPE_XBOX) {
            controllerType = "Xbox";
        }
        else if (type == MoonBridge.LI_CTYPE_PS) {
            controllerType = "DS";
        }
        else if (type == MoonBridge.LI_CTYPE_NINTENDO) {
            controllerType = "NS";
        }
        else {
            controllerType = context.device.getClass().getSimpleName();
        }

        if (usbDeviceContexts.size() > 1) {
            return controllerType + " x" + usbDeviceContexts.size();
        }
        return controllerType;
    }

    class GenericControllerContext implements GameInputDevice{
        public int id;
        public boolean external;

        public int vendorId;
        public int productId;

        public float leftStickDeadzoneRadius;
        public float rightStickDeadzoneRadius;
        public float triggerDeadzone;

        public boolean assignedControllerNumber;
        public boolean reservedControllerNumber;
        public short controllerNumber;

        public int inputMap = 0;
        public byte leftTrigger = 0x00;
        public byte rightTrigger = 0x00;
        public short rightStickX = 0x0000;
        public short rightStickY = 0x0000;
        public short leftStickX = 0x0000;
        public short leftStickY = 0x0000;

        public final ControllerGyroStickTranslator
                gyroStickTranslator =
                new ControllerGyroStickTranslator();

        public final ControllerMouseEmulationTranslator
                mouseEmulationTranslator =
                new ControllerMouseEmulationTranslator();
        private final ControllerMouseEmulationSession
                mouseEmulationSession =
                new ControllerMouseEmulationSession(
                        mouseEmulationScheduler,
                        new Runnable() {
                            @Override
                            public void run() {
                                ControllerSettings settings =
                                        settingsState.get();
                                mouseEmulationTranslator
                                        .translateMotion(
                                                leftStickX,
                                                leftStickY,
                                                rightStickX,
                                                rightStickY,
                                                leftTrigger & 0xFF,
                                                rightTrigger & 0xFF,
                                                settings
                                                        .getMouseSensitivityPercent(),
                                                settings
                                                        .getAnalogStickForScrolling(),
                                                mouseEmulationOutput);
                            }
                        });

        @Override
        public void toggleMouseEmulation() {
            boolean active =
                    mouseEmulationSession.toggle();
            UiToast.makeText(
                    activityContext,
                    "手柄键鼠模式: " +
                            (active ? "开启" : "关闭"),
                    UiToast.LENGTH_SHORT).show();
        }

        public boolean isMouseEmulationActive() {
            return mouseEmulationSession.isActive();
        }

        protected void restoreMouseEmulation(
                boolean active) {
            mouseEmulationSession.setActive(active);
        }

        public void destroy() {
            mouseEmulationSession.destroy();
        }

        public void sendControllerArrival() {}

    }

    class InputDeviceContext extends GenericControllerContext {
        public String name;
        public ControllerVibrationRenderer.Target vibrationTarget;

        public SensorManager sensorManager;
        private SensorEventListener gyroListener;
        private SensorManager gyroRegistrationManager;
        private SensorEventListener accelListener;
        private SensorManager accelRegistrationManager;
        private final ControllerMotionSession motionSession =
                new ControllerMotionSession(
                        motionSensorScheduler,
                        new ControllerMotionSession.Registrations() {
                            @Override
                            public boolean replace(
                                    short controllerNumber,
                                    byte motionType,
                                    short reportRateHz) {
                                return replaceMotionRegistration(
                                        controllerNumber,
                                        motionType,
                                        reportRateHz);
                            }

                            @Override
                            public void sendNeutralGyroscope(
                                    short controllerNumber) {
                                conn.sendControllerMotionEvent(
                                        (byte) controllerNumber,
                                        MoonBridge.LI_MOTION_TYPE_GYRO,
                                        0.f,
                                        0.f,
                                        0.f);
                            }
                        });

        public InputDevice inputDevice;

        public boolean hasRgbLed;
        public LightsManager.LightsSession lightsSession;

        // These are Android BatteryManager status values, not Moonlight values
        public int lastReportedBatteryStatus;
        public float lastReportedBatteryCapacity;

        public int leftStickXAxis = -1;
        public int leftStickYAxis = -1;

        public int rightStickXAxis = -1;
        public int rightStickYAxis = -1;

        public int leftTriggerAxis = -1;
        public int rightTriggerAxis = -1;
        public boolean triggersIdleNegative;
        public boolean leftTriggerAxisUsed, rightTriggerAxisUsed;

        public int hatXAxis = -1;
        public int hatYAxis = -1;
        public boolean hatXAxisUsed, hatYAxisUsed;

        InputDevice.MotionRange touchpadXRange;
        InputDevice.MotionRange touchpadYRange;
        InputDevice.MotionRange touchpadPressureRange;

        private ControllerButtonMapper buttonMapper;
        public boolean hasJoystickAxes;
        public boolean pendingExit;

        public int emulatingButtonFlags = 0;
        public boolean hasSelect;
        public boolean hasMode;
        public boolean hasPaddles;
        public boolean hasShare;
        public boolean needsClickpadEmulation;

        // Used for OUYA bumper state tracking since they force all buttons
        // up when the OUYA button goes down. We watch the last time we get
        // a bumper up and compare that to our maximum delay when we receive
        // a Start button press to see if we should activate one of our
        // emulated button combos.
        public long lastLbUpTime = 0;
        public long lastRbUpTime = 0;

        public long startDownTime = 0;

        public final Runnable batteryStateUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                sendControllerBatteryPacket(InputDeviceContext.this);

                // Requeue the callback
                backgroundThreadHandler.postDelayed(this, BATTERY_RECHECK_INTERVAL_MS);
            }
        };

        @Override
        public void destroy() {
            super.destroy();
            vibrationRenderer.cancel(vibrationTarget);

            motionSession.destroy();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (lightsSession != null) {
                    lightsSession.close();
                }
            }
            //是否上报电池状态
            if (settingsState.get().isBatteryReportingEnabled()) {
                backgroundThreadHandler.removeCallbacks(batteryStateUpdateRunnable);
            }
        }

        @Override
        public void sendControllerArrival() {
            byte type;
            switch (inputDevice.getVendorId()) {
                case 0x045e: // Microsoft
                    type = MoonBridge.LI_CTYPE_XBOX;
                    break;
                case 0x054c: // Sony
                    type = MoonBridge.LI_CTYPE_PS;
                    break;
                case 0x057e: // Nintendo
                    type = MoonBridge.LI_CTYPE_NINTENDO;
                    break;
                default:
                    // Consult SDL's controller type list to see if it knows
                    type = MoonBridge.guessControllerType(inputDevice.getVendorId(), inputDevice.getProductId());
                    break;
            }

            int supportedButtonFlags = 0;
            for (Map.Entry<Integer, Integer> entry :
                    ControllerButtonMapper
                            .getProtocolButtonMappings()
                            .entrySet()) {
                if (inputDevice.hasKeys(entry.getKey())[0]) {
                    supportedButtonFlags |= entry.getValue();
                }
            }

            boolean hasAccelerometer =
                    sensorManager != null &&
                            sensorManager.getDefaultSensor(
                                    Sensor.TYPE_ACCELEROMETER) != null;
            boolean hasGyroscope =
                    sensorManager != null &&
                            sensorManager.getDefaultSensor(
                                    Sensor.TYPE_GYROSCOPE) != null;
            boolean hasTouchpad =
                    (inputDevice.getSources() &
                            InputDevice.SOURCE_TOUCHPAD) ==
                            InputDevice.SOURCE_TOUCHPAD;
            ControllerArrivalReport report =
                    ControllerArrivalReport.builder(
                                    type,
                                    supportedButtonFlags)
                            .hasPaddles(hasPaddles)
                            .hasShareButton(hasShare)
                            .hasHorizontalHatAxis(
                                    getMotionRangeForJoystickAxis(
                                            inputDevice,
                                            MotionEvent.AXIS_HAT_X) != null)
                            .hasVerticalHatAxis(
                                    getMotionRangeForJoystickAxis(
                                            inputDevice,
                                            MotionEvent.AXIS_HAT_Y) != null)
                            .hasAdvancedInputDeviceApis(
                                    Build.VERSION.SDK_INT >=
                                            Build.VERSION_CODES.S)
                            .hasQuadVibrators(
                                    vibrationTarget
                                            .hasQuadVibrators())
                            .hasVibratorManager(
                                    vibrationTarget
                                            .hasVibratorManager())
                            .hasLegacyVibrator(
                                    vibrationTarget
                                            .hasLegacyVibrator())
                            .external(external)
                            .hasRgbLed(hasRgbLed)
                            .hasReliableRgbLedDetection(
                                    Build.VERSION.SDK_INT >=
                                            Build.VERSION_CODES
                                                    .UPSIDE_DOWN_CAKE)
                            .hasAnalogTriggers(
                                    leftTriggerAxis != -1 ||
                                            rightTriggerAxis != -1)
                            .hasAccelerometer(hasAccelerometer)
                            .hasGyroscope(hasGyroscope)
                            .requiresGenericMotionControllerType(
                                    type !=
                                            MoonBridge.LI_CTYPE_PS &&
                                            sensorManager != null)
                            .recognizedByShieldExtensions(
                                    sceManager.isRecognizedDevice(
                                            inputDevice))
                            .hasTouchpad(hasTouchpad)
                            .hasClickpad(
                                    hasTouchpad &&
                                            hasButtonUnderTouchpad(
                                                    inputDevice,
                                                    type))
                            .build();

            needsClickpadEmulation =
                    report.isClickpadEmulationRequired();
            if (needsClickpadEmulation) {
                LimeLog.info(
                        "Reporting an unknown controller type while " +
                                "emulating motion sensors");
            }

            conn.sendControllerArrivalEvent((byte)controllerNumber, getActiveControllerMask(),
                    report.getReportedType(),
                    report.getSupportedButtonFlags(),
                    report.getCapabilities());

            // After reporting arrival to the host, send initial battery state and begin monitoring
            //是否上报电池状态
            if (settingsState.get().isBatteryReportingEnabled()) {
                backgroundThreadHandler.post(batteryStateUpdateRunnable);
            }
        }

        public void migrateContext(InputDeviceContext oldContext) {
            boolean restoreMouseEmulationActive =
                    oldContext.isMouseEmulationActive();
            ControllerMotionSession.DesiredState motionState =
                    oldContext.motionSession.snapshotDesiredState();
            boolean usedDeviceSensorManager =
                    oldContext.sensorManager == deviceSensorManager;
            // Take ownership of the sensor and light sessions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.lightsSession = oldContext.lightsSession;
                oldContext.lightsSession = null;
            }
            // Don't release the controller number, because we will carry it over if it is present.
            // We also want to make sure the change is invisible to the host PC to avoid an add/remove
            // cycle for the gamepad which may break some games.
            oldContext.destroy();
            // Copy over existing controller number state
            this.assignedControllerNumber = oldContext.assignedControllerNumber;
            this.reservedControllerNumber = oldContext.reservedControllerNumber;
            this.controllerNumber = oldContext.controllerNumber;

            // We may have set this device to use the built-in sensor manager. If so, do that again.
            if (usedDeviceSensorManager) {
                this.sensorManager = deviceSensorManager;
            }
            motionSession.restoreDesiredState(motionState);

            // Copy state initialized in reportControllerArrival()
            this.needsClickpadEmulation = oldContext.needsClickpadEmulation;

            // Re-enable sensors on the new context
            enableSensors();

            // Refresh battery state and start the battery state polling again
            //是否上报电池状态
            if (settingsState.get().isBatteryReportingEnabled()) {
                backgroundThreadHandler.post(batteryStateUpdateRunnable);
            }
            restoreMouseEmulation(
                    restoreMouseEmulationActive);
        }

        public void disableSensors() {
            motionSession.disable();
        }

        public void enableSensors() {
            // We allow 1 second for the input device to settle before re-enabling sensors.
            // Pointer capture can cause the input device to change, which can cause
            // InputDeviceSensorManager to crash due to missing null checks on the InputDevice.
            motionSession.enableAfterDeviceSettles();
        }

        private boolean replaceMotionRegistration(
                short motionControllerNumber,
                byte motionType,
                short reportRateHz) {
            SensorEventListener previousListener;
            SensorManager previousManager;
            int sensorType;

            if (motionType == MoonBridge.LI_MOTION_TYPE_ACCEL) {
                previousListener = accelListener;
                previousManager = accelRegistrationManager;
                sensorType = Sensor.TYPE_ACCELEROMETER;
                accelListener = null;
                accelRegistrationManager = null;
            }
            else if (motionType == MoonBridge.LI_MOTION_TYPE_GYRO) {
                previousListener = gyroListener;
                previousManager = gyroRegistrationManager;
                sensorType = Sensor.TYPE_GYROSCOPE;
                gyroListener = null;
                gyroRegistrationManager = null;
            }
            else {
                return false;
            }

            boolean wasActive = previousListener != null;
            if (wasActive && previousManager != null) {
                previousManager.unregisterListener(previousListener);
            }

            SensorManager registrationManager = sensorManager;
            if (reportRateHz == 0 || registrationManager == null) {
                return wasActive;
            }

            Sensor sensor = registrationManager.getDefaultSensor(sensorType);
            if (sensor == null) {
                return wasActive;
            }

            SensorEventListener listener =
                    createSensorListener(
                            this,
                            motionControllerNumber,
                            motionType,
                            registrationManager == deviceSensorManager);
            boolean registered =
                    registrationManager.registerListener(
                            listener,
                            sensor,
                            1_000_000 / reportRateHz);
            if (!registered) {
                return wasActive;
            }

            if (motionType == MoonBridge.LI_MOTION_TYPE_ACCEL) {
                accelListener = listener;
                accelRegistrationManager = registrationManager;
            }
            else {
                gyroListener = listener;
                gyroRegistrationManager = registrationManager;
            }
            return wasActive;
        }
    }

    class UsbDeviceContext extends GenericControllerContext {
        public AbstractController device;

        @Override
        public void destroy() {
            super.destroy();
            // Nothing for now
        }

        @Override
        public void sendControllerArrival() {
            conn.sendControllerArrivalEvent((byte)controllerNumber, getActiveControllerMask(),
                    device.getType(), device.getSupportedButtonFlags(), device.getCapabilities());
        }
    }

    /**
     * 设置自适应扳机
     * @param mode 模式 0关闭 1阻尼 2扳机 6自动步枪
     * @param strength 震动强度
     * @param frequency 震动频率（mode=6生效）
     * @param start 起始位置
     * @param end 结束位置
     * @return
     */
    public void setDualSenseTrigger(int mode,int strength,int frequency,int start,int end) {
        if (stopped) {
            return;
        }
        byte[] data=DualSenseController.setTrigger(mode,strength,frequency,start,end);
        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);
            if(deviceContext.device instanceof DualSenseController){
                deviceContext.device.sendCommand(DualSenseController.getTriggerEffectMode(data,data));
            }
        }
    }

}
