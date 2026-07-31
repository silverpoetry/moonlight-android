package com.limelight;


import android.Manifest;
import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.audio.mic.AndroidMicrophoneUplinkSessionFactory;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardInputController;
import com.limelight.binding.input.KeyboardInputSink;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.StreamInputGateway;
import com.limelight.binding.input.StreamInputGatewayRegistry;
import com.limelight.binding.input.StreamInputController;
import com.limelight.binding.input.protocol.NvConnectionPointerInputSink;
import com.limelight.binding.input.protocol.NvConnectionKeyboardInputSink;
import com.limelight.binding.input.capture.InputCaptureManager;
import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.binding.input.pointer.ExternalPointerInputController;
import com.limelight.binding.input.touch.DirectContactInputController;
import com.limelight.binding.input.touch.TouchInputController;
import com.limelight.binding.input.touch.TouchInputMode;
import com.limelight.binding.input.virtual_controller.VirtualController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardLayoutController;
import com.limelight.binding.video.CrashListener;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.fsr.FsrVideoProcessor;
import com.limelight.fsr.VideoProcessingGLSurfaceView;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.StreamSessionController;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.mic.MicrophoneUplinkConfig;
import com.limelight.nvstream.mic.MicrophoneUplinkState;
import com.limelight.preferences.GlPreferences;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidDisplayAspectProvider;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.android.AndroidHdrCompatibility;
import com.limelight.settings.android.AndroidStreamSettingsBootstrap;
import com.limelight.settings.android.SharedPreferencesCustomResolutionRepository;
import com.limelight.settings.android.SharedPreferencesSettingsRepository;
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
import com.limelight.settings.stream.StreamFramePacingPolicy;
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
import com.limelight.ui.stream.StreamFailureDiagnostics;
import com.limelight.ui.stream.StreamLaunchReporter;
import com.limelight.ui.stream.StreamMediaResourceOwner;
import com.limelight.ui.stream.StreamSessionCallbackRouter;
import com.limelight.ui.stream.StreamSessionUiEffects;
import com.limelight.ui.stream.StreamWifiLockController;
import com.limelight.ui.GameGestures;
import com.limelight.ui.NativeCursorOverlayView;
import com.limelight.ui.StreamLayoutGeometry;
import com.limelight.ui.StreamWindowPolicy;
import com.limelight.ui.StreamUiActions;
import com.limelight.ui.StreamView;
import com.limelight.ui.floatingview.AXFloatingMagnetView;
import com.limelight.ui.floatingview.AXFloatingView;
import com.limelight.ui.floatingview.AXFloatingViewListener;
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
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
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
import android.preference.PreferenceManager;
import android.util.Rational;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class Game extends Activity implements SurfaceHolder.Callback,
        OnGenericMotionListener, OnTouchListener, EvdevListener,
        OnSystemUiVisibilityChangeListener, GameGestures, StreamInputGateway,
        StreamUiActions, GameMenuHost,
        UsbDriverService.UsbDriverStateListener, View.OnKeyListener {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;
    private static final long KEY_CHORD_UP_DELAY_MS = 25;

    private StreamInputController streamInputController;
    private InputSettingsState inputSettingsState;

    private static final int SOFT_KEYBOARD_SHOW_RETRY_MS = 50;

    private ControllerHandler controllerHandler;
    private ControllerSettingsState controllerSettingsState;
    private StreamAudioSettingsState streamAudioSettingsState;
    private StreamUiSettingsState streamUiSettingsState;
    private VirtualControlSettingsState virtualControlSettingsState;
    private VirtualControlLayoutRepository virtualControlLayoutRepository;
    private KeyboardInputController keyboardInputController;
    private KeyBoardController virtualController;

    private KeyBoardController keyBoardController;

    private KeyBoardLayoutController keyBoardLayoutController;

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
    private SharedPreferences tombstonePrefs;

    private NvConnection conn;
    private StreamSessionController sessionController;
    private StreamSessionCallbackRouter sessionCallbackRouter;
    private StreamFailureDiagnostics failureDiagnostics;
    private StreamLaunchReporter launchReporter;
    private StreamSessionUiEffects sessionUiEffects;
    private SpinnerDialog spinner;
    private boolean displayedFailureDialog = false;
    private boolean awaitingRecordAudioPermission = false;
    private RemoteClipboardFileTransferController
            clipboardFileTransferController;
    private boolean autoEnterPip = false;
    private boolean surfaceCreated = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;
    private String streamHost;
    private long streamStartElapsedMs;
    private NvApp app;
    private float desiredRefreshRate;
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
    private VideoProcessingGLSurfaceView fsrView;
    private FsrVideoProcessor fsrVideoProcessor;

    private boolean isHidingOverlays;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private StreamPerformanceOverlayController
            performanceOverlayController;

    private StreamMediaResourceOwner mediaResourceOwner;
    private boolean reportedCrash;
    private boolean micToggleInFlight;
    private boolean pendingMicToggleAfterPermission;

    private StreamWifiLockController wifiLockController;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
            binder.configureSettings(
                    controllerSettingsState,
                    streamAudioSettingsState);
            binder.setListener(controllerHandler);
            binder.setStateListener(Game.this);
            binder.start();
            connectedToUsbDriverService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectedToUsbDriverService = false;
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

    private boolean fsrEnabled;
    private boolean fsrInputSurfaceReady;
    private boolean fsrDisplaySurfaceCreated;
    private volatile boolean streamRenderSurfaceReady;
    private boolean usbPermissionPromptVisible;
    private boolean fsrViewLifecyclePaused;
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

        settingsRepository =
                new SharedPreferencesSettingsRepository(
                        PreferenceManager
                                .getDefaultSharedPreferences(this));
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
        tombstonePrefs = Game.this.getSharedPreferences("DecoderTombstone", 0);
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

        fsrEnabled = isFsrEnabled();
        configureFsrWindowColorMode();

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) streamView.getLayoutParams();
        params.gravity = resolvePhysicalStreamGravity(
                streamDisplaySettings.getGravity(),
                params.gravity);

        if (fsrEnabled) {
            fsrVideoProcessor = new FsrVideoProcessor(this);
            fsrVideoProcessor.setSharpness(
                    streamDisplaySettings
                            .getFsrSharpness()
                            .getFactor());
            fsrVideoProcessor.setFsrEnabled(true);
            fsrView = new VideoProcessingGLSurfaceView(this, false, isFsrNativeHdrOutputEnabled(), fsrVideoProcessor,
                    new VideoProcessingGLSurfaceView.SurfaceListener() {
                        @Override
                        public void onInputSurfaceAvailable(android.graphics.SurfaceTexture surfaceTexture) {
                            Surface inputSurface =
                                    new Surface(surfaceTexture);
                            StreamMediaResourceOwner resources =
                                    mediaResourceOwner;
                            if (resources == null) {
                                inputSurface.release();
                                return;
                            }
                            resources.replaceFsrInputSurface(
                                    inputSurface);
                            fsrInputSurfaceReady = true;
                            if (hasSessionStarted()) {
                                resources.setRenderTarget(
                                        inputSurface);
                            }
                            startConnectionIfReady();
                        }

                        @Override
                        public void onInputSurfaceDestroyed() {
                            fsrInputSurfaceReady = false;
                            if (mediaResourceOwner != null) {
                                mediaResourceOwner
                                        .releaseFsrInputSurface();
                            }
                        }
                    });
            fsrView.setFocusable(false);
            fsrView.setFocusableInTouchMode(false);
            fsrView.setClickable(false);

            FrameLayout.LayoutParams fsrLayoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            fsrLayoutParams.gravity = params.gravity;
            fsrView.setLayoutParams(fsrLayoutParams);

            ViewGroup parent = (ViewGroup) streamView.getParent();
            int streamIndex = parent.indexOfChild(streamView);
            parent.addView(fsrView, streamIndex + 1);

            streamView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            streamView.setZOrderMediaOverlay(true);
            fsrView.getHolder().addCallback(this);
            fsrView.setFrameInputSize(
                    streamDecoderSettings.getWidth(),
                    streamDecoderSettings.getHeight());
            if (isFsrNativeHeightTarget()) {
                fsrView.setFixedSurfacePixelSize(0, 0);
            }
            else {
                StreamLayoutGeometry.Size fsrOutputSize =
                        getFsrOutputSize();
                fsrView.setFixedSurfacePixelSize(
                        fsrOutputSize.width,
                        fsrOutputSize.height);
            }
        }

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

        // Initialize the MediaCodec helper before creating the decoder
        GlPreferences glPrefs = GlPreferences.readPreferences(this);
        MediaCodecHelper.initialize(this, glPrefs.glRenderer);

        // Check if the user has enabled HDR
        boolean willStreamHdr = false;
        if (streamVideoSettings.shouldIgnoreHdrCapability()) {
            willStreamHdr=true;
        }else{
            if (streamDisplaySettings.isHdrEnabled() &&
                    AndroidHdrCompatibility
                            .isHdrStreamingAllowed()) {
                // Start our HDR checklist
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Display display = getWindowManager().getDefaultDisplay();
                    Display.HdrCapabilities hdrCaps = display.getHdrCapabilities();

                    // We must now ensure our display is compatible with HDR10
                    if (hdrCaps != null) {
                        // getHdrCapabilities() returns null on Lenovo Lenovo Mirage Solo (vega), Android 8.0
                        for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                            if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                                willStreamHdr = true;
                                break;
                            }
                        }
                    }

                    if (!willStreamHdr) {
                        // Nope, no HDR for us :(
                        UiToast.makeText(this, "Display does not support HDR10", UiToast.LENGTH_LONG).show();
                    }
                }
                else {
                    UiToast.makeText(this, "HDR requires Android 7.0 or later", UiToast.LENGTH_LONG).show();
                }
            }
        }

        MediaCodecDecoderRenderer decoderRenderer =
                new MediaCodecDecoderRenderer(
                this,
                streamDecoderSettings,
                new CrashListener() {
                    @SuppressLint("ApplySharedPref")
                    @Override
                    public void notifyCrash(Exception e) {
                        // The MediaCodec instance is going down due to a crash
                        // let's tell the user something when they open the app again

                        // We must use commit because the app will crash when we return from this function
                        tombstonePrefs.edit().putInt("CrashCount", tombstonePrefs.getInt("CrashCount", 0) + 1).commit();
                        reportedCrash = true;
                    }
                },
                tombstonePrefs.getInt("CrashCount", 0),
                connMgr.isActiveNetworkMetered(),
                willStreamHdr,
                glPrefs.glRenderer,
                performanceOverlayController);
        mediaResourceOwner = StreamMediaResourceOwner.create(
                decoderRenderer,
                () -> new AndroidAudioRenderer(
                        Game.this,
                        controllerHandler,
                        streamAudioSettingsState));

        // Don't stream HDR if the decoder can't support it
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported() && !decoderRenderer.isAv1Main10Supported()) {
            willStreamHdr = false;
            UiToast.makeText(this, "Decoder does not support HDR10 profile", UiToast.LENGTH_LONG).show();
        }
        // Display a message to the user if HEVC was forced on but we still didn't find a decoder
        if (streamDecoderSettings.getVideoFormat() ==
                StreamDecoderSettings.VideoFormat.FORCE_HEVC &&
                !decoderRenderer.isHevcSupported()) {
            UiToast.makeText(this, "No HEVC decoder found", UiToast.LENGTH_LONG).show();
        }

        // Display a message to the user if AV1 was forced on but we still didn't find a decoder
        if (streamDecoderSettings.getVideoFormat() ==
                StreamDecoderSettings.VideoFormat.FORCE_AV1 &&
                !decoderRenderer.isAv1Supported()) {
            UiToast.makeText(this, "No AV1 decoder found", UiToast.LENGTH_LONG).show();
        }

        // H.264 is always supported
        int supportedVideoFormats = MoonBridge.VIDEO_FORMAT_H264;
        if (decoderRenderer.isHevcSupported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265;
            if (willStreamHdr && decoderRenderer.isHevcMain10Hdr10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265_MAIN10;
            }
        }
        if (decoderRenderer.isAv1Supported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;
            if (willStreamHdr && decoderRenderer.isAv1Main10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN10;
            }
        }

        ControllerSettings controllerSettings =
                controllerSettingsState.get();
        int gamepadMask =
                ControllerHandler.getAttachedControllerMask(
                        this,
                        controllerSettings);
        if (!controllerSettings.isMultiControllerEnabled()) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            gamepadMask = 1;
        }
        if (controllerSettings.isOnscreenControllerEnabled()) {
            // If we're using OSC, always set at least gamepad 1.
            gamepadMask |= 1;
        }

        // Set to the optimal mode for streaming
        float displayRefreshRate = prepareDisplayForRendering();
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        StreamFramePacingPolicy.Decision pacingDecision =
                StreamFramePacingPolicy.resolve(
                        streamDecoderSettings.getFramePacing(),
                        streamDecoderSettings.getFps(),
                        displayRefreshRate);
        int chosenFrameRate = pacingDecision.getTargetFps();
        effectiveFramePacing =
                pacingDecision.getEffectiveMode();
        if (effectiveFramePacing !=
                streamDecoderSettings.getFramePacing()) {
            LimeLog.info(
                    "Using balanced frame pacing for incompatible display refresh rate");
        }
        else if (chosenFrameRate !=
                streamDecoderSettings.getFps()) {
            LimeLog.info(
                    "Adjusting FPS target for screen to " +
                            chosenFrameRate);
        }

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(
                        streamDecoderSettings.getWidth(),
                        streamDecoderSettings.getHeight())
                .setLaunchRefreshRate(
                        streamDecoderSettings.getFps())
                .setRefreshRate(chosenFrameRate)
                .setApp(app)
                .setBitrate(
                        streamDecoderSettings.getBitrateKbps())
                .setEnableSops(
                        streamVideoSettings
                                .shouldOptimizeGameSettings())
                .enableLocalAudioPlayback(
                        streamAudioSettingsState
                                .get()
                                .shouldPlayHostAudio())
                .setMaxPacketSize(1392)
                .setRemoteConfiguration(StreamConfiguration.STREAM_CFG_AUTO) // NvConnection will perform LAN and VPN detection
                .setSupportedVideoFormats(supportedVideoFormats)
                .setAttachedGamepadMask(gamepadMask)
                .setClientRefreshRateX100((int)(displayRefreshRate * 100))
                .setAudioConfiguration(
                        toTransportAudioConfiguration(
                                streamAudioSettingsState
                                        .get()
                                        .getChannelConfiguration()))
                .setColorSpace(decoderRenderer.getPreferredColorSpace())
                .setColorRange(decoderRenderer.getPreferredColorRange())
                .setPPI(RazerUtils.getPPI(this))
                .setRazerVD(
                        streamVideoSettings
                                .getVirtualDisplayMode()
                                .getStorageValue())
                .setPersistGamepadsAfterDisconnect(
                        !controllerSettings
                                .isMultiControllerEnabled())
                .enableNativeCursor(
                        inputSettingsState
                                .get()
                                .isAbsoluteMouseMode())
                .enableClipboardSync(
                        transferSettings.isClipboardSyncEnabled())
                .disableAdaptiveInputThrottling(
                        inputSettingsState
                                .get()
                                .isAdaptiveInputThrottlingDisabled())
                .build();

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
        failureDiagnostics = StreamFailureDiagnostics.create(
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
        sessionCallbackRouter = new StreamSessionCallbackRouter(
                new StreamSessionCallbackRouter.UiHost() {
                    @Override
                    public void onStageStarting(String stage) {
                        handleStageStarting(stage);
                    }

                    @Override
                    public void onStageFailed(
                            String stage,
                            int portFlags,
                            int errorCode) {
                        handleStageFailed(
                                stage,
                                portFlags,
                                errorCode);
                    }

                    @Override
                    public void onConnectionStarted() {
                        handleConnectionStarted();
                    }

                    @Override
                    public void onConnectionTerminated(int errorCode) {
                        handleConnectionTerminated(errorCode);
                    }

                    @Override
                    public void onConnectionStatusUpdate(
                            int connectionStatus) {
                        handleConnectionStatusUpdate(connectionStatus);
                    }

                    @Override
                    public void onMessage(
                            String message,
                            boolean transientMessage) {
                        if (transientMessage) {
                            displayTransientMessage(message);
                        }
                        else {
                            handleStreamMessage(message);
                        }
                    }

                    @Override
                    public void onHdrModeChanged(
                            boolean enabled,
                            byte[] hdrMetadata) {
                        handleHdrModeChanged(enabled, hdrMetadata);
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
                        handleNativeCursor(
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

        //鼠标触控模式
        switchMouseModel(
                inputSettingsState.get()
                        .getTouchModePreferenceValue());

        if (controllerSettingsState
                .get()
                .isOnscreenControllerEnabled()) {
            // create virtual onscreen controller
            initVirtualController();
        }

        //特殊按键屏幕布局
        if (virtualControlSettingsState
                .get()
                .shouldShowVirtualKeysOnStart()) {
            initKeyboardController();
        }

        if (controllerSettingsState
                .get()
                .isUsbDriverEnabled()) {
            // Start the USB driver
            bindService(new Intent(this, UsbDriverService.class),
                    usbDriverServiceConnection, Service.BIND_AUTO_CREATE);
        }

        //悬浮球
        if (streamUiSettingsState
                .get()
                .isFloatingControlEnabled()) {
            initFloatingView();
        }

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

        // The connection will be started when the surface gets created
        if (!fsrEnabled) {
            streamView.getHolder().addCallback(this);
        }

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
//        initFloatingView();

        sessionDependenciesReady = true;
        startConnectionIfReady();
    }

    private void initKeyboardController(){
        keyBoardController = new KeyBoardController(
                controllerHandler,
                (FrameLayout) rootView,
                this,
                inputSettingsState,
                virtualControlSettingsState,
                virtualControlLayoutRepository,
                false, this, this);
//        keyBoardController.refreshLayout();
        keyBoardController.show();
    }


    private void initVirtualController(){
        virtualController = new KeyBoardController(
                controllerHandler,
                (FrameLayout) rootView,
                this,
                inputSettingsState,
                virtualControlSettingsState,
                virtualControlLayoutRepository,
                true, this, this);
//        virtualController.refreshLayout();
        virtualController.show();
    }

    private void initkeyBoardLayoutController(){
        keyBoardLayoutController = new KeyBoardLayoutController(
                controllerHandler,
                (FrameLayout) rootView,
                this,
                virtualControlSettingsState,
                this, this);
        keyBoardLayoutController.refreshLayout();
        keyBoardLayoutController.show();
    }

    //显示隐藏虚拟特殊按键
    public void showHideKeyboardController(){
        if(keyBoardController==null){
            initKeyboardController();
            return;
        }
        keyBoardController.toggleVisibility();
    }

    public void showHidekeyBoardLayoutController(){
        if(keyBoardLayoutController==null){
            initkeyBoardLayoutController();
            return;
        }
        keyBoardLayoutController.switchShowHide();
    }

    //显示隐藏虚拟手柄控制器
    public void showHideVirtualController(){
        if(virtualController==null){
            initVirtualController();
            return;
        }
        virtualController.toggleVisibility();
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
                showHideKeyboardController();
                break;
            case TOGGLE_FULL_KEYBOARD:
                showHidekeyBoardLayoutController();
                break;
            case TOGGLE_VIRTUAL_GAMEPAD:
                showHideVirtualController();
                break;
            case TOGGLE_FLOATING_BUTTON:
                switchFloatView();
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
        if (virtualController != null) {
            return virtualController.isVisible();
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

        if (virtualController != null) {
            // Refresh layout of OSC for possible new screen size
            virtualController.refreshLayout();
        }

        if(keyBoardController !=null){
            keyBoardController.refreshLayout();
        }

        if(keyBoardLayoutController!=null){
            keyBoardLayoutController.refreshLayout();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                isHidingOverlays = true;

                if (virtualController != null) {
                    virtualController.hide();
                }

                if (keyBoardController != null) {
                    keyBoardController.hide();
                }

                if(keyBoardLayoutController!=null){
                    keyBoardLayoutController.hide();
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

    private boolean isRefreshRateEqualMatch(float refreshRate) {
        return refreshRate >= streamDecoderSettings.getFps() &&
                refreshRate <= streamDecoderSettings.getFps() + 3;
    }

    private boolean isRefreshRateGoodMatch(float refreshRate) {
        return refreshRate >= streamDecoderSettings.getFps() &&
                Math.round(refreshRate) %
                        streamDecoderSettings.getFps() <= 3;
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
            Display.Mode bestMode = display.getMode();
            boolean isNativeResolutionStream =
                    streamDisplaySettings.isNativeResolution();
            boolean refreshRateIsGood = isRefreshRateGoodMatch(bestMode.getRefreshRate());
            boolean refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.getRefreshRate());

            LimeLog.info("Current display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            for (Display.Mode candidate : display.getSupportedModes()) {
                boolean refreshRateReduced = candidate.getRefreshRate() < bestMode.getRefreshRate();
                boolean resolutionReduced = candidate.getPhysicalWidth() < bestMode.getPhysicalWidth() ||
                        candidate.getPhysicalHeight() < bestMode.getPhysicalHeight();
                boolean resolutionFitsStream =
                        candidate.getPhysicalWidth() >=
                                streamDecoderSettings.getWidth() &&
                        candidate.getPhysicalHeight() >=
                                streamDecoderSettings.getHeight();

                LimeLog.info("Examining display mode: "+candidate.getPhysicalWidth()+"x"+
                        candidate.getPhysicalHeight()+"x"+candidate.getRefreshRate());

                if (candidate.getPhysicalWidth() > 4096 &&
                        streamDecoderSettings.getWidth() <= 4096) {
                    // Avoid resolutions options above 4K to be safe
                    continue;
                }

                // On non-4K streams, we force the resolution to never change unless it's above
                // 60 FPS, which may require a resolution reduction due to HDMI bandwidth limitations,
                // or it's a native resolution stream.
                if (streamDecoderSettings.getWidth() < 3840 &&
                        streamDecoderSettings.getFps() <= 60 &&
                        !isNativeResolutionStream) {
                    if (display.getMode().getPhysicalWidth() != candidate.getPhysicalWidth() ||
                            display.getMode().getPhysicalHeight() != candidate.getPhysicalHeight()) {
                        continue;
                    }
                }

                // Make sure the resolution doesn't regress unless if it's over 60 FPS
                // where we may need to reduce resolution to achieve the desired refresh rate.
                if (resolutionReduced &&
                        !(streamDecoderSettings.getFps() > 60 &&
                                resolutionFitsStream)) {
                    continue;
                }

                if (mayReduceRefreshRate() && refreshRateIsEqual && !isRefreshRateEqualMatch(candidate.getRefreshRate())) {
                    // If we had an equal refresh rate and this one is not, skip it. In min latency
                    // mode, we want to always prefer the highest frame rate even though it may cause
                    // microstuttering.
                    continue;
                }
                else if (refreshRateIsGood) {
                    // We've already got a good match, so if this one isn't also good, it's not
                    // worth considering at all.
                    if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                        continue;
                    }

                    if (mayReduceRefreshRate()) {
                        // User asked for the lowest possible refresh rate, so don't raise it if we
                        // have a good match already
                        if (candidate.getRefreshRate() > bestMode.getRefreshRate()) {
                            continue;
                        }
                    }
                    else {
                        // User asked for the highest possible refresh rate, so don't reduce it if we
                        // have a good match already
                        if (refreshRateReduced) {
                            continue;
                        }
                    }
                }
                else if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                    // We didn't have a good match and this match isn't good either, so just don't
                    // reduce the refresh rate.
                    if (refreshRateReduced) {
                        continue;
                    }
                } else {
                    // We didn't have a good match and this match is good. Prefer this refresh rate
                    // even if it reduces the refresh rate. Lowering the refresh rate can be beneficial
                    // when streaming a 60 FPS stream on a 90 Hz device. We want to select 60 Hz to
                    // match the frame rate even if the active display mode is 90 Hz.
                }

                bestMode = candidate;
                refreshRateIsGood = isRefreshRateGoodMatch(candidate.getRefreshRate());
                refreshRateIsEqual = isRefreshRateEqualMatch(candidate.getRefreshRate());
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
            if (fsrView != null) {
                fsrView.setDesiredAspectRatio(0.0);
            }
        }
        else {
            // Set the surface to scale based on the aspect ratio of the stream
            streamView.setDesiredAspectRatio(desiredAspectRatio);
            if (fsrView != null) {
                fsrView.setDesiredAspectRatio(desiredAspectRatio);
            }
            LimeLog.info("surfaceChanged-->" + desiredAspectRatio);
        }

        // Set the desired refresh rate that will get passed into setFrameRate() later
        desiredRefreshRate = displayRefreshRate;

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
        if (failureDiagnostics != null) {
            failureDiagnostics.destroy();
            failureDiagnostics = null;
        }
        if (launchReporter != null) {
            launchReporter.destroy();
            launchReporter = null;
        }
        if (streamInputController != null) {
            streamInputController.destroy();
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
        UiHelper.notifyHdrWindowStatus(
                this,
                false,
                isHdrHighBrightnessEnabled());

        if(presentation!=null){
            presentation.dismiss();
        }

        if (controllerHandler != null) {
            controllerHandler.destroy();
        }
        if (keyboardInputController != null) {
            InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            inputManager.unregisterInputDeviceListener(
                    keyboardInputController);
            keyboardInputController = null;
        }

        if (wifiLockController != null) {
            wifiLockController.destroy();
            wifiLockController = null;
        }

        if (connectedToUsbDriverService) {
            // Unbind from the discovery service
            unbindService(usbDriverServiceConnection);
        }

        // Destroy the capture provider
        inputCaptureProvider.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (streamInputController != null) {
            streamInputController.start();
        }

        if (fsrView != null && fsrViewLifecyclePaused) {
            fsrView.onResume();
            fsrViewLifecyclePaused = false;
        }
    }

    @Override
    protected void onPause() {
        if (streamInputController != null) {
            streamInputController.stop();
        }

        if (fsrView != null && !(usbPermissionPromptVisible && !isFinishing())) {
            fsrView.onPause();
            fsrViewLifecyclePaused = true;
        }

        if (isFinishing()) {
            // Stop any further input device notifications before we lose focus (and pointer capture)
            if (controllerHandler != null) {
                controllerHandler.stop();
            }

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

        if (virtualController != null) {
            virtualController.hide();
        }
        if (keyBoardController != null) {
            keyBoardController.hide();
        }

        if(keyBoardLayoutController!=null){
            keyBoardLayoutController.hide();
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

            displayedFailureDialog = true;
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

            // Clear the tombstone count if we terminated normally
            if (!reportedCrash && tombstonePrefs.getInt("CrashCount", 0) != 0) {
                tombstonePrefs.edit()
                        .putInt("CrashCount", 0)
                        .putInt("LastNotifiedCrashCount", 0)
                        .apply();
            }
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
        return isControllerLayoutEditing(virtualController) ||
                isControllerLayoutEditing(keyBoardController);
    }

    private static boolean isControllerLayoutEditing(
            KeyBoardController controller) {
        if (controller == null) {
            return false;
        }

        KeyBoardController.ControllerMode mode =
                controller.getControllerMode();
        return mode == KeyBoardController.ControllerMode.MoveButtons ||
                mode == KeyBoardController.ControllerMode.ResizeButtons ||
                mode == KeyBoardController.ControllerMode
                        .DisableEnableButtons;
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

    private void handleStageStarting(String stage) {
        if (spinner != null) {
            spinner.setMessage(
                    getResources().getString(R.string.conn_starting) +
                            " " + stage);
        }
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

    private void handleStageFailed(
            String stage,
            int portFlags,
            int errorCode) {
        if (!canPresentSessionUi()) {
            return;
        }
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }

        if (displayedFailureDialog) {
            return;
        }
        displayedFailureDialog = true;
        LimeLog.severe(stage + " failed: " + errorCode);

        if (stage.contains("video") &&
                streamView.getHolder().getSurface().isValid()) {
            UiToast.makeText(
                    Game.this,
                    getResources().getText(
                            R.string.video_decoder_init_failed),
                    UiToast.LENGTH_LONG).show();
        }

        StreamFailureDiagnostics diagnostics =
                failureDiagnostics;
        if (diagnostics == null ||
                !diagnostics.request(
                        portFlags,
                        result -> displayStageFailureDialog(
                                stage,
                                errorCode,
                                result.getPortFlags(),
                                result.getProbeResultOr(
                                        MoonBridge
                                                .ML_TEST_RESULT_INCONCLUSIVE)))) {
            displayStageFailureDialog(
                    stage,
                    errorCode,
                    portFlags,
                    MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
        }
    }

    private void displayStageFailureDialog(
            String stage,
            int errorCode,
            int portFlags,
            int portTestResult) {
        String dialogText =
                getResources().getString(R.string.conn_error_msg) +
                        " " + stage + " (error " + errorCode + ")";

        if (portFlags != 0) {
            dialogText += "\n\n" +
                    getResources().getString(R.string.check_ports_msg) +
                    "\n" +
                    MoonBridge.stringifyPortFlags(portFlags, "\n");
        }

        if (isBlockedPortTestResult(portTestResult)) {
            dialogText += "\n\n" +
                    getResources().getString(
                            R.string.nettest_text_blocked);
        }

        Dialog.displayDialog(
                this,
                getResources().getString(R.string.conn_error_title),
                dialogText,
                true);
    }

    private void handleConnectionTerminated(int errorCode) {
        int portFlags =
                MoonBridge.getPortFlagsFromTerminationErrorCode(errorCode);
        if (!canPresentSessionUi()) {
            return;
        }
        controllerHandler.stop();

        if (displayedFailureDialog) {
            return;
        }
        displayedFailureDialog = true;
        LimeLog.severe("Connection terminated: " + errorCode);
        stopConnection();

        if (errorCode ==
                MoonBridge.ML_ERROR_GRACEFUL_TERMINATION) {
            finish();
            return;
        }

        StreamFailureDiagnostics diagnostics =
                failureDiagnostics;
        if (diagnostics == null ||
                !diagnostics.request(
                        portFlags,
                        result ->
                                displayTerminationFailureDialog(
                                        errorCode,
                                        result.getPortFlags(),
                                        result.getProbeResultOr(
                                                MoonBridge
                                                        .ML_TEST_RESULT_INCONCLUSIVE)))) {
            displayTerminationFailureDialog(
                    errorCode,
                    portFlags,
                    MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
        }
    }

    private void displayTerminationFailureDialog(
            int errorCode,
            int portFlags,
            int portTestResult) {
        String message;
        if (isBlockedPortTestResult(portTestResult)) {
            message = getResources().getString(
                    R.string.nettest_text_blocked);
        }
        else {
            message = getTerminationErrorMessage(errorCode);
        }

        if (portFlags != 0) {
            message += "\n\n" +
                    getResources().getString(R.string.check_ports_msg) +
                    "\n" +
                    MoonBridge.stringifyPortFlags(portFlags, "\n");
        }

        Dialog.displayDialog(
                this,
                getResources().getString(
                        R.string.conn_terminated_title),
                message,
                true);
    }

    private String getTerminationErrorMessage(int errorCode) {
        switch (errorCode) {
            case MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC:
                return getResources().getString(
                        R.string.no_video_received_error);

            case MoonBridge.ML_ERROR_NO_VIDEO_FRAME:
                return getResources().getString(
                        R.string.no_frame_received_error);

            case MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION:
            case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
                return getResources().getString(
                        R.string.early_termination_error);

            case MoonBridge.ML_ERROR_FRAME_CONVERSION:
                return getResources().getString(
                        R.string.frame_conversion_error);

            default:
                String errorCodeString = Math.abs(errorCode) > 1000
                        ? Integer.toHexString(errorCode)
                        : Integer.toString(errorCode);
                return getResources().getString(
                        R.string.conn_terminated_msg) +
                        "\n\n" +
                        getResources().getString(
                                R.string.error_code_prefix) +
                        " " + errorCodeString;
        }
    }

    private static boolean isBlockedPortTestResult(
            int portTestResult) {
        return portTestResult !=
                MoonBridge.ML_TEST_RESULT_INCONCLUSIVE &&
                portTestResult != 0;
    }

    private boolean canPresentSessionUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void handleConnectionStatusUpdate(int connectionStatus) {
        if (streamUiSettingsState
                .get()
                .areConnectionWarningsDisabled()) {
            return;
        }

        if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
            if (streamDecoderSettings.getBitrateKbps() >
                    5000) {
                notificationOverlayView.setText(
                        getResources().getString(
                                R.string.slow_connection_msg));
            }
            else {
                notificationOverlayView.setText(
                        getResources().getString(
                                R.string.poor_connection_msg));
            }

            requestedNotificationOverlayVisibility = View.VISIBLE;
        }
        else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
            requestedNotificationOverlayVisibility = View.GONE;
        }

        if (!isHidingOverlays) {
            notificationOverlayView.setVisibility(
                    requestedNotificationOverlayVisibility);
        }
    }

    private void handleConnectionStarted() {
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }

        streamStartElapsedMs = SystemClock.elapsedRealtime();
        updatePipAutoEnter();

        sessionUiEffects.onConnected();
        if (launchReporter != null) {
            launchReporter.reportOnce();
        }

        hideSystemUi(1000);
    }

    private void handleStreamMessage(String message) {
        UiToast.makeText(
                Game.this,
                message,
                UiToast.LENGTH_LONG).show();
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
        if (fsrVideoProcessor != null) {
            fsrVideoProcessor.setHdrToneMappingEnabled(enabled);
        }
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (fsrEnabled && (fsrView == null || holder != fsrView.getHolder())) {
            return;
        }

        if (!surfaceCreated) {
            throw new IllegalStateException("Surface changed before creation!");
        }

        LimeLog.info(
                "surfaceChanged-->" + width + " x " + height +
                        "----" +
                        streamDecoderSettings.getWidth() + " x " +
                        streamDecoderSettings.getHeight());
        if (fsrEnabled) {
            return;
        }
        streamRenderSurfaceReady =
                holder.getSurface().isValid();
        startConnectionIfReady();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        float desiredFrameRate;

        if (fsrEnabled) {
            if (fsrView == null || holder != fsrView.getHolder()) {
                return;
            }
            fsrDisplaySurfaceCreated = true;
            startConnectionIfReady();
        }

        surfaceCreated = true;

        // Android will pick the lowest matching refresh rate for a given frame rate value, so we want
        // to report the true FPS value if refresh rate reduction is enabled. We also report the true
        // FPS value if there's no suitable matching refresh rate. In that case, Android could try to
        // select a lower refresh rate that avoids uneven pull-down (ex: 30 Hz for a 60 FPS stream on
        // a display that maxes out at 50 Hz).
        if (mayReduceRefreshRate() ||
                desiredRefreshRate <
                        streamDecoderSettings.getFps()) {
            desiredFrameRate =
                    streamDecoderSettings.getFps();
        }
        else {
            // Otherwise, we will pretend that our frame rate matches the refresh rate we picked in
            // prepareDisplayForRendering(). This will usually be the highest refresh rate that our
            // frame rate evenly divides into, which ensures the lowest possible display latency.
            desiredFrameRate = desiredRefreshRate;
        }

        // Tell the OS about our frame rate to allow it to adapt the display refresh rate appropriately
        if (shouldLetSystemManageRefreshRate()) {
            LimeLog.info("Skipping Surface.setFrameRate() and leaving refresh rate to the system");
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // We want to change frame rate even if it's not seamless, since prepareDisplayForRendering()
            // will not set the display mode on S+ if it only differs by the refresh rate. It depends
            // on us to trigger the frame rate switch here.
            holder.getSurface().setFrameRate(desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            holder.getSurface().setFrameRate(desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (fsrEnabled) {
            if (fsrView == null || holder != fsrView.getHolder()) {
                return;
            }
            fsrDisplaySurfaceCreated = false;
        }

        if (!surfaceCreated) {
            throw new IllegalStateException("Surface destroyed before creation!");
        }

        surfaceCreated = false;
        streamRenderSurfaceReady = false;

        if (hasSessionStarted()) {
            // Let the decoder know immediately that the surface is gone
            mediaResourceOwner.prepareVideoForStop();

            if (sessionController.getState().needsStop()) {
                stopConnection();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return;
        }

        awaitingRecordAudioPermission = false;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingMicToggleAfterPermission) {
                pendingMicToggleAfterPermission = false;
                switchMic();
            }
            return;
        }

        pendingMicToggleAfterPermission = false;

        UiToast.makeText(this, getResources().getString(R.string.mic_uplink_permission_denied), UiToast.LENGTH_LONG).show();
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
        usbPermissionPromptVisible = true;
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
        usbPermissionPromptVisible = false;
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
            if (updated.isFloatingControlEnabled()) {
                showFloatView();
            }
            else {
                hideFloatView();
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
                fsrEnabled,
                getFsrTargetDisplayName(),
                getFsrSharpnessDisplayName(),
                isFsrNativeHdrOutputEnabled(),
                conn != null && conn.isMicUplinkActive(),
                streamHost,
                streamStartElapsedMs,
                SystemClock.elapsedRealtime(),
                usbControllerActive,
                usbControllerType,
                connectedToUsbDriverService);
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
        if (isVirtualControllerVisible()) {
            virtualController.refreshLayout();
        }
        if (isVirtualKeysVisible()) {
            keyBoardController.refreshLayout();
        }
        if(keyBoardLayoutController!=null){
            keyBoardLayoutController.refreshLayout();
        }
    }

    //切换虚拟手柄模式
    public void switchVirtualController(KeyBoardController.ControllerMode mode){
        if (!isVirtualControllerVisible()) {
            UiToast.makeText(this,"请先打开虚拟手柄开关！",UiToast.LENGTH_SHORT).show();
            return;
        }
        virtualController.switchMode(mode);

    }
    //返回虚拟手柄当前的状态
    public KeyBoardController.ControllerMode getVirtualControllerMode(){
        if(virtualController==null){
            return KeyBoardController.ControllerMode.NONE;
        }
        return virtualController.getControllerMode();
    }

    //切换虚拟手柄模式
    public void switchVirtualKeyController(KeyBoardController.ControllerMode mode){
        if (!isVirtualKeysVisible()) {
            UiToast.makeText(this,"请先打开虚拟按键开关！",UiToast.LENGTH_SHORT).show();
            return;
        }
        keyBoardController.switchMode(mode);

    }
    //返回虚拟手柄当前的状态
    public KeyBoardController.ControllerMode getVirtualKeyControllerMode(){
        if(keyBoardController==null){
            return KeyBoardController.ControllerMode.NONE;
        }
        return keyBoardController.getControllerMode();
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
        return virtualController != null &&
                virtualController.isVisible();
    }

    @Override
    public boolean isVirtualKeysVisible() {
        return keyBoardController != null &&
                keyBoardController.isVisible();
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
        setDualSenseTrigger();
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

    private boolean isFsrEnabled() {
        return streamDisplaySettings.isFsrEnabled();
    }

    private StreamLayoutGeometry.Size getFsrOutputSize() {
        StreamDisplaySettings.FsrTarget target =
                streamDisplaySettings.getFsrTarget();
        return StreamLayoutGeometry.getEvenOutputSize(
                streamDisplaySettings.getStreamWidth(),
                streamDisplaySettings.getStreamHeight(),
                target.getOutputHeight(),
                target.getMinimumOutputWidth());
    }

    private boolean isFsrNativeHeightTarget() {
        return streamDisplaySettings.isNativeHeightFsrTarget();
    }

    private String getFsrTargetDisplayName() {
        switch (streamDisplaySettings.getFsrTarget()) {
            case OUTPUT_4K:
                return "4K";
            case OUTPUT_2K:
                return "2K";
            case NATIVE_HEIGHT:
                return getString(
                        R.string.fsr_target_native_height);
            case UNKNOWN:
            case OFF:
            default:
                return "关闭";
        }
    }

    private String getFsrSharpnessDisplayName() {
        switch (streamDisplaySettings.getFsrSharpness()) {
            case SOFT:
                return "柔和";
            case STRONG:
                return "强";
            case MAXIMUM:
                return "极强";
            case STANDARD:
            default:
                return "标准";
        }
    }

    private boolean isFsrNativeHdrOutputEnabled() {
        return streamDisplaySettings
                .isNativeHdrOutputEnabled();
    }

    private void configureFsrWindowColorMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !fsrEnabled) {
            return;
        }
        boolean nativeHdrOutput = isFsrNativeHdrOutputEnabled();
        getWindow().setColorMode(nativeHdrOutput
                ? ActivityInfo.COLOR_MODE_HDR
                : ActivityInfo.COLOR_MODE_DEFAULT);
        LimeLog.info("HDR validation: FSR window color mode="
                + (nativeHdrOutput ? "HDR (native output)" : "DEFAULT (software tone-map)"));
    }

    private void startConnectionIfReady() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::startConnectionIfReady);
            return;
        }
        if (!sessionDependenciesReady ||
                sessionController == null ||
                !sessionController.canStart()) {
            return;
        }

        if (fsrEnabled) {
            if (!fsrInputSurfaceReady ||
                    !fsrDisplaySurfaceCreated) {
                return;
            }
            Surface fsrInputSurface =
                    mediaResourceOwner.getFsrInputSurface();
            if (fsrInputSurface != null) {
                startSessionWithRenderTarget(fsrInputSurface);
            }
            return;
        }

        Surface renderTarget =
                streamView.getHolder().getSurface();
        if (streamRenderSurfaceReady &&
                renderTarget.isValid()) {
            startSessionWithRenderTarget(renderTarget);
        }
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
        return conn != null && conn.isMicUplinkActive();
    }

    //是否退出串流
    public boolean isQuitSteamingFlag;

    public void quitSteaming(){
        ServerHelper.doQuit(this,streamReqBean, null);
    }

    private AXFloatingView floatingView;
    private void initFloatingView(){
        floatingView = new AXFloatingView(this);
        floatingView.configurePosition(
                streamUiSettingsState.get(),
                (x, y, nearestLeft) -> {
                    if (streamUiSettingsState
                            .get()
                            .shouldRememberFloatingPosition()) {
                        applyStreamUiSettingsUpdate(
                                StreamUiSettingsUpdate
                                        .floatingPosition(
                                                x,
                                                y,
                                                nearestLeft));
                    }
                });
        floatingView.setIconImage(R.drawable.app_icon_axi);
        floatingView.setLayoutParams(AXFloatingView.getLayParams());
        ViewGroup decorViewGroup= (ViewGroup) getWindow().getDecorView();
        decorViewGroup.addView(floatingView);
        floatingView.setFloatingViewListener(new AXFloatingViewListener() {
            @Override
            public void onClick(AXFloatingMagnetView magnetView) {
                switch (streamUiSettingsState
                        .get()
                        .getFloatingAction()) {
                    case GAME_MENU:
                        showGameMenu(null);
                        break;
                    case SOFT_KEYBOARD:
                        toggleKeyboard();
                        break;
                    case FULL_KEYBOARD:
                        showHidekeyBoardLayoutController();
                        break;
                }

            }
        });
//        streamView.setZOrderOnTop(true);
//        streamView.setZOrderMediaOverlay(true);
    }

    public void switchFloatView(){
        if(floatingView==null){
            showFloatView();
            return;
        }
        if (floatingView.getVisibility() == View.VISIBLE) {
            hideFloatView();
        } else {
            showFloatView();
        }
    }

    public void showFloatView(){
        if(floatingView==null){
            initFloatingView();
        }
        floatingView.setVisibility(View.VISIBLE);
    }

    public void hideFloatView(){
        if(floatingView!=null){
            floatingView.setVisibility(View.GONE);
        }
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

    //设置ds5手柄的自适应扳机
    public void setDualSenseTrigger(){
        ControllerSettings settings =
                controllerSettingsState.get();
        controllerHandler.setDualSenseTrigger(
                settings.getAdaptiveTriggerMode(),
                settings.getAdaptiveTriggerStrength(),
                settings.getAdaptiveTriggerFrequency(),
                settings.getAdaptiveTriggerStartPosition(),
                settings.getAdaptiveTriggerEndPosition());
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

    public KeyBoardController getKeyBoardController(){
        return keyBoardController;
    }

    private static MoonBridge.AudioConfiguration
            toTransportAudioConfiguration(
                    StreamAudioSettings.ChannelConfiguration
                            configuration) {
        switch (configuration) {
            case SURROUND_7_1:
                return MoonBridge.AUDIO_CONFIGURATION_71_SURROUND;
            case SURROUND_5_1:
                return MoonBridge.AUDIO_CONFIGURATION_51_SURROUND;
            case STEREO:
            default:
                return MoonBridge.AUDIO_CONFIGURATION_STEREO;
        }
    }

    //开启关闭 麦克风
    public void switchMic(){
        if (conn == null || micToggleInFlight) {
            return;
        }

        MicrophoneUplinkState state = conn.getMicUplinkState();
        if (state == MicrophoneUplinkState.STARTING ||
                state == MicrophoneUplinkState.STOPPING) {
            return;
        }

        if (state == MicrophoneUplinkState.ON) {
            micToggleInFlight = true;
            final NvConnection currentConn = conn;
            new Thread(() -> {
                currentConn.stopMicUplink();
                String message = currentConn.getLastMicUplinkMessage();
                boolean stoppedCleanly =
                        currentConn.getMicUplinkState() !=
                                MicrophoneUplinkState.ERROR;
                runOnUiThread(() -> {
                    micToggleInFlight = false;
                    if (!stoppedCleanly &&
                            message != null && !message.isEmpty()) {
                        UiToast.makeText(this, message, UiToast.LENGTH_SHORT).show();
                    }
                });
            }, "MicToggle").start();
            return;
        }

        if (!conn.isMicUplinkSupported()) {
            UiToast.makeText(this, getResources().getString(R.string.mic_uplink_not_supported), UiToast.LENGTH_LONG).show();
            return;
        }

        if (!isRecordAudioPermissionGranted()) {
            if (awaitingRecordAudioPermission) {
                return;
            }

            pendingMicToggleAfterPermission = true;
            awaitingRecordAudioPermission = true;
            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        micToggleInFlight = true;
        final NvConnection currentConn = conn;
        new Thread(() -> {
            boolean started = currentConn.startMicUplink();
            String message = currentConn.getLastMicUplinkMessage();
            runOnUiThread(() -> {
                micToggleInFlight = false;

                if (!started &&
                        message != null && !message.isEmpty()) {
                    UiToast.makeText(this, message, UiToast.LENGTH_SHORT).show();
                }
            });
        }, "MicToggle").start();
    }

}
