package com.limelight.stream.launch.android;

import com.limelight.computers.ComputerManagerService;
import com.limelight.computers.LegacyHostRuntimeAdapter;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.PendingStreamReconnect;
import com.limelight.stream.launch.PendingStreamReconnectResolver;
import com.limelight.stream.launch.PendingStreamReconnectStore;

import java.util.Objects;

/** Activity-scoped adapter that consumes one pending stream handoff. */
public final class AndroidStreamAutoReconnectController {
    public enum Outcome {
        NO_PENDING,
        HOST_MISMATCH,
        HOST_UNAVAILABLE,
        INVALID_APP,
        STARTED,
        ALREADY_STARTING,
        INVALID_REQUEST,
        LAUNCH_FAILED,
        OWNER_UNAVAILABLE
    }

    private final PendingStreamReconnectStore pendingStore;
    private final AndroidStreamLauncher launcher;
    private final PendingStreamReconnectResolver resolver =
            new PendingStreamReconnectResolver();

    public AndroidStreamAutoReconnectController(
            PendingStreamReconnectStore pendingStore,
            AndroidStreamLauncher launcher) {
        this.pendingStore = Objects.requireNonNull(
                pendingStore,
                "pendingStore");
        this.launcher = Objects.requireNonNull(launcher, "launcher");
    }

    public Outcome maybeResume(
            ComputerManagerService.ComputerManagerBinder binder,
            String currentHostId) {
        PendingStreamReconnect pending = pendingStore.get();
        HostRuntimeSnapshot host = binder == null || pending == null
                ? null
                : binder.getHost(HostId.of(pending.getHostId()));
        ComputerDetails computer = host == null
                ? null
                : LegacyHostRuntimeAdapter.toComputerDetails(host);
        boolean hostAvailable = computer != null &&
                computer.state == ComputerDetails.State.ONLINE &&
                computer.activeAddress != null;
        PendingStreamReconnectResolver.Resolution resolution =
                resolver.resolve(
                        pending,
                        currentHostId,
                        hostAvailable,
                        computer == null ? 0 : computer.runningGameId);
        if (resolution.getOutcome() !=
                PendingStreamReconnectResolver.Outcome.READY) {
            return map(resolution.getOutcome());
        }

        AndroidStreamLauncher.Result result = launcher.launch(
                computer,
                new NvApp(
                        pending.getAppName() == null
                                ? "app"
                                : pending.getAppName(),
                        resolution.getAppId(),
                        pending.supportsHdr()),
                binder.getUniqueId());
        if (result.isStarted()) {
            pendingStore.clearIfCurrent(pending);
        }
        return map(result.getOutcome());
    }

    private static Outcome map(
            AndroidStreamLauncher.Outcome outcome) {
        switch (outcome) {
            case STARTED:
                return Outcome.STARTED;
            case ALREADY_STARTING:
                return Outcome.ALREADY_STARTING;
            case HOST_UNAVAILABLE:
                return Outcome.HOST_UNAVAILABLE;
            case INVALID_REQUEST:
                return Outcome.INVALID_REQUEST;
            case LAUNCH_FAILED:
                return Outcome.LAUNCH_FAILED;
            case OWNER_UNAVAILABLE:
                return Outcome.OWNER_UNAVAILABLE;
            default:
                throw new AssertionError(
                        "Unhandled launch outcome: " + outcome);
        }
    }

    private static Outcome map(
            PendingStreamReconnectResolver.Outcome outcome) {
        switch (outcome) {
            case NO_PENDING:
                return Outcome.NO_PENDING;
            case HOST_MISMATCH:
                return Outcome.HOST_MISMATCH;
            case HOST_UNAVAILABLE:
                return Outcome.HOST_UNAVAILABLE;
            case INVALID_APP:
                return Outcome.INVALID_APP;
            case READY:
            default:
                throw new AssertionError(
                        "Unexpected reconnect resolution: " + outcome);
        }
    }
}
