package com.limelight.computers.session;

import com.limelight.nvstream.http.HostHttpResponseException;
import com.limelight.nvstream.http.NvHTTP;

import java.util.Objects;

/** NvHTTP transport adapter for {@link HostQuitUseCase}. */
public final class NvHttpHostQuitBackend
        implements HostQuitUseCase.Backend {
    static final int SESSION_OWNERSHIP_ERROR_CODE = 599;

    interface QuitTransport {
        boolean quit() throws Exception;
    }

    private final QuitTransport transport;

    public NvHttpHostQuitBackend(NvHTTP http) {
        this(Objects.requireNonNull(http, "http")::quitApp);
    }

    NvHttpHostQuitBackend(QuitTransport transport) {
        this.transport = Objects.requireNonNull(
                transport,
                "transport");
    }

    @Override
    public boolean quit() throws Exception {
        try {
            return transport.quit();
        }
        catch (HostHttpResponseException error) {
            if (error.getErrorCode() ==
                    SESSION_OWNERSHIP_ERROR_CODE) {
                throw new HostQuitUseCase.NotSessionOwnerException(
                        error);
            }
            throw error;
        }
    }
}
