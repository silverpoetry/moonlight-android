package com.limelight.nvstream;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IpPrefix;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.os.Build;
import android.os.CancellationSignal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.xmlpull.v1.XmlPullParserException;

import com.limelight.DebugLog;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.clipboard.android.SharedPreferencesClipboardSyncCheckpointStore;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.HostHttpResponseException;
import com.limelight.nvstream.http.LimelightCryptoProvider;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.mic.MicrophoneUplinkController;
import com.limelight.nvstream.mic.MicrophoneUplinkEndpoint;
import com.limelight.nvstream.mic.MicrophoneUplinkSessionFactory;
import com.limelight.nvstream.mic.MicrophoneUplinkState;

public class NvConnection implements StreamSessionConnection,
        MicrophoneUplinkEndpoint {
    private static final String CURSOR_LOG_TAG = "MoonlightCursor";
    public interface ClipboardFileDownloadListener {
        void onProgress(long transferredBytes, long totalBytes);
        void onComplete(int topLevelItemCount);
        void onError(String message);
        void onCancelled();
    }
    public interface MousePositionListener {
        void onMousePosition(short x, short y, short referenceWidth, short referenceHeight);
    }

    // Context parameters
    private LimelightCryptoProvider cryptoProvider;
    private String uniqueId;
    private ConnectionContext context;
    private static final ConnectionLeaseManager CONNECTION_LEASES =
            new ConnectionLeaseManager();
    private final boolean isMonkey;
    private final Context appContext;
    private final Object connectionLifecycleLock = new Object();
    private final Object mousePositionLock = new Object();
    private boolean startRequested;
    private boolean stopRequested;
    private Thread startThread;
    private ConnectionLeaseManager.Lease bridgeLease;
    private volatile boolean useAbsoluteMousePosition;
    private final MicrophoneUplinkController
            microphoneUplinkController;
    private volatile MousePositionListener mousePositionListener;
    private volatile ClipboardSyncController clipboardSyncController;

    public void downloadRemoteClipboardFiles(
            android.net.Uri destinationTree,
            CancellationSignal cancellationSignal,
            ClipboardFileDownloadListener listener) {
        ClipboardSyncController controller = clipboardSyncController;
        if (controller == null) {
            listener.onError(appContext.getString(
                    R.string.clipboard_sync_not_connected));
            return;
        }
        controller.downloadRemoteFiles(
                destinationTree,
                cancellationSignal,
                listener);
    }
    private double normalizedMouseX = 0.5;
    private double normalizedMouseY = 0.5;
    private int mouseReferenceWidth;
    private int mouseReferenceHeight;

    public NvConnection(
            Context appContext,
            String host,
            int port,
            int httpsPort,
            String uniqueId,
            StreamConfiguration config,
            LimelightCryptoProvider cryptoProvider,
            X509Certificate serverCert,
            MicrophoneUplinkSessionFactory
                    microphoneUplinkSessionFactory)
    {
        this.appContext = appContext;
        this.cryptoProvider = cryptoProvider;
        this.uniqueId = uniqueId;

        this.context = new ConnectionContext();
        this.context.serverAddress =
                new ComputerDetails.AddressTuple(host, port);
        this.context.httpsPort = httpsPort;
        this.context.streamConfig = config;
        this.context.serverCert = serverCert;
        this.useAbsoluteMousePosition = config.getNativeCursorEnabled();
        this.microphoneUplinkController =
                new MicrophoneUplinkController(
                        new MicrophoneUplinkController.Messages() {
                            @Override
                            public String enabled() {
                                return appContext.getString(
                                        R.string.mic_status_enabled);
                            }

                            @Override
                            public String changing() {
                                return appContext.getString(
                                        R.string.mic_status_changing);
                            }

                            @Override
                            public String previousCaptureStopping() {
                                return appContext.getString(
                                        R.string.mic_status_previous_capture_stopping);
                            }

                            @Override
                            public String hostUnsupported() {
                                return appContext.getString(
                                        R.string.mic_status_host_unsupported);
                            }

                            @Override
                            public String unavailable(String reason) {
                                return appContext.getString(
                                        R.string.mic_status_unavailable_with_reason,
                                        reason);
                            }

                            @Override
                            public String disabled() {
                                return appContext.getString(
                                        R.string.mic_status_disabled);
                            }
                        },
                        microphoneUplinkSessionFactory);
        if (isValidMouseReference(config.getWidth(), config.getHeight())) {
            this.mouseReferenceWidth = config.getWidth();
            this.mouseReferenceHeight = config.getHeight();
        }

        // This is unique per connection
        this.context.riKey = generateRiAesKey();
        this.context.riKeyId = generateRiKeyId();

        this.isMonkey = ActivityManager.isUserAMonkey();
    }

    public void setMousePositionListener(MousePositionListener mousePositionListener) {
        synchronized (mousePositionLock) {
            this.mousePositionListener = mousePositionListener;
            traceCursor(
                    "Mouse position listener bound; replay=" +
                            mouseReferenceWidth + "x" +
                            mouseReferenceHeight + " at " +
                            normalizedMouseX + "," + normalizedMouseY);
            notifyCurrentMousePositionLocked(mousePositionListener);
        }
    }

    public void setAbsoluteMousePositionMode(boolean enabled) {
        useAbsoluteMousePosition = enabled;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        ClipboardSyncController controller = clipboardSyncController;
        if (hasFocus && controller != null) {
            controller.onFocusGained();
        }
    }

    private static SecretKey generateRiAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");

            // RI keys are 128 bits
            keyGen.init(128);

            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "AES key generation is unavailable",
                    e);
        }
    }

    private static int generateRiKeyId() {
        return new SecureRandom().nextInt();
    }

    @Override
    public void stop() {
        Thread pendingStart;
        ConnectionLeaseManager.Lease ownedLease;
        synchronized (connectionLifecycleLock) {
            if (stopRequested) {
                return;
            }
            stopRequested = true;
            pendingStart = startThread;
            ownedLease = bridgeLease;
        }

        if (pendingStart != null &&
                pendingStart != Thread.currentThread()) {
            pendingStart.interrupt();
        }

        stopMicUplink();

        if (ownedLease != null) {
            // Only the NvConnection that owns the common-c lease may interrupt
            // or stop the process-global bridge.
            MoonBridge.interruptConnection();

            synchronized (MoonBridge.class) {
                if (ownsBridgeLease(ownedLease)) {
                    stopClipboardSync();
                    MoonBridge.stopConnection();
                    MoonBridge.cleanupBridge();
                    releaseBridgeLease(ownedLease);
                }
            }
        }
        else {
            stopClipboardSync();
        }

        waitForStartThread(pendingStart);
    }

    private void stopClipboardSync() {
        ClipboardSyncController controller;
        synchronized (connectionLifecycleLock) {
            controller = clipboardSyncController;
            clipboardSyncController = null;
        }
        if (controller != null) {
            controller.stop();
        }
    }

    private boolean isStopRequested() {
        synchronized (connectionLifecycleLock) {
            return stopRequested;
        }
    }

    private boolean installBridgeLease(
            ConnectionLeaseManager.Lease acquiredLease) {
        synchronized (connectionLifecycleLock) {
            if (stopRequested) {
                return false;
            }
            bridgeLease = acquiredLease;
            return true;
        }
    }

    private boolean ownsBridgeLease(
            ConnectionLeaseManager.Lease expectedLease) {
        synchronized (connectionLifecycleLock) {
            return bridgeLease == expectedLease;
        }
    }

    private void releaseBridgeLease(
            ConnectionLeaseManager.Lease expectedLease) {
        synchronized (connectionLifecycleLock) {
            if (bridgeLease != expectedLease) {
                return;
            }
            bridgeLease = null;
        }
        expectedLease.close();
    }

    private void waitForStartThread(Thread pendingStart) {
        if (pendingStart == null ||
                pendingStart == Thread.currentThread()) {
            return;
        }

        boolean interrupted = false;
        while (pendingStart.isAlive()) {
            try {
                pendingStart.join();
            } catch (InterruptedException error) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getLastMicUplinkMessage() {
        return microphoneUplinkController.getLastMessage();
    }

    @Override
    public boolean isMicUplinkSupported() {
        return microphoneUplinkController.isSupported();
    }

    @Override
    public boolean isMicUplinkActive() {
        return microphoneUplinkController.isActive();
    }

    @Override
    public MicrophoneUplinkState getMicUplinkState() {
        return microphoneUplinkController.getState();
    }

    @Override
    public boolean stopMicUplink() {
        boolean stopped = microphoneUplinkController.stop();
        if (!stopped) {
            LimeLog.warning(
                    "Failed to stop microphone uplink: " +
                            microphoneUplinkController
                                    .getLastMessage());
        }
        return stopped;
    }

    @Override
    public boolean startMicUplink() {
        boolean started = microphoneUplinkController.start();
        if (!started) {
            LimeLog.warning(
                    "Failed to start microphone uplink: " +
                            microphoneUplinkController
                                    .getLastMessage());
        }
        return started;
    }

    private InetAddress resolveServerAddress() throws IOException {
        // Try to find an address that works for this host
        InetAddress[] addrs = InetAddress.getAllByName(context.serverAddress.address);
        for (InetAddress addr : addrs) {
            try (Socket s = new Socket()) {
                s.setSoLinger(true, 0);
                s.connect(new InetSocketAddress(addr, context.serverAddress.port), 1000);
                return addr;
            } catch (IOException e) {
                LimeLog.warning(
                        "Resolved host address was unreachable",
                        e);
            }
        }

        // If we made it here, we didn't manage to find a working address. If DNS returned any
        // address, we'll use the first available address and hope for the best.
        if (addrs.length > 0) {
            return addrs[0];
        }
        else {
            throw new IOException("No addresses found for "+context.serverAddress);
        }
    }

    private int detectServerConnectionType() {
        ConnectivityManager connMgr = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connMgr.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities netCaps = connMgr.getNetworkCapabilities(activeNetwork);
            if (netCaps != null) {
                if (netCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        !netCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                    // VPNs are treated as remote connections
                    return StreamConfiguration.STREAM_CFG_REMOTE;
                }
                else if (netCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    // Cellular is always treated as remote to avoid any possible
                    // issues with 464XLAT or similar technologies.
                    return StreamConfiguration.STREAM_CFG_REMOTE;
                }
            }

            // Check if the server address is on-link
            LinkProperties linkProperties = connMgr.getLinkProperties(activeNetwork);
            if (linkProperties != null) {
                InetAddress serverAddress;
                try {
                    serverAddress = resolveServerAddress();
                } catch (IOException e) {
                    LimeLog.warning(
                            "Unable to resolve the host for VPN detection",
                            e);

                    // We can't decide without being able to resolve the server address
                    return StreamConfiguration.STREAM_CFG_AUTO;
                }

                // If the address is in the NAT64 prefix, always treat it as remote
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    IpPrefix nat64Prefix = linkProperties.getNat64Prefix();
                    if (nat64Prefix != null && nat64Prefix.contains(serverAddress)) {
                        return StreamConfiguration.STREAM_CFG_REMOTE;
                    }
                }

                for (RouteInfo route : linkProperties.getRoutes()) {
                    // Skip non-unicast routes (which are all we get prior to Android 13)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && route.getType() != RouteInfo.RTN_UNICAST) {
                        continue;
                    }

                    // Find the first route that matches this address
                    if (route.matches(serverAddress)) {
                        // If there's no gateway, this is an on-link destination
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // We want to use hasGateway() because getGateway() doesn't adhere
                            // to documented behavior of returning null for on-link addresses.
                            if (!route.hasGateway()) {
                                return StreamConfiguration.STREAM_CFG_LOCAL;
                            }
                        }
                        else {
                            // getGateway() is documented to return null for on-link destinations,
                            // but it actually returns the unspecified address (0.0.0.0 or ::).
                            InetAddress gateway = route.getGateway();
                            if (gateway == null || gateway.isAnyLocalAddress()) {
                                return StreamConfiguration.STREAM_CFG_LOCAL;
                            }
                        }

                        // We _should_ stop after the first matching route, but for some reason
                        // Android doesn't always report IPv6 routes in descending order of
                        // specificity and metric. To handle that case, we enumerate all matching
                        // routes, assuming that an on-link route will always be preferred.
                    }
                }
            }
        }

        // If we can't determine the connection type, let moonlight-common-c decide.
        return StreamConfiguration.STREAM_CFG_AUTO;
    }

    private NvHTTP startApp() throws XmlPullParserException, IOException
    {
        NvHTTP h = new NvHTTP(
                context.serverAddress,
                context.httpsPort,
                uniqueId,
                context.serverCert,
                cryptoProvider);

        String serverInfo = h.getServerInfo(true);

        context.serverAppVersion = h.getServerVersion(serverInfo);
        if (context.serverAppVersion == null) {
            context.connListener.displayMessage("Server version malformed");
            return null;
        }

        ComputerDetails details = h.getComputerDetails(serverInfo);
        context.isNvidiaServerSoftware = details.nvidiaServer;

        // May be missing for older servers
        context.serverGfeVersion = h.getGfeVersion(serverInfo);

        if (h.getPairState(serverInfo) != PairingManager.PairState.PAIRED) {
            context.connListener.displayMessage("Device not paired with computer");
            return null;
        }

        context.serverCodecModeSupport = (int)h.getServerCodecModeSupport(serverInfo);

        context.negotiatedHdr = (context.streamConfig.getSupportedVideoFormats() & MoonBridge.VIDEO_FORMAT_MASK_10BIT) != 0;
        if ((context.serverCodecModeSupport & 0x20200) == 0 && context.negotiatedHdr) {
            context.connListener.displayTransientMessage("Your PC GPU does not support streaming HDR. The stream will be SDR.");
            context.negotiatedHdr = false;
        }

        //
        // Decide on negotiated stream parameters now
        //

        // Check for a supported stream resolution
        if ((context.streamConfig.getWidth() > 4096 || context.streamConfig.getHeight() > 4096) &&
                (h.getServerCodecModeSupport(serverInfo) & 0x200) == 0 && context.isNvidiaServerSoftware) {
            context.connListener.displayMessage("Your host PC does not support streaming at resolutions above 4K.");
            return null;
        }
        else if ((context.streamConfig.getWidth() > 4096 || context.streamConfig.getHeight() > 4096) &&
                (context.streamConfig.getSupportedVideoFormats() & ~MoonBridge.VIDEO_FORMAT_MASK_H264) == 0) {
            context.connListener.displayMessage("Your streaming device must support HEVC or AV1 to stream at resolutions above 4K.");
            return null;
        }
        else if (context.streamConfig.getHeight() >= 2160 && !h.supports4K(serverInfo)) {
            // Client wants 4K but the server can't do it
            context.connListener.displayTransientMessage("You must update GeForce Experience to stream in 4K. The stream will be 1080p.");

            // Lower resolution to 1080p
            context.negotiatedWidth = 1920;
            context.negotiatedHeight = 1080;
        }
        else {
            // Take what the client wanted
            context.negotiatedWidth = context.streamConfig.getWidth();
            context.negotiatedHeight = context.streamConfig.getHeight();
        }

        // We will perform some connection type detection if the caller asked for it
        if (context.streamConfig.getRemote() == StreamConfiguration.STREAM_CFG_AUTO) {
            context.negotiatedRemoteStreaming = detectServerConnectionType();
            context.negotiatedPacketSize =
                    context.negotiatedRemoteStreaming == StreamConfiguration.STREAM_CFG_REMOTE ?
                            1024 : context.streamConfig.getMaxPacketSize();
        }
        else {
            context.negotiatedRemoteStreaming = context.streamConfig.getRemote();
            context.negotiatedPacketSize = context.streamConfig.getMaxPacketSize();
        }

        //
        // Video stream format will be decided during the RTSP handshake
        //

        NvApp app = context.streamConfig.getApp();

        // If the client did not provide an exact app ID, do a lookup with the applist
        if (!context.streamConfig.getApp().isInitialized()) {
            LimeLog.info("Using deprecated app lookup method - Please specify an app ID in your StreamConfiguration instead");
            app = h.getAppByName(context.streamConfig.getApp().getAppName());
            if (app == null) {
                context.connListener.displayMessage("The app " + context.streamConfig.getApp().getAppName() + " is not in GFE app list");
                return null;
            }
        }

        // If there's a game running, resume it
        if (h.getCurrentGame(serverInfo) != 0) {
            try {
                if (h.getCurrentGame(serverInfo) == app.getAppId()) {
                    if (!h.launchApp(context, "resume", app.getAppId(), context.negotiatedHdr)) {
                        context.connListener.displayMessage("Failed to resume existing session");
                        return null;
                    }
                } else {
                    return quitAndLaunch(h, context) ? h : null;
                }
            } catch (HostHttpResponseException e) {
                if (e.getErrorCode() == 470) {
                    // This is the error you get when you try to resume a session that's not yours.
                    // Because this is fairly common, we'll display a more detailed message.
                    context.connListener.displayMessage("This session wasn't started by this device," +
                            " so it cannot be resumed. End streaming on the original " +
                            "device or the PC itself and try again. (Error code: "+e.getErrorCode()+")");
                    return null;
                }
                else if (e.getErrorCode() == 525) {
                    context.connListener.displayMessage("The application is minimized. Resume it on the PC manually or " +
                            "quit the session and start streaming again.");
                    return null;
                } else {
                    throw e;
                }
            }

            LimeLog.info("Resumed existing game session");
            return h;
        }
        else {
            return launchNotRunningApp(h, context) ? h : null;
        }
    }

    protected boolean quitAndLaunch(NvHTTP h, ConnectionContext context) throws IOException,
            XmlPullParserException {
        try {
            if (!h.quitApp()) {
                context.connListener.displayMessage("Failed to quit previous session! You must quit it manually");
                return false;
            }
        } catch (HostHttpResponseException e) {
            if (e.getErrorCode() == 599) {
                context.connListener.displayMessage("This session wasn't started by this device," +
                        " so it cannot be quit. End streaming on the original " +
                        "device or the PC itself. (Error code: "+e.getErrorCode()+")");
                return false;
            }
            else {
                throw e;
            }
        }

        return launchNotRunningApp(h, context);
    }

    private boolean launchNotRunningApp(NvHTTP h, ConnectionContext context)
            throws IOException, XmlPullParserException {
        // Launch the app since it's not running
        if (!h.launchApp(context, "launch", context.streamConfig.getApp().getAppId(), context.negotiatedHdr)) {
            context.connListener.displayMessage("Failed to launch application");
            return false;
        }

        LimeLog.info("Launched new game session");

        return true;
    }

    @Override
    public void start(final AudioRenderer audioRenderer,
                      final VideoDecoderRenderer videoDecoderRenderer,
                      final NvConnectionListener connectionListener) {
        Thread worker = new Thread(
                () -> runConnectionStart(audioRenderer,
                        videoDecoderRenderer, connectionListener),
                "NvConnectionStart");
        synchronized (connectionLifecycleLock) {
            if (startRequested || stopRequested) {
                throw new IllegalStateException(
                        "NvConnection instances are single-use");
            }
            startRequested = true;
            startThread = worker;
        }
        worker.start();
    }

    private void runConnectionStart(
            AudioRenderer audioRenderer,
            VideoDecoderRenderer videoDecoderRenderer,
            NvConnectionListener connectionListener) {
        try {
            context.connListener = connectionListener;
            context.videoCapabilities =
                    videoDecoderRenderer.getCapabilities();

            String appName =
                    context.streamConfig.getApp().getAppName();
            if (isStopRequested()) {
                return;
            }
            context.connListener.stageStarting(appName);

            NvHTTP sessionHttp;
            try {
                sessionHttp = startApp();
                if (isStopRequested()) {
                    return;
                }
                if (sessionHttp == null) {
                    context.connListener.stageFailed(appName, 0, 0);
                    return;
                }
                context.connListener.stageComplete(appName);
            } catch (HostHttpResponseException error) {
                if (!isStopRequested()) {
                    LimeLog.warning(
                            "Host rejected the stream launch request",
                            error);
                    context.connListener.displayMessage(error.getMessage());
                    context.connListener.stageFailed(
                            appName, 0, error.getErrorCode());
                }
                return;
            } catch (XmlPullParserException | IOException error) {
                if (!isStopRequested()) {
                    LimeLog.warning(
                            "Stream launch request failed",
                            error);
                    context.connListener.displayMessage(error.getMessage());
                    context.connListener.stageFailed(
                            appName,
                            MoonBridge.ML_PORT_FLAG_TCP_47984 |
                                    MoonBridge.ML_PORT_FLAG_TCP_47989,
                            0);
                }
                return;
            }

            ByteBuffer iv = ByteBuffer.allocate(16);
            iv.putInt(context.riKeyId);

            ConnectionLeaseManager.Lease acquiredLease;
            try {
                acquiredLease = CONNECTION_LEASES.acquire();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                if (!isStopRequested()) {
                    context.connListener.displayMessage(
                            error.getMessage());
                    context.connListener.stageFailed(appName, 0, 0);
                }
                return;
            }

            if (!installBridgeLease(acquiredLease)) {
                acquiredLease.close();
                return;
            }

            // Moonlight-core and the Java callback bridge are process-global.
            // The lease establishes ownership across the whole streaming
            // lifetime, while this monitor serializes setup and teardown.
            synchronized (MoonBridge.class) {
                if (isStopRequested()) {
                    releaseBridgeLease(acquiredLease);
                    return;
                }

                boolean bridgeStarted = false;
                try {
                    MoonBridge.setupBridge(videoDecoderRenderer,
                            audioRenderer, connectionListener);
                    boolean clipboardProtocolEnabled = context.streamConfig
                            .getClipboardProtocolEnabled();
                    int clipboardCapabilities = context.streamConfig
                            .getClipboardCapabilities();
                    if (clipboardProtocolEnabled) {
                        clipboardSyncController =
                                new ClipboardSyncController(
                                        appContext,
                                        sessionHttp,
                                        new SharedPreferencesClipboardSyncCheckpointStore(
                                                appContext));
                        clipboardSyncController.start();
                    }
                    int result = MoonBridge.startConnection(
                            context.serverAddress.address,
                            context.serverAppVersion,
                            context.serverGfeVersion,
                            context.rtspSessionUrl,
                            context.serverCodecModeSupport,
                            context.negotiatedWidth,
                            context.negotiatedHeight,
                            context.streamConfig.getRefreshRate(),
                            context.streamConfig.getBitrate(),
                            context.negotiatedPacketSize,
                            context.negotiatedRemoteStreaming,
                            context.streamConfig.getAudioConfiguration()
                                    .toInt(),
                            context.streamConfig
                                    .getSupportedVideoFormats(),
                            context.streamConfig
                                    .getClientRefreshRateX100(),
                            context.riKey.getEncoded(), iv.array(),
                            context.videoCapabilities,
                            context.streamConfig.getColorSpace(),
                            context.streamConfig.getColorRange(),
                            context.streamConfig
                                    .getNativeCursorEnabled(),
                            clipboardProtocolEnabled,
                            clipboardCapabilities,
                            context.streamConfig
                                    .getAdaptiveInputThrottlingDisabled());
                    bridgeStarted = result == 0;
                } finally {
                    if (!bridgeStarted &&
                            ownsBridgeLease(acquiredLease)) {
                        stopClipboardSync();
                        MoonBridge.cleanupBridge();
                        releaseBridgeLease(acquiredLease);
                    }
                }
            }
        } finally {
            synchronized (connectionLifecycleLock) {
                if (startThread == Thread.currentThread()) {
                    startThread = null;
                }
            }
        }
    }

    public void sendMouseMove(final short deltaX, final short deltaY)
    {
        if (!isMonkey) {
            if (useAbsoluteMousePosition) {
                synchronized (mousePositionLock) {
                    if (isValidMouseReference(mouseReferenceWidth, mouseReferenceHeight)) {
                        sendAbsoluteMouseDeltaLocked(deltaX, deltaY,
                                mouseReferenceWidth, mouseReferenceHeight);
                        return;
                    }
                }
            }

            MoonBridge.sendMouseMove(deltaX, deltaY);
        }
    }

    public void sendMousePosition(short x, short y, short referenceWidth, short referenceHeight)
    {
        if (!isMonkey && isValidMouseReference(referenceWidth, referenceHeight)) {
            synchronized (mousePositionLock) {
                sendAbsoluteMousePositionLocked(x, y, referenceWidth, referenceHeight);
            }
        }
    }

    public void sendMouseMoveAsMousePosition(short deltaX, short deltaY, short referenceWidth, short referenceHeight)
    {
        if (!isMonkey && isValidMouseReference(referenceWidth, referenceHeight)) {
            synchronized (mousePositionLock) {
                sendAbsoluteMouseDeltaLocked(deltaX, deltaY,
                        referenceWidth, referenceHeight);
            }
        }
    }

    private void sendAbsoluteMouseDeltaLocked(short deltaX, short deltaY,
                                              int referenceWidth, int referenceHeight) {
        int currentX = (int) Math.round(normalizedMouseX * (referenceWidth - 1));
        int currentY = (int) Math.round(normalizedMouseY * (referenceHeight - 1));
        sendAbsoluteMousePositionLocked(currentX + deltaX, currentY + deltaY,
                referenceWidth, referenceHeight);
    }

    private void sendAbsoluteMousePositionLocked(int x, int y,
                                                 int referenceWidth, int referenceHeight) {
        // This is the sole mutable cursor position for absolute mouse input. The listener and
        // common-c receive the exact same clamped coordinates while this lock preserves ordering.
        int clampedX = clampMouseCoordinate(x, referenceWidth);
        int clampedY = clampMouseCoordinate(y, referenceHeight);

        normalizedMouseX = clampedX / (double) (referenceWidth - 1);
        normalizedMouseY = clampedY / (double) (referenceHeight - 1);
        mouseReferenceWidth = referenceWidth;
        mouseReferenceHeight = referenceHeight;

        short packetX = (short) clampedX;
        short packetY = (short) clampedY;
        short packetReferenceWidth = (short) referenceWidth;
        short packetReferenceHeight = (short) referenceHeight;

        MousePositionListener listener = mousePositionListener;
        notifyMousePosition(listener, packetX, packetY,
                packetReferenceWidth, packetReferenceHeight);
        MoonBridge.sendMousePosition(packetX, packetY,
                packetReferenceWidth, packetReferenceHeight);
    }

    private void notifyCurrentMousePositionLocked(
            MousePositionListener listener) {
        if (listener == null ||
                !isValidMouseReference(
                        mouseReferenceWidth, mouseReferenceHeight)) {
            return;
        }

        notifyMousePosition(
                listener,
                (short) Math.round(
                        normalizedMouseX * (mouseReferenceWidth - 1)),
                (short) Math.round(
                        normalizedMouseY * (mouseReferenceHeight - 1)),
                (short) mouseReferenceWidth,
                (short) mouseReferenceHeight);
    }

    private static void notifyMousePosition(
            MousePositionListener listener,
            short x,
            short y,
            short referenceWidth,
            short referenceHeight) {
        if (listener != null) {
            listener.onMousePosition(x, y, referenceWidth, referenceHeight);
        }
    }

    private static void traceCursor(String message) {
        DebugLog.debug(CURSOR_LOG_TAG, message);
    }

    private static boolean isValidMouseReference(int referenceWidth, int referenceHeight) {
        return referenceWidth > 1 && referenceWidth <= Short.MAX_VALUE &&
                referenceHeight > 1 && referenceHeight <= Short.MAX_VALUE;
    }

    private static int clampMouseCoordinate(int coordinate, int referenceDimension) {
        return Math.max(0, Math.min(referenceDimension - 1, coordinate));
    }

    public void sendMouseButtonDown(final byte mouseButton)
    {
        if (!isMonkey) {
            MoonBridge.sendMouseButton(MouseButtonPacket.PRESS_EVENT, mouseButton);
        }
    }

    public void sendMouseButtonUp(final byte mouseButton)
    {
        if (!isMonkey) {
            MoonBridge.sendMouseButton(MouseButtonPacket.RELEASE_EVENT, mouseButton);
        }
    }

    public void sendControllerInput(final short controllerNumber,
            final short activeGamepadMask, final int buttonFlags,
            final byte leftTrigger, final byte rightTrigger,
            final short leftStickX, final short leftStickY,
            final short rightStickX, final short rightStickY)
    {
        if (!isMonkey) {
            MoonBridge.sendMultiControllerInput(controllerNumber, activeGamepadMask, buttonFlags,
                    leftTrigger, rightTrigger, leftStickX, leftStickY, rightStickX, rightStickY);
        }
    }

    public void sendKeyboardInput(final short keyMap, final byte keyDirection, final byte modifier, final byte flags) {
        if (!isMonkey) {
            MoonBridge.sendKeyboardInput(keyMap, keyDirection, modifier, flags);
        }
    }

    public void sendMouseScroll(final byte scrollClicks) {
        if (!isMonkey) {
            MoonBridge.sendMouseHighResScroll((short)(scrollClicks * 120)); // WHEEL_DELTA
        }
    }

    public void sendMouseHScroll(final byte scrollClicks) {
        if (!isMonkey) {
            MoonBridge.sendMouseHighResHScroll((short)(scrollClicks * 120)); // WHEEL_DELTA
        }
    }

    public void sendMouseHighResScroll(final short scrollAmount) {
        if (!isMonkey) {
            MoonBridge.sendMouseHighResScroll(scrollAmount);
        }
    }

    public void sendMouseHighResHScroll(final short scrollAmount) {
        if (!isMonkey) {
            MoonBridge.sendMouseHighResHScroll(scrollAmount);
        }
    }

    public int sendTouchEvent(byte eventType, int pointerId, float x, float y, float pressureOrDistance,
                              float contactAreaMajor, float contactAreaMinor, short rotation) {
        if (!isMonkey) {
            return MoonBridge.sendTouchEvent(eventType, pointerId, x, y, pressureOrDistance,
                    contactAreaMajor, contactAreaMinor, rotation);
        }
        else {
            return MoonBridge.LI_ERR_UNSUPPORTED;
        }
    }

    public int sendTouchpadEvent(byte eventType, int pointerId, float x, float y, float pressure,
                                 float contactAreaMajor, float contactAreaMinor, short rotation,
                                 short deviceWidthMm, short deviceHeightMm, byte buttonState) {
        if (!isMonkey) {
            return MoonBridge.sendTouchpadEvent(eventType, pointerId, x, y, pressure,
                    contactAreaMajor, contactAreaMinor, rotation,
                    deviceWidthMm, deviceHeightMm, buttonState);
        }
        else {
            return MoonBridge.LI_ERR_UNSUPPORTED;
        }
    }

    public int sendTouchpadFrameEvent(byte contactCount, byte[] eventTypes, int[] pointerIds,
                                      float[] x, float[] y, float[] pressure, short rotation,
                                      short deviceWidthMm, short deviceHeightMm, byte buttonState) {
        if (!isMonkey) {
            return MoonBridge.sendTouchpadFrameEvent(contactCount, eventTypes, pointerIds,
                    x, y, pressure, rotation, deviceWidthMm, deviceHeightMm, buttonState);
        }
        else {
            return MoonBridge.LI_ERR_UNSUPPORTED;
        }
    }

    public int sendPenEvent(byte eventType, byte toolType, byte penButtons, float x, float y,
                            float pressureOrDistance, float contactAreaMajor, float contactAreaMinor,
                            short rotation, byte tilt) {
        if (!isMonkey) {
            return MoonBridge.sendPenEvent(eventType, toolType, penButtons, x, y, pressureOrDistance,
                    contactAreaMajor, contactAreaMinor, rotation, tilt);
        }
        else {
            return MoonBridge.LI_ERR_UNSUPPORTED;
        }
    }

    public int sendControllerArrivalEvent(byte controllerNumber, short activeGamepadMask, byte type,
                                          int supportedButtonFlags, short capabilities) {
        return MoonBridge.sendControllerArrivalEvent(controllerNumber, activeGamepadMask, type, supportedButtonFlags, capabilities);
    }

    public int sendControllerTouchEvent(byte controllerNumber, byte eventType, int pointerId,
                                        float x, float y, float pressure) {
        if (!isMonkey) {
            return MoonBridge.sendControllerTouchEvent(controllerNumber, eventType, pointerId, x, y, pressure);
        }
        else {
            return MoonBridge.LI_ERR_UNSUPPORTED;
        }
    }

    public int sendControllerMotionEvent(byte controllerNumber, byte motionType,
                                         float x, float y, float z) {
        if (!isMonkey) {
            return MoonBridge.sendControllerMotionEvent(controllerNumber, motionType, x, y, z);
        }
        else {
            return MoonBridge.LI_ERR_UNSUPPORTED;
        }
    }

    public void sendControllerBatteryEvent(byte controllerNumber, byte batteryState, byte batteryPercentage) {
        MoonBridge.sendControllerBatteryEvent(controllerNumber, batteryState, batteryPercentage);
    }

    public void sendUtf8Text(final String text) {
        if (!isMonkey) {
            MoonBridge.sendUtf8Text(text);
        }
    }

    public static String findExternalAddressForMdns(String stunHostname, int stunPort) {
        return MoonBridge.findExternalAddressIP4(stunHostname, stunPort);
    }
}
