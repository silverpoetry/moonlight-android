package com.limelight.computers.session;

import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;

import java.util.Objects;

/** NvHTTP transport adapter for {@link HostUnpairUseCase}. */
public final class NvHttpHostUnpairBackend
        implements HostUnpairUseCase.Backend {
    interface UnpairTransport {
        PairingManager.PairState getState() throws Exception;

        void unpair() throws Exception;
    }

    private final UnpairTransport transport;

    public NvHttpHostUnpairBackend(
            NvHTTP http,
            String clientName) {
        NvHTTP checkedHttp = Objects.requireNonNull(http, "http");
        checkedHttp.setClientName(Objects.requireNonNull(
                clientName,
                "clientName"));
        transport = new UnpairTransport() {
            @Override
            public PairingManager.PairState getState()
                    throws Exception {
                return checkedHttp.getPairState();
            }

            @Override
            public void unpair() throws Exception {
                checkedHttp.unpair();
            }
        };
    }

    NvHttpHostUnpairBackend(UnpairTransport transport) {
        this.transport = Objects.requireNonNull(
                transport,
                "transport");
    }

    @Override
    public HostUnpairUseCase.PairingState getState()
            throws Exception {
        return transport.getState() == PairingManager.PairState.PAIRED
                ? HostUnpairUseCase.PairingState.PAIRED
                : HostUnpairUseCase.PairingState.NOT_PAIRED;
    }

    @Override
    public void unpair() throws Exception {
        transport.unpair();
    }
}
