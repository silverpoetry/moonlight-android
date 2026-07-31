package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.SharedPreferencesSettingsRepository;
import com.limelight.settings.android.AndroidDisplayAspectProvider;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsLoader;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsLoader;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsLoader;
import com.limelight.settings.stream.StreamDecoderSettingKeys;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingsLoader;
import com.limelight.settings.stream.StreamBitratePolicy;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.transfer.TransferSettingsLoader;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsLoader;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;

import java.util.Objects;

public class PreferenceConfiguration {
    public enum FormatOption {
        AUTO,
        FORCE_AV1,
        FORCE_HEVC,
        FORCE_H264,
    };

    private static final String LEGACY_RES_FPS_PREF_STRING =
            StreamResolutionSettingKeys.LEGACY_RESOLUTION_AND_FPS
                    .getName();

    public static final String RESOLUTION_PREF_STRING =
            StreamResolutionSettingKeys.RESOLUTION.getName();
    public static final String RESOLUTION_SELECTION_PREF_STRING =
            StreamResolutionSettingKeys.SELECTION.getName();
    public static final String RESOLUTION_ASPECT_RATIO_PREF_STRING =
            StreamResolutionSettingKeys.ASPECT_RATIO.getName();
    public static final String FPS_PREF_STRING =
            StreamResolutionSettingKeys.FPS.getName();
    public static final String BITRATE_PREF_STRING =
            StreamVideoSettingKeys.BITRATE_KBPS.getName();
    public static final String BITRATE_PREF_OLD_STRING =
            StreamVideoSettingKeys.LEGACY_BITRATE_MBPS.getName();
    private static final String STRETCH_PREF_STRING = "checkbox_stretch_video";
    private static final String SOPS_PREF_STRING = "checkbox_enable_sops";
    private static final String DISABLE_TOASTS_PREF_STRING = "checkbox_disable_warnings";
    public static final String OSC_OPACITY_PREF_STRING =
            VirtualControlSettingKeys.CONTROL_OPACITY_PERCENT
                    .getName();
    private static final String LANGUAGE_PREF_STRING = "list_languages";
    private static final String SMALL_ICONS_PREF_STRING = "checkbox_small_icon_mode";
    private static final String VIDEO_FORMAT_PREF_STRING = "video_format";
    private static final String ENABLE_HDR_PREF_STRING = "checkbox_enable_hdr";
    public static final String ENABLE_HDR_HIGH_BRIGHTNESS_PREF_STRING = "checkbox_enable_hdr_high_brightness";
    private static final String ENABLE_PIP_PREF_STRING = "checkbox_enable_pip";
    private static final String ENABLE_PERF_OVERLAY_STRING = "checkbox_enable_perf_overlay";
    static final String UNLOCK_FPS_STRING = "checkbox_unlock_fps";
    public static final String VIBRATE_OSC_PREF_STRING =
            ControllerSettingKeys.ONSCREEN_RUMBLE.getName();
    private static final String LATENCY_TOAST_PREF_STRING = "checkbox_enable_post_stream_toast";
    public static final String BAROMETER_FORCE_PRESS_PREF_STRING =
            InputSettingKeys.BAROMETER_FORCE_PRESS.getName();
    public static final String BAROMETER_FORCE_PRESS_THRESHOLD_PREF_STRING =
            InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD.getName();
    public static final String BAROMETER_FORCE_PRESS_MIN_DURATION_PREF_STRING =
            InputSettingKeys
                    .BAROMETER_FORCE_PRESS_MINIMUM_DURATION
                    .getName();
    public static final int MIN_BAROMETER_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            InputSettingKeys.MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA;
    public static final int MAX_BAROMETER_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            InputSettingKeys.MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA;
    public static final int DEFAULT_BAROMETER_FORCE_PRESS_THRESHOLD_MILLI_HPA =
            InputSettingKeys.DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA;
    public static final int DEFAULT_BAROMETER_FORCE_PRESS_MIN_DURATION_MS =
            InputSettingKeys
                    .DEFAULT_FORCE_PRESS_MINIMUM_DURATION_MS;
    private static final String DISABLE_ADAPTIVE_INPUT_THROTTLING_PREF_STRING = "checkbox_disable_adaptive_input_throttling";
    private static final String REDUCE_REFRESH_RATE_PREF_STRING = "checkbox_reduce_refresh_rate";
    private static final String FULL_RANGE_PREF_STRING = "checkbox_full_range";
    private static final String GAMEPAD_MOTION_SENSORS_PREF_STRING =
            ControllerSettingKeys.MOTION_SENSORS.getName();

    //竖屏模式
    public static final String CHECKBOX_ENABLE_PORTRAIT = "checkbox_enable_portrait";
    //屏幕特殊按键
    private static final String CHECKBOX_ENABLE_KEYBOARD = "checkbox_enable_keyboard";

    //屏幕特殊按键 震动
    public static final String CHECKBOX_ENABLE_KEYBOARD_VIBRATE =
            VirtualControlSettingKeys.KEYBOARD_HAPTICS.getName();

    //自动摇杆

    //触控屏幕灵敏度
    public static final String TOUCH_SENSITIVITY =
            InputSettingKeys.DIRECT_TOUCH_SENSITIVITY_X.getName();

    static final String DEFAULT_RESOLUTION =
            StreamResolutionCodec.DEFAULT_RESOLUTION;
    static final String DEFAULT_FPS =
            StreamResolutionCodec.DEFAULT_FPS;
    private static final boolean DEFAULT_STRETCH = false;
    private static final boolean DEFAULT_SOPS = true;
    private static final boolean DEFAULT_DISABLE_TOASTS = false;
    public static final String DEFAULT_LANGUAGE = "default";
    private static final String DEFAULT_VIDEO_FORMAT = "auto";

    private static final boolean DEFAULT_ENABLE_HDR = false;
    private static final boolean DEFAULT_ENABLE_HDR_HIGH_BRIGHTNESS = false;
    private static final boolean DEFAULT_ENABLE_PIP = false;
    private static final boolean DEFAULT_ENABLE_PERF_OVERLAY = false;
    private static final boolean DEFAULT_UNLOCK_FPS = false;
    private static final boolean DEFAULT_LATENCY_TOAST = false;
    private static final boolean DEFAULT_DISABLE_ADAPTIVE_INPUT_THROTTLING = true;
    private static final boolean DEFAULT_REDUCE_REFRESH_RATE = false;
    private static final boolean DEFAULT_FULL_RANGE = false;

    public static final int FRAME_PACING_MIN_LATENCY = 0;
    public static final int FRAME_PACING_BALANCED = 1;
    public static final int FRAME_PACING_CAP_FPS = 2;
    public static final int FRAME_PACING_MAX_SMOOTHNESS = 3;

    public static final String RES_360P =
            StreamResolutionCodec.RESOLUTION_360P;
    public static final String RES_480P =
            StreamResolutionCodec.RESOLUTION_480P;
    public static final String RES_720P =
            StreamResolutionCodec.RESOLUTION_720P;
    public static final String RES_1080P =
            StreamResolutionCodec.RESOLUTION_1080P;
    public static final String RES_1440P =
            StreamResolutionCodec.RESOLUTION_1440P;
    public static final String RES_4K =
            StreamResolutionCodec.RESOLUTION_4K;
    public static final String RES_NATIVE = "Native";

    public enum ResolutionSelection {
        PRESET,
        CUSTOM_OR_NATIVE
    }

    public static final String RESOLUTION_SELECTION_PRESET =
            StreamResolutionCodec.SELECTION_PRESET;
    public static final String RESOLUTION_SELECTION_CUSTOM_OR_NATIVE =
            StreamResolutionCodec.SELECTION_CUSTOM_OR_NATIVE;
    public static final String RESOLUTION_ASPECT_RATIO_16_9 =
            StreamResolutionCodec.ASPECT_RATIO_16_9;
    public static final String RESOLUTION_ASPECT_RATIO_NATIVE =
            StreamResolutionCodec.ASPECT_RATIO_NATIVE;

    public ResolutionSelection resolutionSelection;
    public String resolutionAspectRatio;
    public int width, height, fps;
    public int bitrate;
    public FormatOption videoFormat;
    public int deadzonePercentage;
    public int oscOpacity;
    public int oscKeyboardOpacity;
    public int oscKeyboardHeight;
    public boolean stretchVideo, enableSops, playHostAudio, disableWarnings;
    public String language;
    public boolean smallIconMode, multiController, usbDriver, flipFaceButtons;
    public boolean onscreenController;
    public boolean onlyL3R3;
    public boolean showGuideButton;
    public boolean enableHdr;
    public boolean enableHdrHighBrightness;
    public boolean enablePip;
    public boolean enablePerfOverlay;

    public boolean enableLatencyToast;
    //软键盘
    public boolean enableQtDialog;
    //竖屏模式
    public boolean enablePortrait;
    //虚拟屏幕键盘按键
    public boolean enableKeyboard;
    //修复JoyCon十字键
    public boolean enableJoyConFix;

    //是否上报手柄的电池信息
    public boolean enableBatteryReport;

    //自由摇杆啊
    public boolean enableNewAnalogStick;

    public boolean enableExDisplay;

    //触控屏幕灵敏度
    public int touchSensitivityX;
    public int touchSensitivityY;
    //超出边界自动回中心点
    public boolean touchSensitivityRotationAuto;

    //触控灵敏度调节范围
    public boolean touchSensitivityGlobal;

    //多点触控灵敏度调节
    public boolean enableTouchSensitivity;

    //触控板模式灵敏度
    public int touchPadSensitivity;

    public int touchPadYSensitity;

    //鼠标触控板模式灵敏度x轴
    public int mouseTouchPadSensitityX;
    public int mouseTouchPadSensitityY;

    // 外设触控板灵敏度
    public int externalTouchPadSensitityX;
    public int externalTouchPadSensitityY;
    public int externalTouchPadScrollAmount;

    //物理光标捕获
    public boolean enableMouseLocalCursor;

    //禁用内置的特殊指令
    public boolean enableClearDefaultSpecial;

    //强制使用设备自身的震动马达
    public boolean enableDeviceRumble;

    public boolean enableKeyboardVibrate;

    public boolean enableKeyboardSquare;

    //虚拟手柄皮肤
    public int gamepad_skin;

    //自由摇杆背景透明度
    public int senableNewAnalogStickOpacity;

    //自由摇杆固定键程
    public boolean senableNewAnalogStickOpacityFixed;

    //启动自定义配置文件
    public boolean enableCustomKeyboardFile;

    public boolean bindAllUsb;
    public boolean mouseEmulation;
    public int mouseEmulationGameMenu;
    public boolean unlockFps;
    public boolean vibrateOsc;
    public boolean vibrateFallbackToDevice;
    public int vibrateFallbackToDeviceStrength;
    public MoonBridge.AudioConfiguration audioConfiguration;
    public int framePacing;
    public boolean absoluteMouseMode;
    public boolean enableNativeCursor;
    public boolean enableClipboardSync;
    public boolean disableAdaptiveInputThrottling;
    public boolean enableAudioFx;
    public boolean reduceRefreshRate;
    public boolean fullRange;
    public boolean gamepadMotionSensors;
    public boolean gamepadTouchpadAsMouse;
    public boolean gamepadMotionSensorsFallbackToDevice;

    //开启虚拟手柄的陀螺仪功能
    public boolean enableVirtualControllerMotion;

    //填充刘海区域
    public boolean enableCutoutModeVideo;

    //部分页面主题色白色
    public boolean uiThemeColorWhite;

    //禁用扳机死区
    public boolean disableTriggerDeadzone;

    //反转左右握把震动顺序
    public boolean enableFlipRumbleFF;

    //显示无障碍模式的键值
    public boolean enableAccessibilityShowLog;

    //亮屏自动回连
    public int enableScreenOnAuto;

    //使用自定义主屏幕背景
    public boolean enableScreenBg;

    //主屏幕背景高斯模糊
    public boolean enableScreenObscure;

    //主屏幕文本
    public String screenLabel;

    //忽略应用列表的弹出菜单
    public boolean passAppMenu;

    //虚拟按键正常模式的颜色
    public int virtualkeyViewNormalColor;

    //雷蛇虚拟显示器
    public int razerVD;

    //内置的虚拟按键布局
    public int virtualKeyboardFileUsed;

    //usb手柄驱动 上报陀螺仪信息
    public boolean usbGyroscopeReport;

    //虚拟手柄按键 缩放系数
    public int virtualGamePadScaleFactor;

    //低延迟模式 实验性
    public boolean lowLatencyExperiment;

    //手柄键鼠模式 鼠标指针灵敏度
    public int mouseGamePadSensitity;

    //禁言游戏模式=quest可能有效
    public boolean enableGameManagerQuest;

    //强制显示模式
    public boolean enforceDisplayMode;

    //禁用虚拟手柄摇杆l3r3
    public boolean disableRockerClickL3R3;

    //忽略校验HDR
    public boolean ignoreCheckHDR;

    //解锁屏幕方向锁定
    public boolean autoScreenOrientation;

    public boolean isNativeResolution() {
        return resolutionSelection != ResolutionSelection.PRESET && isNativeResolution(width, height);
    }

    //记住全键盘 组合键模式
    public boolean keyboard_axi_combination;

    public static boolean isStandardResolutionPreset(String resString) {
        return StreamResolutionCodec.isStandardResolutionPreset(
                resString);
    }

    public static ResolutionSelection getResolutionSelectionFromString(String value) {
        if (RESOLUTION_SELECTION_PRESET.equals(value)) {
            return ResolutionSelection.PRESET;
        }

        return ResolutionSelection.CUSTOM_OR_NATIVE;
    }

    public static String getResolutionSelectionString(ResolutionSelection selection) {
        return selection == ResolutionSelection.PRESET ?
                RESOLUTION_SELECTION_PRESET : RESOLUTION_SELECTION_CUSTOM_OR_NATIVE;
    }

    public static boolean isNativeResolution(int width, int height) {
        // It's not a native resolution if it matches an existing resolution option
        if (width == 640 && height == 360) {
            return false;
        }
        else if (width == 854 && height == 480) {
            return false;
        }
        else if (width == 1280 && height == 720) {
            return false;
        }
        else if (width == 1920 && height == 1080) {
            return false;
        }
        else if (width == 2560 && height == 1440) {
            return false;
        }
        else if (width == 3840 && height == 2160) {
            return false;
        }

        return true;
    }

    // If we have a screen that has semi-square dimensions, we may want to change our behavior
    // to allow any orientation and vertical+horizontal resolutions.
    public static boolean isSquarishScreen(int width, int height) {
        float longDim = Math.max(width, height);
        float shortDim = Math.min(width, height);

        // We just put the arbitrary cutoff for a square-ish screen at 1.3
        return longDim / shortDim < 1.3f;
    }

    private static StreamResolutionCodec.DisplayAspect getDisplayAspect(
            Context context) {
        return AndroidDisplayAspectProvider.get(context);
    }

    public static int getDefaultBitrate(Context context, String resString, String fpsString,
                                        ResolutionSelection selection, String aspectRatio) {
        StreamResolutionCodec.Result resolution =
                StreamResolutionCodec.decode(
                        resString,
                        getResolutionSelectionString(selection),
                        aspectRatio,
                        fpsString,
                        getDisplayAspect(context));
        return calculateDefaultBitrate(
                resolution.getWidth(),
                resolution.getHeight(),
                resolution.getFps());
    }

    private static int calculateDefaultBitrate(
            int width,
            int height,
            int fps) {
        return StreamBitratePolicy.calculateDefaultBitrateKbps(
                width,
                height,
                fps);
    }

    public static int getDefaultBitrate(Context context, String resString, String fpsString) {
        SettingsRepository repository =
                new SharedPreferencesSettingsRepository(
                        PreferenceManager.getDefaultSharedPreferences(
                                context));
        String selectionValue = repository.contains(
                StreamResolutionSettingKeys.SELECTION)
                ? repository.get(
                        StreamResolutionSettingKeys.SELECTION)
                : isStandardResolutionPreset(resString)
                        ? RESOLUTION_SELECTION_PRESET
                        : RESOLUTION_SELECTION_CUSTOM_OR_NATIVE;
        ResolutionSelection selection = getResolutionSelectionFromString(
                selectionValue);
        String aspectRatio = repository.get(
                StreamResolutionSettingKeys.ASPECT_RATIO);
        return getDefaultBitrate(context, resString, fpsString, selection, aspectRatio);
    }

    public static boolean getDefaultSmallMode(Context context) {
        PackageManager manager = context.getPackageManager();
        if (manager != null) {
            // TVs shouldn't use small mode by default
            if (manager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                return false;
            }

            // API 21 uses LEANBACK instead of TELEVISION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (manager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
                    return false;
                }
            }
        }

        // Use small mode on anything smaller than a 7" tablet
        return context.getResources().getConfiguration().smallestScreenWidthDp < 500;
    }

    public static int getDefaultBitrate(Context context) {
        SettingsRepository repository =
                new SharedPreferencesSettingsRepository(
                        PreferenceManager.getDefaultSharedPreferences(
                                context));
        StreamResolutionCodec.Result resolution =
                StreamResolutionSettingsLoader.load(
                        repository,
                        getDisplayAspect(context));
        return calculateDefaultBitrate(
                resolution.getWidth(),
                resolution.getHeight(),
                resolution.getFps());
    }

    private static FormatOption getVideoFormatValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String str = prefs.getString(VIDEO_FORMAT_PREF_STRING, DEFAULT_VIDEO_FORMAT);
        if (str.equals("auto")) {
            return FormatOption.AUTO;
        }
        else if (str.equals("forceav1")) {
            return FormatOption.FORCE_AV1;
        }
        else if (str.equals("forceh265")) {
            return FormatOption.FORCE_HEVC;
        }
        else if (str.equals("neverh265")) {
            return FormatOption.FORCE_H264;
        }
        else {
            // Should never get here
            return FormatOption.AUTO;
        }
    }

    private static int getFramePacingValue(
            SettingsRepository repository) {
        String str = repository.get(
                StreamDecoderSettingKeys.FRAME_PACING);
        if (str.equals("latency")) {
            return FRAME_PACING_MIN_LATENCY;
        }
        else if (str.equals("balanced")) {
            return FRAME_PACING_BALANCED;
        }
        else if (str.equals("cap-fps")) {
            return FRAME_PACING_CAP_FPS;
        }
        else if (str.equals("smoothness")) {
            return FRAME_PACING_MAX_SMOOTHNESS;
        }
        else {
            // Should never get here
            return FRAME_PACING_MIN_LATENCY;
        }
    }

    /**
     * Updates the temporary legacy view from the canonical audio snapshot.
     *
     * <p>Runtime code must consume {@link StreamAudioSettings} directly.
     * This bridge exists only while legacy menu surfaces still receive this
     * mutable configuration object.</p>
     */
    public void applyAudioSettings(StreamAudioSettings settings) {
        Objects.requireNonNull(settings, "settings");

        switch (settings.getChannelConfiguration()) {
            case SURROUND_7_1:
                audioConfiguration =
                        MoonBridge.AUDIO_CONFIGURATION_71_SURROUND;
                break;
            case SURROUND_5_1:
                audioConfiguration =
                        MoonBridge.AUDIO_CONFIGURATION_51_SURROUND;
                break;
            case STEREO:
            default:
                audioConfiguration =
                        MoonBridge.AUDIO_CONFIGURATION_STEREO;
                break;
        }

        playHostAudio = settings.shouldPlayHostAudio();
        enableAudioFx = settings.areAudioEffectsEnabled();
    }

    public static void resetStreamingSettings(Context context) {
        // We consider resolution, FPS, bitrate, HDR, and video format as "streaming settings" here
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(BITRATE_PREF_STRING)
                .remove(BITRATE_PREF_OLD_STRING)
                .remove(LEGACY_RES_FPS_PREF_STRING)
                .remove(RESOLUTION_PREF_STRING)
                .remove(FPS_PREF_STRING)
                .remove(VIDEO_FORMAT_PREF_STRING)
                .remove(ENABLE_HDR_PREF_STRING)
                .remove(UNLOCK_FPS_STRING)
                .remove(FULL_RANGE_PREF_STRING)
                .apply();
    }

    public static void completeLanguagePreferenceMigration(Context context) {
        // Put our language option back to default which tells us that we've already migrated it
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(LANGUAGE_PREF_STRING, DEFAULT_LANGUAGE).apply();
    }

    public static boolean isShieldAtvFirmwareWithBrokenHdr() {
        // This particular Shield TV firmware crashes when using HDR
        // https://www.nvidia.com/en-us/geforce/forums/notifications/comment/155192/
        return Build.MANUFACTURER.equalsIgnoreCase("NVIDIA") &&
                Build.FINGERPRINT.contains("PPR1.180610.011/4079208_2235.1395");
    }

    public static PreferenceConfiguration readPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsRepository repository =
                new SharedPreferencesSettingsRepository(prefs);
        SettingsMigrationRunner.migrate(repository);
        PreferenceConfiguration config = new PreferenceConfiguration();

        StreamResolutionCodec.Result resolution =
                StreamResolutionSettingsLoader.load(
                        repository,
                        getDisplayAspect(context));
        config.width = resolution.getWidth();
        config.height = resolution.getHeight();
        config.fps = resolution.getFps();
        config.resolutionSelection =
                resolution.getSelection() ==
                        StreamResolutionCodec.Selection.PRESET
                        ? ResolutionSelection.PRESET
                        : ResolutionSelection.CUSTOM_OR_NATIVE;
        config.resolutionAspectRatio =
                resolution.getCanonicalAspectRatio();
        InputSettings inputSettings =
                InputSettingsLoader.load(repository);

        if (!prefs.contains(SMALL_ICONS_PREF_STRING)) {
            // We need to write small icon mode's default to disk for the settings page to display
            // the current state of the option properly
            prefs.edit().putBoolean(SMALL_ICONS_PREF_STRING, getDefaultSmallMode(context)).apply();
        }

        if (!prefs.contains(GAMEPAD_MOTION_SENSORS_PREF_STRING) && Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            // Android 12 has a nasty bug that causes crashes when the app touches the InputDevice's
            // associated InputDeviceSensorManager (just calling getSensorManager() is enough).
            // As a workaround, we will override the default value for the gamepad motion sensor
            // option to disabled on Android 12 to reduce the impact of this bug.
            // https://cs.android.com/android/_/android/platform/frameworks/base/+/8970010a5e9f3dc5c069f56b4147552accfcbbeb
            prefs.edit().putBoolean(GAMEPAD_MOTION_SENSORS_PREF_STRING, false).apply();
        }
        ControllerSettings controllerSettings =
                ControllerSettingsLoader.load(repository);
        StreamAudioSettings audioSettings =
                StreamAudioSettingsLoader.load(repository);
        TransferSettings transferSettings =
                TransferSettingsLoader.load(repository);
        VirtualControlSettings virtualControlSettings =
                VirtualControlSettingsLoader.load(repository);
        StreamUiSettings streamUiSettings =
                StreamUiSettingsLoader.load(repository);

        // This must happen after the preferences migration to ensure the preferences are populated
        config.bitrate = prefs.getInt(BITRATE_PREF_STRING, prefs.getInt(BITRATE_PREF_OLD_STRING, 0) * 1000);
        if (config.bitrate == 0) {
            config.bitrate = getDefaultBitrate(context);
        }

        config.applyAudioSettings(audioSettings);

        config.videoFormat = getVideoFormatValue(context);
        config.framePacing = getFramePacingValue(repository);

        config.deadzonePercentage =
                controllerSettings.getStickDeadzonePercent();

        config.oscOpacity =
                virtualControlSettings.getControlOpacityPercent();

        config.language = prefs.getString(LANGUAGE_PREF_STRING, DEFAULT_LANGUAGE);

        // Checkbox preferences
        config.disableWarnings = prefs.getBoolean(DISABLE_TOASTS_PREF_STRING, DEFAULT_DISABLE_TOASTS);
        config.enableSops = prefs.getBoolean(SOPS_PREF_STRING, DEFAULT_SOPS);
        config.stretchVideo = prefs.getBoolean(STRETCH_PREF_STRING, DEFAULT_STRETCH);
        config.smallIconMode = prefs.getBoolean(SMALL_ICONS_PREF_STRING, getDefaultSmallMode(context));
        config.multiController =
                controllerSettings.isMultiControllerEnabled();
        config.usbDriver =
                controllerSettings.isUsbDriverEnabled();
        config.onscreenController =
                controllerSettings.isOnscreenControllerEnabled();
        config.onlyL3R3 =
                controllerSettings.isOnlyL3R3Enabled();
        config.showGuideButton =
                virtualControlSettings.isGuideButtonVisible();
        config.enableHdr = prefs.getBoolean(ENABLE_HDR_PREF_STRING, DEFAULT_ENABLE_HDR) && !isShieldAtvFirmwareWithBrokenHdr();
        config.enableHdrHighBrightness = prefs.getBoolean(ENABLE_HDR_HIGH_BRIGHTNESS_PREF_STRING,
                DEFAULT_ENABLE_HDR_HIGH_BRIGHTNESS);
        config.enablePip = prefs.getBoolean(ENABLE_PIP_PREF_STRING, DEFAULT_ENABLE_PIP);
        config.enablePerfOverlay =
                streamUiSettings.isPerformanceOverlayEnabled();
        config.bindAllUsb =
                controllerSettings.shouldClaimAllUsbDevices();
        config.mouseEmulation =
                controllerSettings.isMouseEmulationEnabled();
        config.unlockFps = prefs.getBoolean(UNLOCK_FPS_STRING, DEFAULT_UNLOCK_FPS);
        config.vibrateOsc =
                controllerSettings.isOnscreenRumbleEnabled();
        config.vibrateFallbackToDevice =
                controllerSettings.isFallbackDeviceRumbleEnabled();
        config.vibrateFallbackToDeviceStrength =
                controllerSettings
                        .getFallbackDeviceRumbleStrengthPercent();
        config.flipFaceButtons =
                controllerSettings.areFaceButtonsFlipped();
        config.enableLatencyToast = prefs.getBoolean(LATENCY_TOAST_PREF_STRING, DEFAULT_LATENCY_TOAST);
        //软键盘
        config.enableQtDialog =
                controllerSettings
                        .doesMouseEmulationOpenGameMenu();
        config.enablePortrait = prefs.getBoolean(CHECKBOX_ENABLE_PORTRAIT,false);

        config.enableKeyboard = prefs.getBoolean(CHECKBOX_ENABLE_KEYBOARD,false);

        config.enableKeyboardVibrate =
                virtualControlSettings.isKeyboardHapticsEnabled();
        //兼容joycon手柄
        config.enableJoyConFix =
                controllerSettings.isJoyConFixEnabled();
        //全键盘透明度
        config.oscKeyboardOpacity =
                virtualControlSettings.getKeyboardOpacityPercent();

        config.enableBatteryReport =
                controllerSettings.isBatteryReportingEnabled();

        config.gamepad_skin =
                virtualControlSettings.getGamepadSkin();

        config.senableNewAnalogStickOpacity =
                virtualControlSettings
                        .getFreeStickOpacityPercent();

        config.oscKeyboardHeight =
                virtualControlSettings.getKeyboardHeightDp();

        config.enableNewAnalogStick =
                virtualControlSettings.areFreeSticksEnabled();

        config.enableExDisplay=prefs.getBoolean("checkbox_enable_exdisplay",false);

        config.touchSensitivityX =
                inputSettings.getDirectTouchSensitivityX();

        config.touchSensitivityY =
                inputSettings.getDirectTouchSensitivityY();

        config.touchSensitivityRotationAuto =
                inputSettings.isDirectTouchRecenterEnabled();

        config.touchSensitivityGlobal =
                inputSettings.isDirectTouchSensitivityGlobal();

        config.enableTouchSensitivity =
                inputSettings.isDirectTouchSensitivityEnabled();

        config.enableMouseLocalCursor=prefs.getBoolean("checkbox_mouse_local_cursor",false);

        config.enableClearDefaultSpecial=prefs.getBoolean("checkbox_enable_clear_default_special_button", false);

        config.enableDeviceRumble =
                controllerSettings.isDeviceRumbleEnabled();

        config.enableKeyboardSquare =
                virtualControlSettings.areSquareButtonsEnabled();

        config.touchPadSensitivity =
                inputSettings.getVirtualTouchpadSensitivityX();

        config.touchPadYSensitity =
                inputSettings.getVirtualTouchpadSensitivityY();

        config.senableNewAnalogStickOpacityFixed =
                virtualControlSettings.areFixedFreeSticksEnabled();

        config.enableVirtualControllerMotion =
                controllerSettings
                        .isVirtualControllerMotionEnabled();

        config.enableCutoutModeVideo=prefs.getBoolean("checkbox_cutout_mode_video",false);

        config.enableCustomKeyboardFile=prefs.getBoolean("checkbox_enable_custom_axi_keyboard_file",false);

        config.mouseTouchPadSensitityX =
                inputSettings.getTouchpadPointerSensitivityX();
        config.mouseTouchPadSensitityY =
                inputSettings.getTouchpadPointerSensitivityY();
        config.externalTouchPadSensitityX =
                inputSettings.getExternalTouchpadSensitivityX();
        config.externalTouchPadSensitityY =
                inputSettings.getExternalTouchpadSensitivityY();
        config.externalTouchPadScrollAmount =
                inputSettings.getExternalTouchpadScrollAmount();

        config.uiThemeColorWhite=prefs.getBoolean("checkbox_ui_theme_white",true);
        config.disableTriggerDeadzone =
                controllerSettings.isTriggerDeadzoneDisabled();

        config.enableFlipRumbleFF =
                controllerSettings.areRumbleMotorsFlipped();

        config.enableAccessibilityShowLog=prefs.getBoolean("checkbox_enable_accessibility_show_log",false);

        config.enableScreenOnAuto=prefs.getInt("enable_screen_on_auto",0);

        config.enableScreenBg=prefs.getBoolean("checkbox_enable_screen_bg",false);

        config.enableScreenObscure=prefs.getBoolean("checkbox_enable_screen_obscure",true);

        config.screenLabel=prefs.getString("change_screen_label_key","");

        config.mouseEmulationGameMenu =
                controllerSettings.getMouseEmulationButton();

        config.passAppMenu=prefs.getBoolean("checkbox_enable_pass_menu",false);

        config.virtualkeyViewNormalColor =
                virtualControlSettings.getNormalColor();

        config.virtualKeyboardFileUsed=prefs.getInt("virtual_Key_board_file_used",0);

        config.razerVD=prefs.getInt("vdValue",0);

        config.usbGyroscopeReport =
                controllerSettings
                        .isUsbGyroscopeReportingEnabled();

        config.virtualGamePadScaleFactor =
                virtualControlSettings.getGamepadScalePercent();

        config.lowLatencyExperiment=prefs.getBoolean("enable_lowLatency_experiment",true);

        config.mouseGamePadSensitity =
                controllerSettings.getMouseSensitivityPercent();

        config.enableGameManagerQuest=prefs.getBoolean("checkbox_enable_game_manager_quest",false);

        config.disableRockerClickL3R3 =
                virtualControlSettings.isStickClickDisabled();

        config.enforceDisplayMode=prefs.getBoolean("checkbox_enforce_display_mode",false);
        config.absoluteMouseMode =
                inputSettings.isAbsoluteMouseMode();
        config.enableNativeCursor = config.absoluteMouseMode;
        config.enableClipboardSync =
                transferSettings.isClipboardSyncEnabled();
        config.disableAdaptiveInputThrottling = prefs.getBoolean(DISABLE_ADAPTIVE_INPUT_THROTTLING_PREF_STRING,
                DEFAULT_DISABLE_ADAPTIVE_INPUT_THROTTLING);
        config.reduceRefreshRate = prefs.getBoolean(REDUCE_REFRESH_RATE_PREF_STRING, DEFAULT_REDUCE_REFRESH_RATE);
        config.fullRange = prefs.getBoolean(FULL_RANGE_PREF_STRING, DEFAULT_FULL_RANGE);
        config.gamepadTouchpadAsMouse =
                controllerSettings.isTouchpadAsMouse();
        config.gamepadMotionSensors =
                controllerSettings.areMotionSensorsEnabled();
        config.gamepadMotionSensorsFallbackToDevice =
                controllerSettings
                        .isMotionSensorsFallbackToDeviceEnabled();

        config.ignoreCheckHDR=prefs.getBoolean("ignoreCheckHDR",false);

        config.autoScreenOrientation =
                virtualControlSettings
                        .isAutomaticScreenOrientationEnabled();

        config.keyboard_axi_combination =
                virtualControlSettings
                        .isKeyboardCombinationModeEnabled();

        return config;
    }

}
