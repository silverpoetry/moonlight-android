package com.limelight;


import android.Manifest;
import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.PointerInputCompat;
import com.limelight.binding.input.PointerInputSink;
import com.limelight.binding.input.KeyboardChordSender;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.StreamInputGateway;
import com.limelight.binding.input.StreamInputGatewayRegistry;
import com.limelight.binding.input.protocol.NvConnectionPointerInputSink;
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
import com.limelight.nvstream.MicUplinkConnection;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.StreamSessionController;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.gamemenu.GameMenuFragment;
import com.limelight.ui.gamemenu.GameMenuHost;
import com.limelight.ui.gamemenu.GameMenuSession;
import com.limelight.ui.clipboard.RemoteClipboardFileTransferController;
import com.limelight.ui.performance.PerformanceOverlayRuntimeState;
import com.limelight.ui.performance.StreamPerformanceOverlayController;
import com.limelight.ui.GameGestures;
import com.limelight.ui.NativeCursorOverlayView;
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
import com.limelight.utils.UiHelper;
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
import android.net.wifi.WifiManager;
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
import android.view.KeyCharacterMap;
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
        OnGenericMotionListener, OnTouchListener, NvConnectionListener, EvdevListener,
        OnSystemUiVisibilityChangeListener, GameGestures, StreamInputGateway,
        StreamUiActions, GameMenuHost,
        UsbDriverService.UsbDriverStateListener, View.OnKeyListener {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;
    private static final long KEY_CHORD_UP_DELAY_MS = 25;

    private TouchInputController touchInputController;
    private ExternalPointerInputController
            externalPointerInputController;

    private static final int SOFT_KEYBOARD_SHOW_RETRY_MS = 50;

    private ControllerHandler controllerHandler;
    private KeyboardTranslator keyboardTranslator;
    private KeyBoardController virtualController;

    private KeyBoardController keyBoardController;

    private KeyBoardLayoutController keyBoardLayoutController;

    public PreferenceConfiguration prefConfig;
    private SharedPreferences tombstonePrefs;

    private NvConnection conn;
    private StreamSessionController sessionController;
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

    private InputCaptureProvider inputCaptureProvider;
    private int modifierFlags = 0;
    private boolean grabbedInput = true;
    private boolean cursorVisible = false;
    private boolean waitingForAllModifiersUp = false;
    private int specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private StreamView streamView;
    private NativeCursorOverlayView nativeCursorOverlayView;
    private VideoProcessingGLSurfaceView fsrView;
    private FsrVideoProcessor fsrVideoProcessor;

    private boolean isHidingOverlays;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private StreamPerformanceOverlayController
            performanceOverlayController;

    private MediaCodecDecoderRenderer decoderRenderer;
    private AndroidAudioRenderer audioRenderer;
    private boolean reportedCrash;
    private boolean micToggleInFlight;
    private boolean pendingMicToggleAfterPermission;

    private WifiManager.WifiLock highPerfWifiLock;
    private WifiManager.WifiLock lowLatencyWifiLock;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
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
    private Surface fsrInputSurface;
    private boolean usbPermissionPromptVisible;
    private boolean fsrViewLifecyclePaused;
    private BackNavigationRegistration backNavigationRegistration;
    private StreamInputGatewayRegistry.Registration inputGatewayRegistration;
    private boolean showSoftKeyboardWhenFocused;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiHelper.setLocale(this);

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

        // Read the stream preferences
        prefConfig = PreferenceConfiguration.readPreferences(this);
        tombstonePrefs = Game.this.getSharedPreferences("DecoderTombstone", 0);
        backNavigationRegistration =
                BackNavigationRegistration.register(this, this::handleStreamBackPressed);

        // Preserve compact-screen preferences while allowing adaptive windows
        // to follow the user's current orientation.
        setPreferredOrientationForCurrentDisplay();

        boolean useEntireDisplay = prefConfig.stretchVideo ||
                prefConfig.enableCutoutModeVideo ||
                shouldIgnoreInsetsForResolution(prefConfig.width, prefConfig.height);
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
        int gravityModel = Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("screen_gravity_list", "0"));
        params.gravity = resolvePhysicalStreamGravity(gravityModel, params.gravity);

        if (fsrEnabled) {
            fsrVideoProcessor = new FsrVideoProcessor(this);
            fsrVideoProcessor.setSharpness(getFsrSharpness());
            fsrVideoProcessor.setFsrEnabled(true);
            fsrView = new VideoProcessingGLSurfaceView(this, false, isFsrNativeHdrOutputEnabled(), fsrVideoProcessor,
                    new VideoProcessingGLSurfaceView.SurfaceListener() {
                        @Override
                        public void onInputSurfaceAvailable(android.graphics.SurfaceTexture surfaceTexture) {
                            if (fsrInputSurface != null) {
                                fsrInputSurface.release();
                            }
                            fsrInputSurface = new Surface(surfaceTexture);
                            fsrInputSurfaceReady = true;
                            if (hasSessionStarted()) {
                                decoderRenderer.setRenderTarget(fsrInputSurface);
                            }
                            startConnectionIfReady();
                        }

                        @Override
                        public void onInputSurfaceDestroyed() {
                            fsrInputSurfaceReady = false;
                            if (fsrInputSurface != null) {
                                fsrInputSurface.release();
                                fsrInputSurface = null;
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
            fsrView.setFrameInputSize(prefConfig.width, prefConfig.height);
            if (isFsrNativeHeightTarget()) {
                fsrView.setFixedSurfacePixelSize(0, 0);
            }
            else {
                int[] fsrOutputSize = getFsrOutputSize();
                fsrView.setFixedSurfacePixelSize(fsrOutputSize[0], fsrOutputSize[1]);
            }
        }

        // Listen for touch events on the background touch view to enable trackpad mode
        // to work on areas outside of the StreamView itself. We use a separate View
        // for this rather than just handling it at the Activity level, because that
        // allows proper touch splitting, which the OSC relies upon.
        View backgroundTouchView = findViewById(R.id.backgroundTouchView);
        backgroundTouchView.setOnTouchListener(this);

        rootView=streamView.getParent();
        if (prefConfig.enableNativeCursor && rootView instanceof FrameLayout) {
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
                        prefConfig,
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

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            highPerfWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Moonlight High Perf Lock");
            highPerfWifiLock.setReferenceCounted(false);
            highPerfWifiLock.acquire();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lowLatencyWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Moonlight Low Latency Lock");
                lowLatencyWifiLock.setReferenceCounted(false);
                lowLatencyWifiLock.acquire();
            }
        } catch (SecurityException e) {
            // Some Samsung Galaxy S10+/S10e devices throw a SecurityException from
            // WifiLock.acquire() even though we have android.permission.WAKE_LOCK in our manifest.
            e.printStackTrace();
        }

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
        if(prefConfig.ignoreCheckHDR){
            willStreamHdr=true;
        }else{
            if (prefConfig.enableHdr) {
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

        decoderRenderer = new MediaCodecDecoderRenderer(
                this,
                prefConfig,
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

        // Don't stream HDR if the decoder can't support it
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported() && !decoderRenderer.isAv1Main10Supported()) {
            willStreamHdr = false;
            UiToast.makeText(this, "Decoder does not support HDR10 profile", UiToast.LENGTH_LONG).show();
        }
        // Display a message to the user if HEVC was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_HEVC && !decoderRenderer.isHevcSupported()) {
            UiToast.makeText(this, "No HEVC decoder found", UiToast.LENGTH_LONG).show();
        }

        // Display a message to the user if AV1 was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_AV1 && !decoderRenderer.isAv1Supported()) {
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

        int gamepadMask = ControllerHandler.getAttachedControllerMask(this);
        if (!prefConfig.multiController) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            gamepadMask = 1;
        }
        if (prefConfig.onscreenController) {
            // If we're using OSC, always set at least gamepad 1.
            gamepadMask |= 1;
        }

        // Set to the optimal mode for streaming
        float displayRefreshRate = prepareDisplayForRendering();
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        int roundedRefreshRate = Math.round(displayRefreshRate);
        int chosenFrameRate = prefConfig.fps;
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    // Use frame drops when rendering above the screen frame rate
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Using drop mode for FPS > Hz");
                } else if (roundedRefreshRate <= 49) {
                    // Let's avoid clearly bogus refresh rates and fall back to legacy rendering
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Bogus refresh rate: " + roundedRefreshRate);
                }
                else {
                    chosenFrameRate = roundedRefreshRate - 1;
                    LimeLog.info("Adjusting FPS target for screen to " + chosenFrameRate);
                }
            }
        }

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(prefConfig.width, prefConfig.height)
                .setLaunchRefreshRate(prefConfig.fps)
                .setRefreshRate(chosenFrameRate)
                .setApp(app)
                .setBitrate(prefConfig.bitrate)
                .setEnableSops(prefConfig.enableSops)
                .enableLocalAudioPlayback(prefConfig.playHostAudio)
                .setMaxPacketSize(1392)
                .setRemoteConfiguration(StreamConfiguration.STREAM_CFG_AUTO) // NvConnection will perform LAN and VPN detection
                .setSupportedVideoFormats(supportedVideoFormats)
                .setAttachedGamepadMask(gamepadMask)
                .setClientRefreshRateX100((int)(displayRefreshRate * 100))
                .setAudioConfiguration(prefConfig.audioConfiguration)
                .setColorSpace(decoderRenderer.getPreferredColorSpace())
                .setColorRange(decoderRenderer.getPreferredColorRange())
                .setPPI(RazerUtils.getPPI(this))
                .setRazerVD(prefConfig.razerVD)
                .setPersistGamepadsAfterDisconnect(!prefConfig.multiController)
                .enableNativeCursor(prefConfig.enableNativeCursor)
                .enableClipboardSync(prefConfig.enableClipboardSync)
                .disableAdaptiveInputThrottling(prefConfig.disableAdaptiveInputThrottling)
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
                PlatformBinding.getCryptoProvider(this), serverCert);
        PointerInputSink pointerInputSink =
                new NvConnectionPointerInputSink(conn);
        DirectContactInputController directContactInputController =
                new DirectContactInputController(
                        streamView,
                        pointerInputSink,
                        prefConfig);
        externalPointerInputController =
                new ExternalPointerInputController(
                        streamView,
                        pointerInputSink,
                        inputCaptureProvider,
                        directContactInputController,
                        prefConfig);
        clipboardFileTransferController =
                new RemoteClipboardFileTransferController(this, conn);
        sessionController = new StreamSessionController(conn, this);
        touchInputController = new TouchInputController(
                this,
                streamView,
                pointerInputSink,
                directContactInputController,
                prefConfig,
                new TouchInputController.Host() {
                    @Override
                    public void showSoftKeyboard() {
                        Game.this.showKeyboard();
                    }
                });
        if (prefConfig.enableNativeCursor) {
            conn.setMousePositionListener(new NvConnection.MousePositionListener() {
                @Override
                public void onMousePosition(short x, short y, short referenceWidth, short referenceHeight) {
                    setNativeCursorOverlayFromReference(x, y, referenceWidth, referenceHeight);
                }
            });
        }
        startConnectionIfReady();
        controllerHandler = new ControllerHandler(this, conn, this, prefConfig);
        keyboardTranslator = new KeyboardTranslator();

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(keyboardTranslator, null);

        //鼠标触控模式
        String mouseModel=PreferenceManager.getDefaultSharedPreferences(this).getString("mouse_model_list_axi", "0");
        switchMouseModel(Integer.parseInt(mouseModel));

        if (prefConfig.onscreenController) {
            // create virtual onscreen controller
            initVirtualController();
        }

        //特殊按键屏幕布局
        if(prefConfig.enableKeyboard){
            initKeyboardController();
        }

        if (prefConfig.usbDriver) {
            // Start the USB driver
            bindService(new Intent(this, UsbDriverService.class),
                    usbDriverServiceConnection, Service.BIND_AUTO_CREATE);
        }

        //悬浮球
        if(prefConfig.enableAXFloating){
            initFloatingView();
        }

        if (!decoderRenderer.isAvcSupported()) {
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
        if(prefConfig.enableExDisplay){
            showSecondScreen();
        }

        //强制体感
        setMotionForceGyro();

        //光标是否显示
        if(!cursorVisible&&prefConfig.enableMouseLocalCursor){
            switchMouseLocalCursor();
        }
//        cursorVisible=prefConfig.enableMouseLocalCursor;
//        initFloatingView();

    }

    private void initKeyboardController(){
        keyBoardController = new KeyBoardController(
                controllerHandler, (FrameLayout) rootView, this, prefConfig,
                false, this, this);
//        keyBoardController.refreshLayout();
        keyBoardController.show();
    }


    private void initVirtualController(){
        virtualController = new KeyBoardController(
                controllerHandler, (FrameLayout) rootView, this, prefConfig,
                true, this, this);
//        virtualController.refreshLayout();
        virtualController.show();
    }

    private void initkeyBoardLayoutController(){
        keyBoardLayoutController = new KeyBoardLayoutController(
                controllerHandler, (FrameLayout) rootView, this, prefConfig,
                this, this);
        keyBoardLayoutController.refreshLayout();
        keyBoardLayoutController.show();
    }

    //显示隐藏虚拟特殊按键
    public void showHideKeyboardController(){
        if(keyBoardController==null){
            initKeyboardController();
            prefConfig.enableKeyboard=true;
            return;
        }
        prefConfig.enableKeyboard=keyBoardController.switchShowHide() != 0;
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
            prefConfig.onscreenController=true;
            return;
        }
        prefConfig.onscreenController= virtualController.switchShowHide() != 0;
    }

    @Override
    public void performStreamUiAction(StreamUiActions.Action action) {
        switch (action) {
            case TOGGLE_SOFT_KEYBOARD:
                if (!hasWindowFocus()) {
                    streamView.postDelayed(this::toggleKeyboard, 10);
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
        StreamOrientationController.applyGameOrientation(
                this, prefConfig, isPortrait);
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
                    prefConfig.onscreenController=false;
                }

                if (keyBoardController != null) {
                    keyBoardController.hide();
                    prefConfig.enableKeyboard=false;
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
                UiHelper.notifyStreamEnteringPiP(this);
            }
            else {
                isHidingOverlays = false;

                // Restore overlays to previous state when leaving PiP
//                if (virtualController != null) {
//                    if(!prefConfig.onscreenController){
//                        virtualController.hide();
//                    }
//                }
//
//                if (keyBoardController != null) {
//                    if(!prefConfig.enableKeyboard){
//                        keyBoardController.hide();
//                    }
//                }
                performanceOverlayController
                        .restoreAfterPictureInPicture();

                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);

                // Enable sensors again after exiting PiP
                controllerHandler.enableSensors();

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(this);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private PictureInPictureParams getPictureInPictureParams(boolean autoEnter) {
        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(prefConfig.width, prefConfig.height))
                        .setSourceRectHint(new Rect(
                                streamView.getLeft(), streamView.getTop(),
                                streamView.getRight(), streamView.getBottom()));

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
        if (!prefConfig.enablePip) {
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
        this.modifierFlags = 0;

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
        return refreshRate >= prefConfig.fps &&
                refreshRate <= prefConfig.fps + 3;
    }

    private boolean isRefreshRateGoodMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                Math.round(refreshRate) % prefConfig.fps <= 3;
    }

    private boolean shouldIgnoreInsetsForResolution(int width, int height) {
        // Never ignore insets for non-native resolutions
        if (!prefConfig.isNativeResolution()) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display display = getWindowManager().getDefaultDisplay();
            for (Display.Mode candidate : display.getSupportedModes()) {
                // Ignore insets if this is an exact match for the display resolution
                if ((width == candidate.getPhysicalWidth() && height == candidate.getPhysicalHeight()) ||
                        (height == candidate.getPhysicalWidth() && width == candidate.getPhysicalHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean mayReduceRefreshRate() {
        return prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS ||
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS ||
                (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED && prefConfig.reduceRefreshRate);
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
            boolean isNativeResolutionStream = prefConfig.isNativeResolution();
            boolean refreshRateIsGood = isRefreshRateGoodMatch(bestMode.getRefreshRate());
            boolean refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.getRefreshRate());

            LimeLog.info("Current display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            for (Display.Mode candidate : display.getSupportedModes()) {
                boolean refreshRateReduced = candidate.getRefreshRate() < bestMode.getRefreshRate();
                boolean resolutionReduced = candidate.getPhysicalWidth() < bestMode.getPhysicalWidth() ||
                        candidate.getPhysicalHeight() < bestMode.getPhysicalHeight();
                boolean resolutionFitsStream = candidate.getPhysicalWidth() >= prefConfig.width &&
                        candidate.getPhysicalHeight() >= prefConfig.height;

                LimeLog.info("Examining display mode: "+candidate.getPhysicalWidth()+"x"+
                        candidate.getPhysicalHeight()+"x"+candidate.getRefreshRate());

                if (candidate.getPhysicalWidth() > 4096 && prefConfig.width <= 4096) {
                    // Avoid resolutions options above 4K to be safe
                    continue;
                }

                // On non-4K streams, we force the resolution to never change unless it's above
                // 60 FPS, which may require a resolution reduction due to HDMI bandwidth limitations,
                // or it's a native resolution stream.
                if (prefConfig.width < 3840 && prefConfig.fps <= 60 && !isNativeResolutionStream) {
                    if (display.getMode().getPhysicalWidth() != candidate.getPhysicalWidth() ||
                            display.getMode().getPhysicalHeight() != candidate.getPhysicalHeight()) {
                        continue;
                    }
                }

                // Make sure the resolution doesn't regress unless if it's over 60 FPS
                // where we may need to reduce resolution to achieve the desired refresh rate.
                if (resolutionReduced && !(prefConfig.fps > 60 && resolutionFitsStream)) {
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
                if (prefConfig.enforceDisplayMode ||Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
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
                    if (prefConfig.fps <= 60) {
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

            double screenAspectRatio = ((double)screenSize.y) / screenSize.x;
            double streamAspectRatio = ((double)prefConfig.height) / prefConfig.width;
            if (Math.abs(screenAspectRatio - streamAspectRatio) < 0.001) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        if (prefConfig.stretchVideo || aspectRatioMatch) {
            // Set the surface to the size of the video
            streamView.getHolder().setFixedSize(prefConfig.width, prefConfig.height);
            if (fsrView != null) {
                fsrView.setDesiredAspectRatio(0.0);
            }
        }
        else {
            // Set the surface to scale based on the aspect ratio of the stream
            streamView.setDesiredAspectRatio((double)prefConfig.width / (double)prefConfig.height);
            if (fsrView != null) {
                fsrView.setDesiredAspectRatio((double)prefConfig.width / (double)prefConfig.height);
            }
            LimeLog.info("surfaceChanged-->"+(double)prefConfig.width / (double)prefConfig.height);
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
            decoderRenderer.notifyVideoBackground();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decoderRenderer.notifyVideoForeground();
        }

        // Correct the system UI visibility flags
        hideSystemUi(50);
        UiHelper.refreshStreamWindowInsets(this);
    }

    @Override
    protected void onDestroy() {
        unregisterInputGateway();
        if (touchInputController != null) {
            touchInputController.destroy();
            touchInputController = null;
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
        super.onDestroy();

        UiHelper.notifyHdrWindowStatus(this, false);

        if(presentation!=null){
            presentation.dismiss();
        }

        if (controllerHandler != null) {
            controllerHandler.destroy();
        }
        if (keyboardTranslator != null) {
            InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            inputManager.unregisterInputDeviceListener(keyboardTranslator);
        }

        if (lowLatencyWifiLock != null) {
            lowLatencyWifiLock.release();
        }
        if (highPerfWifiLock != null) {
            highPerfWifiLock.release();
        }

        if (connectedToUsbDriverService) {
            // Unbind from the discovery service
            unbindService(usbDriverServiceConnection);
        }

        if (fsrInputSurface != null) {
            fsrInputSurface.release();
            fsrInputSurface = null;
        }

        // Destroy the capture provider
        inputCaptureProvider.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (touchInputController != null) {
            touchInputController.start();
        }

        if (fsrView != null && fsrViewLifecyclePaused) {
            fsrView.onResume();
            fsrViewLifecyclePaused = false;
        }
    }

    @Override
    protected void onPause() {
        if (touchInputController != null) {
            touchInputController.stop();
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
            int videoFormat = decoderRenderer.getActiveVideoFormat();

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
            if (prefConfig.enableLatencyToast) {
                int averageEndToEndLat = decoderRenderer.getAverageEndToEndLatency();
                int averageDecoderLat = decoderRenderer.getAverageDecoderLatency();
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
        if (prefConfig.enableScreenOnAuto != 0 && !isFinishing()) {
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
        if(prefConfig.enableScreenOnAuto==1){
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putInt("enable_screen_on_auto",0)
                    .apply();
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

    // Returns true if the key stroke was consumed
    private boolean handleSpecialKeys(int androidKeyCode, boolean down) {
        int modifierMask = 0;
        int nonModifierKeyCode = KeyEvent.KEYCODE_UNKNOWN;

        if (androidKeyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
            androidKeyCode == KeyEvent.KEYCODE_CTRL_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_CTRL;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_SHIFT_LEFT ||
                 androidKeyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_SHIFT;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_ALT_LEFT ||
                 androidKeyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_ALT;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_META_LEFT ||
                androidKeyCode == KeyEvent.KEYCODE_META_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_META;
        }
        else {
            nonModifierKeyCode = androidKeyCode;
        }

        if (down) {
            this.modifierFlags |= modifierMask;
        }
        else {
            this.modifierFlags &= ~modifierMask;
        }

        // Handle the special combos on the key up
        if (waitingForAllModifiersUp || specialKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            if (specialKeyCode == androidKeyCode) {
                // If this is a key up for the special key itself, eat that because the host never saw the original key down
                return true;
            }
            else if (modifierFlags != 0) {
                // While we're waiting for modifiers to come up, eat all key downs and allow all key ups to pass
                return down;
            }
            else {
                // When all modifiers are up, perform the special action
                switch (specialKeyCode) {
                    // Toggle input grab
                    case KeyEvent.KEYCODE_Z:
                        Handler h = getWindow().getDecorView().getHandler();
                        if (h != null) {
                            h.postDelayed(toggleGrab, 250);
                        }
                        break;

                    // Quit
                    case KeyEvent.KEYCODE_Q:
                        finish();
                        break;

                    // Toggle cursor visibility
                    case KeyEvent.KEYCODE_C:
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
                        break;

                    default:
                        break;
                }

                // Reset special key state
                specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
                waitingForAllModifiersUp = false;
            }
        }
        // Check if Ctrl+Alt+Shift is down when a non-modifier key is pressed
        else if ((modifierFlags & (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT | KeyboardPacket.MODIFIER_SHIFT)) ==
                (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT | KeyboardPacket.MODIFIER_SHIFT) &&
                (down && nonModifierKeyCode != KeyEvent.KEYCODE_UNKNOWN)) {
            switch (androidKeyCode) {
                case KeyEvent.KEYCODE_Z:
                case KeyEvent.KEYCODE_Q:
                case KeyEvent.KEYCODE_C:
                    // Remember that a special key combo was activated, so we can consume all key
                    // events until the modifiers come up
                    specialKeyCode = androidKeyCode;
                    waitingForAllModifiersUp = true;
                    return true;

                default:
                    // This isn't a special combo that we consume on the client side
                    return false;
            }
        }

        // Not a special combo
        return false;
    }

    // We cannot simply use modifierFlags for all key event processing, because
    // some IMEs will not generate real key events for pressing Shift. Instead
    // they will simply send key events with isShiftPressed() returning true,
    // and we will need to send the modifier flag ourselves.
    private byte getModifierState(KeyEvent event) {
        // Start with the global modifier state to ensure we cover the case
        // detailed in https://github.com/moonlight-stream/moonlight-android/issues/840
        byte modifier = getModifierState();
        if (event.isShiftPressed()) {
            modifier |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if (event.isCtrlPressed()) {
            modifier |= KeyboardPacket.MODIFIER_CTRL;
        }
        if (event.isAltPressed()) {
            modifier |= KeyboardPacket.MODIFIER_ALT;
        }
        if (event.isMetaPressed()) {
            modifier |= KeyboardPacket.MODIFIER_META;
        }
        return modifier;
    }

    private byte getModifierState() {
        return (byte) modifierFlags;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return handleKeyDown(event) || super.onKeyDown(keyCode, event);
    }

    private boolean handleKeyDown(KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            cancelPendingStreamBackExit();
        }

        // Handle a synthetic back button event that some Android OS versions
        // create as a result of a right-click. This event WILL repeat if
        // the right mouse button is held down, so we ignore those.
        int eventSource = event.getSource();
        if (PointerInputCompat.isMouseSource(eventSource) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            // Send the right mouse button event if mouse back and forward
            // are disabled. If they are enabled, handleMotionEvent() will take
            // care of this.
            if (!prefConfig.mouseNavButtons) {
                conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
            }

            // Always return true, otherwise the back press will be propagated
            // up to the parent and finish the activity.
            return true;
        }

        boolean handled = false;

        if (ControllerHandler.isGameControllerDevice(event.getDevice())) {
            // Always try the controller handler first, unless it's an alphanumeric keyboard device.
            // Otherwise, controller handler will eat keyboard d-pad events.
            handled = controllerHandler.handleButtonDown(event);
        }

        // Try the keyboard handler if it wasn't handled as a game controller
        if (!handled) {
            // Let this method take duplicate key down events
            if (handleSpecialKeys(event.getKeyCode(), true)) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            // We'll send it as a raw key event if we have a key mapping, otherwise we'll send it
            // as UTF-8 text (if it's a printable character).
            short translated = keyboardTranslator.translate(event.getKeyCode(), event.getDeviceId());
            if (translated == 0) {
                // Make sure it has a valid Unicode representation and it's not a dead character
                // (which we don't support). If those are true, we can send it as UTF-8 text.
                //
                // NB: We need to be sure this happens before the getRepeatCount() check because
                // UTF-8 events don't auto-repeat on the host side.
                int unicodeChar = event.getUnicodeChar();
                if ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0 && (unicodeChar & KeyCharacterMap.COMBINING_ACCENT_MASK) != 0) {
                    conn.sendUtf8Text(""+(char)unicodeChar);
                    return true;
                }

                return false;
            }

            // Eat repeat down events
            if (event.getRepeatCount() > 0) {
                return true;
            }

            conn.sendKeyboardInput(translated, KeyboardPacket.KEY_DOWN, getModifierState(event),
                    keyboardTranslator.hasNormalizedMapping(event.getKeyCode(), event.getDeviceId()) ? 0 : MoonBridge.SS_KBE_FLAG_NON_NORMALIZED);
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyUp(event) || super.onKeyUp(keyCode, event);
    }

    private boolean handleKeyUp(KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        // Handle a synthetic back button event that some Android OS versions
        // create as a result of a right-click.
        int eventSource = event.getSource();
        if (PointerInputCompat.isMouseSource(eventSource) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            // Send the right mouse button event if mouse back and forward
            // are disabled. If they are enabled, handleMotionEvent() will take
            // care of this.
            if (!prefConfig.mouseNavButtons) {
                conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
            }

            // Always return true, otherwise the back press will be propagated
            // up to the parent and finish the activity.
            return true;
        }

        boolean handled = false;
        if (ControllerHandler.isGameControllerDevice(event.getDevice())) {
            // Always try the controller handler first, unless it's an alphanumeric keyboard device.
            // Otherwise, controller handler will eat keyboard d-pad events.
            handled = controllerHandler.handleButtonUp(event);
        }

        // Try the keyboard handler if it wasn't handled as a game controller
        if (!handled) {
            if (handleSpecialKeys(event.getKeyCode(), false)) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            short translated = keyboardTranslator.translate(event.getKeyCode(), event.getDeviceId());
            if (translated == 0) {
                // If we sent this event as UTF-8 on key down, also report that it was handled
                // when we get the key up event for it.
                int unicodeChar = event.getUnicodeChar();
                return (unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0 && (unicodeChar & KeyCharacterMap.COMBINING_ACCENT_MASK) != 0;
            }

            conn.sendKeyboardInput(translated, KeyboardPacket.KEY_UP, getModifierState(event),
                    keyboardTranslator.hasNormalizedMapping(event.getKeyCode(), event.getDeviceId()) ? 0 : MoonBridge.SS_KBE_FLAG_NON_NORMALIZED);
        }

        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return handleKeyMultiple(event) || super.onKeyMultiple(keyCode, repeatCount, event);
    }

    private boolean handleKeyMultiple(KeyEvent event) {
        // We can receive keys from a software keyboard that don't correspond to any existing
        // KEYCODE value. Android will give those to us as an ACTION_MULTIPLE KeyEvent.
        //
        // Despite the fact that the Android docs say this is unused since API level 29, these
        // events are still sent as of Android 13 for the above case.
        //
        // For other cases of ACTION_MULTIPLE, we will not report those as handled so hopefully
        // they will be passed to us again as regular singular key events.
        if (event.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN || event.getCharacters() == null) {
            return false;
        }

        conn.sendUtf8Text(event.getCharacters());
        return true;
    }

    @Override
    public void sendImeText(String text) {
        if (!isInputReady() || !grabbedInput ||
                text == null || text.isEmpty()) {
            return;
        }

        conn.sendUtf8Text(text);
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

        for (int i = 0; i < count; i++) {
            conn.sendKeyboardInput(keyCode, KeyboardPacket.KEY_DOWN, getModifierState(), (byte) 0);
            conn.sendKeyboardInput(keyCode, KeyboardPacket.KEY_UP, getModifierState(), (byte) 0);
        }
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
        if (!inputManager.showSoftInput(streamView, InputMethodManager.SHOW_IMPLICIT)) {
            streamView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (streamView.isImeActive()) {
                        streamView.requestFocus();
                        inputManager.showSoftInput(streamView, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }, SOFT_KEYBOARD_SHOW_RETRY_MS);
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
            int gravityModel,
            int defaultGravity) {
        switch (gravityModel) {
            case 1:
                return Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            case 2:
                return Gravity.LEFT | Gravity.TOP;
            case 3:
                return Gravity.RIGHT | Gravity.TOP;
            case 4:
                return Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            case 5:
                return Gravity.LEFT | Gravity.BOTTOM;
            case 6:
                return Gravity.RIGHT | Gravity.BOTTOM;
            default:
                return defaultGravity;
        }
    }

    // Returns true if the event was consumed
    // NB: View is only present if called from a view callback
    private boolean handleMotionEvent(View view, MotionEvent event) {
        // Pass through mouse/touch/joystick input if we're not grabbing
        if (!grabbedInput) {
            return false;
        }

        if (event.getActionMasked() != MotionEvent.ACTION_HOVER_MOVE) {
            cancelPendingStreamBackExit();
        }

        int eventSource = event.getSource();
        int deviceSources = event.getDevice() != null ? event.getDevice().getSources() : 0;
        if ((eventSource & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            if (controllerHandler.handleMotionEvent(event)) {
                return true;
            }
        }
        else if ((deviceSources & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && controllerHandler.tryHandleTouchpadEvent(event)) {
            return true;
        }
        else if (ExternalPointerInputController
                .isPointerClassEvent(event)) {
            if (externalPointerInputController.canHandle(event)) {
                return externalPointerInputController
                        .handleMotionEvent(view, event);
            }

            // This case is for fingers.
            if (virtualController != null &&
                    (virtualController.getControllerMode() == KeyBoardController.ControllerMode.MoveButtons ||
                     virtualController.getControllerMode() == KeyBoardController.ControllerMode.ResizeButtons||
                     virtualController.getControllerMode() == KeyBoardController.ControllerMode.DisableEnableButtons)) {
                // Ignore presses when the virtual controller is being configured
                return true;
            }

            if (keyBoardController != null &&
                    (keyBoardController.getControllerMode() == KeyBoardController.ControllerMode.MoveButtons ||
                            keyBoardController.getControllerMode() == KeyBoardController.ControllerMode.ResizeButtons||
                            keyBoardController.getControllerMode() == KeyBoardController.ControllerMode.DisableEnableButtons)) {
                // Ignore presses when the virtual controller is being configured
                return true;
            }
            return touchInputController.handleMotionEvent(view, event);
        }

        // Unknown class
        return false;
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

    @Override
    public void stageStarting(final String stage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.setMessage(getResources().getString(R.string.conn_starting) + " " + stage);
                }
            }
        });
    }

    @Override
    public void stageComplete(String stage) {
    }

    private void stopConnection() {
        if (touchInputController != null) {
            touchInputController.cancelActiveInput();
        }
        if (sessionController != null && sessionController.stop()) {
            UiHelper.notifyHdrWindowStatus(this, false);
            updatePipAutoEnter();
            audioRenderer = null;

            controllerHandler.stop();

            // Update GameManager state to indicate we're no longer in game
            UiHelper.notifyStreamEnded(this);

        }
    }

    @Override
    public void stageFailed(final String stage, final int portFlags, final int errorCode) {
        // Perform a connection test if the failure could be due to a blocked port
        // This does network I/O, so don't do it on the main thread.
        final int portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443, portFlags);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.dismiss();
                    spinner = null;
                }

                if (!displayedFailureDialog) {
                    displayedFailureDialog = true;
                    LimeLog.severe(stage + " failed: " + errorCode);

                    // If video initialization failed and the surface is still valid, display extra information for the user
                    if (stage.contains("video") && streamView.getHolder().getSurface().isValid()) {
                        UiToast.makeText(Game.this, getResources().getText(R.string.video_decoder_init_failed), UiToast.LENGTH_LONG).show();
                    }

                    String dialogText = getResources().getString(R.string.conn_error_msg) + " " + stage +" (error "+errorCode+")";

                    if (portFlags != 0) {
                        dialogText += "\n\n" + getResources().getString(R.string.check_ports_msg) + "\n" +
                                MoonBridge.stringifyPortFlags(portFlags, "\n");
                    }

                    if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0)  {
                        dialogText += "\n\n" + getResources().getString(R.string.nettest_text_blocked);
                    }

                    Dialog.displayDialog(Game.this, getResources().getString(R.string.conn_error_title), dialogText, true);
                }
            }
        });
    }

    @Override
    public void connectionTerminated(final int errorCode) {
        // Perform a connection test if the failure could be due to a blocked port
        // This does network I/O, so don't do it on the main thread.
        final int portFlags = MoonBridge.getPortFlagsFromTerminationErrorCode(errorCode);
        final int portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER,443, portFlags);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Let the display go to sleep now
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Stop processing controller input
                controllerHandler.stop();

                // Ungrab input
                setInputGrabState(false);

                if (!displayedFailureDialog) {
                    displayedFailureDialog = true;
                    LimeLog.severe("Connection terminated: " + errorCode);
                    stopConnection();

                    // Display the error dialog if it was an unexpected termination.
                    // Otherwise, just finish the activity immediately.
                    if (errorCode != MoonBridge.ML_ERROR_GRACEFUL_TERMINATION) {
                        String message;

                        if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
                            // If we got a blocked result, that supersedes any other error message
                            message = getResources().getString(R.string.nettest_text_blocked);
                        }
                        else {
                            switch (errorCode) {
                                case MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC:
                                    message = getResources().getString(R.string.no_video_received_error);
                                    break;

                                case MoonBridge.ML_ERROR_NO_VIDEO_FRAME:
                                    message = getResources().getString(R.string.no_frame_received_error);
                                    break;

                                case MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION:
                                case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
                                    message = getResources().getString(R.string.early_termination_error);
                                    break;

                                case MoonBridge.ML_ERROR_FRAME_CONVERSION:
                                    message = getResources().getString(R.string.frame_conversion_error);
                                    break;

                                default:
                                    String errorCodeString;
                                    // We'll assume large errors are hex values
                                    if (Math.abs(errorCode) > 1000) {
                                        errorCodeString = Integer.toHexString(errorCode);
                                    }
                                    else {
                                        errorCodeString = Integer.toString(errorCode);
                                    }
                                    message = getResources().getString(R.string.conn_terminated_msg) + "\n\n" +
                                            getResources().getString(R.string.error_code_prefix) + " " + errorCodeString;
                                    break;
                            }
                        }

                        if (portFlags != 0) {
                            message += "\n\n" + getResources().getString(R.string.check_ports_msg) + "\n" +
                                    MoonBridge.stringifyPortFlags(portFlags, "\n");
                        }

                        Dialog.displayDialog(Game.this, getResources().getString(R.string.conn_terminated_title),
                                message, true);
                    }
                    else {
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public void connectionStatusUpdate(final int connectionStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (prefConfig.disableWarnings) {
                    return;
                }

                if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
                    if (prefConfig.bitrate > 5000) {
                        notificationOverlayView.setText(getResources().getString(R.string.slow_connection_msg));
                    }
                    else {
                        notificationOverlayView.setText(getResources().getString(R.string.poor_connection_msg));
                    }

                    requestedNotificationOverlayVisibility = View.VISIBLE;
                }
                else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
                    requestedNotificationOverlayVisibility = View.GONE;
                }

                if (!isHidingOverlays) {
                    notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);
                }
            }
        });
    }

    @Override
    public void connectionStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.dismiss();
                    spinner = null;
                }

                streamStartElapsedMs = SystemClock.elapsedRealtime();
                updatePipAutoEnter();

                // Hide the mouse cursor now after a short delay.
                // Doing it before dismissing the spinner seems to be undone
                // when the spinner gets displayed. On Android Q, even now
                // is too early to capture. We will delay a second to allow
                // the spinner to dismiss before capturing.
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setInputGrabState(true);
                    }
                }, 500);

                // Keep the display on
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Update GameManager state to indicate we're in game
                UiHelper.notifyStreamConnected(Game.this);

                hideSystemUi(1000);
            }
        });

        // Report this shortcut being used (off the main thread to prevent ANRs)
        ComputerDetails computer = new ComputerDetails();
        computer.name = pcName;
        computer.uuid = Game.this.getIntent().getStringExtra(EXTRA_PC_UUID);
        ShortcutHelper shortcutHelper = new ShortcutHelper(this);
        shortcutHelper.reportComputerShortcutUsed(computer);
        if (appName != null) {
            // This may be null if launched from the "Resume Session" PC context menu item
            shortcutHelper.reportGameLaunched(computer, app);
        }
    }

    @Override
    public void displayMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UiToast.makeText(Game.this, message, UiToast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void displayTransientMessage(final String message) {
        if (!prefConfig.disableWarnings) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UiToast.makeText(Game.this, message, UiToast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void rumble(short controllerNumber, short lowFreqMotor, short highFreqMotor) {
        LimeLog.info(String.format((Locale)null, "Rumble on gamepad %d: %04x %04x", controllerNumber, lowFreqMotor, highFreqMotor));
        controllerHandler.handleRumble(controllerNumber, lowFreqMotor, highFreqMotor);
        //联动扳机震动
        if(prefConfig.gameTriggerRumbleLink){
            rumbleTriggers(controllerNumber,lowFreqMotor,highFreqMotor);
        }
        if (performanceOverlayController != null) {
            performanceOverlayController.updateRumble(
                    controllerNumber,
                    lowFreqMotor,
                    highFreqMotor);
        }
    }

    @Override
    public void rumbleTriggers(short controllerNumber, short leftTrigger, short rightTrigger) {
        LimeLog.info(String.format((Locale)null, "Rumble on gamepad triggers %d: %04x %04x", controllerNumber, leftTrigger, rightTrigger));

        controllerHandler.handleRumbleTriggers(controllerNumber, leftTrigger, rightTrigger);
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        LimeLog.info("Display HDR mode: " + (enabled ? "enabled" : "disabled"));
        decoderRenderer.setHdrMode(enabled, hdrMetadata);
        if (fsrVideoProcessor != null) {
            fsrVideoProcessor.setHdrToneMappingEnabled(enabled);
        }
        UiHelper.notifyHdrWindowStatus(this, enabled);
    }

    @Override
    public void setMotionEventState(short controllerNumber, byte motionType, short reportRateHz) {
        LimeLog.info("axi-->: controllerNumber" + controllerNumber+"-motionType:"+motionType+"-reportRateHz:"+reportRateHz);
        controllerHandler.handleSetMotionEventState(controllerNumber, motionType, reportRateHz);
    }

    @Override
    public void setControllerLED(short controllerNumber, byte r, byte g, byte b) {
        controllerHandler.handleSetControllerLED(controllerNumber, r, g, b);
    }

    @Override
    public void nativeCursor(boolean visible, boolean shapeChanged, int format, int x, int y,
                             int width, int height, int hotspotX, int hotspotY,
                             int shapeId, int scaleX, int scaleY, byte[] imageData) {
        if (nativeCursorOverlayView == null) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float encodedToViewX = prefConfig.width > 0
                        ? streamView.getWidth() / (float)prefConfig.width
                        : 1f;
                float encodedToViewY = prefConfig.height > 0
                        ? streamView.getHeight() / (float)prefConfig.height
                        : 1f;
                float captureToEncodedX = scaleX > 0 ? scaleX / 65536f : 1f;
                float captureToEncodedY = scaleY > 0 ? scaleY / 65536f : 1f;
                nativeCursorOverlayView.setCursorScale(
                        captureToEncodedX * encodedToViewX,
                        captureToEncodedY * encodedToViewY);
                nativeCursorOverlayView.updateCursor(visible, shapeChanged, format,
                        width, height, hotspotX, hotspotY, shapeId, imageData);
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (fsrEnabled && (fsrView == null || holder != fsrView.getHolder())) {
            return;
        }

        if (!surfaceCreated) {
            throw new IllegalStateException("Surface changed before creation!");
        }

        LimeLog.info("surfaceChanged-->"+width+" x "+height + "----"+prefConfig.width+" x "+prefConfig.height);
        if (fsrEnabled) {
            return;
        }
        startSessionWithRenderTarget(holder.getSurface());
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
        if (mayReduceRefreshRate() || desiredRefreshRate < prefConfig.fps) {
            desiredFrameRate = prefConfig.fps;
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

        if (hasSessionStarted()) {
            // Let the decoder know immediately that the surface is gone
            decoderRenderer.prepareForStop();

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
        conn.sendMouseHighResScroll((short) (up?prefConfig.mouseSCAmount*50:-50*prefConfig.mouseSCAmount));
    }

    @Override
    public void sendHighResolutionScroll(boolean up) {
        if (isInputReady()) {
            mouseHighResScroll(up);
        }
    }

    @Override
    public void keyboardEvent(boolean buttonDown, short keyCode) {
        short keyMap = keyboardTranslator.translate(keyCode, -1);
        if (keyMap != 0) {
            // handleSpecialKeys() takes the Android keycode
            if (handleSpecialKeys(keyCode, buttonDown)) {
                return;
            }

            if (buttonDown) {
                conn.sendKeyboardInput(keyMap, KeyboardPacket.KEY_DOWN, getModifierState(), (byte)0);
            }
            else {
                conn.sendKeyboardInput(keyMap, KeyboardPacket.KEY_UP, getModifierState(), (byte)0);
            }
        }
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
                return handleKeyDown(keyEvent);
            case KeyEvent.ACTION_UP:
                return handleKeyUp(keyEvent);
            case KeyEvent.ACTION_MULTIPLE:
                return handleKeyMultiple(keyEvent);
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

        if (prefConfig.enableQtDialog && (dialogGameMenu == null || !dialogGameMenu.isVisible())) {
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
        if (mode != null && touchInputController != null) {
            touchInputController.setMode(mode);
        }
    }

    public boolean toggleAbsoluteMouseMode() {
        prefConfig.absoluteMouseMode = !prefConfig.absoluteMouseMode;
        if (conn != null) {
            conn.setAbsoluteMousePositionMode(prefConfig.absoluteMouseMode);
        }
        return prefConfig.absoluteMouseMode;
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
        prefConfig.enableTouchSensitivity=!prefConfig.enableTouchSensitivity;
    }

    //更新虚拟布局视图
    public void updateVirtualView(){
        if (virtualController != null && prefConfig.onscreenController) {
            virtualController.refreshLayout();
        }
        if(keyBoardController !=null && prefConfig.enableKeyboard){
            keyBoardController.refreshLayout();
        }
        if(keyBoardLayoutController!=null){
            keyBoardLayoutController.refreshLayout();
        }
    }

    //切换虚拟手柄模式
    public void switchVirtualController(KeyBoardController.ControllerMode mode){
        if(virtualController==null||!prefConfig.onscreenController){
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
        if(keyBoardController==null||!prefConfig.enableKeyboard){
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
            touchInputController.setInputSuspended(true);
            streamView.setEnableZoomAndPan(true);
            return;
        }
        touchInputController.setInputSuspended(false);
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
    public PreferenceConfiguration getStreamPreferences() {
        return prefConfig;
    }

    @Override
    public boolean isGamepadMouseEmulationAvailable() {
        return gameMenuSession.isMouseEmulationAvailable();
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
    public void onGameMenuDismissed(GameMenuFragment menu) {
        cancelPendingStreamBackExit();
        if (gameMenuSession.close(menu)) {
            dialogGameMenu = null;
        }
    }

    @Override
    public void sendKeyboardChord(short[] keyCodes) {
        if (conn != null && isInputReady()) {
            KeyboardChordSender.send(conn, keyCodes);
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

    @Override
    public void applyRumbleOverlayVisibility() {
        if (performanceOverlayController != null) {
            performanceOverlayController.applyRumbleVisibility();
        }
    }

    @Override
    public void applyPerformanceOverlayInteractivity() {
        if (performanceOverlayController != null) {
            performanceOverlayController.applyCompactInteractivity();
        }
    }

    @Override
    public void applyPerformanceOverlayScale() {
        if (performanceOverlayController != null) {
            performanceOverlayController.applyCompactScale();
        }
    }

    @Override
    public void applyMotionEmulationSettings() {
        setMotionForceGyro();
    }

    @Override
    public void applyPerformanceOverlayMargin() {
        if (performanceOverlayController != null) {
            performanceOverlayController.applyCompactMargin();
        }
    }

    @Override
    public void applyAudioHapticsSettings() {
        setAudioHapticsSettings();
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
        if (prefConfig.enableExDisplay) {
            return false;
        }
        return !"off".equalsIgnoreCase(getFsrTarget());
    }

    private float getFsrSharpness() {
        String value = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("list_fsr_sharpness", "standard");
        if ("soft".equalsIgnoreCase(value)) {
            return 0.55f;
        }
        if ("strong".equalsIgnoreCase(value)) {
            return 1.45f;
        }
        if ("max".equalsIgnoreCase(value)) {
            return 1.85f;
        }
        return 0.85f;
    }

    private int[] getFsrOutputSize() {
        String target = getFsrTarget();
        int targetHeight = "4k".equalsIgnoreCase(target) ? 2160 : 1440;
        float aspect = prefConfig.width > 0 && prefConfig.height > 0
                ? (prefConfig.width / (float) prefConfig.height)
                : (16f / 9f);
        int targetWidth = Math.round(targetHeight * aspect);
        if ("4k".equalsIgnoreCase(target)) {
            targetWidth = Math.max(targetWidth, 3840);
        } else if ("2k".equalsIgnoreCase(target)) {
            targetWidth = Math.max(targetWidth, 2560);
        }
        return new int[] {targetWidth & ~1, targetHeight & ~1};
    }

    private String getFsrTarget() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("list_fsr_target", "off");
    }

    private boolean isFsrNativeHeightTarget() {
        return "native_height".equalsIgnoreCase(getFsrTarget());
    }

    private String getFsrTargetDisplayName() {
        String target = getFsrTarget();
        if ("4k".equalsIgnoreCase(target)) {
            return "4K";
        }
        if ("2k".equalsIgnoreCase(target)) {
            return "2K";
        }
        if ("native_height".equalsIgnoreCase(target)) {
            return getString(R.string.fsr_target_native_height);
        }
        return "关闭";
    }

    private String getFsrSharpnessDisplayName() {
        String value = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("list_fsr_sharpness", "standard");
        if ("soft".equalsIgnoreCase(value)) {
            return "柔和";
        }
        if ("strong".equalsIgnoreCase(value)) {
            return "强";
        }
        if ("max".equalsIgnoreCase(value)) {
            return "极强";
        }
        return "标准";
    }

    private boolean isFsrNativeHdrOutputEnabled() {
        String value = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("list_fsr_hdr_output", "native");
        return prefConfig.enableHdr && "native".equalsIgnoreCase(value);
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
        if (!fsrEnabled || sessionController == null ||
                sessionController.getState().hasStarted() ||
                !fsrInputSurfaceReady || !fsrDisplaySurfaceCreated) {
            return;
        }

        startSessionWithRenderTarget(fsrInputSurface);
    }

    private void startSessionWithRenderTarget(Surface renderTarget) {
        if (sessionController == null ||
                sessionController.getState().hasStarted()) {
            return;
        }

        decoderRenderer.setRenderTarget(renderTarget);
        audioRenderer = new AndroidAudioRenderer(Game.this, controllerHandler, prefConfig.enableAudioFx,
                prefConfig.enableAudioHaptics, prefConfig.audioHapticsStrength,
                prefConfig.audioHapticsVoiceFilter, prefConfig.audioHapticsOutputTarget);
        UiHelper.notifyStreamConnecting(Game.this);
        try {
            if (sessionController.start(audioRenderer, decoderRenderer)) {
                return;
            }
        } catch (RuntimeException | Error error) {
            audioRenderer = null;
            UiHelper.notifyStreamEnded(Game.this);
            throw error;
        }
        audioRenderer = null;
        UiHelper.notifyStreamEnded(Game.this);
    }

    private boolean hasSessionStarted() {
        return sessionController != null &&
                sessionController.getState().hasStarted();
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
        floatingView.setIconImage(R.drawable.app_icon_axi);
        floatingView.setLayoutParams(AXFloatingView.getLayParams());
        ViewGroup decorViewGroup= (ViewGroup) getWindow().getDecorView();
        decorViewGroup.addView(floatingView);
        floatingView.setFloatingViewListener(new AXFloatingViewListener() {
            @Override
            public void onClick(AXFloatingMagnetView magnetView) {
                switch (prefConfig.axFloatingOperate){
                    case 0://游戏菜单
                        showGameMenu(null);
                        break;
                    case 1://软键盘
                        toggleKeyboard();
                        break;
                    case 2://全键盘
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
            conn.sendUtf8Text(text);
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
        controllerHandler.setDualSenseTrigger(prefConfig.ds5TriggerMode,
                prefConfig.ds5TriggerStrength,
                prefConfig.ds5TriggerFrequency,prefConfig.ds5TriggerStart,prefConfig.ds5TriggerEnd);
    }

    public void setMotionForceGyro(){
        if(prefConfig.gameForceGyro){
            if(controllerHandler!=null){
                controllerHandler.handleSetMotionEventState((short) 0, MoonBridge.LI_MOTION_TYPE_GYRO, (short) 100);
            }
        }
    }

    public KeyBoardController getKeyBoardController(){
        return keyBoardController;
    }

    public void setAudioHapticsSettings() {
        if (audioRenderer != null) {
            audioRenderer.updateAudioHapticsSettings(prefConfig.enableAudioHaptics,
                    prefConfig.audioHapticsStrength, prefConfig.audioHapticsVoiceFilter,
                    prefConfig.audioHapticsOutputTarget);
        }
        if (controllerHandler != null) {
            controllerHandler.refreshAudioHapticsState();
        }
    }

    //开启关闭 麦克风
    public void switchMic(){
        if (conn == null || micToggleInFlight) {
            return;
        }

        NvConnection.MicUplinkState state = conn.getMicUplinkState();
        if (state == NvConnection.MicUplinkState.STARTING ||
                state == NvConnection.MicUplinkState.STOPPING) {
            return;
        }

        if (state == NvConnection.MicUplinkState.ON) {
            micToggleInFlight = true;
            final NvConnection currentConn = conn;
            new Thread(() -> {
                currentConn.stopMicUplink();
                String message = currentConn.getLastMicUplinkMessage();
                boolean stoppedCleanly =
                        currentConn.getMicUplinkState() !=
                                NvConnection.MicUplinkState.ERROR;
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

        if (!MicUplinkConnection.isSupported()) {
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
