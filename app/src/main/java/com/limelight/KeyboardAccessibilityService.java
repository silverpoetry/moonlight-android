package com.limelight;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.FileObserver;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.limelight.binding.input.StreamInputGateway;
import com.limelight.binding.input.StreamInputGatewayRegistry;
import com.limelight.input.accessibility.KeyboardRemappingFileStore;
import com.limelight.settings.android.AndroidSettingObserver;
import com.limelight.settings.input.InputSettingKeys;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KeyboardAccessibilityService extends AccessibilityService {
    //不屏蔽的按键列表
    private static final List<Integer> PASSTHROUGH_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_POWER
    );
    private AndroidSettingObserver<Boolean> keyLoggingObserver;
    private volatile boolean accessibilityKeyLoggingEnabled;
    private volatile Map<Integer, Integer> keyRemappings =
            Collections.emptyMap();
    private FileObserver keyRemappingObserver;

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        //如果是手柄类型则忽略
        InputDevice device = event.getDevice();
        if (device != null &&
                (device.getSources() & InputDevice.SOURCE_GAMEPAD) ==
                        InputDevice.SOURCE_GAMEPAD) {
            return super.onKeyEvent(event);
        }

        int action = event.getAction();
        if (action != KeyEvent.ACTION_DOWN &&
                action != KeyEvent.ACTION_UP) {
            return super.onKeyEvent(event);
        }

        StreamInputGateway inputGateway =
                StreamInputGatewayRegistry.getInstance().getActiveGateway();
        if (inputGateway == null || !inputGateway.isInputReady() ||
                PASSTHROUGH_KEYS.contains(event.getKeyCode())) {
            return super.onKeyEvent(event);
        }

        if (action == KeyEvent.ACTION_DOWN &&
                accessibilityKeyLoggingEnabled) {
            LimeLog.info(
                    "Accessibility key: scancode=" +
                            event.getScanCode() +
                            ", keycode=" + event.getKeyCode());
        }

        Integer remappedKeyCode = findRemappedKeyCode(event.getScanCode());
        if (remappedKeyCode != null) {
            inputGateway.sendKeyEvent(
                    new KeyEvent(action, remappedKeyCode));
        }
        else {
            inputGateway.sendKeyEvent(event);
        }
        return true;
    }

    private Integer findRemappedKeyCode(int scanCode) {
        // Xiaomi devices may expose the physical Esc key as Android Back.
        if (scanCode == 1) {
            return KeyEvent.KEYCODE_ESCAPE;
        }

        return keyRemappings.get(scanCode);
    }

    @Override
    public void onServiceConnected() {
        LimeLog.info("Keyboard service is connected");
        observeInputSettings();
        observeKeyRemappingFile();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.packageNames = new String[] { getApplicationContext().getPackageName() };
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        setServiceInfo(info);
    }

    private void observeInputSettings() {
        if (keyLoggingObserver != null) {
            return;
        }
        keyLoggingObserver = new AndroidSettingObserver<>(
                this,
                InputSettingKeys.ACCESSIBILITY_KEY_LOGGING,
                value -> accessibilityKeyLoggingEnabled = value);
        keyLoggingObserver.start();
    }

    private void observeKeyRemappingFile() {
        if (keyRemappingObserver != null) {
            return;
        }
        reloadKeyRemappings();
        keyRemappingObserver = createKeyRemappingObserver(getFilesDir());
        keyRemappingObserver.startWatching();
    }

    @SuppressWarnings("deprecation")
    private FileObserver createKeyRemappingObserver(File directory) {
        return new FileObserver(
                directory.getAbsolutePath(),
                FileObserver.CLOSE_WRITE |
                        FileObserver.MOVED_TO |
                        FileObserver.DELETE |
                        FileObserver.MOVED_FROM) {
            @Override
            public void onEvent(int event, String path) {
                if (KeyboardRemappingFileStore.isManagedFileName(path)) {
                    reloadKeyRemappings();
                }
            }
        };
    }

    private void reloadKeyRemappings() {
        try {
            keyRemappings = KeyboardRemappingFileStore.load(this);
        }
        catch (IOException | RuntimeException error) {
            LimeLog.warning(
                    "Unable to load accessibility key mapping: " +
                            error.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        if (keyLoggingObserver != null) {
            keyLoggingObserver.close();
            keyLoggingObserver = null;
        }
        if (keyRemappingObserver != null) {
            keyRemappingObserver.stopWatching();
            keyRemappingObserver = null;
        }
        keyRemappings = Collections.emptyMap();
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
//        LimeLog.info("onAccessibilityEvent:"+accessibilityEvent.toString());
    }
    @Override
    public void onInterrupt() {

    }

}
