package com.limelight.stream.launch.android;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

import androidx.annotation.MainThread;

import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.audio.mic.AndroidMicrophoneUplinkSessionFactory;
import com.limelight.binding.input.AndroidControllerInventory;
import com.limelight.binding.video.AndroidDecoderCrashStore;
import com.limelight.binding.video.DecoderCrashTracker;
import com.limelight.binding.video.PerfOverlayRelay;
import com.limelight.binding.video.gl.android.SharedPreferencesGlDeviceSnapshotStore;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.mic.MicrophoneUplinkConfig;
import com.limelight.platform.AndroidDisplayCompat;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidDisplayAspectProvider;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.android.AndroidStreamSettingsBootstrap;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsLoader;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.controller.ControllerSettingsLoader;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsLoader;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDecoderSettingsLoader;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.stream.StreamDisplaySettingsLoader;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsLoader;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.transfer.TransferSettingsLoader;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsLoader;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;
import com.limelight.stream.launch.StreamLaunchRequest;
import com.limelight.ui.stream.AndroidStreamDisplayController;
import com.limelight.ui.stream.AndroidStreamHdrCapabilityProvider;
import com.limelight.ui.stream.AndroidStreamMediaRuntimeFactory;
import com.limelight.ui.stream.StreamHdrRequestPolicy;
import com.limelight.ui.stream.StreamSessionConfigurationAdapter;
import com.limelight.ui.stream.StreamSessionConfigurationPlanner;
import com.limelight.ui.stream.StreamWifiLockController;
import com.limelight.utils.RazerUtils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.UUID;

/** Builds the immutable transport and media runtime for background startup. */
public final class AndroidPreparedStreamSessionFactory {
    private AndroidPreparedStreamSessionFactory() {
    }

    @MainThread
    public static AndroidPreparedStreamSession create(
            Activity activity,
            StreamLaunchRequest request,
            AndroidPreparedStreamSession.Listener listener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        Context appContext = activity.getApplicationContext();
        SettingsRepository settingsRepository =
                AndroidSettingsRepository.create(appContext);
        AndroidStreamSettingsBootstrap.prepare(settingsRepository);

        StreamVideoSettings videoSettings =
                StreamVideoSettingsLoader.load(
                        settingsRepository,
                        AndroidDisplayAspectProvider.get(activity));
        StreamVideoSettingsState videoSettingsState =
                new StreamVideoSettingsState(videoSettings);
        StreamAudioSettings audioSettings =
                StreamAudioSettingsLoader.load(settingsRepository);
        StreamAudioSettingsState audioSettingsState =
                new StreamAudioSettingsState(audioSettings);
        StreamUiSettings uiSettings =
                StreamUiSettingsLoader.load(settingsRepository);
        StreamUiSettingsState uiSettingsState =
                new StreamUiSettingsState(uiSettings);
        StreamDisplaySettings displaySettings =
                StreamDisplaySettingsLoader.load(
                        settingsRepository,
                        videoSettings);
        StreamDecoderSettings decoderSettings =
                StreamDecoderSettingsLoader.load(
                        settingsRepository,
                        videoSettings,
                        audioSettings,
                        uiSettings);
        TransferSettings transferSettings =
                TransferSettingsLoader.load(settingsRepository);
        InputSettings inputSettings =
                InputSettingsLoader.load(settingsRepository);
        InputSettingsState inputSettingsState =
                new InputSettingsState(inputSettings);
        ControllerSettings controllerSettings =
                ControllerSettingsLoader.load(settingsRepository);
        ControllerSettingsState controllerSettingsState =
                new ControllerSettingsState(controllerSettings);
        VirtualControlSettingsState virtualControlSettingsState =
                new VirtualControlSettingsState(
                        VirtualControlSettingsLoader.load(
                                settingsRepository));

        NvApp app = new NvApp(
                request.getAppName(),
                request.getAppId(),
                request.supportsHdr());
        StreamHdrRequestPolicy.Decision hdrDecision =
                StreamHdrRequestPolicy.decide(
                        displaySettings.isHdrEnabled(),
                        videoSettings.shouldIgnoreHdrCapability(),
                        AndroidStreamHdrCapabilityProvider.sample(activity));
        DecoderCrashTracker crashTracker = new DecoderCrashTracker(
                new AndroidDecoderCrashStore(appContext));
        PerfOverlayRelay performanceRelay = new PerfOverlayRelay();
        Display display = AndroidDisplayCompat.getActivityDisplay(activity);
        AndroidStreamMediaRuntimeFactory.Result mediaRuntime =
                AndroidStreamMediaRuntimeFactory.create(
                        appContext,
                        display.getAppVsyncOffsetNanos(),
                        new SharedPreferencesGlDeviceSnapshotStore(
                                appContext).read(),
                        decoderSettings,
                        crashTracker,
                        hdrDecision.isHdrRequested(),
                        performanceRelay,
                        () -> new AndroidAudioRenderer(
                                appContext,
                                audioSettingsState));
        if (!mediaRuntime.getResourceOwner().isAvcSupported()) {
            mediaRuntime.getResourceOwner().destroy();
            throw new IllegalStateException(
                    "No hardware accelerated H.264 decoder is available");
        }

        AndroidStreamDisplayController.Preparation displayPreparation =
                AndroidStreamDisplayController.inspect(
                        activity,
                        decoderSettings,
                        displaySettings,
                        decoderSettings.getFramePacing());
        int gamepadMask = AndroidControllerInventory.from(activity)
                .getInitialControllerMask(controllerSettings);
        StreamSessionConfigurationPlanner.Plan configurationPlan =
                StreamSessionConfigurationPlanner.plan(
                        new StreamSessionConfigurationPlanner
                                .SettingsSnapshot(
                                decoderSettings,
                                videoSettings,
                                audioSettingsState.get(),
                                controllerSettings,
                                inputSettings,
                                transferSettings),
                        new StreamSessionConfigurationPlanner.Environment(
                                app,
                                mediaRuntime.getDecoderCapabilities(),
                                gamepadMask,
                                displayPreparation
                                        .getEffectiveDisplayRefreshRate(),
                                RazerUtils.getPPI(activity),
                                hdrDecision.isHdrRequested()));
        StreamConfiguration configuration =
                StreamSessionConfigurationAdapter
                        .toTransportConfiguration(
                                configurationPlan
                                        .getConfigurationDocument());
        NvConnection connection = new NvConnection(
                appContext,
                request.getHostAddress(),
                request.getHostPort(),
                request.getHttpsPort(),
                request.getClientId(),
                configuration,
                PlatformBinding.getCryptoProvider(appContext),
                parseCertificate(request.getServerCertificate()),
                new AndroidMicrophoneUplinkSessionFactory(
                        MicrophoneUplinkConfig.protocolV1()));
        AndroidStreamStagingSurface stagingSurface =
                new AndroidStreamStagingSurface(
                        decoderSettings.getWidth(),
                        decoderSettings.getHeight());
        StreamWifiLockController wifiLockController =
                StreamWifiLockController.create(appContext);
        wifiLockController.acquire();
        return new AndroidPreparedStreamSession(
                UUID.randomUUID().toString(),
                request,
                new Handler(Looper.getMainLooper()),
                listener,
                settingsRepository,
                videoSettings,
                videoSettingsState,
                audioSettingsState,
                uiSettingsState,
                displaySettings,
                decoderSettings,
                transferSettings,
                inputSettingsState,
                controllerSettingsState,
                virtualControlSettingsState,
                configurationPlan.getEffectiveFramePacing(),
                configurationPlan.getWarnings(),
                hdrDecision.getWarning(),
                connection,
                mediaRuntime.getResourceOwner(),
                crashTracker,
                stagingSurface,
                performanceRelay,
                wifiLockController);
    }

    private static X509Certificate parseCertificate(byte[] encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(
                            new ByteArrayInputStream(encoded));
        }
        catch (CertificateException failure) {
            throw new IllegalArgumentException(
                    "Pinned host certificate is invalid",
                    failure);
        }
    }
}
