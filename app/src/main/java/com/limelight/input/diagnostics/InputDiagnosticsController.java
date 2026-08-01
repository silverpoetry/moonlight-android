package com.limelight.input.diagnostics;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.limelight.LimeLog;
import com.limelight.binding.input.AndroidInputDeviceRegistration;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Owns Android input-device, motion-sensor, and vibration diagnostics.
 * The activity is only a renderer and never participates in stream input.
 */
public final class InputDiagnosticsController
        implements InputManager.InputDeviceListener,
        SensorEventListener {
    public interface Listener {
        void onDeviceSummaryChanged(String summary);

        void onInputEventChanged(String summary);

        void onSensorSummaryChanged(String summary);

        void onControllerVibrationAvailabilityChanged(
                boolean available,
                String deviceName);
    }

    private static final long SENSOR_RENDER_INTERVAL_NS = 100_000_000L;
    private static final long TEST_VIBRATION_DURATION_MS = 180L;
    private static final int TEST_VIBRATION_AMPLITUDE = 180;

    private final InputManager inputManager;
    private final SensorManager sensorManager;
    private final Vibrator deviceVibrator;
    private final Listener listener;

    private AndroidInputDeviceRegistration inputRegistration;
    private int activeControllerId = -1;
    private long lastSensorRenderNs;
    private final float[] accelerometer = new float[3];
    private final float[] gyroscope = new float[3];
    private boolean hasAccelerometer;
    private boolean hasGyroscope;
    private boolean started;

    public static InputDiagnosticsController create(
            Context context,
            Listener listener) {
        Objects.requireNonNull(context, "context");
        InputManager inputManager =
                (InputManager) context.getSystemService(
                        Context.INPUT_SERVICE);
        SensorManager sensorManager =
                (SensorManager) context.getSystemService(
                        Context.SENSOR_SERVICE);
        if (inputManager == null || sensorManager == null) {
            throw new IllegalStateException(
                    "Required Android input services are unavailable");
        }
        return new InputDiagnosticsController(
                inputManager,
                sensorManager,
                resolveDeviceVibrator(context),
                listener);
    }

    InputDiagnosticsController(
            InputManager inputManager,
            SensorManager sensorManager,
            Vibrator deviceVibrator,
            Listener listener) {
        this.inputManager = Objects.requireNonNull(
                inputManager,
                "inputManager");
        this.sensorManager = Objects.requireNonNull(
                sensorManager,
                "sensorManager");
        this.deviceVibrator = Objects.requireNonNull(
                deviceVibrator,
                "deviceVibrator");
        this.listener = Objects.requireNonNull(
                listener,
                "listener");
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        inputRegistration = AndroidInputDeviceRegistration.register(
                inputManager,
                this);
        registerSensors();
        refreshDevices();
        renderSensors();
    }

    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        if (inputRegistration != null) {
            inputRegistration.unregister();
            inputRegistration = null;
        }
        sensorManager.unregisterListener(this);
        deviceVibrator.cancel();
        Vibrator controllerVibrator = resolveControllerVibrator();
        if (controllerVibrator != null) {
            controllerVibrator.cancel();
        }
    }

    public void refreshDevices() {
        int[] ids = inputManager.getInputDeviceIds();
        StringBuilder summary = new StringBuilder();
        int visibleDeviceCount = 0;
        for (int id : ids) {
            InputDevice device = inputManager.getInputDevice(id);
            if (device == null || device.isVirtual()) {
                continue;
            }
            if (visibleDeviceCount > 0) {
                summary.append("\n\n");
            }
            appendDevice(summary, device);
            visibleDeviceCount++;
        }
        if (visibleDeviceCount == 0) {
            summary.append("未检测到外部输入设备");
        }
        listener.onDeviceSummaryChanged(summary.toString());
        publishControllerVibrationAvailability();
    }

    public void onKeyEvent(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        rememberController(event.getDeviceId());
        String action;
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                action = "按下";
                break;
            case KeyEvent.ACTION_UP:
                action = "松开";
                break;
            default:
                action = "重复";
                break;
        }
        listener.onInputEventChanged(String.format(
                Locale.ROOT,
                "%s · %s\nkeyCode=%d (%s)\nscanCode=%d · repeat=%d\n设备：%s",
                action,
                KeyEvent.keyCodeToString(event.getKeyCode()),
                event.getKeyCode(),
                KeyEvent.keyCodeToString(event.getKeyCode()),
                event.getScanCode(),
                event.getRepeatCount(),
                deviceName(event.getDeviceId())));
    }

    public void onMotionEvent(MotionEvent event) {
        Objects.requireNonNull(event, "event");
        InputDevice device = event.getDevice();
        if (device == null || !isControllerSource(event.getSource())) {
            return;
        }
        rememberController(device.getId());
        StringBuilder summary = new StringBuilder();
        summary.append("摇杆/轴事件 · ")
                .append(device.getName());
        int renderedAxes = 0;
        for (InputDevice.MotionRange range :
                device.getMotionRanges()) {
            if (!isControllerSource(range.getSource())) {
                continue;
            }
            float value = event.getAxisValue(range.getAxis());
            summary.append(String.format(
                    Locale.ROOT,
                    "\n%s = %.4f  [%.2f, %.2f] flat=%.3f",
                    MotionEvent.axisToString(range.getAxis()),
                    value,
                    range.getMin(),
                    range.getMax(),
                    range.getFlat()));
            renderedAxes++;
        }
        if (renderedAxes == 0) {
            summary.append("\n该事件未报告可用摇杆轴");
        }
        listener.onInputEventChanged(summary.toString());
    }

    public boolean testDeviceVibration() {
        return vibrate(deviceVibrator);
    }

    public boolean testControllerVibration() {
        Vibrator vibrator = resolveControllerVibrator();
        return vibrator != null && vibrate(vibrator);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        refreshDevices();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (activeControllerId == deviceId) {
            activeControllerId = -1;
        }
        refreshDevices();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        refreshDevices();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!started || event.values.length < 3) {
            return;
        }
        float[] target;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            target = accelerometer;
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            target = gyroscope;
        } else {
            return;
        }
        System.arraycopy(event.values, 0, target, 0, 3);
        if (event.timestamp - lastSensorRenderNs >=
                SENSOR_RENDER_INTERVAL_NS) {
            lastSensorRenderNs = event.timestamp;
            renderSensors();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Values are diagnostic only; accuracy changes need no separate UI.
    }

    private void registerSensors() {
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE);
        hasAccelerometer = accelerometerSensor != null &&
                sensorManager.registerListener(
                        this,
                        accelerometerSensor,
                        SensorManager.SENSOR_DELAY_UI);
        hasGyroscope = gyroscopeSensor != null &&
                sensorManager.registerListener(
                        this,
                        gyroscopeSensor,
                        SensorManager.SENSOR_DELAY_UI);
    }

    private void renderSensors() {
        listener.onSensorSummaryChanged(String.format(
                Locale.ROOT,
                "加速度计：%s\nx=%.4f  y=%.4f  z=%.4f m/s²\n\n" +
                        "陀螺仪：%s\nx=%.4f  y=%.4f  z=%.4f rad/s",
                hasAccelerometer ? "可用" : "不可用",
                accelerometer[0],
                accelerometer[1],
                accelerometer[2],
                hasGyroscope ? "可用" : "不可用",
                gyroscope[0],
                gyroscope[1],
                gyroscope[2]));
    }

    private void rememberController(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        if (device == null || !isControllerSource(device.getSources())) {
            return;
        }
        activeControllerId = deviceId;
        publishControllerVibrationAvailability();
    }

    private void publishControllerVibrationAvailability() {
        InputDevice device = inputManager.getInputDevice(
                activeControllerId);
        Vibrator vibrator = resolveVibrator(device);
        listener.onControllerVibrationAvailabilityChanged(
                vibrator != null && vibrator.hasVibrator(),
                device == null ? "尚未操作手柄" : device.getName());
    }

    @Nullable
    private Vibrator resolveControllerVibrator() {
        return resolveVibrator(inputManager.getInputDevice(
                activeControllerId));
    }

    @Nullable
    private static Vibrator resolveVibrator(
            @Nullable InputDevice device) {
        if (device == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return device.getVibratorManager().getDefaultVibrator();
        }
        return getInputDeviceVibratorBeforeS(device);
    }

    @SuppressWarnings("deprecation")
    private static Vibrator getInputDeviceVibratorBeforeS(
            InputDevice device) {
        return device.getVibrator();
    }

    private static Vibrator resolveDeviceVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager =
                    (VibratorManager) context.getSystemService(
                            Context.VIBRATOR_MANAGER_SERVICE);
            if (manager != null) {
                return manager.getDefaultVibrator();
            }
        }
        Vibrator vibrator = ContextCompat.getSystemService(
                context,
                Vibrator.class);
        if (vibrator == null) {
            throw new IllegalStateException(
                    "Android vibrator service is unavailable");
        }
        return vibrator;
    }

    private static boolean vibrate(Vibrator vibrator) {
        try {
            if (!vibrator.hasVibrator()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        TEST_VIBRATION_DURATION_MS,
                        vibrator.hasAmplitudeControl()
                                ? TEST_VIBRATION_AMPLITUDE
                                : VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrateBeforeOreo(vibrator);
            }
            return true;
        } catch (RuntimeException e) {
            LimeLog.warning(
                    "Input diagnostics vibration failed: " +
                            e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static void vibrateBeforeOreo(Vibrator vibrator) {
        vibrator.vibrate(TEST_VIBRATION_DURATION_MS);
    }

    private static void appendDevice(
            StringBuilder output,
            InputDevice device) {
        output.append(device.getName())
                .append("\nID=")
                .append(device.getId())
                .append(" · vendor=")
                .append(device.getVendorId())
                .append(" · product=")
                .append(device.getProductId())
                .append("\n类型：")
                .append(sourceLabel(device.getSources()));

        List<InputDevice.MotionRange> ranges =
                device.getMotionRanges();
        int axisCount = 0;
        for (InputDevice.MotionRange range : ranges) {
            if (isControllerSource(range.getSource())) {
                axisCount++;
            }
        }
        output.append(" · 轴=").append(axisCount)
                .append(" · 键盘=")
                .append(device.getKeyboardType() ==
                        InputDevice.KEYBOARD_TYPE_NONE
                        ? "否"
                        : "是");
    }

    private static String sourceLabel(int sources) {
        StringBuilder result = new StringBuilder();
        appendSource(result, sources,
                InputDevice.SOURCE_GAMEPAD, "手柄按键");
        appendSource(result, sources,
                InputDevice.SOURCE_JOYSTICK, "摇杆");
        appendSource(result, sources,
                InputDevice.SOURCE_KEYBOARD, "键盘");
        appendSource(result, sources,
                InputDevice.SOURCE_MOUSE, "鼠标");
        appendSource(result, sources,
                InputDevice.SOURCE_TOUCHPAD, "触控板");
        if (result.length() == 0) {
            result.append(String.format(
                    Locale.ROOT,
                    "0x%08x",
                    sources));
        }
        return result.toString();
    }

    private static void appendSource(
            StringBuilder output,
            int sources,
            int source,
            String label) {
        if ((sources & source) != source) {
            return;
        }
        if (output.length() > 0) {
            output.append(" / ");
        }
        output.append(label);
    }

    private static boolean isControllerSource(int source) {
        return (source & InputDevice.SOURCE_GAMEPAD) ==
                InputDevice.SOURCE_GAMEPAD ||
                (source & InputDevice.SOURCE_JOYSTICK) ==
                        InputDevice.SOURCE_JOYSTICK;
    }

    private String deviceName(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        return device == null ? "未知设备" : device.getName();
    }
}
