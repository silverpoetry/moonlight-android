package com.limelight;


import android.Manifest;
import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.audio.mic.AndroidMicrophoneUplinkSessionFactory;
import com.limelight.binding.input.AndroidControllerInventory;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardInputController;
import com.limelight.binding.input.KeyboardInputSink;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.StreamInputGateway;
import com.limelight.binding.input.StreamInputGatewayRegistry;
import com.limelight.binding.input.StreamInputController;
import com.limelight.binding.input.StreamInputLifecycleController;
import com.limelight.binding.input.protocol.NvConnectionPointerInputSink;
import com.limelight.binding.input.protocol.NvConnectionKeyboardInputSink;
import com.limelight.binding.input.capture.InputCaptureManager;
import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.driver.UsbDriverServiceEndpoint;
import com.limelight.binding.input.driver.UsbDriverSessionController;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.binding.input.pointer.ExternalPointerInputController;
import com.limelight.binding.input.touch.DirectContactInputController;
import com.limelight.binding.input.touch.TouchInputController;
import com.limelight.binding.input.touch.TouchInputMode;
import com.limelight.binding.input.virtual_controller.VirtualController;
import com.limelight.binding.input.virtual_controller.keyboard.AndroidVirtualControlsFactory;
import com.limelight.binding.input.virtual_controller.keyboard.StreamVirtualControlsController;
import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.binding.video.AndroidDecoderCrashStore;
import com.limelight.binding.video.DecoderCrashTracker;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.StreamSessionController;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.mic.MicrophoneUplinkConfig;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidDisplayAspectProvider;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.android.AndroidStreamSettingsBootstrap;
import com.limelight.settings.android.SharedPreferencesCustomResolutionRepository;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsLoader;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsLoader;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.controller.ControllerSettingsUpdate;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsLoader;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.input.InputSettingsUpdate;
import com.limelight.settings.stream.CustomResolutionRepository;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDecoderSettingsLoader;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.stream.StreamDisplaySettingsLoader;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsLoader;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.transfer.TransferSettingsLoader;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult;
import com.limelight.settings.ui.GameMenuCardLayoutRepository;
import com.limelight.settings.ui.SettingsGameMenuCardLayoutRepository;
import com.limelight.shortcuts.GameMenuShortcut;
import com.limelight.shortcuts.GameMenuShortcutRepository;
import com.limelight.shortcuts.android.SharedPreferencesGameMenuShortcutRepository;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsLoader;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.settings.ui.StreamUiSettingsUpdate;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutRepository;
import com.limelight.virtualcontrols.layout.android.AndroidVirtualControlLayoutRepository;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate;
import com.limelight.ui.gamemenu.GameMenuFragment;
import com.limelight.ui.gamemenu.GameMenuHost;
import com.limelight.ui.gamemenu.GameMenuSession;
import com.limelight.ui.clipboard.RemoteClipboardFileTransferController;
import com.limelight.ui.performance.PerformanceOverlayRuntimeState;
import com.limelight.ui.performance.PerformanceOverlayConfiguration;
import com.limelight.ui.performance.StreamPerformanceOverlayController;
import com.limelight.ui.stream.AndroidStreamConnectionMessages;
import com.limelight.ui.stream.AndroidStreamHdrCapabilityProvider;
import com.limelight.ui.stream.AndroidStreamMediaRuntimeFactory;
import com.limelight.ui.stream.StreamDisplayModeSelector;
import com.limelight.ui.stream.StreamDecoderCapabilities;
import com.limelight.ui.stream.StreamFailureDiagnostics;
import com.limelight.ui.stream.StreamHdrRequestPolicy;
import com.limelight.ui.stream.StreamLaunchReporter;
import com.limelight.ui.stream.StreamMediaResourceOwner;
import com.limelight.ui.stream.StreamMicrophoneController;
import com.limelight.ui.stream.StreamRenderSurfaceController;
import com.limelight.ui.stream.StreamSessionCallbackRouter;
import com.limelight.ui.stream.StreamSessionConfigurationAdapter;
import com.limelight.ui.stream.StreamSessionConfigurationPlanner;
import com.limelight.ui.stream.StreamSessionPresentationController;
import com.limelight.ui.stream.StreamSessionUiEffects;
import com.limelight.ui.stream.StreamWifiLockController;
import com.limelight.ui.GameGestures;
import com.limelight.ui.NativeCursorOverlayView;
import com.limelight.ui.StreamLayoutGeometry;
import com.limelight.ui.StreamWindowPolicy;
import com.limelight.ui.StreamUiActions;
import com.limelight.ui.StreamView;
import com.limelight.ui.floatingview.StreamFloatingControlController;
import com.limelight.utils.AutoReconnectHelper;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.Dialog;
import com.limelight.utils.RazerUtils;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.StreamOrientationController;
import com.limelight.utils.StreamOrientationRequest;
import com.limelight.utils.UiHelper;
import com.limelight.utils.ViewWindowGeometry;
import android.annotation.SuppressLint;
import androidx.annotation.RequiresApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Rational;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.limelight.utils.UiToast;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class Game extends Activity implements OnGenericMotionListener,
        OnTouchListener, EvdevListener,
        OnSystemUiVisibilityChangeListener, GameGestures, StreamInputGateway,
        StreamUiActions, GameMenuHost,
        UsbDriverService.UsbDriverStateListener, View.OnKeyListener {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;
    private static final long KEY_CHORD_UP_DELAY_MS = 25;

    private StreamInputController streamInputController;
    private StreamInputLifecycleController inputLifecycleController;
    private InputSettingsState inputSettingsState;

    private static final int SOFT_KEYBOARD_SHOW_RETRY_MS = 50;

    private ControllerHandler controllerHandler;
    private ControllerSettingsState controllerSettingsState;
    private StreamAudioSettingsState streamAudioSettingsState;
    private StreamUiSettingsState streamUiSettingsState;
    private VirtualControlSettingsState virtualControlSettingsState;
    private VirtualControlLayoutRepository virtualControlLayoutRepository;
    private KeyboardInputController keyboardInputController;
    private StreamVirtualControlsController virtualControlsController;

    private StreamDisplaySettings streamDisplaySettings;
    private StreamDecoderSettings streamDecoderSettings;
    private StreamDecoderSettings.FramePacing
            effectiveFramePacing;
    private StreamVideoSettings streamVideoSettings;
    private StreamVideoSettingsState streamVideoSettingsState;
    private CustomResolutionRepository
            customResolutionRepository;
    private TransferSettings transferSettings;
    private SettingsRepository settingsRepository;
    private GameMenuCardLayoutRepository
            gameMenuCardLayoutRepository;
    private GameMenuShortcutRepository
            gameMenuShortcutRepository;
    private DecoderCrashTracker decoderCrashTracker;

    private NvConnection conn;
    private StreamSessionController sessionController;
    private StreamSessionCallbackRouter sessionCallbackRouter;
    private StreamSessionPresentationController
            sessionPresentationController;
    private StreamLaunchReporter launchReporter;
    private StreamSessionUiEffects sessionUiEffects;
    private StreamMicrophoneController microphoneController;
    private SpinnerDialog spinner;
    private RemoteClipboardFileTransferController
            clipboardFileTransferController;
    private boolean autoEnterPip = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;
    private String streamHost;
    private long streamStartElapsedMs;
    private NvApp app;
    private float selectedDisplayRefreshRate;
    private volatile boolean sessionDependenciesReady;

    private InputCaptureProvider inputCaptureProvider;
    private boolean grabbedInput = true;
    private boolean cursorVisible = false;
    private StreamView streamView;
    private final Runnable toggleKeyboardWhenFocused =
            this::toggleKeyboard;
    private final Runnable showSoftKeyboardRetry = () -> {
        if (!canPresentSessionUi() ||
                streamView == null ||
                !streamView.isImeActive()) {
            return;
        }
        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
        streamView.requestFocus();
        inputManager.showSoftInput(
                streamView,
                InputMethodManager.SHOW_IMPLICIT);
    };
    private NativeCursorOverlayView nativeCursorOverlayView;

    private boolean isHidingOverlays;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private StreamPerformanceOverlayController
            performanceOverlayController;
    private StreamFloatingControlController
            floatingControlController;

    private StreamMediaResourceOwner mediaResourceOwner;
    private StreamRenderSurfaceController
            renderSurfaceController;

    private StreamWifiLockController wifiLockController;

    private UsbDriverSessionController usbDriverSessionController;
    private final ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder =
                    (UsbDriverService.UsbDriverBinder) iBinder;
            UsbDriverSessionController sessionController =
                    usbDriverSessionController;
            if (sessionController != null) {
                sessionController.onConnected(
                        new UsbDriverServiceEndpoint(
                                binder,
                                controllerSettingsState,
                                streamAudioSettingsState,
                                controllerHandler,
                                Game.this));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            UsbDriverSessionController sessionController =
                    usbDriverSessionController;
            if (sessionController != null) {
                sessionController.onDisconnected();
            }
        }
    };

    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_PORT = "Port";
    public static final String EXTRA_HTTPS_PORT = "HttpsPort";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_ID = "AppId";
    public static final String EXTRA_UNIQUEID = "UniqueId";
    public static final String EXTRA_PC_UUID = "UUID";
    public static final String EXTRA_PC_NAME = "PcName";
    public static final String EXTRA_APP_HDR = "HDR";
    public static final String EXTRA_SERVER_CERT = "ServerCert";

    private ViewParent rootView;

    private StreamReqBean streamReqBean;
    private ConnectivityManager connManager;

    private BackNavigationRegistration backNavigationRegistration;
    private StreamInputGatewayRegistry.Registration inputGatewayRegistration;
    private boolean showSoftKeyboardWhenFocused;
    private final int[] windowLocationScratch = new int[2];

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidAppLocale.apply(this);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Full-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // If we're going to use immersive mode, we want to have
        // the entire screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Listen for UI visibility events
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        // Change volume button behavior
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Inflate the content
        setContentView(R.layout.activity_game);

        connManager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Start the spinner
        spinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.conn_establishing_title),
                getResources().getString(R.string.conn_establishing_msg), true);

        settingsRepository = AndroidSettingsRepository.create(this);
        AndroidStreamSettingsBootstrap.prepare(
                settingsRepository);
        gameMenuCardLayoutRepository =
                new SettingsGameMenuCardLayoutRepository(
                        settingsRepository);
        gameMenuShortcutRepository =
                new SharedPreferencesGameMenuShortcutRepository(
                        getSharedPreferences(
                                SharedPreferencesGameMenuShortcutRepository
                                        .PREFERENCES_NAME,
                                Context.MODE_PRIVATE),
                        getSharedPreferences(
                                SharedPreferencesGameMenuShortcutRepository
                                        .LEGACY_IMPORTED_PREFERENCES_NAME,
                                Context.MODE_PRIVATE));
        streamVideoSettings =
                StreamVideoSettingsLoader.load(
                        settingsRepository,
                        AndroidDisplayAspectProvider.get(this));
        streamVideoSettingsState =
                new StreamVideoSettingsState(
                        streamVideoSettings);
        StreamAudioSettings streamAudioSettings =
                StreamAudioSettingsLoader.load(
                        settingsRepository);
        streamAudioSettingsState =
                new StreamAudioSettingsState(
                        streamAudioSettings);
        StreamUiSettings streamUiSettings =
                StreamUiSettingsLoader.load(
                        settingsRepository);
        streamUiSettingsState =
                new StreamUiSettingsState(streamUiSettings);
        streamDisplaySettings =
                StreamDisplaySettingsLoader.load(
                        settingsRepository,
                        streamVideoSettings);
        streamDecoderSettings =
                StreamDecoderSettingsLoader.load(
                        settingsRepository,
                        streamVideoSettings,
                        streamAudioSettings,
                        streamUiSettings);
        effectiveFramePacing =
                streamDecoderSettings.getFramePacing();
        customResolutionRepository =
                new SharedPreferencesCustomResolutionRepository(
                        getSharedPreferences(
                                SharedPreferencesCustomResolutionRepository
                                        .PREFERENCES_NAME,
                                Context.MODE_PRIVATE));
        transferSettings =
                TransferSettingsLoader.load(settingsRepository);
        inputSettingsState =
                new InputSettingsState(
                        InputSettingsLoader.load(
                                settingsRepository));
        controllerSettingsState =
                new ControllerSettingsState(
                        ControllerSettingsLoader.load(
                                settingsRepository));
        virtualControlSettingsState =
                new VirtualControlSettingsState(
                        VirtualControlSettingsLoader.load(
                                settingsRepository));
        virtualControlLayoutRepository =
                new AndroidVirtualControlLayoutRepository(this);
        decoderCrashTracker = new DecoderCrashTracker(
                new AndroidDecoderCrashStore(this));
        backNavigationRegistration =
                BackNavigationRegistration.register(this, this::handleStreamBackPressed);

        // Preserve compact-screen preferences while allowing adaptive windows
        // to follow the user's current orientation.
        setPreferredOrientationForCurrentDisplay();

        boolean useEntireDisplay =
                StreamWindowPolicy.shouldUseEntireDisplay(
                        streamDisplaySettings.isStretchVideo(),
                        streamDisplaySettings
                                .isDisplayCutoutEnabled(),
                        streamDisplaySettings.isNativeResolution(),
                        matchesPhysicalDisplayMode(
                                streamDisplaySettings
                                        .getStreamWidth(),
                                streamDisplaySettings
                                        .getStreamHeight()));
        UiHelper.configureStreamWindowInsets(this, useEntireDisplay);
        // Listen for non-touch events on the game surface
        streamView = findViewById(R.id.surfaceView);
        streamView.setOnGenericMotionListener(this);
        streamView.setOnKeyListener(this);
        streamView.setInputGateway(this);

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) streamView.getLayoutParams();
        params.gravity = resolvePhysicalStreamGravity(
                streamDisplaySettings.getGravity(),
                params.gravity);

        // Listen for touch events on the background touch view to enable trackpad mode
        // to work on areas outside of the StreamView itself. We use a separate View
        // for this rather than just handling it at the Activity level, because that
        // allows proper touch splitting, which the OSC relies upon.
        View backgroundTouchView = findViewById(R.id.backgroundTouchView);
        backgroundTouchView.setOnTouchListener(this);

        rootView=streamView.getParent();
        if (inputSettingsState.get().isAbsoluteMouseMode() &&
                rootView instanceof FrameLayout) {
            nativeCursorOverlayView = new NativeCursorOverlayView(this);
            FrameLayout.LayoutParams cursorLayoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            ((FrameLayout) rootView).addView(nativeCursorOverlayView, cursorLayoutParams);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Request unbuffered input event dispatching for all input classes we handle here.
            // Without this, input events are buffered to be delivered in lock-step with VBlank,
            // artificially increasing input latency while streaming.
            streamView.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                    InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                    InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                    InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                    InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
            backgroundTouchView.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                    InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                    InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                    InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                    InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
        }

        notificationOverlayView = findViewById(R.id.notificationOverlay);

        performanceOverlayController =
                new StreamPerformanceOverlayController(
                        this,
                        streamUiSettingsState,
                        this::createPerformanceOverlayConfiguration,
                        this::createPerformanceOverlayRuntimeState,
                        () -> showGameMenu(null));

        inputCaptureProvider = InputCaptureManager.getInputCaptureProvider(this, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            streamView.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
//                    LimeLog.info("onCapturedPointer="+motionEvent.toString());
//                    LimeLog.info("onCapturedPointer-Device="+motionEvent.getDevice().toString());
                    return handleMotionEvent(view, motionEvent);
                }
            });
        }

        // Warn the user if they're on a metered connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr.isActiveNetworkMetered()) {
            displayTransientMessage(getResources().getString(R.string.conn_metered));
        }

        wifiLockController =
                StreamWifiLockController.create(this);
        wifiLockController.acquire();

        appName = Game.this.getIntent().getStringExtra(EXTRA_APP_NAME);
        pcName = Game.this.getIntent().getStringExtra(EXTRA_PC_NAME);

        String host = Game.this.getIntent().getStringExtra(EXTRA_HOST);
        streamHost = host;
        int port = Game.this.getIntent().getIntExtra(EXTRA_PORT, NvHTTP.DEFAULT_HTTP_PORT);
        int httpsPort = Game.this.getIntent().getIntExtra(EXTRA_HTTPS_PORT, 0); // 0 is treated as unknown
        int appId = Game.this.getIntent().getIntExtra(EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);
        String uniqueId = Game.this.getIntent().getStringExtra(EXTRA_UNIQUEID);
        boolean appSupportsHdr = Game.this.getIntent().getBooleanExtra(EXTRA_APP_HDR, false);
        byte[] derCertData = Game.this.getIntent().getByteArrayExtra(EXTRA_SERVER_CERT);

        app = new NvApp(appName != null ? appName : "app", appId, appSupportsHdr);

        X509Certificate serverCert = null;
        try {
            if (derCertData != null) {
                serverCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(derCertData));
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        if (appId == StreamConfiguration.INVALID_APP_ID) {
            finish();
            return;
        }

        StreamHdrRequestPolicy.Decision hdrDecision =
                StreamHdrRequestPolicy.decide(
                        streamDisplaySettings.isHdrEnabled(),
                        streamVideoSettings.shouldIgnoreHdrCapability(),
                        AndroidStreamHdrCapabilityProvider.sample(this));
        showHdrRequestWarning(hdrDecision.getWarning());
        boolean hdrRequested = hdrDecision.isHdrRequested();

        AndroidStreamMediaRuntimeFactory.Result mediaRuntime =
                AndroidStreamMediaRuntimeFactory.create(
                        this,
                        streamDecoderSettings,
                        decoderCrashTracker,
                        hdrRequested,
                        performanceOverlayController,
                        () -> new AndroidAudioRenderer(
                                Game.this,
                                controllerHandler,
                                streamAudioSettingsState));
        mediaResourceOwner = mediaRuntime.getResourceOwner();
        StreamDecoderCapabilities decoderCapabilities =
                mediaRuntime.getDecoderCapabilities();

        ControllerSettings controllerSettings =
                controllerSettingsState.get();
        int discoveredGamepadMask =
                AndroidControllerInventory.from(this)
                        .getInitialControllerMask(
                                controllerSettings);

        // Set to the optimal mode for streaming
        float displayRefreshRate = prepareDisplayForRendering();
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        StreamSessionConfigurationPlanner.Plan configurationPlan =
                StreamSessionConfigurationPlanner.plan(
                        new StreamSessionConfigurationPlanner
                                .SettingsSnapshot(
                                streamDecoderSettings,
                                streamVideoSettings,
                                streamAudioSettingsState.get(),
                                controllerSettings,
                                inputSettingsState.get(),
                                transferSettings),
                        new StreamSessionConfigurationPlanner.Environment(
                                app,
                                decoderCapabilities,
                                discoveredGamepadMask,
                                displayRefreshRate,
                                RazerUtils.getPPI(this),
                                hdrRequested));
        effectiveFramePacing =
                configurationPlan.getEffectiveFramePacing();
        showStreamConfigurationWarnings(
                configurationPlan.getWarnings());
        StreamConfiguration config =
                StreamSessionConfigurationAdapter
                        .toTransportConfiguration(
                                configurationPlan
                                        .getConfigurationDocument());
        if (effectiveFramePacing !=
                streamDecoderSettings.getFramePacing()) {
            LimeLog.info(
                    "Using balanced frame pacing for incompatible display refresh rate");
        }
        else if (config.getRefreshRate() !=
                streamDecoderSettings.getFps()) {
            LimeLog.info(
                    "Adjusting FPS target for screen to " +
                            config.getRefreshRate());
        }

        streamReqBean=new StreamReqBean();
        streamReqBean.setAppName(appName);
        streamReqBean.setServerCert(serverCert);
        streamReqBean.setHttpsPort(httpsPort);
        streamReqBean.setUniqueId(uniqueId);
        streamReqBean.setActiveAddress(new ComputerDetails.AddressTuple(host, port));
        streamReqBean.setCryptoProvider(PlatformBinding.getCryptoProvider(this));
        // Initialize the connection
        conn = new NvConnection(getApplicationContext(),
                new ComputerDetails.AddressTuple(host, port),
                httpsPort, uniqueId, config,
                PlatformBinding.getCryptoProvider(this),
                serverCert,
                new AndroidMicrophoneUplinkSessionFactory(
                        MicrophoneUplinkConfig.protocolV1()));
        PointerInputSink pointerInputSink =
                new NvConnectionPointerInputSink(conn);
        KeyboardInputSink keyboardInputSink =
                new NvConnectionKeyboardInputSink(conn);
        DirectContactInputController directContactInputController =
                new DirectContactInputController(
                        streamView,
                        pointerInputSink,
                        inputSettingsState);
        ExternalPointerInputController externalPointerInputController =
                new ExternalPointerInputController(
                        streamView,
                        pointerInputSink,
                        inputCaptureProvider,
                        directContactInputController,
                        inputSettingsState);
        clipboardFileTransferController =
                new RemoteClipboardFileTransferController(
                        this,
                        conn,
                        settingsRepository);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        microphoneController = StreamMicrophoneController.create(
                conn,
                new StreamMicrophoneController.PermissionGateway() {
                    @Override
                    public boolean isGranted() {
                        return isRecordAudioPermissionGranted();
                    }

                    @Override
                    public void requestPermission() {
                        if (Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.M) {
                            requestPermissions(
                                    new String[] {
                                            Manifest.permission.RECORD_AUDIO
                                    },
                                    REQUEST_RECORD_AUDIO_PERMISSION);
                        }
                    }
                },
                new StreamMicrophoneController.Feedback() {
                    @Override
                    public void onUnsupported() {
                        showMicrophoneMessage(
                                getString(R.string
                                        .mic_uplink_not_supported),
                                UiToast.LENGTH_LONG);
                    }

                    @Override
                    public void onPermissionDenied() {
                        showMicrophoneMessage(
                                getString(R.string
                                        .mic_uplink_permission_denied),
                                UiToast.LENGTH_LONG);
                    }

                    @Override
                    public void onOperationFailed(String message) {
                        showMicrophoneMessage(
                                message,
                                UiToast.LENGTH_SHORT);
                    }

                    @Override
                    public void onStateChanged() {
                        if (dialogGameMenu != null) {
                            dialogGameMenu
                                    .refreshMicrophoneState();
                        }
                    }
                },
                mainHandler::post);
        StreamFailureDiagnostics failureDiagnostics =
                StreamFailureDiagnostics.create(
                        portFlags -> MoonBridge.testClientConnectivity(
                                ServerHelper.CONNECTION_TEST_SERVER,
                                443,
                                portFlags),
                        command -> mainHandler.post(command));
        ComputerDetails launchComputer = new ComputerDetails();
        launchComputer.name = pcName;
        launchComputer.uuid = getIntent().getStringExtra(EXTRA_PC_UUID);
        NvApp launchedApp = app;
        boolean reportGameLaunch = appName != null;
        ShortcutHelper launchShortcutHelper =
                new ShortcutHelper(getApplicationContext());
        launchReporter = StreamLaunchReporter.create(() -> {
            launchShortcutHelper.reportComputerShortcutUsed(
                    launchComputer);
            if (reportGameLaunch) {
                launchShortcutHelper.reportGameLaunched(
                        launchComputer,
                        launchedApp);
            }
        });
        sessionUiEffects = new StreamSessionUiEffects(
                new StreamSessionUiEffects.Host() {
                    @Override
                    public void setKeepScreenOn(
                            boolean keepScreenOn) {
                        if (keepScreenOn) {
                            getWindow().addFlags(
                                    WindowManager.LayoutParams
                                            .FLAG_KEEP_SCREEN_ON);
                        }
                        else {
                            getWindow().clearFlags(
                                    WindowManager.LayoutParams
                                            .FLAG_KEEP_SCREEN_ON);
                        }
                    }

                    @Override
                    public void notifyStreamConnecting() {
                        UiHelper.notifyStreamConnecting(
                                Game.this,
                                isGameModeIntegrationDisabled());
                    }

                    @Override
                    public void notifyStreamConnected() {
                        UiHelper.notifyStreamConnected(
                                Game.this,
                                isGameModeIntegrationDisabled());
                    }

                    @Override
                    public void notifyStreamEnded() {
                        UiHelper.notifyStreamEnded(
                                Game.this,
                                isGameModeIntegrationDisabled());
                    }

                    @Override
                    public void setInputGrabbed(boolean grabbed) {
                        setInputGrabState(grabbed);
                    }
                },
                mainHandler);
        sessionPresentationController =
                new StreamSessionPresentationController(
                        new StreamSessionPresentationController.Host() {
                            @Override
                            public boolean canPresentSessionUi() {
                                return Game.this.canPresentSessionUi();
                            }

                            @Override
                            public void updateConnectingMessage(
                                    String message) {
                                if (spinner != null) {
                                    spinner.setMessage(message);
                                }
                            }

                            @Override
                            public void dismissConnectingIndicator() {
                                Game.this.dismissConnectingIndicator();
                            }

                            @Override
                            public boolean isRenderSurfaceValid() {
                                return streamView.getHolder()
                                        .getSurface()
                                        .isValid();
                            }

                            @Override
                            public void showLongMessage(String message) {
                                UiToast.makeText(
                                        Game.this,
                                        message,
                                        UiToast.LENGTH_LONG).show();
                            }

                            @Override
                            public void showFailureDialog(
                                    String title,
                                    String message) {
                                Dialog.displayDialog(
                                        Game.this,
                                        title,
                                        message,
                                        true);
                            }

                            @Override
                            public void stopControllerInput() {
                                controllerHandler.stop();
                            }

                            @Override
                            public void stopConnection() {
                                Game.this.stopConnection();
                            }

                            @Override
                            public void finishGracefully() {
                                finish();
                            }

                            @Override
                            public boolean
                                    areConnectionWarningsDisabled() {
                                return streamUiSettingsState
                                        .get()
                                        .areConnectionWarningsDisabled();
                            }

                            @Override
                            public int getBitrateKbps() {
                                return streamDecoderSettings
                                        .getBitrateKbps();
                            }

                            @Override
                            public void setConnectionWarning(
                                    StreamSessionPresentationController
                                            .ConnectionWarning warning) {
                                Game.this.setConnectionWarning(warning);
                            }

                            @Override
                            public void onSessionConnected() {
                                Game.this.onSessionConnected();
                            }

                            @Override
                            public void onHdrModeChanged(
                                    boolean enabled,
                                    byte[] hdrMetadata) {
                                Game.this.handleHdrModeChanged(
                                        enabled,
                                        hdrMetadata);
                            }

                            @Override
                            public void onNativeCursor(
                                    boolean visible,
                                    boolean shapeChanged,
                                    int format,
                                    int x,
                                    int y,
                                    int width,
                                    int height,
                                    int hotspotX,
                                    int hotspotY,
                                    int shapeId,
                                    int scaleX,
                                    int scaleY,
                                    byte[] imageData) {
                                Game.this.handleNativeCursor(
                                        visible,
                                        shapeChanged,
                                        format,
                                        x,
                                        y,
                                        width,
                                        height,
                                        hotspotX,
                                        hotspotY,
                                        shapeId,
                                        scaleX,
                                        scaleY,
                                        imageData);
                            }
                        },
                        new StreamSessionPresentationController.Diagnostics() {
                            @Override
                            public boolean request(
                                    int portFlags,
                                    Callback callback) {
                                return failureDiagnostics.request(
                                        portFlags,
                                        result -> callback.onResult(
                                                result.getPortFlags(),
                                                result.getProbeResultOr(
                                                        MoonBridge
                                                                .ML_TEST_RESULT_INCONCLUSIVE)));
                            }

                            @Override
                            public void destroy() {
                                failureDiagnostics.destroy();
                            }
                        },
                        AndroidStreamConnectionMessages.create(this),
                        MoonBridge::getPortFlagsFromTerminationErrorCode,
                        MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
        sessionCallbackRouter = new StreamSessionCallbackRouter(
                sessionPresentationController,
                new StreamSessionCallbackRouter.FeedbackHost() {
                    @Override
                    public void onRumble(
                            short controllerNumber,
                            short lowFreqMotor,
                            short highFreqMotor) {
                        handleRumble(
                                controllerNumber,
                                lowFreqMotor,
                                highFreqMotor);
                    }

                    @Override
                    public void onRumbleTriggers(
                            short controllerNumber,
                            short leftTrigger,
                            short rightTrigger) {
                        handleRumbleTriggers(
                                controllerNumber,
                                leftTrigger,
                                rightTrigger);
                    }

                    @Override
                    public void onMotionEventState(
                            short controllerNumber,
                            byte motionType,
                            short reportRateHz) {
                        handleMotionEventState(
                                controllerNumber,
                                motionType,
                                reportRateHz);
                    }

                    @Override
                    public void onControllerLed(
                            short controllerNumber,
                            byte red,
                            byte green,
                            byte blue) {
                        handleControllerLed(
                                controllerNumber,
                                red,
                                green,
                                blue);
                    }
                },
                mainHandler);
        sessionController = new StreamSessionController(
                conn,
                sessionCallbackRouter);
        TouchInputController touchInputController =
                new TouchInputController(
                        this,
                        streamView,
                        pointerInputSink,
                        directContactInputController,
                        inputSettingsState,
                        new TouchInputController.Host() {
                            @Override
                            public void showSoftKeyboard() {
                                Game.this.showKeyboard();
                            }
                        });
        if (inputSettingsState.get().isAbsoluteMouseMode()) {
            conn.setMousePositionListener(new NvConnection.MousePositionListener() {
                @Override
                public void onMousePosition(short x, short y, short referenceWidth, short referenceHeight) {
                    setNativeCursorOverlayFromReference(x, y, referenceWidth, referenceHeight);
                }
            });
        }
        controllerHandler = new ControllerHandler(
                this,
                conn,
                this,
                controllerSettingsState,
                streamAudioSettingsState);
        keyboardInputController = new KeyboardInputController(
                new KeyboardTranslator(),
                controllerHandler,
                keyboardInputSink,
                pointerInputSink,
                inputSettingsState,
                new KeyboardInputController.Host() {
                    @Override
                    public boolean isInputGrabbed() {
                        return grabbedInput;
                    }

                    @Override
                    public void onNonBackKeyDown() {
                        cancelPendingStreamBackExit();
                    }

                    @Override
                    public void requestToggleInputGrab() {
                        Handler handler =
                                getWindow().getDecorView().getHandler();
                        if (handler != null) {
                            handler.postDelayed(toggleGrab, 250);
                        }
                    }

                    @Override
                    public void requestQuit() {
                        finish();
                    }

                    @Override
                    public void requestToggleCursorVisibility() {
                        switchMouseLocalCursor();
                    }
                });
        streamInputController = new StreamInputController(
                controllerHandler,
                externalPointerInputController,
                touchInputController,
                inputSettingsState,
                new StreamInputController.Host() {
                    @Override
                    public boolean shouldSuppressTouchscreenInput() {
                        return isTouchscreenInputSuppressed();
                    }
                });
        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(
                keyboardInputController,
                null);
        inputLifecycleController =
                new StreamInputLifecycleController(
                        streamInputController,
                        controllerHandler,
                        new StreamInputLifecycleController
                                .KeyboardRegistration() {
                            @Override
                            public void unregister() {
                                inputManager
                                        .unregisterInputDeviceListener(
                                                keyboardInputController);
                            }
                        });

        virtualControlsController =
                new StreamVirtualControlsController(
                        new AndroidVirtualControlsFactory(
                                controllerHandler,
                                (FrameLayout) rootView,
                                this,
                                inputSettingsState,
                                virtualControlSettingsState,
                                virtualControlLayoutRepository,
                                this,
                                this));

        //鼠标触控模式
        switchMouseModel(
                inputSettingsState.get()
                        .getTouchModePreferenceValue());

        if (controllerSettingsState
                .get()
                .isOnscreenControllerEnabled()) {
            // create virtual onscreen controller
            virtualControlsController.showVirtualGamepad();
        }

        //特殊按键屏幕布局
        if (virtualControlSettingsState
                .get()
                .shouldShowVirtualKeysOnStart()) {
            virtualControlsController.showVirtualKeys();
        }

        if (controllerSettingsState
                .get()
                .isUsbDriverEnabled()) {
            usbDriverSessionController =
                    new UsbDriverSessionController(
                            new UsbDriverSessionController
                                    .ServiceBinding() {
                                @Override
                                public boolean bind() {
                                    return bindService(
                                            new Intent(
                                                    Game.this,
                                                    UsbDriverService.class),
                                            usbDriverServiceConnection,
                                            Service.BIND_AUTO_CREATE);
                                }

                                @Override
                                public void unbind() {
                                    unbindService(
                                            usbDriverServiceConnection);
                                }
                            });
            usbDriverSessionController.bind();
        }

        floatingControlController =
                createFloatingControlController();
        floatingControlController.applyEnabled(
                streamUiSettingsState.get()
                        .isFloatingControlEnabled());

        if (!mediaResourceOwner.isAvcSupported()) {
            if (spinner != null) {
                spinner.dismiss();
                spinner = null;
            }

            // If we can't find an AVC decoder, we can't proceed
            Dialog.displayDialog(this, getResources().getString(R.string.conn_error_title),
                    "This device or ROM doesn't support hardware accelerated H.264 playback.", true);
            return;
        }

        // The connection starts only after both the session dependencies and
        // the decoder render Surface are ready.
        renderSurfaceController =
                new StreamRenderSurfaceController(
                        streamView.getHolder(),
                        streamDecoderSettings.getWidth(),
                        streamDecoderSettings.getHeight(),
                        streamDecoderSettings.getFps(),
                        selectedDisplayRefreshRate,
                        mayReduceRefreshRate(),
                        shouldLetSystemManageRefreshRate(),
                        new StreamRenderSurfaceController.Host() {
                            @Override
                            public boolean canStartSession() {
                                return sessionDependenciesReady &&
                                        sessionController != null &&
                                        sessionController.canStart();
                            }

                            @Override
                            public void startSession(
                                    Surface renderTarget) {
                                startSessionWithRenderTarget(
                                        renderTarget);
                            }

                            @Override
                            public boolean hasSessionStarted() {
                                return Game.this
                                        .hasSessionStarted();
                            }

                            @Override
                            public boolean sessionNeedsStop() {
                                return sessionController != null &&
                                        sessionController
                                                .getState()
                                                .needsStop();
                            }

                            @Override
                            public void prepareVideoForStop() {
                                mediaResourceOwner
                                        .prepareVideoForStop();
                            }

                            @Override
                            public void stopSession() {
                                stopConnection();
                            }
                        });
        renderSurfaceController.bind();

        //外接显示器模式
        if (streamDisplaySettings.isExternalDisplayEnabled()) {
            showSecondScreen();
        }

        //强制体感
        setMotionForceGyro();

        //光标是否显示
        if (!cursorVisible &&
                inputSettingsState
                        .get()
                        .isLocalSystemCursorEnabled()) {
            switchMouseLocalCursor();
        }
        sessionDependenciesReady = true;
        renderSurfaceController.startIfReady();
    }

    //显示隐藏虚拟特殊按键
    public void toggleVirtualKeys(){
        if (virtualControlsController != null) {
            virtualControlsController.toggleVirtualKeys();
        }
    }

    public void toggleFullKeyboard(){
        if (virtualControlsController != null) {
            virtualControlsController.toggleFullKeyboard();
        }
    }

    //显示隐藏虚拟手柄控制器
    public void toggleVirtualGamepad(){
        if (virtualControlsController != null) {
            virtualControlsController.toggleVirtualGamepad();
        }
    }

    @Override
    public void performStreamUiAction(StreamUiActions.Action action) {
        switch (action) {
            case TOGGLE_SOFT_KEYBOARD:
                if (!hasWindowFocus()) {
                    streamView.postDelayed(
                            toggleKeyboardWhenFocused,
                            10);
                }
                else {
                    toggleKeyboard();
                }
                break;
            case TOGGLE_VIRTUAL_KEYS:
                toggleVirtualKeys();
                break;
            case TOGGLE_FULL_KEYBOARD:
                toggleFullKeyboard();
                break;
            case TOGGLE_VIRTUAL_GAMEPAD:
                toggleVirtualGamepad();
                break;
            case TOGGLE_FLOATING_BUTTON:
                if (floatingControlController != null) {
                    floatingControlController.toggleVisibility();
                }
                break;
            case TOGGLE_PERFORMANCE_OVERLAY:
                showHUD();
                break;
            case OPEN_STREAM_MENU:
                showGameMenu(null);
                break;
        }
    }

    private void setPreferredOrientationForCurrentDisplay() {
        StreamVideoSettings videoSettings =
                streamVideoSettingsState.get();
        VirtualControlSettings virtualControlSettings =
                virtualControlSettingsState.get();
        StreamOrientationController.applyGameOrientation(
                this,
                new StreamOrientationRequest(
                        isVirtualControllerVisibleForOrientation(),
                        streamDisplaySettings.isNativeResolution(),
                        streamDisplaySettings.getStreamWidth(),
                        streamDisplaySettings.getStreamHeight(),
                        videoSettings.isPortrait() || isPortrait,
                        virtualControlSettings
                                .isAutomaticScreenOrientationEnabled()));
    }

    private boolean isVirtualControllerVisibleForOrientation() {
        if (virtualControlsController != null &&
                virtualControlsController.isVirtualGamepadCreated()) {
            return virtualControlsController.isVirtualGamepadVisible();
        }
        return controllerSettingsState
                .get()
                .isOnscreenControllerEnabled();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);


        // Set requested orientation for possible new screen size
        setPreferredOrientationForCurrentDisplay();

        if (virtualControlsController != null) {
            virtualControlsController.refreshCreatedLayouts();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                isHidingOverlays = true;

                if (virtualControlsController != null) {
                    virtualControlsController.hideAll();
                }

                performanceOverlayController
                        .hideForPictureInPicture();
                notificationOverlayView.setVisibility(View.GONE);

                // Disable sensors while in PiP mode
                controllerHandler.disableSensors();

                // Update GameManager state to indicate we're in PiP (still gaming, but interruptible)
                UiHelper.notifyStreamEnteringPiP(
                        this,
                        isGameModeIntegrationDisabled());
            }
            else {
                isHidingOverlays = false;

                performanceOverlayController
                        .restoreAfterPictureInPicture();

                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);

                // Enable sensors again after exiting PiP
                controllerHandler.enableSensors();

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(
                        this,
                        isGameModeIntegrationDisabled());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private PictureInPictureParams getPictureInPictureParams(boolean autoEnter) {
        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(
                                streamDecoderSettings.getWidth(),
                                streamDecoderSettings.getHeight()));
        Rect sourceBounds = new Rect();
        if (ViewWindowGeometry.getVisibleBoundsInWindow(
                streamView,
                getWindow().getDecorView(),
                sourceBounds,
                windowLocationScratch)) {
            builder.setSourceRectHint(sourceBounds);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter);
            builder.setSeamlessResizeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName);
                if (pcName != null) {
                    builder.setSubtitle(pcName);
                }
            }
            else if (pcName != null) {
                builder.setTitle(pcName);
            }
        }

        return builder.build();
    }

    private void updatePipAutoEnter() {
        if (!streamUiSettingsState
                .get()
                .isPictureInPictureEnabled()) {
            return;
        }

        boolean autoEnter = isSessionConnected() &&
                suppressPipRefCount == 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(getPictureInPictureParams(autoEnter));
        }
        else {
            autoEnterPip = autoEnter;
        }
    }

    public void setMetaKeyCaptureState(boolean enabled) {
        // This uses custom APIs present on some Samsung devices to allow capture of
        // meta key events while streaming.
        try {
            Class<?> semWindowManager = Class.forName("com.samsung.android.view.SemWindowManager");
            Method getInstanceMethod = semWindowManager.getMethod("getInstance");
            Object manager = getInstanceMethod.invoke(null);

            if (manager != null) {
                Class<?>[] parameterTypes = new Class<?>[2];
                parameterTypes[0] = ComponentName.class;
                parameterTypes[1] = boolean.class;
                Method requestMetaKeyEventMethod = semWindowManager.getDeclaredMethod("requestMetaKeyEvent", parameterTypes);
                requestMetaKeyEventMethod.invoke(manager, this.getComponentName(), enabled);
            }
            else {
                LimeLog.warning("SemWindowManager.getInstance() returned null");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // PiP is only supported on Oreo and later, and we don't need to manually enter PiP on
        // Android S and later. On Android R, we will use onPictureInPictureRequested() instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (autoEnterPip) {
                try {
                    // This has thrown all sorts of weird exceptions on Samsung devices
                    // running Oreo. Just eat them and close gracefully on leave, rather
                    // than crashing.
                    enterPictureInPictureMode(getPictureInPictureParams(false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        // Enter PiP when requested unless we're on Android 12 which supports auto-enter.
        if (autoEnterPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureMode(getPictureInPictureParams(false));
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // We can't guarantee the state of modifiers keys which may have
        // lifted while focus was not on us. Clear the modifier state.
        if (keyboardInputController != null) {
            keyboardInputController.resetModifierState();
        }

        // With Android native pointer capture, capture is lost when focus is lost,
        // so it must be requested again when focus is regained.
        inputCaptureProvider.onWindowFocusChanged(hasFocus);
        if (conn != null) {
            conn.onWindowFocusChanged(hasFocus);
        }
        if (hasFocus && showSoftKeyboardWhenFocused) {
            showSoftKeyboardWhenFocused = false;
            showKeyboard();
        }
    }

    private boolean matchesPhysicalDisplayMode(int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display display = getWindowManager().getDefaultDisplay();
            for (Display.Mode candidate : display.getSupportedModes()) {
                if (StreamWindowPolicy.matchesPhysicalResolution(
                        width,
                        height,
                        candidate.getPhysicalWidth(),
                        candidate.getPhysicalHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean mayReduceRefreshRate() {
        return effectiveFramePacing ==
                StreamDecoderSettings.FramePacing.CAP_FPS ||
                effectiveFramePacing ==
                        StreamDecoderSettings.FramePacing
                                .MAXIMUM_SMOOTHNESS ||
                (effectiveFramePacing ==
                        StreamDecoderSettings.FramePacing.BALANCED &&
                        streamDecoderSettings
                                .isRefreshRateReductionEnabled());
    }

    private boolean shouldLetSystemManageRefreshRate() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return false;
        }

        String deviceBrand = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.ROOT);
        return deviceBrand.contains("xiaomi") ||
                deviceBrand.contains("redmi") ||
                deviceBrand.contains("poco");
    }

    private float prepareDisplayForRendering() {
        Display display = getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams windowLayoutParams = getWindow().getAttributes();
        boolean systemManagedRefreshRate = shouldLetSystemManageRefreshRate();
        float displayRefreshRate;

        // On M, we can explicitly set the optimal display mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode currentMode = display.getMode();

            LimeLog.info("Current display mode: " +
                    currentMode.getPhysicalWidth() + "x" +
                    currentMode.getPhysicalHeight() + "x" +
                    currentMode.getRefreshRate());

            ArrayList<Display.Mode> platformModes =
                    new ArrayList<>();
            ArrayList<StreamDisplayModeSelector.Mode>
                    selectorModes = new ArrayList<>();
            for (Display.Mode candidate : display.getSupportedModes()) {
                LimeLog.info("Examining display mode: " +
                        candidate.getPhysicalWidth() + "x" +
                        candidate.getPhysicalHeight() + "x" +
                        candidate.getRefreshRate());
                platformModes.add(candidate);
                selectorModes.add(toSelectorMode(candidate));
            }
            StreamDisplayModeSelector.Mode selectedMode =
                    StreamDisplayModeSelector.select(
                            streamDecoderSettings.getWidth(),
                            streamDecoderSettings.getHeight(),
                            streamDecoderSettings.getFps(),
                            streamDisplaySettings
                                    .isNativeResolution(),
                            mayReduceRefreshRate(),
                            toSelectorMode(currentMode),
                            selectorModes);
            Display.Mode bestMode = currentMode;
            for (int index = 0;
                    index < platformModes.size();
                    index++) {
                if (platformModes.get(index).getModeId() ==
                        selectedMode.id) {
                    bestMode = platformModes.get(index);
                    break;
                }
            }

            LimeLog.info("Best display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            // Only apply new window layout parameters if we've actually changed the display mode
            if (display.getMode().getModeId() != bestMode.getModeId() && !systemManagedRefreshRate) {
                // If we only changed refresh rate and we're on an OS that supports Surface.setFrameRate()
                // use that instead of using preferredDisplayModeId to avoid the possibility of triggering
                // bugs that can cause the system to switch from 4K60 to 4K24 on Chromecast 4K.
                if (streamVideoSettings.shouldEnforceDisplayMode() ||
                        Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.S ||
                        display.getMode().getPhysicalWidth() != bestMode.getPhysicalWidth() ||
                        display.getMode().getPhysicalHeight() != bestMode.getPhysicalHeight()) {
                    // Apply the display mode change
                    windowLayoutParams.preferredDisplayModeId = bestMode.getModeId();
                    getWindow().setAttributes(windowLayoutParams);
                }
                else {
                    LimeLog.info("Using setFrameRate() instead of preferredDisplayModeId due to matching resolution");
                }
            }
            else if (systemManagedRefreshRate) {
                LimeLog.info("Leaving refresh rate selection to the system on this device");
            }
            else {
                LimeLog.info("Current display mode is already the best display mode");
            }

            displayRefreshRate = bestMode.getRefreshRate();
        }
        // On L, we can at least tell the OS that we want a refresh rate
        else {
            float bestRefreshRate = display.getRefreshRate();
            for (float candidate : display.getSupportedRefreshRates()) {
                LimeLog.info("Examining refresh rate: "+candidate);

                if (candidate > bestRefreshRate) {
                    // Ensure the frame rate stays around 60 Hz for <= 60 FPS streams
                    if (streamDecoderSettings.getFps() <= 60) {
                        if (candidate >= 63) {
                            continue;
                        }
                    }

                    bestRefreshRate = candidate;
                }
            }

            LimeLog.info("Selected refresh rate: "+bestRefreshRate);
            if (!systemManagedRefreshRate) {
                windowLayoutParams.preferredRefreshRate = bestRefreshRate;
            }
            displayRefreshRate = bestRefreshRate;

            // Apply the refresh rate change
            if (!systemManagedRefreshRate) {
                getWindow().setAttributes(windowLayoutParams);
            }
        }

        // Until Marshmallow, we can't ask for a 4K display mode, so we'll
        // need to hint the OS to provide one.
        boolean aspectRatioMatch = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // We'll calculate whether we need to scale by aspect ratio. If not, we'll use
            // setFixedSize so we can handle 4K properly. The only known devices that have
            // >= 4K screens have exactly 4K screens, so we'll be able to hit this good path
            // on these devices. On Marshmallow, we can start changing to 4K manually but no
            // 4K devices run 6.0 at the moment.
            Point screenSize = new Point(0, 0);
            display.getSize(screenSize);

            if (StreamLayoutGeometry.hasCompatibleAspectRatio(
                    screenSize.x,
                    screenSize.y,
                    streamDecoderSettings.getWidth(),
                    streamDecoderSettings.getHeight(),
                    0.001)) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        double desiredAspectRatio =
                StreamLayoutGeometry.getAspectRatio(
                        streamDecoderSettings.getWidth(),
                        streamDecoderSettings.getHeight());
        if (streamDisplaySettings.isStretchVideo() ||
                aspectRatioMatch) {
            // Set the surface to the size of the video
            streamView.getHolder().setFixedSize(
                    streamDecoderSettings.getWidth(),
                    streamDecoderSettings.getHeight());
            streamView.setDesiredAspectRatio(0.0);
        }
        else {
            // Set the surface to scale based on the aspect ratio of the stream
            streamView.setDesiredAspectRatio(desiredAspectRatio);
            LimeLog.info("surfaceChanged-->" + desiredAspectRatio);
        }

        // Set the desired refresh rate that will get passed into setFrameRate() later
        selectedDisplayRefreshRate = displayRefreshRate;

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // TVs may take a few moments to switch refresh rates, and we can probably assume
            // it will be eventually activated.
            // TODO: Improve this
            return displayRefreshRate;
        }
        else {
            // Use the lower of the current refresh rate and the selected refresh rate.
            // The preferred refresh rate may not actually be applied (ex: Battery Saver mode).
            return Math.min(getWindowManager().getDefaultDisplay().getRefreshRate(), displayRefreshRate);
        }
    }

    @SuppressLint("InlinedApi")
    private final Runnable hideSystemUi = new Runnable() {
            @Override
            public void run() {
                // TODO: Do we want to use WindowInsetsController here on R+ instead of
                // SYSTEM_UI_FLAG_IMMERSIVE_STICKY? They seem to do the same thing as of S...

                // In multi-window mode on N+, we need to drop our layout flags or we'll
                // be drawing underneath the system UI.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                    Game.this.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
                else {
                    // Use immersive mode
                    Game.this.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
    };

    private void hideSystemUi(int delay) {
        Handler h = getWindow().getDecorView().getHandler();
        if (h != null) {
            h.removeCallbacks(hideSystemUi);
            h.postDelayed(hideSystemUi, delay);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static StreamDisplayModeSelector.Mode toSelectorMode(
            Display.Mode mode) {
        return new StreamDisplayModeSelector.Mode(
                mode.getModeId(),
                mode.getPhysicalWidth(),
                mode.getPhysicalHeight(),
                mode.getRefreshRate());
    }

    private void cancelPendingUiCallbacks() {
        View decorView = getWindow().getDecorView();
        Handler handler = decorView.getHandler();
        if (handler != null) {
            handler.removeCallbacks(hideSystemUi);
            handler.removeCallbacks(toggleGrab);
        }
        if (streamView != null) {
            streamView.removeCallbacks(toggleKeyboardWhenFocused);
            streamView.removeCallbacks(showSoftKeyboardRetry);
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        setPreferredOrientationForCurrentDisplay();

        // In multi-window, we don't want to use the full-screen layout
        // flag. It will cause us to collide with the system UI.
        // This function will also be called for PiP so we can cover
        // that case here too.
        if (isInMultiWindowMode) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mediaResourceOwner.notifyVideoBackground();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mediaResourceOwner.notifyVideoForeground();
        }

        // Correct the system UI visibility flags
        hideSystemUi(50);
        UiHelper.refreshStreamWindowInsets(this);
    }

    @Override
    protected void onDestroy() {
        sessionDependenciesReady = false;
        unregisterInputGateway();
        cancelPendingUiCallbacks();
        if (renderSurfaceController != null) {
            renderSurfaceController.destroy();
            renderSurfaceController = null;
        }
        if (launchReporter != null) {
            launchReporter.destroy();
            launchReporter = null;
        }
        if (microphoneController != null) {
            microphoneController.destroy();
            microphoneController = null;
        }
        if (inputLifecycleController != null) {
            inputLifecycleController.detachRouting();
            streamInputController = null;
        }
        if (sessionController != null) {
            sessionController.destroy();
            sessionController = null;
        }
        if (sessionCallbackRouter != null) {
            sessionCallbackRouter.destroy();
            sessionCallbackRouter = null;
        }
        if (sessionPresentationController != null) {
            sessionPresentationController.destroy();
            sessionPresentationController = null;
        }
        if (sessionUiEffects != null) {
            sessionUiEffects.destroy();
            sessionUiEffects = null;
        }
        if (mediaResourceOwner != null) {
            mediaResourceOwner.destroy();
            mediaResourceOwner = null;
        }
        if (backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }
        if (clipboardFileTransferController != null) {
            clipboardFileTransferController.destroy();
            clipboardFileTransferController = null;
        }
        if (performanceOverlayController != null) {
            performanceOverlayController.destroy();
            performanceOverlayController = null;
        }
        if (floatingControlController != null) {
            floatingControlController.destroy();
            floatingControlController = null;
        }
        if (virtualControlsController != null) {
            virtualControlsController.destroy();
            virtualControlsController = null;
        }
        UiHelper.notifyHdrWindowStatus(
                this,
                false,
                isHdrHighBrightnessEnabled());

        if(presentation!=null){
            presentation.dismiss();
        }

        if (usbDriverSessionController != null) {
            usbDriverSessionController.destroy();
            usbDriverSessionController = null;
        }

        if (inputLifecycleController != null) {
            inputLifecycleController.destroy();
            inputLifecycleController = null;
            controllerHandler = null;
            keyboardInputController = null;
        }

        if (wifiLockController != null) {
            wifiLockController.destroy();
            wifiLockController = null;
        }

        // Destroy the capture provider
        inputCaptureProvider.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (inputLifecycleController != null) {
            inputLifecycleController.resume();
        }

    }

    @Override
    protected void onPause() {
        if (inputLifecycleController != null) {
            inputLifecycleController.pause(isFinishing());
        }

        if (isFinishing()) {
            // Ungrab input to prevent further input device notifications
            setInputGrabState(false);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        showSoftKeyboardWhenFocused = false;
        unregisterInputGateway();
        super.onStop();

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (virtualControlsController != null) {
            virtualControlsController.hideAll();
        }

        if(dialogGameMenu!=null&&dialogGameMenu.isVisible()){
            dialogGameMenu.dismiss();
        }

        if (clipboardFileTransferController != null &&
                clipboardFileTransferController.isSelectingDirectory()) {
            return;
        }

        if (conn != null) {
            int videoFormat =
                    mediaResourceOwner.getActiveVideoFormat();

            if (sessionPresentationController != null) {
                sessionPresentationController
                        .suppressFailurePresentation();
            }
            stopConnection();
            if(isQuitSteamingFlag){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        quitSteaming();
                    }
                },200); // 延时100毫秒
            }
            if (streamUiSettingsState
                    .get()
                    .isLatencyToastEnabled()) {
                int averageEndToEndLat =
                        mediaResourceOwner
                                .getAverageEndToEndLatency();
                int averageDecoderLat =
                        mediaResourceOwner.getAverageDecoderLatency();
                String message = null;
                if (averageEndToEndLat > 0) {
                    message = getResources().getString(R.string.conn_client_latency)+" "+averageEndToEndLat+" ms";
                    if (averageDecoderLat > 0) {
                        message += " ("+getResources().getString(R.string.conn_client_latency_hw)+" "+averageDecoderLat+" ms)";
                    }
                }
                else if (averageDecoderLat > 0) {
                    message = getResources().getString(R.string.conn_hardware_latency)+" "+averageDecoderLat+" ms";
                }

                // Add the video codec to the post-stream toast
                if (message != null) {
                    message += " [";

                    if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H264) != 0) {
                        message += "H.264";
                    }
                    else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H265) != 0) {
                        message += "HEVC";
                    }
                    else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_AV1) != 0) {
                        message += "AV1";
                    }
                    else {
                        message += "UNKNOWN";
                    }

                    if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_10BIT) != 0) {
                        message += " HDR";
                    }

                    message += "]";
                }

                if (message != null) {
                    UiToast.makeText(this, message, UiToast.LENGTH_LONG).show();
                }
            }

            decoderCrashTracker.completeCleanly();
        }
        if (streamVideoSettingsState
                .get()
                .getScreenOnPolicy() !=
                StreamVideoSettings.ScreenOnPolicy.DISABLED &&
                !isFinishing()) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isInteractive()) {
                AutoReconnectHelper.savePendingStream(getIntent());
            }
            else {
                isAutoLink = true;
            }
            return;
        }
        finish();
    }

    @Override
    public void finish() {
        AutoReconnectHelper.clearPendingStream();
        super.finish();
        if (streamVideoSettingsState != null &&
                streamVideoSettingsState
                        .get()
                        .getScreenOnPolicy() ==
                        StreamVideoSettings
                                .ScreenOnPolicy
                                .CURRENT_SESSION) {
            applyStreamVideoSettingsUpdate(
                    StreamVideoSettingsUpdate.screenOnPolicy(
                            StreamVideoSettings
                                    .ScreenOnPolicy
                                    .DISABLED));
        }
    }

    private boolean isAutoLink=false;

    @Override
    protected void onStart() {
        super.onStart();
        unregisterInputGateway();
        inputGatewayRegistration =
                StreamInputGatewayRegistry.getInstance().register(this);
        if (isAutoLink) {
            isAutoLink = false;
            recreate();
        }
    }

    private void setInputGrabState(boolean grab) {
        // Grab/ungrab the mouse cursor
        if (grab) {
            inputCaptureProvider.enableCapture();

            // Enabling capture may hide the cursor again, so
            // we will need to show it again.
            if (cursorVisible) {
                inputCaptureProvider.showCursor();
            }
        }
        else {
            inputCaptureProvider.disableCapture();
        }

        // Grab/ungrab system keyboard shortcuts
        setMetaKeyCaptureState(grab);

        grabbedInput = grab;
    }

    private final Runnable toggleGrab = new Runnable() {
        @Override
        public void run() {
            setInputGrabState(!grabbedInput);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyboardInputController.handleKeyDown(event) ||
                super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyboardInputController.handleKeyUp(event) ||
                super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return keyboardInputController.handleKeyMultiple(event) ||
                super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public void sendImeText(String text) {
        if (!isInputReady() || !grabbedInput ||
                text == null || text.isEmpty()) {
            return;
        }

        keyboardInputController.sendText(text);
    }

    @Override
    public void sendImeBackspace(int count) {
        sendImeKey((short) KeyboardTranslator.VK_BACK_SPACE, count);
    }

    @Override
    public void sendImeForwardDelete(int count) {
        sendImeKey((short) 0x2e, count);
    }

    private void sendImeKey(short keyCode, int count) {
        if (!isInputReady() || !grabbedInput || count <= 0) {
            return;
        }

        keyboardInputController.sendRepeatedKey(keyCode, count);
    }

    @Override
    public void toggleKeyboard() {
        LimeLog.info("Toggling keyboard overlay");
        if (streamView.isImeActive()) {
            hideKeyboard();
        }
        else {
            showKeyboard();
        }
    }

    public void showKeyboard() {
        cancelPendingStreamBackExit();
        LimeLog.info("Showing keyboard overlay");
        final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        streamView.requestFocus();
        streamView.setImeActive(true);
        inputManager.restartInput(streamView);
        if (!inputManager.showSoftInput(
                streamView,
                InputMethodManager.SHOW_IMPLICIT)) {
            streamView.removeCallbacks(showSoftKeyboardRetry);
            streamView.postDelayed(
                    showSoftKeyboardRetry,
                    SOFT_KEYBOARD_SHOW_RETRY_MS);
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        streamView.setImeActive(false);
        inputManager.hideSoftInputFromWindow(streamView.getWindowToken(), 0);
        inputManager.restartInput(streamView);
    }

    private void unregisterInputGateway() {
        if (inputGatewayRegistration != null) {
            inputGatewayRegistration.unregister();
            inputGatewayRegistration = null;
        }
    }

    /**
     * Resolves the user's explicitly selected physical screen edge. LEFT and RIGHT are
     * intentional here: changing the UI language must not move the decoded video to the
     * opposite side of the display.
     */
    @SuppressLint("RtlHardcoded")
    private static int resolvePhysicalStreamGravity(
            StreamDisplaySettings.Gravity gravityModel,
            int defaultGravity) {
        switch (gravityModel) {
            case TOP_CENTER:
                return Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            case TOP_LEFT:
                return Gravity.LEFT | Gravity.TOP;
            case TOP_RIGHT:
                return Gravity.RIGHT | Gravity.TOP;
            case BOTTOM_CENTER:
                return Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            case BOTTOM_LEFT:
                return Gravity.LEFT | Gravity.BOTTOM;
            case BOTTOM_RIGHT:
                return Gravity.RIGHT | Gravity.BOTTOM;
            case DEFAULT:
            default:
                return defaultGravity;
        }
    }

    // Returns true if the event was consumed
    // NB: View is only present if called from a view callback
    private boolean handleMotionEvent(View view, MotionEvent event) {
        // Pass through mouse/touch/joystick input if we're not grabbing
        if (!grabbedInput || streamInputController == null) {
            return false;
        }

        if (event.getActionMasked() != MotionEvent.ACTION_HOVER_MOVE) {
            cancelPendingStreamBackExit();
        }
        return streamInputController.handleMotionEvent(view, event);
    }

    private boolean isTouchscreenInputSuppressed() {
        return virtualControlsController != null &&
                virtualControlsController.isEditingLayout();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return handleMotionEvent(null, event) || super.onGenericMotionEvent(event);

    }

    private void runNativeCursorOverlayUpdate(Runnable runnable) {
        if (nativeCursorOverlayView == null) {
            return;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        }
        else {
            runOnUiThread(runnable);
        }
    }

    private void setNativeCursorOverlayFromReference(short x, short y, short referenceWidth, short referenceHeight) {
        if (referenceWidth <= 1 || referenceHeight <= 1) {
            return;
        }

        runNativeCursorOverlayUpdate(new Runnable() {
            @Override
            public void run() {
                nativeCursorOverlayView.setCursorPositionFromReference(
                        streamView, x, y, referenceWidth, referenceHeight);
            }
        });
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event) {
        return handleMotionEvent(view, event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            cancelPendingStreamBackExit();

            // Tell the OS not to buffer input events for us
            //
            // NB: This is still needed even when we call the newer requestUnbufferedDispatch()!
            view.requestUnbufferedDispatch(event);
        }

        return handleMotionEvent(view, event);
    }

    private void stopConnection() {
        if (streamInputController != null) {
            streamInputController.cancelActiveInput();
        }
        if (sessionController != null && sessionController.stop()) {
            UiHelper.notifyHdrWindowStatus(
                    this,
                    false,
                    isHdrHighBrightnessEnabled());
            updatePipAutoEnter();
            mediaResourceOwner.releaseStartResources();

            controllerHandler.stop();
            sessionUiEffects.onEnded();
        }
    }

    private boolean canPresentSessionUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void dismissConnectingIndicator() {
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }
    }

    private void setConnectionWarning(
            StreamSessionPresentationController.ConnectionWarning warning) {
        if (warning ==
                StreamSessionPresentationController.ConnectionWarning.NONE) {
            requestedNotificationOverlayVisibility = View.GONE;
        }
        else {
            notificationOverlayView.setText(getString(
                    warning == StreamSessionPresentationController
                            .ConnectionWarning.SLOW
                            ? R.string.slow_connection_msg
                            : R.string.poor_connection_msg));
            requestedNotificationOverlayVisibility = View.VISIBLE;
        }

        if (!isHidingOverlays) {
            notificationOverlayView.setVisibility(
                    requestedNotificationOverlayVisibility);
        }
    }

    private void onSessionConnected() {
        streamStartElapsedMs = SystemClock.elapsedRealtime();
        updatePipAutoEnter();

        sessionUiEffects.onConnected();
        if (launchReporter != null) {
            launchReporter.reportOnce();
        }

        hideSystemUi(1000);
    }

    private void displayTransientMessage(String message) {
        if (!streamUiSettingsState
                .get()
                .areConnectionWarningsDisabled()) {
            UiToast.makeText(
                    Game.this,
                    message,
                    UiToast.LENGTH_LONG).show();
        }
    }

    private void showStreamConfigurationWarnings(
            List<StreamSessionConfigurationPlanner.Warning> warnings) {
        for (StreamSessionConfigurationPlanner.Warning warning : warnings) {
            String message;
            switch (warning) {
                case HDR_DECODER_UNSUPPORTED:
                    message = getString(
                            R.string
                                    .stream_warning_hdr_decoder_unsupported);
                    break;
                case FORCED_HEVC_DECODER_UNAVAILABLE:
                    message = getString(
                            R.string
                                    .stream_warning_hevc_decoder_unavailable);
                    break;
                case FORCED_AV1_DECODER_UNAVAILABLE:
                    message = getString(
                            R.string
                                    .stream_warning_av1_decoder_unavailable);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unhandled stream warning: " + warning);
            }
            UiToast.makeText(
                    this,
                    message,
                    UiToast.LENGTH_LONG).show();
        }
    }

    private void showHdrRequestWarning(
            StreamHdrRequestPolicy.Warning warning) {
        int messageResource;
        switch (warning) {
            case NONE:
                return;
            case ANDROID_VERSION_UNSUPPORTED:
                messageResource = R.string
                        .stream_warning_hdr_android_version_unsupported;
                break;
            case DISPLAY_HDR10_UNSUPPORTED:
                messageResource = R.string
                        .stream_warning_hdr_display_unsupported;
                break;
            default:
                throw new IllegalStateException(
                        "Unhandled HDR request warning: " + warning);
        }
        UiToast.makeText(
                this,
                getString(messageResource),
                UiToast.LENGTH_LONG).show();
    }

    private void handleRumble(
            short controllerNumber,
            short lowFreqMotor,
            short highFreqMotor) {
        LimeLog.info(String.format((Locale)null, "Rumble on gamepad %d: %04x %04x", controllerNumber, lowFreqMotor, highFreqMotor));
        controllerHandler.handleRumble(controllerNumber, lowFreqMotor, highFreqMotor);
        //联动扳机震动
        if (controllerSettingsState
                .get()
                .isTriggerRumbleLinkEnabled()) {
            handleRumbleTriggers(
                    controllerNumber,
                    lowFreqMotor,
                    highFreqMotor);
        }
        if (performanceOverlayController != null) {
            performanceOverlayController.updateRumble(
                    controllerNumber,
                    lowFreqMotor,
                    highFreqMotor);
        }
    }

    private void handleRumbleTriggers(
            short controllerNumber,
            short leftTrigger,
            short rightTrigger) {
        LimeLog.info(String.format((Locale)null, "Rumble on gamepad triggers %d: %04x %04x", controllerNumber, leftTrigger, rightTrigger));

        controllerHandler.handleRumbleTriggers(controllerNumber, leftTrigger, rightTrigger);
    }

    private void handleHdrModeChanged(
            boolean enabled,
            byte[] hdrMetadata) {
        LimeLog.info("Display HDR mode: " + (enabled ? "enabled" : "disabled"));
        mediaResourceOwner.setHdrMode(enabled, hdrMetadata);
        UiHelper.notifyHdrWindowStatus(
                this,
                enabled,
                isHdrHighBrightnessEnabled());
    }

    private void handleMotionEventState(
            short controllerNumber,
            byte motionType,
            short reportRateHz) {
        LimeLog.info("axi-->: controllerNumber" + controllerNumber+"-motionType:"+motionType+"-reportRateHz:"+reportRateHz);
        controllerHandler.handleSetMotionEventState(controllerNumber, motionType, reportRateHz);
    }

    private void handleControllerLed(
            short controllerNumber,
            byte r,
            byte g,
            byte b) {
        controllerHandler.handleSetControllerLED(controllerNumber, r, g, b);
    }

    private void handleNativeCursor(
            boolean visible,
            boolean shapeChanged,
            int format,
            int x,
            int y,
            int width,
            int height,
            int hotspotX,
            int hotspotY,
            int shapeId,
            int scaleX,
            int scaleY,
            byte[] imageData) {
        if (nativeCursorOverlayView == null) {
            return;
        }

        nativeCursorOverlayView.setCursorScaleFromStream(
                streamView,
                streamDecoderSettings.getWidth(),
                streamDecoderSettings.getHeight(),
                scaleX,
                scaleY);
        nativeCursorOverlayView.updateCursor(
                visible,
                shapeChanged,
                format,
                width,
                height,
                hotspotX,
                hotspotY,
                shapeId,
                imageData);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return;
        }

        if (microphoneController != null) {
            microphoneController.onPermissionResult(
                    grantResults.length > 0 &&
                            grantResults[0] ==
                                    PackageManager.PERMISSION_GRANTED);
        }
    }

    @Override
    public void mouseMove(int deltaX, int deltaY) {
        conn.sendMouseMove((short) deltaX, (short) deltaY);
    }

    @Override
    public void sendRelativeMouseMove(int deltaX, int deltaY) {
        if (isInputReady()) {
            mouseMove(deltaX, deltaY);
        }
    }

    @Override
    public void mouseButtonEvent(int buttonId, boolean down) {
        byte buttonIndex;

        switch (buttonId)
        {
        case EvdevListener.BUTTON_LEFT:
            buttonIndex = MouseButtonPacket.BUTTON_LEFT;
            break;
        case EvdevListener.BUTTON_MIDDLE:
            buttonIndex = MouseButtonPacket.BUTTON_MIDDLE;
            break;
        case EvdevListener.BUTTON_RIGHT:
            buttonIndex = MouseButtonPacket.BUTTON_RIGHT;
            break;
        case EvdevListener.BUTTON_X1:
            buttonIndex = MouseButtonPacket.BUTTON_X1;
            break;
        case EvdevListener.BUTTON_X2:
            buttonIndex = MouseButtonPacket.BUTTON_X2;
            break;
        default:
            LimeLog.warning("Unhandled button: "+buttonId);
            return;
        }

        if (down) {
            conn.sendMouseButtonDown(buttonIndex);
        }
        else {
            conn.sendMouseButtonUp(buttonIndex);
        }
    }

    @Override
    public void sendMouseButton(int buttonId, boolean down) {
        if (isInputReady()) {
            mouseButtonEvent(buttonId, down);
        }
    }

    @Override
    public void mouseVScroll(byte amount) {
        conn.sendMouseScroll(amount);
    }

    @Override
    public void mouseHScroll(byte amount) {
        conn.sendMouseHScroll(amount);
    }

    public void mouseHighResScroll(boolean up){
        int amount =
                inputSettingsState
                        .get()
                        .getMouseWheelScrollAmount() *
                        50;
        conn.sendMouseHighResScroll(
                (short) (up ? amount : -amount));
    }

    @Override
    public void sendHighResolutionScroll(boolean up) {
        if (isInputReady()) {
            mouseHighResScroll(up);
        }
    }

    @Override
    public void keyboardEvent(boolean buttonDown, short keyCode) {
        keyboardInputController.sendAndroidKey(
                buttonDown,
                keyCode);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Don't do anything if we're not connected
        if (!isSessionConnected()) {
            return;
        }

        // This flag is set for all devices
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            hideSystemUi(2000);
        }
        else if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            hideSystemUi(2000);
        }
    }

    @Override
    public void onUsbPermissionPromptStarting() {
        if (spinner != null) {
            spinner.setFinishOnCancelEnabled(false);
        }
        // Disable PiP auto-enter while the USB permission prompt is on-screen. This prevents
        // us from entering PiP while the user is interacting with the OS permission dialog.
        suppressPipRefCount++;
        updatePipAutoEnter();
    }

    @Override
    public void onUsbPermissionPromptCompleted() {
        if (spinner != null) {
            spinner.setFinishOnCancelEnabled(true);
        }
        suppressPipRefCount--;
        updatePipAutoEnter();
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        switch (keyEvent.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return keyboardInputController.handleKeyDown(keyEvent);
            case KeyEvent.ACTION_UP:
                return keyboardInputController.handleKeyUp(keyEvent);
            case KeyEvent.ACTION_MULTIPLE:
                return keyboardInputController
                        .handleKeyMultiple(keyEvent);
            default:
                return false;
        }
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        return event != null && isInputReady() &&
                onKey(null, event.getKeyCode(), event);
    }

    private static final long BACK_EXIT_INTERVAL_MS = 2000;
    private long lastBackPressedElapsedMs;

    public void cancelPendingStreamBackExit() {
        lastBackPressedElapsedMs = 0;
    }

    @Override
    public void onBackPressed() {
        handleStreamBackPressed();
    }

    public void handleStreamBackPressed() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBackPressedElapsedMs <= BACK_EXIT_INTERVAL_MS) {
            if (dialogGameMenu != null && dialogGameMenu.isVisible()) {
                dialogGameMenu.dismiss();
            }
            cancelPendingStreamBackExit();
            finish();
            return;
        }

        lastBackPressedElapsedMs = now;
        UiToast.makeText(this, "再按一次返回退出串流", UiToast.LENGTH_SHORT).show();

        if (controllerSettingsState
                .get()
                .doesMouseEmulationOpenGameMenu() &&
                (dialogGameMenu == null ||
                        !dialogGameMenu.isVisible())) {
            showGameMenu(null);
        }
    }

    public void switchMouseModel(){
        String[] strings=getResources().getStringArray(R.array.mouse_model_names_axi);
        String[] items =Arrays.copyOf(strings,strings.length+1);
        items[items.length-1]="切换本地鼠标(需外接物理鼠标)";
//        {"多点触控模式","普通鼠标模式","触控板模式","禁用鼠标/触控","普通鼠标模式（左右键互换）","切换本地鼠标(需外接物理鼠标)"}
        new AlertDialog.Builder(this).setItems(items, (dialog, which) -> {
            dialog.dismiss();
            //切换本地鼠标
            if(which==7){
                switchMouseLocalCursor();
                return;
            }
            switchMouseModel(which);
        }).setTitle("请选择鼠标模式").create().show();
    }

    //本地鼠标光标切换
    public void switchMouseLocalCursor(){
        if (!grabbedInput) {
            inputCaptureProvider.enableCapture();
            grabbedInput = true;
        }
        cursorVisible = !cursorVisible;
        if (cursorVisible) {
            inputCaptureProvider.showCursor();
        } else {
            inputCaptureProvider.hideCursor();
        }
    }

    public void switchMouseModel(int which){
        TouchInputMode mode = TouchInputMode.fromPreferenceValue(which);
        if (mode != null && streamInputController != null) {
            streamInputController.setTouchMode(mode);
        }
    }

    public boolean toggleAbsoluteMouseMode() {
        boolean enabled = !streamInputController
                .getSettings()
                .isAbsoluteMouseMode();
        streamInputController.setAbsoluteMouseMode(enabled);
        if (conn != null) {
            conn.setAbsoluteMousePositionMode(enabled);
        }
        return enabled;
    }

    @Override
    public InputSettings getInputSettings() {
        return inputSettingsState.get();
    }

    @Override
    public ControllerSettings getControllerSettings() {
        return controllerSettingsState.get();
    }

    @Override
    public void applyInputSettingsUpdate(
            InputSettingsUpdate update) {
        InputSettings updated =
                update.applyTo(inputSettingsState.get());
        update.persist(settingsRepository);
        inputSettingsState.replace(updated);
    }

    @Override
    public void applyControllerSettingsUpdate(
            ControllerSettingsUpdate update) {
        ControllerSettings previous =
                controllerSettingsState.get();
        ControllerSettings updated =
                update.applyTo(previous);
        update.persist(settingsRepository);
        controllerSettingsState.replace(updated);
        if (previous.isBatteryReportingEnabled() !=
                updated.isBatteryReportingEnabled() &&
                controllerHandler != null) {
            controllerHandler.refreshBatteryReportingState();
        }
        if (!previous.isForceGyroEnabled() &&
                updated.isForceGyroEnabled()) {
            setMotionForceGyro();
        }
    }

    @Override
    public StreamAudioSettings getStreamAudioSettings() {
        return streamAudioSettingsState.get();
    }

    @Override
    public StreamVideoSettings getStreamVideoSettings() {
        return streamVideoSettingsState.get();
    }

    @Override
    public void applyStreamVideoSettingsUpdate(
            StreamVideoSettingsUpdate update) {
        StreamVideoSettings updated =
                update.applyTo(streamVideoSettingsState.get());
        update.persist(settingsRepository);
        streamVideoSettingsState.replace(updated);
    }

    @Override
    public void onDisplayConfigurationApplied() {
        if (dialogGameMenu != null) {
            dialogGameMenu.dismiss();
        }
        requestStreamDisconnect();
    }

    @Override
    public CustomResolutionRepository
            getCustomResolutionRepository() {
        return customResolutionRepository;
    }

    @Override
    public void applyStreamAudioSettingsUpdate(
            StreamAudioSettingsUpdate update) {
        StreamAudioSettings updated =
                update.applyTo(streamAudioSettingsState.get());
        update.persist(settingsRepository);
        streamAudioSettingsState.replace(updated);
        if (mediaResourceOwner != null) {
            mediaResourceOwner.updateAudioSettings(updated);
        }
        if (controllerHandler != null) {
            controllerHandler.refreshAudioHapticsState();
        }
    }

    @Override
    public StreamUiSettings getStreamUiSettings() {
        return streamUiSettingsState.get();
    }

    private boolean isGameModeIntegrationDisabled() {
        return streamUiSettingsState != null &&
                streamUiSettingsState.get()
                        .isGameModeIntegrationDisabled();
    }

    private boolean isHdrHighBrightnessEnabled() {
        return streamVideoSettings != null &&
                streamVideoSettings
                        .isHdrHighBrightnessEnabled();
    }

    @Override
    public void applyStreamUiSettingsUpdate(
            StreamUiSettingsUpdate update) {
        StreamUiSettings previous =
                streamUiSettingsState.get();
        StreamUiSettings updated =
                update.applyTo(previous);
        update.persist(settingsRepository);
        streamUiSettingsState.replace(updated);
        applyStreamUiSettingsEffects(previous, updated);
    }

    private void applyStreamUiSettingsEffects(
            StreamUiSettings previous,
            StreamUiSettings updated) {
        if (previous.isFloatingControlEnabled() !=
                updated.isFloatingControlEnabled()) {
            if (floatingControlController != null) {
                floatingControlController.applyEnabled(
                        updated.isFloatingControlEnabled());
            }
        }
        if (performanceOverlayController == null) {
            return;
        }
        if (previous.isRumbleOverlayEnabled() !=
                updated.isRumbleOverlayEnabled()) {
            performanceOverlayController
                    .applyRumbleVisibility();
        }
        if (previous.isCompactPerformanceInteractive() !=
                updated.isCompactPerformanceInteractive()) {
            performanceOverlayController
                    .applyCompactInteractivity();
        }
        if (previous.getCompactPerformanceScalePercent() !=
                updated.getCompactPerformanceScalePercent()) {
            performanceOverlayController.applyCompactScale();
        }
        if (previous.getCompactPerformanceMarginTopDp() !=
                updated.getCompactPerformanceMarginTopDp()) {
            performanceOverlayController.applyCompactMargin();
        }
    }

    private PerformanceOverlayRuntimeState
            createPerformanceOverlayRuntimeState() {
        boolean usbControllerActive =
                controllerHandler != null &&
                        controllerHandler.hasActiveUsbController();
        String usbControllerType =
                usbControllerActive ?
                        controllerHandler
                                .getActiveUsbControllerTypeDisplayName() :
                        null;
        return new PerformanceOverlayRuntimeState(
                isMicUplinkActive(),
                streamHost,
                streamStartElapsedMs,
                SystemClock.elapsedRealtime(),
                usbControllerActive,
                usbControllerType,
                usbDriverSessionController != null &&
                        usbDriverSessionController.isConnected());
    }

    private PerformanceOverlayConfiguration
            createPerformanceOverlayConfiguration() {
        return new PerformanceOverlayConfiguration(
                streamUiSettingsState.get(),
                streamDecoderSettings,
                streamDisplaySettings,
                streamAudioSettingsState.get(),
                controllerSettingsState.get());
    }

    public void showHUD(){
        if (performanceOverlayController != null) {
            performanceOverlayController.toggleVisibility();
        }
    }

    public void switchHUD(){
        if (performanceOverlayController != null) {
            performanceOverlayController.toggleExpandedMode();
        }
    }

    //切换触控灵敏度开关
    public void switchTouchSensitivity(){
        boolean enabled = !streamInputController
                .getSettings()
                .isDirectTouchSensitivityEnabled();
        streamInputController
                .setDirectTouchSensitivityEnabled(enabled);
    }

    //更新虚拟布局视图
    public void updateVirtualView(){
        virtualControlSettingsState.replace(
                VirtualControlSettingsLoader.load(
                        settingsRepository));
        if (virtualControlsController != null) {
            virtualControlsController.refreshCreatedLayouts();
        }
    }

    //切换虚拟手柄模式
    public void setVirtualGamepadEditMode(VirtualControlEditMode mode){
        if (virtualControlsController == null ||
                !virtualControlsController.setVirtualGamepadMode(mode)) {
            UiToast.makeText(
                    this,
                    R.string.game_menu_virtual_gamepad_mode_help,
                    UiToast.LENGTH_SHORT).show();
        }
    }
    //返回虚拟手柄当前的状态
    public VirtualControlEditMode getVirtualGamepadEditMode(){
        return virtualControlsController == null
                ? VirtualControlEditMode.NONE
                : virtualControlsController.getVirtualGamepadMode();
    }

    //切换虚拟手柄模式
    public void setVirtualKeysEditMode(VirtualControlEditMode mode){
        if (virtualControlsController == null ||
                !virtualControlsController.setVirtualKeysMode(mode)) {
            UiToast.makeText(
                    this,
                    R.string.game_menu_virtual_key_mode_help,
                    UiToast.LENGTH_SHORT).show();
        }
    }
    //返回虚拟手柄当前的状态
    public VirtualControlEditMode getVirtualKeysEditMode(){
        return virtualControlsController == null
                ? VirtualControlEditMode.NONE
                : virtualControlsController.getVirtualKeysMode();
    }


    public boolean isPortrait;

    //横竖屏切换
    public void switchLandscapePortraitScreen(){
        isPortrait = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        setPreferredOrientationForCurrentDisplay();
    }

    //画面平移缩放
    public void screenMoveZoom(){
        if(!streamView.isEnableZoomAndPan()){
            streamInputController.setTouchInputSuspended(true);
            streamView.setEnableZoomAndPan(true);
            return;
        }
        streamInputController.setTouchInputSuspended(false);
        streamView.setEnableZoomAndPan(false);
    }

    public boolean getScreenMoveZoom(){
        return streamView.isEnableZoomAndPan();
    }

    public void disconnect() {
        finish();
    }

    private GameMenuFragment dialogGameMenu;
    private final GameMenuSession<GameMenuFragment> gameMenuSession =
            new GameMenuSession<>();

    @Override
    public void showGameMenu(GameInputDevice device) {
        if (dialogGameMenu != null && !dialogGameMenu.isRemoving()) {
            if (device != null) {
                gameMenuSession.open(dialogGameMenu, device);
            }
            return;
        }

        android.app.Fragment existing = getFragmentManager()
                .findFragmentByTag(GameMenuFragment.FRAGMENT_TAG);
        if (existing instanceof GameMenuFragment) {
            dialogGameMenu = (GameMenuFragment) existing;
            gameMenuSession.open(dialogGameMenu, device);
            return;
        }

        dialogGameMenu = GameMenuFragment.newInstance(
                UiHelper.dpToPx(this, 364));
        gameMenuSession.open(dialogGameMenu, device);
        dialogGameMenu.show(getFragmentManager());
    }

    @Override
    public boolean isGamepadMouseEmulationAvailable() {
        return gameMenuSession.isMouseEmulationAvailable();
    }

    @Override
    public boolean isVirtualControllerVisible() {
        return virtualControlsController != null &&
                virtualControlsController.isVirtualGamepadVisible();
    }

    @Override
    public boolean isVirtualKeysVisible() {
        return virtualControlsController != null &&
                virtualControlsController.isVirtualKeysVisible();
    }

    @Override
    public GameMenuCardLayoutLoadResult
            loadGameMenuCardLayout() {
        return gameMenuCardLayoutRepository.load();
    }

    @Override
    public void saveGameMenuCardLayout(
            GameMenuCardLayout layout) {
        gameMenuCardLayoutRepository.save(layout);
    }

    @Override
    public List<GameMenuShortcut> loadGameMenuShortcuts() {
        return gameMenuShortcutRepository.load();
    }

    @Override
    public boolean saveGameMenuShortcut(
            GameMenuShortcut shortcut) {
        return gameMenuShortcutRepository.save(shortcut);
    }

    @Override
    public boolean deleteGameMenuShortcut(
            String shortcutId) {
        return gameMenuShortcutRepository.delete(shortcutId);
    }

    @Override
    public void toggleGamepadMouseEmulation() {
        gameMenuSession.toggleMouseEmulation();
    }

    @Override
    public void requestStreamDisconnect() {
        finish();
    }

    @Override
    public void requestStreamQuit() {
        isQuitSteamingFlag = true;
        disconnect();
    }

    @Override
    public void requestSoftKeyboard() {
        if (hasWindowFocus()) {
            showKeyboard();
        }
        else {
            showSoftKeyboardWhenFocused = true;
        }
    }

    @Override
    public VirtualControlSettings getVirtualControlSettings() {
        return virtualControlSettingsState.get();
    }

    @Override
    public boolean isOnscreenControllerRumbleEnabled() {
        return controllerSettingsState
                .get()
                .isOnscreenRumbleEnabled();
    }

    @Override
    public void applyVirtualControlSettingsUpdate(
            VirtualControlSettingsUpdate<?> update) {
        VirtualControlSettings updatedSettings =
                update.applyTo(
                        virtualControlSettingsState.get());
        update.persist(settingsRepository);
        virtualControlSettingsState.replace(updatedSettings);
    }

    @Override
    public void setOnscreenControllerRumbleEnabled(
            boolean enabled) {
        settingsRepository.edit()
                .put(
                        ControllerSettingKeys.ONSCREEN_RUMBLE,
                        enabled)
                .apply();
        controllerSettingsState.replace(
                ControllerSettingsLoader.load(
                        settingsRepository));
    }

    @Override
    public void onGameMenuDismissed(GameMenuFragment menu) {
        cancelPendingStreamBackExit();
        if (gameMenuSession.close(menu)) {
            dialogGameMenu = null;
        }
    }

    @Override
    public void sendKeyboardChord(short[] keyCodes) {
        if (keyboardInputController != null && isInputReady()) {
            keyboardInputController.sendChord(keyCodes);
        }
    }

    @Override
    public void sendAndroidKeyChord(int[] keyCodes) {
        if (keyCodes == null || keyCodes.length == 0 ||
                !isInputReady()) {
            return;
        }

        int[] chord = Arrays.copyOf(keyCodes, keyCodes.length);
        for (int keyCode : chord) {
            KeyEvent event =
                    new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            event.setSource(0);
            sendKeyEvent(event);
        }
        streamView.postDelayed(() -> {
            for (int index = chord.length - 1; index >= 0; index--) {
                KeyEvent event = new KeyEvent(
                        KeyEvent.ACTION_UP, chord[index]);
                event.setSource(0);
                sendKeyEvent(event);
            }
        }, KEY_CHORD_UP_DELAY_MS);
    }

    @Override
    public void applyDualSenseTriggerSettings() {
        refreshAdaptiveTriggerState();
    }


    private SecondaryDisplayPresentation presentation;
    public void showSecondScreen(){
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        int mainDisplayId = Display.DEFAULT_DISPLAY;
        int secondaryDisplayId = -1;
        for (Display display : displays) {
//            LimeLog.info(display.toString());
            if (display.getDisplayId() != mainDisplayId) {
                secondaryDisplayId = display.getDisplayId();
                break;
            }
        }
        if (secondaryDisplayId != -1) {
            Display secondaryDisplay = displayManager.getDisplay(secondaryDisplayId);
            presentation = new SecondaryDisplayPresentation(this, secondaryDisplay);
            presentation.show();
            if(rootView!= null) {
                ((ViewGroup)rootView).removeView(streamView); // <- fix
                presentation.addView(streamView);
            }

        }
    }


    // 设置surfaceView的圆角 setSurfaceviewCorner(UiHelper.dpToPx(this,24));
    private void setSurfaceviewCorner(final float radius) {

        streamView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                int leftMargin = 0;
                int topMargin = 0;
                Rect selfRect = new Rect(leftMargin, topMargin, rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
                outline.setRoundRect(selfRect, radius);
            }
        });
        streamView.setClipToOutline(true);
    }

    private void startSessionWithRenderTarget(Surface renderTarget) {
        if (sessionController == null ||
                !sessionController.canStart()) {
            return;
        }

        StreamMediaResourceOwner.StartResources resources =
                mediaResourceOwner.prepareStart(renderTarget);
        sessionUiEffects.onConnecting();
        try {
            if (sessionController.start(
                    resources.getAudioRenderer(),
                    resources.getVideoRenderer())) {
                return;
            }
        } catch (RuntimeException | Error error) {
            mediaResourceOwner.releaseStartResources();
            sessionUiEffects.onEnded();
            throw error;
        }
        mediaResourceOwner.releaseStartResources();
        sessionUiEffects.onEnded();
    }

    private boolean hasSessionStarted() {
        return sessionController != null &&
                sessionController.hasStartBeenRequested();
    }

    public boolean isSessionConnected() {
        return sessionController != null &&
                sessionController.getState().isStreaming();
    }

    @Override
    public boolean isInputReady() {
        return isSessionConnected();
    }

    private boolean isRecordAudioPermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isMicUplinkActive() {
        return microphoneController != null &&
                microphoneController.isActive();
    }

    //是否退出串流
    public boolean isQuitSteamingFlag;

    public void quitSteaming(){
        ServerHelper.doQuit(this,streamReqBean, null);
    }

    private StreamFloatingControlController
            createFloatingControlController() {
        return new StreamFloatingControlController(
                this,
                (ViewGroup) getWindow().getDecorView(),
                streamUiSettingsState,
                (x, y, nearestLeft) ->
                        applyStreamUiSettingsUpdate(
                                StreamUiSettingsUpdate
                                        .floatingPosition(
                                                x,
                                                y,
                                                nearestLeft)),
                action -> {
                    switch (action) {
                        case GAME_MENU:
                            showGameMenu(null);
                            break;
                        case SOFT_KEYBOARD:
                            toggleKeyboard();
                            break;
                        case FULL_KEYBOARD:
                            performStreamUiAction(
                                    StreamUiActions.Action
                                            .TOGGLE_FULL_KEYBOARD);
                            break;
                    }
                });
    }

    public void sendClipboardText(){
        if(conn==null){
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            String text=clip.getItemAt(0).coerceToText(this).toString();
            keyboardInputController.sendText(text);
        }
    }

    public void pullRemoteClipboardFiles() {
        if (clipboardFileTransferController != null) {
            clipboardFileTransferController.pullRemoteFiles();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (clipboardFileTransferController != null) {
            clipboardFileTransferController.onActivityResult(
                    requestCode, resultCode, data);
        }
    }

    private void refreshAdaptiveTriggerState() {
        controllerHandler.refreshAdaptiveTriggerState();
    }

    public void setMotionForceGyro(){
        if (controllerSettingsState
                .get()
                .isForceGyroEnabled()) {
            if(controllerHandler!=null){
                controllerHandler.handleSetMotionEventState((short) 0, MoonBridge.LI_MOTION_TYPE_GYRO, (short) 100);
            }
        }
    }

    @Override
    public void switchMic() {
        if (microphoneController != null) {
            microphoneController.toggle();
        }
    }

    private void showMicrophoneMessage(
            String message,
            int duration) {
        if (message == null || message.isEmpty() || isFinishing()) {
            return;
        }
        UiToast.makeText(this, message, duration).show();
    }

}
