package com.limelight.computers.pairing;

import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;

import java.security.cert.X509Certificate;
import java.util.Objects;

/** NvHTTP adapter for the transport-independent pairing use case. */
public final class NvHttpPairingBackend
        implements HostPairingUseCase.Backend {
    private final NvHTTP http;

    public NvHttpPairingBackend(NvHTTP http) {
        this.http = Objects.requireNonNull(http, "http");
    }

    @Override
    public HostPairingUseCase.BackendState getState()
            throws Exception {
        return http.getPairState() == PairingManager.PairState.PAIRED
                ? HostPairingUseCase.BackendState.PAIRED
                : HostPairingUseCase.BackendState.NOT_PAIRED;
    }

    @Override
    public HostPairingUseCase.PairingAttempt pair(String pin)
            throws Exception {
        PairingManager manager = http.getPairingManager();
        PairingManager.PairState state = manager.pair(
                http.getServerInfo(true),
                pin);
        return mapPairingResult(state, manager.getPairedCert());
    }

    static HostPairingUseCase.PairingAttempt mapPairingResult(
            PairingManager.PairState state,
            X509Certificate certificate) {
        Objects.requireNonNull(state, "state");
        switch (state) {
            case PAIRED:
                return new HostPairingUseCase.PairingAttempt(
                        HostPairingUseCase.AttemptStatus.PAIRED,
                        certificate);
            case PIN_WRONG:
                return new HostPairingUseCase.PairingAttempt(
                        HostPairingUseCase.AttemptStatus.PIN_WRONG,
                        null);
            case ALREADY_IN_PROGRESS:
                return new HostPairingUseCase.PairingAttempt(
                        HostPairingUseCase.AttemptStatus.ALREADY_IN_PROGRESS,
                        null);
            case FAILED:
            case NOT_PAIRED:
                return new HostPairingUseCase.PairingAttempt(
                        HostPairingUseCase.AttemptStatus.FAILED,
                        null);
            default:
                throw new IllegalStateException(
                        "Unsupported pairing result: " + state);
        }
    }

    @Override
    public void rollbackPairing() throws Exception {
        http.unpair();
    }
}
