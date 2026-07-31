package com.limelight;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.limelight.binding.input.StreamInputGateway;
import com.limelight.binding.input.StreamInputGatewayRegistry;
import com.limelight.settings.android.AndroidSettingObserver;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.utils.FileUriUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class KeyboardAccessibilityService extends AccessibilityService {
    private static final String KEY_REMAP_FILE_NAME =
            "axi_switch_keyboard.json";

    //不屏蔽的按键列表
    private static final List<Integer> PASSTHROUGH_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_POWER
    );
    private AndroidSettingObserver<Boolean> keyLoggingObserver;
    private volatile boolean accessibilityKeyLoggingEnabled;

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

        File mappingFile = new File(getFilesDir(), KEY_REMAP_FILE_NAME);
        String authority =
                getApplicationContext().getPackageName() + ".fileprovider";
        Uri uri = FileProvider.getUriForFile(this, authority, mappingFile);
        String mappingJson = FileUriUtils.openUriForRead(this, uri);
        if (TextUtils.isEmpty(mappingJson)) {
            return null;
        }

        try {
            JSONArray mappings =
                    new JSONObject(mappingJson).getJSONArray("data");
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                if (scanCode == mapping.getInt("scancode")) {
                    return mapping.getInt("code");
                }
            }
        }
        catch (Exception error) {
            LimeLog.warning(
                    "Unable to parse accessibility key mapping: " +
                            error.getMessage());
        }
        return null;
    }

    @Override
    public void onServiceConnected() {
        LimeLog.info("Keyboard service is connected");
        observeInputSettings();
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

    @Override
    public void onDestroy() {
        if (keyLoggingObserver != null) {
            keyLoggingObserver.close();
            keyLoggingObserver = null;
        }
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
