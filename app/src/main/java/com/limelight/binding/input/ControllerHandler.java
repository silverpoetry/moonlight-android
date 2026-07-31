package com.limelight.binding.input;

import androidx.annotation.RequiresApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibratorManager;
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
import com.limelight.binding.input.driver.UsbDriverListener;
import com.limelight.binding.input.protocol.NvConnectionKeyboardInputSink;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.ui.GameGestures;

import org.cgutman.shieldcontrollerextensions.SceManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class ControllerHandler implements InputManager.InputDeviceListener,
        UsbDriverListener, GamepadInputHandler,
        StreamInputLifecycleController.ControllerDevices {
    private static final int BATTERY_RECHECK_INTERVAL_MS = 120 * 1000;

    private final SparseArray<InputDeviceContext> inputDeviceContexts = new SparseArray<>();
    private final SparseArray<UsbDeviceContext> usbDeviceContexts = new SparseArray<>();
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
    private final AndroidControllerInventory
            controllerInventory;
    private final ControllerVibrationRenderer vibrationRenderer;
    private final RazerKishiHapticsController
            razerKishiHapticsController;
    private final SensorManager deviceSensorManager;
    private final SceManager sceManager;
    private final AndroidControllerArrivalProbe
            controllerArrivalProbe;
    private final AndroidControllerDeviceProfileProbe
            controllerDeviceProfileProbe;
    private final AndroidControllerBackButtonProbe
            controllerBackButtonProbe;
    private final AndroidControllerTouchpadAdapter
            controllerTouchpadAdapter;
    private final UsbControllerInputAdapter
            usbControllerInputAdapter;
    private final Handler mainThreadHandler;
    private final ControllerMouseEmulationSession.Scheduler
            mouseEmulationScheduler;
    private final ControllerButtonReleaseSession.Scheduler
            buttonReleaseScheduler;
    private final ControllerMotionSession.Scheduler
            motionSensorScheduler;
    private final ControllerBatterySession.Scheduler
            batteryReportScheduler;
    private final HandlerThread backgroundHandlerThread;
    private final Handler backgroundThreadHandler;
    private boolean stopped = false;

    private final ControllerSettingsState settingsState;
    private final StreamAudioSettingsState audioSettingsState;
    private final ControllerSlotAllocator slotAllocator;
    private final ControllerInputReportAggregator.Sources
            controllerInputSources;

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
                razerKishiHapticsController.isFeatureEnabled();
        boolean deviceReceivesAudioHaptics =
                selectiveSuppression &&
                        razerKishiHapticsController.canUseDevice(
                                context.vendorId,
                                context.productId,
                                context.name);
        return ControllerHapticsPolicy
                .shouldSuppressStandardRumble(
                        settings,
                        selectiveSuppression,
                        deviceReceivesAudioHaptics);
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
        return !stopped &&
                razerKishiHapticsController.submitFrame(
                        frame,
                        intensityGain);
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

        razerKishiHapticsController.refresh();
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
        UsbManager usbManager = (UsbManager) activityContext
                .getSystemService(Context.USB_SERVICE);
        this.razerKishiHapticsController =
                RazerKishiHapticsController.create(
                        usbManager,
                        this::shouldUseControllerAudioHaptics);
        Vibrator deviceVibrator = (Vibrator) activityContext
                .getSystemService(Context.VIBRATOR_SERVICE);
        this.deviceSensorManager = (SensorManager) activityContext.getSystemService(Context.SENSOR_SERVICE);
        this.inputManager = (InputManager) activityContext.getSystemService(Context.INPUT_SERVICE);
        this.controllerInventory =
                new AndroidControllerInventory(
                        inputManager,
                        usbManager);
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
        this.buttonReleaseScheduler =
                new ControllerButtonReleaseSession.Scheduler() {
                    @Override
                    public long now() {
                        return SystemClock.uptimeMillis();
                    }

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
        this.batteryReportScheduler =
                new ControllerBatterySession.Scheduler() {
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
        this.controllerArrivalProbe =
                new AndroidControllerArrivalProbe(
                        MoonBridge::guessControllerType,
                        sceManager::isRecognizedDevice);
        this.controllerDeviceProfileProbe =
                new AndroidControllerDeviceProfileProbe(
                        MoonBridge::guessControllerHasPaddles,
                        MoonBridge::guessControllerHasShareButton);
        this.controllerBackButtonProbe =
                new AndroidControllerBackButtonProbe(inputManager);
        this.controllerTouchpadAdapter =
                new AndroidControllerTouchpadAdapter(
                        new AndroidControllerTouchpadAdapter.Output() {
                            @Override
                            public int sendControllerTouch(
                                    byte controllerNumber,
                                    byte touchType,
                                    int pointerId,
                                    float x,
                                    float y,
                                    float pressure) {
                                return conn.sendControllerTouchEvent(
                                        controllerNumber,
                                        touchType,
                                        pointerId,
                                        x,
                                        y,
                                        pressure);
                            }
                        });
        this.usbControllerInputAdapter =
                new UsbControllerInputAdapter(
                        new UsbControllerInputAdapter.Output() {
                            @Override
                            public void sendMotion(
                                    byte controllerNumber,
                                    byte motionType,
                                    float x,
                                    float y,
                                    float z) {
                                conn.sendControllerMotionEvent(
                                        controllerNumber,
                                        motionType,
                                        x,
                                        y,
                                        z);
                            }

                            @Override
                            public void sendTouch(
                                    byte controllerNumber,
                                    byte eventType,
                                    int pointerId,
                                    float x,
                                    float y,
                                    float pressure) {
                                conn.sendControllerTouchEvent(
                                        controllerNumber,
                                        eventType,
                                        pointerId,
                                        x,
                                        y,
                                        pressure);
                            }
                        });
        this.vibrationRenderer = new ControllerVibrationRenderer(
                settingsState,
                sceManager,
                deviceVibrator,
                deviceVibratorManager);
        this.defaultContext = new InputDeviceContext(
                ControllerLedSession.unavailable(),
                ControllerBatteryReporter.unavailableSource(),
                ControllerButtonMapper.builder(
                                0,
                                0,
                                Build.VERSION.SDK_INT)
                        .ignoreBack(true)
                        .hasHatAxes(true)
                        .build(),
                new ControllerButtonMappingState(false, false),
                new ControllerChordEmulationState(false, false),
                new ControllerMouseModeActivationState(),
                true);
        this.defaultContext.vibrationTarget =
                vibrationRenderer.emptyTarget();

        int deadzonePercentage =
                settingsState.get().getStickDeadzonePercent();

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
        defaultContext.slotLease.selectFixed((short) 0);
        defaultContext.slotLease.completeAssignment();
        defaultContext.external = false;
        this.controllerInputSources =
                new ControllerInputReportAggregator.Sources() {
                    @Override
                    public int size() {
                        return inputDeviceContexts.size() +
                                usbDeviceContexts.size() + 1;
                    }

                    @Override
                    public ControllerInputReportAggregator.Source sourceAt(
                            int index) {
                        int inputDeviceCount =
                                inputDeviceContexts.size();
                        if (index < inputDeviceCount) {
                            return inputDeviceContexts.valueAt(index);
                        }

                        int usbIndex = index - inputDeviceCount;
                        int usbDeviceCount = usbDeviceContexts.size();
                        if (usbIndex < usbDeviceCount) {
                            return usbDeviceContexts.valueAt(usbIndex);
                        }
                        if (usbIndex == usbDeviceCount) {
                            return defaultContext;
                        }
                        throw new IndexOutOfBoundsException(
                                "Controller source index: " + index);
                    }
                };

        // Some devices (GPD XD) have a back button which sends input events
        // with device ID == 0. This hits the default context which would normally
        // consume these. Instead, let's ignore them since that's probably the
        // most likely case.
        // Get the initially attached set of gamepads. As each gamepad receives
        // its initial InputEvent, we will move these from this set onto the
        // active reservation set, which allows them to unplug cleanly
        // if they are removed.
        slotAllocator = new ControllerSlotAllocator(
                controllerInventory.getInitialControllerMask(
                        settingsState.get()));

        // Register ourselves for input device notifications
        inputManager.registerInputDeviceListener(this, null);
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

        razerKishiHapticsController.destroy();
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

    @Override
    public boolean isGameControllerDevice(InputDevice device) {
        return controllerInventory.isGameControllerDevice(
                device,
                Build.VERSION.SDK_INT);
    }

    private void releaseControllerNumber(GenericControllerContext context) {
        // If we reserved a controller number, remove that reservation
        short controllerNumber =
                context.slotLease.getControllerNumber();
        if (context.slotLease.releaseReservation(slotAllocator)) {
            LimeLog.info(
                    "Controller number " + controllerNumber +
                            " is now available");
        }

        // If this device sent data as a gamepad, zero the values before removing.
        // We must do this after releasing the slot so this
        // causes the device to be removed on the server PC.
        if (context.slotLease.isAssigned()) {
            conn.sendControllerInput(controllerNumber, getActiveControllerMask(),
                    (short) 0,
                    (byte) 0, (byte) 0,
                    (short) 0, (short) 0,
                    (short) 0, (short) 0);
        }
    }

    private static ControllerAssociationPolicy.DeviceFacts getAssociationFacts(
            InputDevice device) {
        return new ControllerAssociationPolicy.DeviceFacts(
                device.getName(),
                device.getDescriptor(),
                (device.getSources() & InputDevice.SOURCE_JOYSTICK) ==
                        InputDevice.SOURCE_JOYSTICK);
    }

    private InputDevice findAssociatedJoystick(
            InputDevice originalDevice) {
        ControllerAssociationPolicy.DeviceFacts originalFacts =
                getAssociationFacts(originalDevice);

        // Android exposes a DS4 touchpad immediately before its joystick.
        // Check the reverse order as well for other split-device layouts.
        InputDevice candidate = InputDevice.getDevice(
                originalDevice.getId() + 1);
        if (isAssociatedJoystick(originalFacts, candidate)) {
            return candidate;
        }

        candidate = InputDevice.getDevice(
                originalDevice.getId() - 1);
        return isAssociatedJoystick(originalFacts, candidate)
                ? candidate
                : null;
    }

    private boolean isAssociatedJoystick(
            ControllerAssociationPolicy.DeviceFacts originalFacts,
            InputDevice candidate) {
        return ControllerAssociationPolicy.isAssociatedJoystick(
                originalFacts,
                candidate == null
                        ? null
                        : getAssociationFacts(candidate));
    }

    private void reserveControllerNumber(
            GenericControllerContext context) {
        if (!context.slotLease.reserveNext(slotAllocator)) {
            LimeLog.warning(
                    "No controller number is available; " +
                            "falling back to controller 0");
        }
    }

    private void assignAssociatedJoystickNumber(
            InputDeviceContext context) {
        InputDevice associatedDevice = findAssociatedJoystick(
                context.inputDevice);
        if (associatedDevice == null) {
            LimeLog.info("No associated joystick device found");
            context.slotLease.selectFixed((short) 0);
            return;
        }

        InputDeviceContext associatedContext =
                inputDeviceContexts.get(associatedDevice.getId());
        if (associatedContext == null) {
            associatedContext = createInputDeviceContextForDevice(
                    associatedDevice);
            inputDeviceContexts.put(
                    associatedDevice.getId(),
                    associatedContext);
        }

        if (!associatedContext.slotLease.isAssigned()) {
            assignControllerNumberIfNeeded(associatedContext);
        }

        context.slotLease.selectFixed(
                associatedContext.slotLease.getControllerNumber());
        LimeLog.info(
                "Propagated controller number from " +
                        associatedContext.name);
    }

    private void applyInputDeviceAssignmentStrategy(
            InputDeviceContext context,
            ControllerAssignmentPolicy.Strategy strategy) {
        switch (strategy) {
            case FIXED_PLAYER_ONE:
                LimeLog.info("Using controller number 0");
                context.slotLease.selectFixed((short) 0);
                break;
            case RESERVE_NEXT:
                LimeLog.info(
                        "Reserving the next available controller number");
                reserveControllerNumber(context);
                break;
            case FIND_ASSOCIATED_JOYSTICK:
                assignAssociatedJoystickNumber(context);
                break;
            default:
                throw new AssertionError(
                        "Unhandled controller assignment strategy: " +
                                strategy);
        }
    }

    private void applyUsbControllerAssignmentStrategy(
            GenericControllerContext context,
            ControllerAssignmentPolicy.Strategy strategy) {
        switch (strategy) {
            case FIXED_PLAYER_ONE:
                LimeLog.info("Using controller number 0");
                context.slotLease.selectFixed((short) 0);
                break;
            case RESERVE_NEXT:
                LimeLog.info(
                        "Reserving the next available controller number");
                reserveControllerNumber(context);
                break;
            case FIND_ASSOCIATED_JOYSTICK:
            default:
                throw new AssertionError(
                        "Invalid USB controller assignment strategy: " +
                                strategy);
        }
    }

    // Called before sending input but after we've determined that this
    // is definitely a controller (not a keyboard, mouse, or something else)
    private void assignControllerNumberIfNeeded(GenericControllerContext context) {
        if (context.slotLease.isAssigned()) {
            return;
        }

        ControllerSettings settings = settingsState.get();
        if (context instanceof InputDeviceContext) {
            InputDeviceContext devContext = (InputDeviceContext) context;

            LimeLog.info(devContext.name+" ("+context.id+") needs a controller number assigned");
            applyInputDeviceAssignmentStrategy(
                    devContext,
                    ControllerAssignmentPolicy.forInputDevice(
                            devContext.external,
                            devContext.hasJoystickAxes,
                            settings.isMultiControllerEnabled()));

            // If the gamepad doesn't have motion sensors, use the on-device sensors as a fallback for player 1
            if (settings
                    .isMotionSensorsFallbackToDeviceEnabled() &&
                    context.slotLease.getControllerNumber() == 0 &&
                    !devContext.motionRegistrations.hasManager()) {
                devContext.motionRegistrations.setManager(
                        deviceSensorManager);
            }
        }
        else {
            applyUsbControllerAssignmentStrategy(
                    context,
                    ControllerAssignmentPolicy.forUsbController(
                            settings.isMultiControllerEnabled()));
        }

        LimeLog.info(
                "Assigned as controller " +
                        context.slotLease.getControllerNumber());
        context.slotLease.completeAssignment();

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

    private InputDeviceContext createInputDeviceContextForDevice(InputDevice dev) {
        boolean external =
                AndroidInputDeviceClassifier.isExternal(dev);
        boolean ignoreBack =
                controllerBackButtonProbe.shouldIgnore(dev, external);
        AndroidControllerDeviceProfile profile =
                controllerDeviceProfileProbe.probe(
                        dev,
                        external,
                        ignoreBack,
                        (float) stickDeadzone,
                        settingsState.get()
                                .isTriggerDeadzoneDisabled());
        InputDeviceContext context = new InputDeviceContext(
                dev,
                profile,
                vibrationRenderer.selectTarget(dev, external),
                new ControllerLedSession(
                        AndroidControllerLedTarget.create(dev)),
                new AndroidControllerBatterySource(
                        dev,
                        sceManager));

        LimeLog.info(
                "Creating controller context for device: " +
                        profile.getName());
        LimeLog.info("Vendor ID: " + dev.getVendorId());
        LimeLog.info("Product ID: "+dev.getProductId());
        LimeLog.info(dev.toString());
        SensorManager inputDeviceSensorManager =
                AndroidControllerMotionSource.findAvailableManager(
                        dev,
                        settingsState.get().areMotionSensorsEnabled());
        if (inputDeviceSensorManager != null) {
            context.motionRegistrations.setManager(
                    inputDeviceSensorManager);
        }

        ControllerAxisProfile axisProfile = profile.getAxisProfile();
        boolean nonStandardDualShock4 =
                axisProfile.isNonStandardDualShock4();
        boolean linuxStandardFaceButtons =
                axisProfile.hasLinuxStandardFaceButtons();
        if (nonStandardDualShock4) {
            LimeLog.info("Detected non-standard DualShock 4 mapping");
        }
        else if (linuxStandardFaceButtons) {
            LimeLog.info("Detected DualShock 4 (Linux standard mapping)");
        }

        LimeLog.info(
                "Analog stick deadzone: " +
                        context.leftStickDeadzoneRadius + " " +
                        context.rightStickDeadzoneRadius);
        LimeLog.info(
                "Trigger deadzone: " +
                        context.triggerDeadzone);

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

    public void refreshBatteryReportingState() {
        if (stopped) {
            return;
        }

        boolean enabled = settingsState.get()
                .isBatteryReportingEnabled();
        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            inputDeviceContexts.valueAt(i)
                    .batterySession.setEnabled(enabled);
        }
    }

    private void sendControllerInputPacket(
            GenericControllerContext originalContext) {
        assignControllerNumberIfNeeded(originalContext);

        short controllerNumber =
                originalContext.slotLease.getControllerNumber();
        ControllerInputReportAggregator.aggregateAndSend(
                controllerInputSources,
                controllerNumber,
                originalContext.isMouseEmulationActive(),
                originalContext.aggregatedInputOutput);
    }

    private void emitAggregatedControllerInput(
            GenericControllerContext originalContext,
            int inputMap,
            byte leftTrigger,
            byte rightTrigger,
            short leftStickX,
            short leftStickY,
            short rightStickX,
            short rightStickY) {
        short controllerNumber =
                originalContext.slotLease.getControllerNumber();
        short activeControllerMask = getActiveControllerMask();
        if (originalContext.isMouseEmulationActive()) {
            originalContext.mouseEmulationTranslator.translate(
                    inputMap,
                    mouseEmulationOutput);

            conn.sendControllerInput(
                    controllerNumber,
                    activeControllerMask,
                    (short) 0,
                    (byte) 0,
                    (byte) 0,
                    (short) 0,
                    (short) 0,
                    (short) 0,
                    (short) 0);
        }
        else {
            if (settingsState.get().isForceGyroEnabled()) {
                setControllerLeftTriggerState(
                        controllerNumber,
                        leftTrigger);
            }
            conn.sendControllerInput(
                    controllerNumber,
                    activeControllerMask,
                    inputMap,
                    leftTrigger,
                    rightTrigger,
                    leftStickX,
                    leftStickY,
                    rightStickX,
                    rightStickY);
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
                context.buttonMappingState,
                event.getKeyCode(),
                event.getScanCode(),
                event.getFlags(),
                event.hasNoModifiers(),
                settingsState.get().isJoyConFixEnabled());
    }

    private void handleAxisSet(InputDeviceContext context, float lsX, float lsY, float rsX,
                               float rsY, float lt, float rt, float hatX, float hatY) {

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            context.inputState.updateLeftStick(
                    lsX,
                    lsY,
                    context.leftStickDeadzoneRadius);
        }

        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            context.inputState.updateRightStick(
                    rsX,
                    rsY,
                    context.rightStickDeadzoneRadius);
        }

        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            context.inputState.updateTriggerAxes(
                    lt,
                    rt,
                    context.triggersIdleNegative,
                    context.triggerDeadzone);
        }

        if (context.hatXAxis != -1 && context.hatYAxis != -1) {
            context.inputState.updateHat(hatX, hatY);
        }

        sendControllerInputPacket(context);
    }

    @Override
    public boolean tryHandleTouchpadEvent(MotionEvent event) {
        // Only get a context if one already exists. We want to ensure we don't report non-gamepads.
        InputDeviceContext context = inputDeviceContexts.get(event.getDeviceId());
        if (context == null) {
            return false;
        }
        return controllerTouchpadAdapter.tryHandle(
                event,
                context,
                settingsState.get().isTouchpadAsMouse());
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

            if (deviceContext.slotLease.getControllerNumber() ==
                    controllerNumber) {
                foundMatchingDevice = true;

                if (shouldSuppressInputDeviceRumble(deviceContext)) {
                    continue;
                }

                vibrated |= rumbleInputDeviceContext(deviceContext, lowFreqMotor, highFreqMotor);
            }
        }

        for (int i = 0; i < usbDeviceContexts.size(); i++) {
            UsbDeviceContext deviceContext = usbDeviceContexts.valueAt(i);

            if (deviceContext.slotLease.getControllerNumber() ==
                    controllerNumber) {
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

            if (deviceContext.slotLease.getControllerNumber() ==
                    controllerNumber) {
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

            if (deviceContext.slotLease.getControllerNumber() ==
                    controllerNumber) {
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
        ControllerMotionEventProcessor processor =
                new ControllerMotionEventProcessor(
                        controllerNumber,
                        motionType,
                        needsDeviceOrientationCorrection,
                        settingsState::get,
                        () -> activityContext
                                .getWindowManager()
                                .getDefaultDisplay()
                                .getRotation(),
                        this::getControllerLeftTriggerState,
                        new ControllerMotionEventProcessor.Output() {
                            @Override
                            public void sendControllerInput(
                                    short rightStickX,
                                    short rightStickY) {
                                context.inputState.setRightStick(
                                        rightStickX,
                                        rightStickY);
                                sendControllerInputPacket(context);
                            }

                            @Override
                            public void sendMotion(
                                    short outputControllerNumber,
                                    byte outputMotionType,
                                    float x,
                                    float y,
                                    float z) {
                                conn.sendControllerMotionEvent(
                                        (byte) outputControllerNumber,
                                        outputMotionType,
                                        x,
                                        y,
                                        z);
                            }
                        },
                        context.gyroStickTranslator);
        return new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                processor.process(
                        sensorEvent.values[0],
                        sensorEvent.values[1],
                        sensorEvent.values[2]);
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
            defaultContext.motionRegistrations.setManager(
                    deviceSensorManager);
            defaultContext.motionSession.setReportRate(
                    controllerNumber,
                    motionType,
                    reportRateHz);
        }

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext = inputDeviceContexts.valueAt(i);

            if (deviceContext.slotLease.getControllerNumber() ==
                    controllerNumber) {
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

        for (int i = 0; i < inputDeviceContexts.size(); i++) {
            InputDeviceContext deviceContext =
                    inputDeviceContexts.valueAt(i);
            if (deviceContext.slotLease.getControllerNumber() ==
                    controllerNumber) {
                deviceContext.ledSession.setColor(r, g, b);
            }
        }
    }

    private enum DigitalButtonApplication {
        APPLIED,
        SUPPRESSED,
        UNHANDLED
    }

    private DigitalButtonApplication classifyDigitalButton(
            InputDeviceContext context,
            ControllerDigitalButtonMapping.Target target) {
        if (target ==
                ControllerDigitalButtonMapping.Target.UNHANDLED) {
            return DigitalButtonApplication.UNHANDLED;
        }
        if (ControllerDigitalButtonMapping.isSuppressedByHat(
                target,
                context.inputState.isHorizontalHatUsed(),
                context.inputState.isVerticalHatUsed())) {
            return DigitalButtonApplication.SUPPRESSED;
        }

        if (target ==
                ControllerDigitalButtonMapping.Target.LEFT_TRIGGER) {
            return context.inputState.isLeftTriggerAxisUsed()
                    ? DigitalButtonApplication.SUPPRESSED
                    : DigitalButtonApplication.APPLIED;
        }
        if (target ==
                ControllerDigitalButtonMapping.Target.RIGHT_TRIGGER) {
            return context.inputState.isRightTriggerAxisUsed()
                    ? DigitalButtonApplication.SUPPRESSED
                    : DigitalButtonApplication.APPLIED;
        }
        return DigitalButtonApplication.APPLIED;
    }

    private void applyDigitalButton(
            InputDeviceContext context,
            ControllerDigitalButtonMapping.Target target,
            boolean pressed,
            long eventTime,
            int repeatCount) {
        if (target ==
                ControllerDigitalButtonMapping.Target.LEFT_TRIGGER) {
            context.inputState.setDigitalTrigger(true, pressed);
            return;
        }
        if (target ==
                ControllerDigitalButtonMapping.Target.RIGHT_TRIGGER) {
            context.inputState.setDigitalTrigger(false, pressed);
            return;
        }

        if (pressed) {
            context.mouseModeActivationState.observeButtonDown(
                    target,
                    eventTime,
                    repeatCount);
            if (target ==
                    ControllerDigitalButtonMapping.Target.SPECIAL) {
                context.chordEmulationState.observeModeButton();
            }
            else if (target ==
                    ControllerDigitalButtonMapping.Target.BACK) {
                context.chordEmulationState.observeSelectButton();
            }
            context.inputState.setButtonMask(
                    target.getInputMask(),
                    true);
        }
        else {
            context.inputState.setButtonMask(
                    target.getInputMask(),
                    false);
            if (target ==
                    ControllerDigitalButtonMapping.Target.LEFT_BUMPER) {
                context.chordEmulationState.recordLeftBumperUp(
                        eventTime);
            }
            else if (target ==
                    ControllerDigitalButtonMapping.Target.RIGHT_BUMPER) {
                context.chordEmulationState.recordRightBumperUp(
                        eventTime);
            }
        }
    }

    private void activateMouseEmulationAction(
            InputDeviceContext context,
            ControllerSettings settings) {
        if (settings.doesMouseEmulationOpenGameMenu()) {
            gestures.showGameMenu(context);
        }
        else {
            context.toggleMouseEmulation();
        }
    }

    private void handleMouseEmulationButtonRelease(
            InputDeviceContext context,
            ControllerSettings settings,
            ControllerDigitalButtonMapping.Target target,
            long eventTime) {
        if (context.mouseModeActivationState.shouldActivateOnRelease(
                target,
                settings.isMouseEmulationEnabled(),
                settings.getMouseEmulationButton(),
                context.inputState.getInputMap(),
                eventTime)) {
            activateMouseEmulationAction(context, settings);
        }
    }

    private DigitalButtonApplication completeButtonUp(
            InputDeviceContext context,
            ControllerSettings settings,
            ControllerDigitalButtonMapping.Target target,
            long eventTime) {
        DigitalButtonApplication application =
                classifyDigitalButton(context, target);
        if (application != DigitalButtonApplication.APPLIED) {
            return application;
        }

        handleMouseEmulationButtonRelease(
                context,
                settings,
                target,
                eventTime);
        applyDigitalButton(
                context,
                target,
                false,
                eventTime,
                0);
        context.inputState.setInputMap(
                context.chordEmulationState.applyButtonUp(
                        context.inputState.getInputMap()));

        sendControllerInputPacket(context);

        if (context.chordEmulationState
                .shouldFinishAfterButtonUp(
                        context.inputState.getInputMap())) {
            // All buttons from the quit combo are lifted. Finish the activity now.
            activityContext.finish();
        }
        return DigitalButtonApplication.APPLIED;
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
        ControllerDigitalButtonMapping.Target target =
                ControllerDigitalButtonMapping.resolve(
                        keyCode,
                        event.getScanCode(),
                        context.hasPaddles);
        DigitalButtonApplication application =
                classifyDigitalButton(context, target);
        if (application == DigitalButtonApplication.UNHANDLED) {
            return false;
        }
        if (application == DigitalButtonApplication.SUPPRESSED) {
            context.buttonReleaseSession.abandonButton(target);
            return true;
        }

        if (context.buttonReleaseSession.deferButtonUp(
                target,
                settings,
                event.getEventTime())) {
            return true;
        }
        completeButtonUp(
                context,
                settings,
                target,
                event.getEventTime());
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
        ControllerDigitalButtonMapping.Target target =
                ControllerDigitalButtonMapping.resolve(
                        keyCode,
                        event.getScanCode(),
                        context.hasPaddles);

        context.buttonReleaseSession.flushPendingRelease(target);
        DigitalButtonApplication application =
                classifyDigitalButton(context, target);
        if (application == DigitalButtonApplication.UNHANDLED) {
            return false;
        }
        if (application == DigitalButtonApplication.SUPPRESSED) {
            context.buttonReleaseSession.abandonButton(target);
            return true;
        }

        applyDigitalButton(
                context,
                target,
                true,
                event.getEventTime(),
                event.getRepeatCount());

        context.inputState.setInputMap(
                context.chordEmulationState.applyButtonDown(
                        context.inputState.getInputMap(),
                        event.getEventTime()));

        // We don't need to send repeat key down events, but the platform
        // sends us events that claim to be repeats but they're from different
        // devices, so we just send them all and deal with some duplicates.
        sendControllerInputPacket(context);
        context.buttonReleaseSession.recordButtonDown(
                target,
                event.getRepeatCount());
        return true;
    }

    public void reportOscState(int buttonFlags,
                               short leftStickX, short leftStickY,
                               short rightStickX, short rightStickY,
                               byte leftTrigger, byte rightTrigger) {
        defaultContext.inputState.replace(
                buttonFlags,
                leftTrigger,
                rightTrigger,
                leftStickX,
                leftStickY,
                rightStickX,
                rightStickY);

        sendControllerInputPacket(defaultContext);
    }

    @Override
    public void reportControllerState(
            int controllerId,
            int buttonFlags,
            float leftStickX,
            float leftStickY,
            float rightStickX,
            float rightStickY,
            float leftTrigger,
            float rightTrigger) {
        UsbDeviceContext context =
                usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }
        usbControllerInputAdapter.handleState(
                context,
                buttonFlags,
                leftStickX,
                leftStickY,
                rightStickX,
                rightStickY,
                leftTrigger,
                rightTrigger);
    }

    @Override
    public void reportControllerMotion(
            int controllerId,
            byte motionType,
            float motionX,
            float motionY,
            float motionZ) {
        UsbDeviceContext context =
                usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }
        if (!settingsState.get()
                .isUsbGyroscopeReportingEnabled()) {
            return;
        }
        usbControllerInputAdapter.handleMotion(
                context,
                motionType,
                motionX,
                motionY,
                motionZ);
    }

    @Override
    public void reportControllerTouchpadEvent(
            int controllerId,
            byte eventType,
            int pointerId,
            float x,
            float y,
            float pressure) {
        UsbDeviceContext context =
                usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }

        usbControllerInputAdapter.handleTouch(
                context,
                eventType,
                pointerId,
                x,
                y,
                pressure);
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

    class GenericControllerContext implements GameInputDevice,
            ControllerInputReportAggregator.Source {
        public int id;
        public boolean external;

        public int vendorId;
        public int productId;

        public float leftStickDeadzoneRadius;
        public float rightStickDeadzoneRadius;
        public float triggerDeadzone;
        private final boolean includedAcrossMouseModes;

        GenericControllerContext() {
            this(false);
        }

        GenericControllerContext(boolean includedAcrossMouseModes) {
            this.includedAcrossMouseModes =
                    includedAcrossMouseModes;
        }

        final ControllerSlotLease slotLease =
                new ControllerSlotLease();
        final ControllerInputState inputState =
                new ControllerInputState();

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
                                                inputState
                                                        .getLeftStickX(),
                                                inputState
                                                        .getLeftStickY(),
                                                inputState
                                                        .getRightStickX(),
                                                inputState
                                                        .getRightStickY(),
                                                inputState
                                                        .getLeftTrigger() & 0xFF,
                                                inputState
                                                        .getRightTrigger() & 0xFF,
                                                settings
                                                        .getMouseSensitivityPercent(),
                                                settings
                                                        .getAnalogStickForScrolling(),
                                                mouseEmulationOutput);
                            }
                        });
        private final ControllerInputReportAggregator.Output
                aggregatedInputOutput =
                new ControllerInputReportAggregator.Output() {
                    @Override
                    public void send(
                            int inputMap,
                            byte leftTrigger,
                            byte rightTrigger,
                            short leftStickX,
                            short leftStickY,
                            short rightStickX,
                            short rightStickY) {
                        emitAggregatedControllerInput(
                                GenericControllerContext.this,
                                inputMap,
                                leftTrigger,
                                rightTrigger,
                                leftStickX,
                                leftStickY,
                                rightStickX,
                                rightStickY);
                    }
                };

        @Override
        public boolean isAssigned() {
            return slotLease.isAssigned();
        }

        @Override
        public short getControllerNumber() {
            return slotLease.getControllerNumber();
        }

        @Override
        public boolean isIncludedAcrossMouseModes() {
            return includedAcrossMouseModes;
        }

        @Override
        public ControllerInputState getControllerInputState() {
            return inputState;
        }

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

        @Override
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

    class InputDeviceContext extends GenericControllerContext
            implements AndroidControllerTouchpadAdapter.Target {
        public String name;
        public ControllerVibrationRenderer.Target vibrationTarget;

        private final ControllerMotionRegistrations<
                SensorManager,
                Sensor,
                SensorEventListener> motionRegistrations;
        private final ControllerMotionSession motionSession;

        public InputDevice inputDevice;

        private final ControllerLedSession ledSession;

        public int leftStickXAxis = -1;
        public int leftStickYAxis = -1;

        public int rightStickXAxis = -1;
        public int rightStickYAxis = -1;

        public int leftTriggerAxis = -1;
        public int rightTriggerAxis = -1;
        public boolean triggersIdleNegative;

        public int hatXAxis = -1;
        public int hatYAxis = -1;

        InputDevice.MotionRange touchpadXRange;
        InputDevice.MotionRange touchpadYRange;
        InputDevice.MotionRange touchpadPressureRange;

        @Override
        public InputDevice.MotionRange getTouchpadXRange() {
            return touchpadXRange;
        }

        @Override
        public InputDevice.MotionRange getTouchpadYRange() {
            return touchpadYRange;
        }

        @Override
        public InputDevice.MotionRange getTouchpadPressureRange() {
            return touchpadPressureRange;
        }

        @Override
        public void sendControllerInput() {
            sendControllerInputPacket(this);
        }

        private final ControllerButtonMapper buttonMapper;
        private final ControllerButtonMappingState buttonMappingState;
        private final ControllerChordEmulationState
                chordEmulationState;
        private final ControllerMouseModeActivationState
                mouseModeActivationState;
        private final ControllerButtonReleaseSession
                buttonReleaseSession;
        public boolean hasJoystickAxes;
        public boolean hasPaddles;
        public boolean hasShare;

        private final ControllerBatterySession batterySession;

        private InputDeviceContext(
                ControllerLedSession ledSession,
                ControllerBatteryReporter.Source batterySource,
                ControllerButtonMapper buttonMapper,
                ControllerButtonMappingState buttonMappingState,
                ControllerChordEmulationState chordEmulationState,
                ControllerMouseModeActivationState
                        mouseModeActivationState,
                boolean includedAcrossMouseModes) {
            super(includedAcrossMouseModes);
            this.ledSession = Objects.requireNonNull(
                    ledSession,
                    "ledSession");
            this.buttonMapper = Objects.requireNonNull(
                    buttonMapper,
                    "buttonMapper");
            this.buttonMappingState = Objects.requireNonNull(
                    buttonMappingState,
                    "buttonMappingState");
            this.chordEmulationState = Objects.requireNonNull(
                    chordEmulationState,
                    "chordEmulationState");
            this.mouseModeActivationState = Objects.requireNonNull(
                    mouseModeActivationState,
                    "mouseModeActivationState");
            this.buttonReleaseSession =
                    new ControllerButtonReleaseSession(
                            buttonReleaseScheduler,
                            (target, settings, eventTime) ->
                                    completeButtonUp(
                                            this,
                                            settings,
                                            target,
                                            eventTime));
            ControllerBatteryReporter batteryReporter =
                    new ControllerBatteryReporter(
                            batterySource,
                            (protocolState, percentage) ->
                                    conn.sendControllerBatteryEvent(
                                            (byte) slotLease
                                                    .getControllerNumber(),
                                            protocolState,
                                            percentage));
            this.batterySession = new ControllerBatterySession(
                    batteryReportScheduler,
                    batteryReporter::report,
                    BATTERY_RECHECK_INTERVAL_MS);
            this.motionRegistrations =
                    new ControllerMotionRegistrations<>(
                            new AndroidControllerMotionBackend(
                                    deviceSensorManager,
                                    (controllerNumber,
                                            motionType,
                                            needsOrientationCorrection) ->
                                            createSensorListener(
                                                    this,
                                                    controllerNumber,
                                                    motionType,
                                                    needsOrientationCorrection),
                                    controllerNumber ->
                                            conn.sendControllerMotionEvent(
                                                    (byte) controllerNumber,
                                                    MoonBridge
                                                            .LI_MOTION_TYPE_GYRO,
                                                    0.f,
                                                    0.f,
                                                    0.f)));
            this.motionSession = new ControllerMotionSession(
                    motionSensorScheduler,
                    motionRegistrations);
        }

        private InputDeviceContext(
                InputDevice inputDevice,
                AndroidControllerDeviceProfile profile,
                ControllerVibrationRenderer.Target vibrationTarget,
                ControllerLedSession ledSession,
                ControllerBatteryReporter.Source batterySource) {
            this(
                    ledSession,
                    batterySource,
                    profile.getButtonMapper(),
                    profile.createButtonMappingState(),
                    new ControllerChordEmulationState(
                            profile.hasModeButton(),
                            profile.hasSelectButton()),
                    new ControllerMouseModeActivationState(),
                    false);
            this.inputDevice = Objects.requireNonNull(
                    inputDevice,
                    "inputDevice");
            this.vibrationTarget = Objects.requireNonNull(
                    vibrationTarget,
                    "vibrationTarget");
            Objects.requireNonNull(profile, "profile");

            name = profile.getName();
            id = profile.getDeviceId();
            external = profile.isExternal();
            vendorId = profile.getVendorId();
            productId = profile.getProductId();
            hasPaddles = profile.hasPaddles();
            hasShare = profile.hasShareButton();
            touchpadXRange = profile.getTouchpadXRange();
            touchpadYRange = profile.getTouchpadYRange();
            touchpadPressureRange =
                    profile.getTouchpadPressureRange();

            ControllerAxisProfile axisProfile =
                    profile.getAxisProfile();
            leftStickXAxis =
                    AndroidControllerAxisProbe.toAndroidAxis(
                            axisProfile.getLeftStickX());
            leftStickYAxis =
                    AndroidControllerAxisProbe.toAndroidAxis(
                            axisProfile.getLeftStickY());
            rightStickXAxis =
                    AndroidControllerAxisProbe.toAndroidAxis(
                            axisProfile.getRightStickX());
            rightStickYAxis =
                    AndroidControllerAxisProbe.toAndroidAxis(
                            axisProfile.getRightStickY());
            leftTriggerAxis =
                    AndroidControllerAxisProbe.toAndroidAxis(
                            axisProfile.getLeftTrigger());
            rightTriggerAxis =
                    AndroidControllerAxisProbe.toAndroidAxis(
                            axisProfile.getRightTrigger());
            hatXAxis = AndroidControllerAxisProbe.toAndroidAxis(
                    axisProfile.getHatX());
            hatYAxis = AndroidControllerAxisProbe.toAndroidAxis(
                    axisProfile.getHatY());
            triggersIdleNegative =
                    axisProfile.areTriggersIdleNegative();
            hasJoystickAxes = axisProfile.hasLeftStick();
            leftStickDeadzoneRadius =
                    profile.getLeftStickDeadzoneRadius();
            rightStickDeadzoneRadius =
                    profile.getRightStickDeadzoneRadius();
            triggerDeadzone = profile.getTriggerDeadzone();
        }

        @Override
        public void destroy() {
            buttonReleaseSession.destroy();
            super.destroy();
            vibrationRenderer.cancel(vibrationTarget);

            motionSession.destroy();
            batterySession.destroy();
            ledSession.destroy();
        }

        @Override
        public void sendControllerArrival() {
            boolean hasAccelerometer =
                    motionRegistrations.hasSensor(
                            ControllerMotionRegistrations.SensorKind
                                    .ACCELEROMETER);
            boolean hasGyroscope =
                    motionRegistrations.hasSensor(
                            ControllerMotionRegistrations.SensorKind
                                    .GYROSCOPE);
            ControllerArrivalReport report =
                    controllerArrivalProbe.probe(
                            inputDevice,
                            AndroidControllerArrivalProbe
                                    .RuntimeCapabilities.builder()
                                    .hasPaddles(hasPaddles)
                                    .hasShareButton(hasShare)
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
                                    .hasRgbLed(
                                            ledSession.isAvailable())
                                    .hasAnalogTriggers(
                                            leftTriggerAxis != -1 ||
                                                    rightTriggerAxis != -1)
                                    .hasAccelerometer(hasAccelerometer)
                                    .hasGyroscope(hasGyroscope)
                                    .hasMotionManager(
                                            motionRegistrations.hasManager())
                                    .build());

            chordEmulationState.setClickpadEmulationRequired(
                    report.isClickpadEmulationRequired());
            if (chordEmulationState
                    .isClickpadEmulationRequired()) {
                LimeLog.info(
                        "Reporting an unknown controller type while " +
                                "emulating motion sensors");
            }

            conn.sendControllerArrivalEvent(
                    (byte) slotLease.getControllerNumber(),
                    getActiveControllerMask(),
                    report.getReportedType(),
                    report.getSupportedButtonFlags(),
                    report.getCapabilities());

            // After reporting arrival to the host, send the initial battery
            // state and begin monitoring when the feature is enabled.
            batterySession.setEnabled(settingsState.get()
                    .isBatteryReportingEnabled());
        }

        public void migrateContext(InputDeviceContext oldContext) {
            boolean restoreMouseEmulationActive =
                    oldContext.isMouseEmulationActive();
            ControllerMotionSession.DesiredState motionState =
                    oldContext.motionSession.snapshotDesiredState();
            ControllerLedSession.DesiredState ledState =
                    oldContext.ledSession.snapshotDesiredState();
            boolean usedDeviceSensorManager =
                    oldContext.motionRegistrations.usesManager(
                            deviceSensorManager);
            restoreInputSessionFrom(oldContext);
            // Don't release the controller number, because we will carry it over if it is present.
            // We also want to make sure the change is invisible to the host PC to avoid an add/remove
            // cycle for the gamepad which may break some games.
            oldContext.destroy();
            // Transfer the slot without releasing it. Releasing and
            // reacquiring here would briefly remove the controller on the host.
            oldContext.slotLease.transferTo(this.slotLease);

            // We may have set this device to use the built-in sensor manager. If so, do that again.
            if (usedDeviceSensorManager) {
                this.motionRegistrations.setManager(
                        deviceSensorManager);
            }
            motionSession.restoreDesiredState(motionState);
            ledSession.restoreDesiredState(ledState);

            // Re-enable sensors on the new context
            enableSensors();

            // Refresh battery state and start polling on the new context.
            batterySession.setEnabled(settingsState.get()
                    .isBatteryReportingEnabled());
            restoreMouseEmulation(
                    restoreMouseEmulationActive);
        }

        private void restoreInputSessionFrom(
                InputDeviceContext oldContext) {
            inputState.restoreFrom(oldContext.inputState);
            buttonMappingState.restoreLearnedStateFrom(
                    oldContext.buttonMappingState);
            chordEmulationState.restoreFrom(
                    oldContext.chordEmulationState);
            mouseModeActivationState.restoreFrom(
                    oldContext.mouseModeActivationState);
            buttonReleaseSession.restoreFrom(
                    oldContext.buttonReleaseSession);
            mouseEmulationTranslator.restoreFrom(
                    oldContext.mouseEmulationTranslator);
            gyroStickTranslator.restoreFrom(
                    oldContext.gyroStickTranslator);
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

    }

    class UsbDeviceContext extends GenericControllerContext
            implements UsbControllerInputAdapter.Target {
        public AbstractController device;

        @Override
        public float getLeftStickDeadzoneRadius() {
            return leftStickDeadzoneRadius;
        }

        @Override
        public float getRightStickDeadzoneRadius() {
            return rightStickDeadzoneRadius;
        }

        @Override
        public float getTriggerDeadzone() {
            return triggerDeadzone;
        }

        @Override
        public byte ensureAssignedControllerNumber() {
            assignControllerNumberIfNeeded(this);
            return (byte) slotLease.getControllerNumber();
        }

        @Override
        public void sendControllerInput() {
            sendControllerInputPacket(this);
        }

        @Override
        public void destroy() {
            super.destroy();
            // Nothing for now
        }

        @Override
        public void sendControllerArrival() {
            conn.sendControllerArrivalEvent(
                    (byte) slotLease.getControllerNumber(),
                    getActiveControllerMask(),
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
