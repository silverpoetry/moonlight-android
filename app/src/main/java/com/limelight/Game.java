package com.limelight;


import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.audio.mic.AndroidMicrophoneUplinkSessionFactory;
import com.limelight.binding.input.AndroidControllerInventory;
import com.limelight.binding.input.AndroidInputDeviceRegistration;
import com.limelight.binding.input.AndroidKeyboardInputHost;
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
import com.limelight.binding.input.StreamInputSuppressionHost;
import com.limelight.binding.input.protocol.NvConnectionPointerInputSink;
import com.limelight.binding.input.protocol.NvConnectionKeyboardInputSink;
import com.limelight.binding.input.capture.AndroidStreamInputCaptureController;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.driver.UsbDriverServiceEndpoint;
import com.limelight.binding.input.driver.UsbDriverSessionController;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.binding.input.pointer.ExternalPointerInputController;
import com.limelight.binding.input.touch.DirectContactInputController;
import com.limelight.binding.input.touch.TouchInputController;
import com.limelight.binding.input.touch.TouchInputMode;
import com.limelight.binding.input.virtual_controller.keyboard.AndroidVirtualControlsFactory;
import com.limelight.binding.input.virtual_controller.keyboard.StreamVirtualControlsController;
import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode;
import com.limelight.binding.video.AndroidDecoderCrashStore;
import com.limelight.binding.video.gl.android.SharedPreferencesGlDeviceSnapshotStore;
import com.limelight.binding.video.DecoderCrashTracker;
import com.limelight.computers.http.android.AndroidNvHttpClientFactory;
import com.limelight.computers.session.DeferredHostQuitController;
import com.limelight.computers.session.HostQuitUseCase;
import com.limelight.computers.session.NvHttpHostQuitBackend;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.StreamSessionController;
import com.limelight.computers.http.HostHttpTarget;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.mic.MicrophoneUplinkConfig;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidDisplayAspectProvider;
import com.limelight.settings.android.AndroidSettingsGroupObserver;
import com.limelight.settings.android.AndroidStreamSettingsBootstrap;
import com.limelight.settings.android.SharedPreferencesCustomResolutionRepository;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsLoader;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsLoader;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.input.InputSettingsLoader;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.runtime.StreamSettingsSession;
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
import com.limelight.settings.ui.GameMenuCardLayoutRepository;
import com.limelight.settings.ui.SettingsGameMenuCardLayoutRepository;
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
import com.limelight.virtualcontrols.action.VirtualControlAction;
import com.limelight.ui.gamemenu.GameMenuHost;
import com.limelight.ui.gamemenu.AndroidGameMenuController;
import com.limelight.ui.gamemenu.AndroidDeviceBatteryProvider;
import com.limelight.ui.gamemenu.GameMenuHostProvider;
import com.limelight.ui.gamemenu.GameMenuState;
import com.limelight.ui.gamemenu.StreamGameMenuHost;
import com.limelight.ui.clipboard.RemoteClipboardFileTransferController;
import com.limelight.ui.performance.PerformanceOverlayRuntimeState;
import com.limelight.ui.performance.PerformanceOverlayConfiguration;
import com.limelight.ui.performance.StreamPerformanceOverlayController;
import com.limelight.ui.stream.AndroidStreamConnectionMessages;
import com.limelight.ui.stream.AndroidStreamConnectingIndicator;
import com.limelight.ui.stream.AndroidStreamConnectionWarningPresenter;
import com.limelight.ui.stream.AndroidStreamDisplayController;
import com.limelight.ui.stream.AndroidExternalDisplayController;
import com.limelight.ui.stream.AndroidStreamFailureDiagnosticsFactory;
import com.limelight.ui.stream.AndroidStreamHdrCapabilityProvider;
import com.limelight.ui.stream.AndroidStreamHdrModeController;
import com.limelight.ui.stream.AndroidStreamLaunchReporterFactory;
import com.limelight.ui.stream.AndroidStreamMediaRuntimeFactory;
import com.limelight.ui.stream.AndroidStreamMicrophoneControllerFactory;
import com.limelight.ui.stream.AndroidStreamNativeCursorController;
import com.limelight.ui.stream.AndroidStreamOverlayVisibilityHost;
import com.limelight.ui.stream.AndroidStreamPictureInPictureController;
import com.limelight.ui.stream.AndroidStreamSessionUiEffectsHost;
import com.limelight.ui.stream.AndroidStreamSessionPresentationHost;
import com.limelight.ui.stream.AndroidStreamSystemUiController;
import com.limelight.ui.stream.StreamControllerFeedbackHost;
import com.limelight.ui.stream.StreamDecoderCapabilities;
import com.limelight.ui.stream.StreamDisplayRefreshPolicy;
import com.limelight.ui.stream.StreamHdrRequestPolicy;
import com.limelight.ui.stream.StreamLaunchReporter;
import com.limelight.ui.stream.StreamMediaResourceOwner;
import com.limelight.ui.stream.StreamMicrophoneController;
import com.limelight.ui.stream.StreamOverlayVisibilityController;
import com.limelight.ui.stream.StreamRenderSurfaceController;
import com.limelight.ui.stream.StreamRenderSessionHost;
import com.limelight.ui.stream.StreamSessionCallbackRouter;
import com.limelight.ui.stream.StreamSessionConfigurationAdapter;
import com.limelight.ui.stream.StreamSessionConfigurationPlanner;
import com.limelight.ui.stream.StreamSessionPresentationController;
import com.limelight.ui.stream.StreamSessionUiEffects;
import com.limelight.ui.stream.StreamWifiLockController;
import com.limelight.ui.GameGestures;
import com.limelight.ui.StreamWindowPolicy;
import com.limelight.ui.StreamUiActions;
import com.limelight.ui.StreamView;
import com.limelight.ui.floatingview.StreamFloatingControlController;
import com.limelight.ui.hosts.HostQuitMessageResolver;
import com.limelight.stream.launch.PendingStreamReconnect;
import com.limelight.stream.launch.PendingStreamReconnectStore;
import com.limelight.stream.launch.android.AndroidPendingStreamReconnectMapper;
import com.limelight.stream.launch.android.AndroidStreamLaunchContract;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.Dialog;
import com.limelight.utils.RazerUtils;
import com.limelight.utils.StreamOrientationController;
import com.limelight.utils.StreamOrientationRequest;
import com.limelight.utils.UiHelper;
import android.annotation.SuppressLint;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
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
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.limelight.utils.UiToast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;


public class Game extends BaseActivity implements OnGenericMotionListener,
        OnTouchListener, EvdevListener,
        GameGestures, StreamInputGateway,
        StreamUiActions, GameMenuHostProvider,
        StreamGameMenuHost.Actions,
        UsbDriverService.UsbDriverStateListener, View.OnKeyListener {
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
    private StreamSettingsSession streamSettingsSession;
    private AndroidSettingsGroupObserver forcePressSettingsObserver;
    private GameMenuCardLayoutRepository
            gameMenuCardLayoutRepository;
    private GameMenuShortcutRepository
            gameMenuShortcutRepository;
    private AndroidGameMenuController gameMenuController;
    private AndroidDeviceBatteryProvider deviceBatteryProvider;
    private GameMenuHost gameMenuHost;
    private DecoderCrashTracker decoderCrashTracker;

    private NvConnection conn;
    private StreamSessionController sessionController;
    private StreamSessionCallbackRouter sessionCallbackRouter;
    private StreamSessionPresentationController
            sessionPresentationController;
    private StreamLaunchReporter launchReporter;
    private StreamSessionUiEffects sessionUiEffects;
    private AndroidStreamSystemUiController systemUiController;
    private StreamMicrophoneController microphoneController;
    private AndroidStreamConnectingIndicator connectingIndicator;
    private RemoteClipboardFileTransferController
            clipboardFileTransferController;
    private final ActivityResultLauncher<Intent>
            clipboardDirectoryLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> handleClipboardDirectoryResult(
                            result.getResultCode(),
                            result.getData()));
    private AndroidStreamPictureInPictureController
            pictureInPictureController;
    private String pcName;
    private String appName;
    private String streamHost;
    private long streamStartElapsedMs;
    private NvApp app;
    private volatile boolean sessionDependenciesReady;

    private AndroidStreamInputCaptureController
            inputCaptureController;
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
                0);
    };
    private AndroidStreamNativeCursorController nativeCursorController;

    private TextView notificationOverlayView;
    private StreamOverlayVisibilityController
            overlayVisibilityController;
    private StreamPerformanceOverlayController
            performanceOverlayController;
    private StreamFloatingControlController
            floatingControlController;

    private StreamMediaResourceOwner mediaResourceOwner;
    private AndroidStreamHdrModeController hdrModeController;
    private StreamRenderSurfaceController
            renderSurfaceController;
    private AndroidExternalDisplayController
            externalDisplayController;

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

    private ViewParent rootView;

    private HostHttpTarget streamHttpTarget;
    private HostQuitUseCase.Backend pendingHostQuitBackend;
    private boolean hostQuitRequested;
    private ConnectivityManager connManager;

    private BackNavigationRegistration backNavigationRegistration;
    private StreamInputGatewayRegistry.Registration inputGatewayRegistration;
    private boolean showSoftKeyboardWhenFocused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        systemUiController = new AndroidStreamSystemUiController(
                this,
                this::isSessionConnected);
        systemUiController.attachInitialLayout();
        addOnMultiWindowModeChangedListener(
                info -> handleMultiWindowModeChanged(
                        info.isInMultiWindowMode()));

        // Change volume button behavior
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Inflate the content
        setContentView(R.layout.activity_game);

        connManager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        connectingIndicator =
                new AndroidStreamConnectingIndicator(this);

        settingsRepository = AndroidSettingsRepository.create(this);
        AndroidStreamSettingsBootstrap.prepare(
                settingsRepository);
        gameMenuCardLayoutRepository =
                new SettingsGameMenuCardLayoutRepository(
                        settingsRepository);
        gameMenuShortcutRepository =
                new SharedPreferencesGameMenuShortcutRepository(
                        this);
        gameMenuController = new AndroidGameMenuController(this);
        deviceBatteryProvider = new AndroidDeviceBatteryProvider(this);
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
                        this);
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
        streamSettingsSession = createStreamSettingsSession();
        forcePressSettingsObserver = new AndroidSettingsGroupObserver(
                this,
                Arrays.asList(
                        InputSettingKeys.BAROMETER_FORCE_PRESS,
                        InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD,
                        InputSettingKeys
                                .BAROMETER_FORCE_PRESS_MINIMUM_DURATION),
                streamSettingsSession::refreshForcePressSettings);
        forcePressSettingsObserver.start();
        gameMenuHost = new StreamGameMenuHost(
                streamSettingsSession,
                customResolutionRepository,
                gameMenuCardLayoutRepository,
                gameMenuShortcutRepository,
                gameMenuController,
                this);
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
                        AndroidStreamDisplayController
                                .matchesPhysicalDisplayMode(
                                        this,
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
        nativeCursorController =
                new AndroidStreamNativeCursorController(
                        this,
                        streamView,
                        rootView,
                        inputSettingsState.get()
                                .isAbsoluteMouseMode(),
                        streamDecoderSettings.getWidth(),
                        streamDecoderSettings.getHeight());

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

        inputCaptureController =
                AndroidStreamInputCaptureController.create(
                        this,
                        this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            streamView.setOnCapturedPointerListener(
                    this::handleMotionEvent);
        }

        // Warn the user if they're on a metered connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr.isActiveNetworkMetered()) {
            displayTransientMessage(getResources().getString(R.string.conn_metered));
        }

        wifiLockController =
                StreamWifiLockController.create(this);
        wifiLockController.acquire();

        appName = Game.this.getIntent().getStringExtra(
                AndroidStreamLaunchContract.EXTRA_APP_NAME);
        pcName = Game.this.getIntent().getStringExtra(
                AndroidStreamLaunchContract.EXTRA_HOST_NAME);

        String host = Game.this.getIntent().getStringExtra(
                AndroidStreamLaunchContract.EXTRA_HOST);
        streamHost = host;
        int port = Game.this.getIntent().getIntExtra(
                AndroidStreamLaunchContract.EXTRA_PORT,
                NvHTTP.DEFAULT_HTTP_PORT);
        int httpsPort = Game.this.getIntent().getIntExtra(
                AndroidStreamLaunchContract.EXTRA_HTTPS_PORT,
                0); // 0 is treated as unknown
        int appId = Game.this.getIntent().getIntExtra(
                AndroidStreamLaunchContract.EXTRA_APP_ID,
                StreamConfiguration.INVALID_APP_ID);
        String uniqueId = Game.this.getIntent().getStringExtra(
                AndroidStreamLaunchContract.EXTRA_UNIQUE_ID);
        boolean appSupportsHdr = Game.this.getIntent().getBooleanExtra(
                AndroidStreamLaunchContract.EXTRA_APP_HDR,
                false);
        byte[] derCertData = Game.this.getIntent().getByteArrayExtra(
                AndroidStreamLaunchContract.EXTRA_SERVER_CERTIFICATE);

        app = new NvApp(appName != null ? appName : "app", appId, appSupportsHdr);

        X509Certificate serverCert = null;
        try {
            if (derCertData != null) {
                serverCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(derCertData));
            }
        } catch (CertificateException e) {
            LimeLog.warning(
                    "Unable to parse the pinned host certificate",
                    e);
        }

        if (appId == StreamConfiguration.INVALID_APP_ID) {
            finish();
            return;
        }

        pictureInPictureController =
                new AndroidStreamPictureInPictureController(
                        this,
                        streamView,
                        streamDecoderSettings.getWidth(),
                        streamDecoderSettings.getHeight(),
                        streamUiSettingsState
                                .get()
                                .isPictureInPictureEnabled(),
                        appName,
                        pcName);

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
                        new SharedPreferencesGlDeviceSnapshotStore(
                                this).read(),
                        streamDecoderSettings,
                        decoderCrashTracker,
                        hdrRequested,
                        performanceOverlayController,
                        () -> new AndroidAudioRenderer(
                                Game.this,
                                controllerHandler,
                                streamAudioSettingsState));
        mediaResourceOwner = mediaRuntime.getResourceOwner();
        hdrModeController = new AndroidStreamHdrModeController(
                this,
                mediaResourceOwner,
                this::isHdrHighBrightnessEnabled);
        StreamDecoderCapabilities decoderCapabilities =
                mediaRuntime.getDecoderCapabilities();

        ControllerSettings controllerSettings =
                controllerSettingsState.get();
        int discoveredGamepadMask =
                AndroidControllerInventory.from(this)
                        .getInitialControllerMask(
                                controllerSettings);

        AndroidStreamDisplayController displayController =
                new AndroidStreamDisplayController(
                        this,
                        streamView,
                        streamDecoderSettings,
                        streamDisplaySettings,
                        streamVideoSettings);
        AndroidStreamDisplayController.Preparation
                displayPreparation =
                displayController.prepare(effectiveFramePacing);
        float displayRefreshRate = displayPreparation
                .getEffectiveDisplayRefreshRate();
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

        streamHttpTarget = new HostHttpTarget(
                host,
                port,
                httpsPort,
                uniqueId,
                serverCert);
        // Initialize the connection
        conn = new NvConnection(getApplicationContext(),
                host,
                port,
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
                        inputCaptureController.getProvider(),
                        directContactInputController,
                        inputSettingsState);
        clipboardFileTransferController =
                new RemoteClipboardFileTransferController(
                        this,
                        conn,
                        settingsRepository,
                        clipboardDirectoryLauncher::launch);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        microphoneController =
                AndroidStreamMicrophoneControllerFactory.create(
                        this,
                        conn,
                        gameMenuController::refreshMicrophoneState,
                        mainHandler::post);
        StreamSessionPresentationController.Diagnostics
                failureDiagnostics =
                AndroidStreamFailureDiagnosticsFactory.create(
                        mainHandler);
        launchReporter = AndroidStreamLaunchReporterFactory.create(
                this,
                pcName,
                getIntent().getStringExtra(
                        AndroidStreamLaunchContract.EXTRA_HOST_ID),
                app,
                appName != null);
        sessionUiEffects = new StreamSessionUiEffects(
                new AndroidStreamSessionUiEffectsHost(
                        this,
                        inputCaptureController,
                        this::isGameModeIntegrationDisabled),
                mainHandler);
        TouchInputController touchInputController =
                new TouchInputController(
                        this,
                        streamView,
                        pointerInputSink,
                        directContactInputController,
                        inputSettingsState,
                        this::showKeyboard);
        if (inputSettingsState.get().isAbsoluteMouseMode()) {
            conn.setMousePositionListener(
                    nativeCursorController
                            ::updatePositionFromReference);
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
                new AndroidKeyboardInputHost(
                        this,
                        inputCaptureController,
                        this::cancelPendingStreamBackExit,
                        toggleGrab,
                        this::switchMouseLocalCursor));
        streamInputController = new StreamInputController(
                controllerHandler,
                externalPointerInputController,
                touchInputController,
                inputSettingsState,
                new StreamInputSuppressionHost(
                        this::isTouchscreenInputSuppressed));
        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        AndroidInputDeviceRegistration keyboardRegistration =
                AndroidInputDeviceRegistration.register(
                        inputManager,
                        keyboardInputController);
        inputLifecycleController =
                new StreamInputLifecycleController(
                        streamInputController,
                        controllerHandler,
                        keyboardRegistration);

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

        overlayVisibilityController =
                new StreamOverlayVisibilityController(
                        new AndroidStreamOverlayVisibilityHost(
                                this,
                                virtualControlsController,
                                performanceOverlayController,
                                notificationOverlayView,
                                controllerHandler,
                                this::isGameModeIntegrationDisabled));

        AndroidStreamConnectionWarningPresenter warningPresenter =
                new AndroidStreamConnectionWarningPresenter(
                        this,
                        notificationOverlayView,
                        overlayVisibilityController);
        sessionPresentationController =
                new StreamSessionPresentationController(
                        new AndroidStreamSessionPresentationHost(
                                this,
                                streamView,
                                controllerHandler,
                                streamUiSettingsState,
                                streamDecoderSettings,
                                connectingIndicator,
                                warningPresenter,
                                hdrModeController,
                                nativeCursorController,
                                this::stopConnection,
                                this::onSessionConnected),
                        failureDiagnostics,
                        AndroidStreamConnectionMessages.create(this),
                        MoonBridge::getPortFlagsFromTerminationErrorCode,
                        MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
        sessionCallbackRouter = new StreamSessionCallbackRouter(
                sessionPresentationController,
                new StreamControllerFeedbackHost(
                        controllerHandler,
                        controllerSettingsState,
                        performanceOverlayController),
                mainHandler);
        sessionController = new StreamSessionController(
                conn,
                sessionCallbackRouter);

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
            connectingIndicator.dismiss();

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
                        displayPreparation
                                .getSelectedDisplayRefreshRate(),
                        StreamDisplayRefreshPolicy
                                .mayReduceRefreshRate(
                                        effectiveFramePacing,
                                        streamDecoderSettings
                                                .isRefreshRateReductionEnabled()),
                        displayPreparation
                                .isSystemManagedRefreshRate(),
                        new StreamRenderSessionHost(
                                sessionController,
                                mediaResourceOwner,
                                sessionUiEffects,
                                () -> sessionDependenciesReady,
                                this::stopConnection));
        renderSurfaceController.bind();

        //外接显示器模式
        if (streamDisplaySettings.isExternalDisplayEnabled()) {
            externalDisplayController =
                    new AndroidExternalDisplayController(
                            this,
                            streamView,
                            (ViewGroup) rootView);
            externalDisplayController
                    .showOnFirstSecondaryDisplay();
        }

        //强制体感
        setMotionForceGyro();

        //光标是否显示
        if (!inputCaptureController.isLocalCursorVisible() &&
                inputSettingsState
                        .get()
                        .isLocalSystemCursorEnabled()) {
            switchMouseLocalCursor();
        }
        sessionDependenciesReady = true;
        renderSurfaceController.startIfReady();
    }

    @Override
    protected boolean shouldEnableEdgeToEdge() {
        return false;
    }

    private StreamSettingsSession createStreamSettingsSession() {
        return new StreamSettingsSession(
                settingsRepository,
                inputSettingsState,
                controllerSettingsState,
                streamAudioSettingsState,
                streamVideoSettingsState,
                streamUiSettingsState,
                virtualControlSettingsState,
                new StreamSettingsSession.Effects() {
                    @Override
                    public void onInputSettingsChanged(
                            InputSettings previous,
                            InputSettings current) {
                        if (streamInputController != null) {
                            streamInputController
                                    .onInputSettingsChanged(
                                            previous,
                                            current);
                        }
                    }

                    @Override
                    public void onBatteryReportingChanged() {
                        if (controllerHandler != null) {
                            controllerHandler
                                    .refreshBatteryReportingState();
                        }
                    }

                    @Override
                    public void onForceGyroEnabled() {
                        setMotionForceGyro();
                    }

                    @Override
                    public void onAudioSettingsChanged(
                            StreamAudioSettings settings) {
                        applyStreamAudioSettingsEffects(settings);
                    }

                    @Override
                    public void onUiSettingsChanged(
                            StreamUiSettings previous,
                            StreamUiSettings current) {
                        applyStreamUiSettingsEffects(previous, current);
                    }

                    @Override
                    public void onVirtualControlSettingsReloaded() {
                        if (virtualControlsController != null) {
                            virtualControlsController
                                    .refreshCreatedLayouts();
                        }
                    }
                });
    }

    //显示隐藏虚拟特殊按键
    @Override
    public void toggleVirtualKeys(){
        if (virtualControlsController != null) {
            virtualControlsController.toggleVirtualKeys();
        }
    }

    @Override
    public void toggleFullKeyboard(){
        if (virtualControlsController != null) {
            virtualControlsController.toggleFullKeyboard();
        }
    }

    //显示隐藏虚拟手柄控制器
    @Override
    public void toggleVirtualGamepad(){
        if (virtualControlsController != null) {
            virtualControlsController.toggleVirtualGamepad();
        }
    }

    @Override
    public void performStreamUiAction(VirtualControlAction action) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                overlayVisibilityController != null) {
            overlayVisibilityController
                    .onPictureInPictureModeChanged(
                            isInPictureInPictureMode());
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (pictureInPictureController != null) {
            pictureInPictureController.onUserLeaveHint();
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        if (pictureInPictureController != null) {
            return pictureInPictureController
                    .onPictureInPictureRequested();
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
        if (inputCaptureController != null) {
            inputCaptureController.onWindowFocusChanged(hasFocus);
        }
        if (conn != null) {
            conn.onWindowFocusChanged(hasFocus);
        }
        if (systemUiController != null) {
            systemUiController.onWindowFocusChanged(hasFocus);
        }
        if (hasFocus && showSoftKeyboardWhenFocused) {
            showSoftKeyboardWhenFocused = false;
            showKeyboard();
        }
    }

    private void cancelPendingUiCallbacks() {
        View decorView = getWindow().getDecorView();
        Handler handler = decorView.getHandler();
        if (handler != null) {
            handler.removeCallbacks(toggleGrab);
        }
        if (streamView != null) {
            streamView.removeCallbacks(toggleKeyboardWhenFocused);
            streamView.removeCallbacks(showSoftKeyboardRetry);
        }
    }

    private void handleMultiWindowModeChanged(
            boolean isInMultiWindowMode) {
        setPreferredOrientationForCurrentDisplay();

        // In multi-window, we don't want to use the full-screen layout
        // flag. It will cause us to collide with the system UI.
        // This function will also be called for PiP so we can cover
        // that case here too.
        if (isInMultiWindowMode) {
            mediaResourceOwner.notifyVideoBackground();
        }
        else {
            mediaResourceOwner.notifyVideoForeground();
        }

        systemUiController.onMultiWindowModeChanged(
                isInMultiWindowMode);
        UiHelper.refreshStreamWindowInsets(this);
    }

    @Override
    protected void onDestroy() {
        sessionDependenciesReady = false;
        if (forcePressSettingsObserver != null) {
            forcePressSettingsObserver.close();
            forcePressSettingsObserver = null;
        }
        unregisterInputGateway();
        cancelPendingUiCallbacks();
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
        if (systemUiController != null) {
            systemUiController.destroy();
            systemUiController = null;
        }
        if (connectingIndicator != null) {
            connectingIndicator.destroy();
            connectingIndicator = null;
        }
        if (nativeCursorController != null) {
            nativeCursorController.destroy();
            nativeCursorController = null;
        }
        if (pictureInPictureController != null) {
            pictureInPictureController.destroy();
            pictureInPictureController = null;
        }
        if (overlayVisibilityController != null) {
            overlayVisibilityController.destroy();
            overlayVisibilityController = null;
        }
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
        gameMenuHost = null;
        if (gameMenuController != null) {
            gameMenuController.destroy();
            gameMenuController = null;
        }
        if (inputLifecycleController != null) {
            inputLifecycleController.detachRouting();
            streamInputController = null;
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
        if (hdrModeController != null) {
            hdrModeController.clearWindowState();
            hdrModeController = null;
        }

        if (externalDisplayController != null) {
            externalDisplayController.destroy();
            externalDisplayController = null;
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

        if (inputCaptureController != null) {
            inputCaptureController.destroy();
            inputCaptureController = null;
        }
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
            if (inputCaptureController != null) {
                inputCaptureController.setInputGrabbed(false);
            }
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        showSoftKeyboardWhenFocused = false;
        unregisterInputGateway();
        super.onStop();

        if (connectingIndicator != null) {
            connectingIndicator.dismiss();
        }
        Dialog.closeDialogs();

        if (virtualControlsController != null) {
            virtualControlsController.hideAll();
        }

        if (gameMenuController != null) {
            gameMenuController.dismiss();
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
            if (hostQuitRequested) {
                HostQuitUseCase.Backend backend =
                        pendingHostQuitBackend;
                pendingHostQuitBackend = null;
                hostQuitRequested = false;
                if (backend != null) {
                    scheduleDeferredHostQuit(
                            backend,
                            appName);
                }
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
                PendingStreamReconnect reconnect =
                        AndroidPendingStreamReconnectMapper.fromIntent(
                                getIntent());
                if (reconnect != null) {
                    getPendingStreamReconnectStore().save(reconnect);
                }
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
        getPendingStreamReconnectStore().clear();
        super.finish();
        if (streamVideoSettingsState != null &&
                streamVideoSettingsState
                        .get()
                        .getScreenOnPolicy() ==
                        StreamVideoSettings
                                .ScreenOnPolicy
                                .CURRENT_SESSION) {
            streamSettingsSession.applyVideo(
                    StreamVideoSettingsUpdate.screenOnPolicy(
                            StreamVideoSettings
                                    .ScreenOnPolicy
                                    .DISABLED));
        }
    }

    private boolean isAutoLink=false;

    private PendingStreamReconnectStore
            getPendingStreamReconnectStore() {
        return ((MoonlightApplication) getApplication())
                .getPendingStreamReconnectStore();
    }

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

    private final Runnable toggleGrab = new Runnable() {
        @Override
        public void run() {
            if (inputCaptureController != null) {
                inputCaptureController.toggleInputGrabbed();
            }
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
        if (!isInputReady() ||
                !inputCaptureController.isInputGrabbed() ||
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
        if (!isInputReady() ||
                !inputCaptureController.isInputGrabbed() ||
                count <= 0) {
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
                0)) {
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
        if (inputCaptureController == null ||
                !inputCaptureController.isInputGrabbed() ||
                streamInputController == null) {
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
            hdrModeController.clearWindowState();
            if (pictureInPictureController != null) {
                pictureInPictureController
                        .setSessionConnected(false);
            }
            mediaResourceOwner.releaseStartResources();

            controllerHandler.stop();
            sessionUiEffects.onEnded();
        }
    }

    private boolean canPresentSessionUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void onSessionConnected() {
        streamStartElapsedMs = SystemClock.elapsedRealtime();
        if (pictureInPictureController != null) {
            pictureInPictureController.setSessionConnected(true);
        }

        sessionUiEffects.onConnected();
        if (launchReporter != null) {
            launchReporter.reportOnce();
        }

        systemUiController.scheduleImmersiveMode(1_000L);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        AndroidStreamMicrophoneControllerFactory
                .handlePermissionResult(
                        microphoneController,
                        requestCode,
                        grantResults);
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
    public void onUsbPermissionPromptStarting() {
        connectingIndicator.setFinishOnCancelEnabled(false);
        if (pictureInPictureController != null) {
            pictureInPictureController
                    .acquireAutoEnterSuppression();
        }
    }

    @Override
    public void onUsbPermissionPromptCompleted() {
        connectingIndicator.setFinishOnCancelEnabled(true);
        if (pictureInPictureController != null) {
            pictureInPictureController
                    .releaseAutoEnterSuppression();
        }
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == actionMultiple()) {
            return keyboardInputController
                    .handleKeyMultiple(keyEvent);
        }
        switch (keyEvent.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return keyboardInputController.handleKeyDown(keyEvent);
            case KeyEvent.ACTION_UP:
                return keyboardInputController.handleKeyUp(keyEvent);
            default:
                return false;
        }
    }

    /** ACTION_MULTIPLE remains the only carrier for some composed IME text. */
    @SuppressWarnings("deprecation")
    private static int actionMultiple() {
        return KeyEvent.ACTION_MULTIPLE;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        return event != null && isInputReady() &&
                onKey(null, event.getKeyCode(), event);
    }

    private static final long BACK_EXIT_INTERVAL_MS = 2000;
    private long lastBackPressedElapsedMs;

    @Override
    public void cancelPendingStreamBackExit() {
        lastBackPressedElapsedMs = 0;
    }

    public void handleStreamBackPressed() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBackPressedElapsedMs <= BACK_EXIT_INTERVAL_MS) {
            if (gameMenuController != null) {
                gameMenuController.dismiss();
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
                (gameMenuController == null ||
                        !gameMenuController.isVisible())) {
            showGameMenu(null);
        }
    }

    public void switchMouseModel(){
        String[] strings=getResources().getStringArray(R.array.mouse_mode_names);
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
    @Override
    public void switchMouseLocalCursor(){
        if (inputCaptureController != null) {
            inputCaptureController.toggleLocalCursorVisibility();
        }
    }

    @Override
    public void switchMouseModel(int which){
        TouchInputMode mode = TouchInputMode.fromPreferenceValue(which);
        if (mode != null && streamInputController != null) {
            streamInputController.setTouchMode(mode);
        }
    }

    @Override
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

    private void applyStreamAudioSettingsEffects(
            StreamAudioSettings updated) {
        if (mediaResourceOwner != null) {
            mediaResourceOwner.updateAudioSettings(updated);
        }
        if (controllerHandler != null) {
            controllerHandler.refreshAudioHapticsState();
        }
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

    private void applyStreamUiSettingsEffects(
            StreamUiSettings previous,
            StreamUiSettings updated) {
        if (previous.isPictureInPictureEnabled() !=
                updated.isPictureInPictureEnabled() &&
                pictureInPictureController != null) {
            pictureInPictureController.setEnabled(
                    updated.isPictureInPictureEnabled());
        }
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

    @Override
    public void showHUD(){
        if (performanceOverlayController != null) {
            performanceOverlayController.toggleVisibility();
        }
    }

    @Override
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

    //切换虚拟手柄模式
    @Override
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
    @Override
    public VirtualControlEditMode getVirtualGamepadEditMode(){
        return virtualControlsController == null
                ? VirtualControlEditMode.NONE
                : virtualControlsController.getVirtualGamepadMode();
    }

    //切换虚拟手柄模式
    @Override
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
    @Override
    public VirtualControlEditMode getVirtualKeysEditMode(){
        return virtualControlsController == null
                ? VirtualControlEditMode.NONE
                : virtualControlsController.getVirtualKeysMode();
    }


    public boolean isPortrait;

    //横竖屏切换
    @Override
    public void switchLandscapePortraitScreen(){
        isPortrait = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        setPreferredOrientationForCurrentDisplay();
    }

    //画面平移缩放
    @Override
    public void screenMoveZoom(){
        if(!streamView.isEnableZoomAndPan()){
            streamInputController.setTouchInputSuspended(true);
            streamView.setEnableZoomAndPan(true);
            return;
        }
        streamInputController.setTouchInputSuspended(false);
        streamView.setEnableZoomAndPan(false);
    }

    @Override
    public boolean getScreenMoveZoom(){
        return streamView.isEnableZoomAndPan();
    }

    public void disconnect() {
        finish();
    }

    @Override
    public void showGameMenu(GameInputDevice device) {
        if (gameMenuController != null) {
            gameMenuController.show(device);
        }
    }

    @Override
    public GameMenuHost getGameMenuHost() {
        return gameMenuHost;
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
    public int getDeviceBatteryPercent() {
        return deviceBatteryProvider == null ?
                GameMenuState.UNKNOWN_BATTERY_PERCENT :
                deviceBatteryProvider.getBatteryPercent();
    }

    @Override
    public void requestStreamDisconnect() {
        finish();
    }

    @Override
    public void requestStreamQuit() {
        if (hostQuitRequested) {
            UiToast.makeText(
                    this,
                    R.string.host_operation_in_progress,
                    UiToast.LENGTH_SHORT).show();
            return;
        }
        if (streamHttpTarget == null) {
            UiToast.makeText(
                    this,
                    R.string.host_operation_unavailable,
                    UiToast.LENGTH_LONG).show();
            disconnect();
            return;
        }
        try {
            pendingHostQuitBackend =
                    new NvHttpHostQuitBackend(
                            AndroidNvHttpClientFactory.create(
                                    getApplicationContext(),
                                    streamHttpTarget));
            hostQuitRequested = true;
            UiToast.makeText(
                    this,
                    getText(R.string.applist_quit_app) + " " +
                            appName + "...",
                    UiToast.LENGTH_SHORT).show();
        }
        catch (IOException failure) {
            pendingHostQuitBackend = null;
            hostQuitRequested = false;
            UiToast.makeText(
                    this,
                    HostQuitMessageResolver.resolveFailure(
                            this,
                            appName,
                            failure),
                    UiToast.LENGTH_LONG).show();
        }
        disconnect();
    }

    private void scheduleDeferredHostQuit(
            HostQuitUseCase.Backend backend,
            String appName) {
        Context applicationContext = getApplicationContext();
        DeferredHostQuitController controller =
                DeferredHostQuitController.create();
        DeferredHostQuitController.RequestStatus status =
                controller.request(
                        backend,
                        200L,
                        result -> {
                            CharSequence message = result.isSuccessful()
                                    ? HostQuitMessageResolver.resolve(
                                            applicationContext,
                                            appName,
                                            result.getOutcome())
                                    : HostQuitMessageResolver.resolveFailure(
                                            applicationContext,
                                            appName,
                                            result.getFailure());
                            UiToast.makeText(
                                    applicationContext,
                                    message,
                                    UiToast.LENGTH_LONG).show();
                        });
        if (status != DeferredHostQuitController.RequestStatus.ACCEPTED) {
            UiToast.makeText(
                    applicationContext,
                    R.string.host_operation_unavailable,
                    UiToast.LENGTH_LONG).show();
        }
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

    public boolean isSessionConnected() {
        return sessionController != null &&
                sessionController.getState().isStreaming();
    }

    @Override
    public boolean isInputReady() {
        return isSessionConnected();
    }

    @Override
    public boolean isMicUplinkActive() {
        return microphoneController != null &&
                microphoneController.isActive();
    }

    private StreamFloatingControlController
            createFloatingControlController() {
        return new StreamFloatingControlController(
                this,
                (ViewGroup) getWindow().getDecorView(),
                streamUiSettingsState,
                (x, y, nearestLeft) ->
                        streamSettingsSession.applyUi(
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
                                    VirtualControlAction
                                            .TOGGLE_FULL_KEYBOARD);
                            break;
                    }
                });
    }

    @Override
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

    @Override
    public void pullRemoteClipboardFiles() {
        if (clipboardFileTransferController != null) {
            clipboardFileTransferController.pullRemoteFiles();
        }
    }

    void handleClipboardDirectoryResult(int resultCode, Intent data) {
        if (clipboardFileTransferController != null) {
            clipboardFileTransferController.handleDirectoryResult(
                    resultCode,
                    data);
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

}
