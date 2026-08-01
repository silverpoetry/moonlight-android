package com.limelight.computers.pairing;

import com.limelight.computers.model.HostId;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Serialized pairing state machine independent of Android UI and transport. */
public final class HostPairingUseCase {
    public enum BackendState {
        NOT_PAIRED,
        PAIRED
    }

    public enum AttemptStatus {
        PAIRED,
        PIN_WRONG,
        FAILED,
        ALREADY_IN_PROGRESS
    }

    public enum Outcome {
        PAIRED,
        ALREADY_PAIRED,
        PIN_WRONG,
        HOST_IN_GAME,
        FAILED,
        ALREADY_IN_PROGRESS,
        CANCELED
    }

    public interface Backend {
        BackendState getState() throws Exception;

        PairingAttempt pair(String pin) throws Exception;

        void rollbackPairing() throws Exception;
    }

    public interface PinObserver {
        void onPinGenerated(String pin);
    }

    public interface CredentialWriter {
        void persist(
                HostId hostId,
                X509Certificate certificate) throws Exception;
    }

    public interface HostStateInvalidator {
        void invalidate(HostId hostId);
    }

    public interface CancellationSignal {
        boolean isCanceled();
    }

    interface PinGenerator {
        String generate();
    }

    public static final class PairingAttempt {
        private final AttemptStatus status;
        private final X509Certificate certificate;

        public PairingAttempt(
                AttemptStatus status,
                X509Certificate certificate) {
            this.status = Objects.requireNonNull(status, "status");
            if (status == AttemptStatus.PAIRED && certificate == null) {
                throw new IllegalArgumentException(
                        "A paired result requires a certificate");
            }
            if (status != AttemptStatus.PAIRED && certificate != null) {
                throw new IllegalArgumentException(
                        "Only a paired result may contain a certificate");
            }
            this.certificate = certificate;
        }

        public AttemptStatus getStatus() {
            return status;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }
    }

    private final AtomicBoolean active = new AtomicBoolean();
    private final PinGenerator pinGenerator;

    public HostPairingUseCase() {
        this(new SecurePinGenerator());
    }

    HostPairingUseCase(PinGenerator pinGenerator) {
        this.pinGenerator = Objects.requireNonNull(
                pinGenerator,
                "pinGenerator");
    }

    public Outcome execute(
            HostId hostId,
            boolean hostIsRunningApplication,
            Backend backend,
            PinObserver pinObserver,
            CredentialWriter credentialWriter,
            HostStateInvalidator stateInvalidator) throws Exception {
        return execute(
                hostId,
                hostIsRunningApplication,
                backend,
                pinObserver,
                credentialWriter,
                stateInvalidator,
                () -> false);
    }

    public Outcome execute(
            HostId hostId,
            boolean hostIsRunningApplication,
            Backend backend,
            PinObserver pinObserver,
            CredentialWriter credentialWriter,
            HostStateInvalidator stateInvalidator,
            CancellationSignal cancellationSignal) throws Exception {
        Objects.requireNonNull(hostId, "hostId");
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(pinObserver, "pinObserver");
        Objects.requireNonNull(credentialWriter, "credentialWriter");
        Objects.requireNonNull(stateInvalidator, "stateInvalidator");
        Objects.requireNonNull(cancellationSignal, "cancellationSignal");

        if (!active.compareAndSet(false, true)) {
            return Outcome.ALREADY_IN_PROGRESS;
        }
        try {
            if (cancellationSignal.isCanceled()) {
                return Outcome.CANCELED;
            }
            if (backend.getState() == BackendState.PAIRED) {
                return Outcome.ALREADY_PAIRED;
            }
            if (cancellationSignal.isCanceled()) {
                return Outcome.CANCELED;
            }

            String pin = pinGenerator.generate();
            pinObserver.onPinGenerated(pin);
            if (cancellationSignal.isCanceled()) {
                return Outcome.CANCELED;
            }

            // Pairing is the irreversible boundary. Once the request reaches
            // the host, finish credential persistence even if the UI owner is
            // destroyed so remote and local pairing state cannot diverge.
            PairingAttempt attempt = Objects.requireNonNull(
                    backend.pair(pin),
                    "pairingAttempt");
            switch (attempt.getStatus()) {
                case PAIRED:
                    persistCredentialOrRollback(
                            hostId,
                            attempt.getCertificate(),
                            backend,
                            credentialWriter);
                    stateInvalidator.invalidate(hostId);
                    return Outcome.PAIRED;
                case PIN_WRONG:
                    return Outcome.PIN_WRONG;
                case ALREADY_IN_PROGRESS:
                    return Outcome.ALREADY_IN_PROGRESS;
                case FAILED:
                    return hostIsRunningApplication
                            ? Outcome.HOST_IN_GAME
                            : Outcome.FAILED;
                default:
                    throw new AssertionError(
                            "Unhandled pairing attempt status");
            }
        }
        finally {
            active.set(false);
        }
    }

    private static void persistCredentialOrRollback(
            HostId hostId,
            X509Certificate certificate,
            Backend backend,
            CredentialWriter credentialWriter) throws Exception {
        try {
            credentialWriter.persist(hostId, certificate);
        }
        catch (Exception persistenceFailure) {
            try {
                backend.rollbackPairing();
            }
            catch (Exception rollbackFailure) {
                persistenceFailure.addSuppressed(rollbackFailure);
            }
            throw persistenceFailure;
        }
    }

    private static final class SecurePinGenerator
            implements PinGenerator {
        private final SecureRandom random = new SecureRandom();

        @Override
        public String generate() {
            return String.format(
                    (Locale) null,
                    "%d%d%d%d",
                    random.nextInt(10),
                    random.nextInt(10),
                    random.nextInt(10),
                    random.nextInt(10));
        }
    }
}
