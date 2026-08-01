package com.limelight.computers.session;

import java.util.Objects;

/** Transport-independent use case for removing this client's host pairing. */
public final class HostUnpairUseCase {
    public enum PairingState {
        PAIRED,
        NOT_PAIRED
    }

    public enum Outcome {
        UNPAIRED,
        ALREADY_UNPAIRED,
        REJECTED
    }

    public interface Backend {
        PairingState getState() throws Exception;

        void unpair() throws Exception;
    }

    public Outcome execute(Backend backend) throws Exception {
        Objects.requireNonNull(backend, "backend");
        if (backend.getState() != PairingState.PAIRED) {
            return Outcome.ALREADY_UNPAIRED;
        }
        backend.unpair();
        return backend.getState() == PairingState.NOT_PAIRED
                ? Outcome.UNPAIRED
                : Outcome.REJECTED;
    }
}
