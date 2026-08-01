package com.limelight.computers.pairing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.limelight.computers.model.HostId;

import org.junit.Test;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class HostPairingUseCaseTest {
    @Test
    public void successfulPairPersistsCredentialBeforeInvalidatingState()
            throws Exception {
        X509Certificate certificate = TestCertificates.certificate();
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        AtomicReference<String> pin = new AtomicReference<>();
        AtomicReference<X509Certificate> persisted = new AtomicReference<>();
        AtomicReference<Boolean> invalidated = new AtomicReference<>(false);

        HostPairingUseCase.Outcome outcome = useCase.execute(
                HostId.of("host"),
                false,
                backend(
                        HostPairingUseCase.BackendState.NOT_PAIRED,
                        new HostPairingUseCase.PairingAttempt(
                                HostPairingUseCase.AttemptStatus.PAIRED,
                                certificate)),
                pin::set,
                (hostId, value) -> {
                    assertFalse(invalidated.get());
                    persisted.set(value);
                },
                hostId -> invalidated.set(true));

        assertEquals(HostPairingUseCase.Outcome.PAIRED, outcome);
        assertEquals("1234", pin.get());
        assertSame(certificate, persisted.get());
        assertTrue(invalidated.get());
    }

    @Test
    public void alreadyPairedSkipsPinAndCredentialWrite()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        AtomicReference<Boolean> touched = new AtomicReference<>(false);

        HostPairingUseCase.Outcome outcome = useCase.execute(
                HostId.of("host"),
                false,
                backend(HostPairingUseCase.BackendState.PAIRED, null),
                pin -> touched.set(true),
                (hostId, certificate) -> {
                    touched.set(true);
                },
                hostId -> touched.set(true));

        assertEquals(
                HostPairingUseCase.Outcome.ALREADY_PAIRED,
                outcome);
        assertFalse(touched.get());
    }

    @Test
    public void failedAttemptDistinguishesBusyHost() throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        HostPairingUseCase.PairingAttempt failed =
                new HostPairingUseCase.PairingAttempt(
                        HostPairingUseCase.AttemptStatus.FAILED,
                        null);

        assertEquals(
                HostPairingUseCase.Outcome.HOST_IN_GAME,
                useCase.execute(
                        HostId.of("host"),
                        true,
                        backend(
                                HostPairingUseCase.BackendState.NOT_PAIRED,
                                failed),
                        pin -> {},
                        (hostId, certificate) -> {},
                        hostId -> {}));
    }

    @Test
    public void duplicateAttemptIsRejectedWhileFirstIsRunning()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<HostPairingUseCase.Outcome> first =
                new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                first.set(useCase.execute(
                        HostId.of("one"),
                        false,
                        new HostPairingUseCase.Backend() {
                            @Override
                            public HostPairingUseCase.BackendState getState()
                                    throws Exception {
                                entered.countDown();
                                release.await();
                                return HostPairingUseCase.BackendState.PAIRED;
                            }

                            @Override
                            public HostPairingUseCase.PairingAttempt pair(
                                    String pin) {
                                throw new AssertionError();
                            }

                            @Override
                            public void rollbackPairing() {
                                throw new AssertionError();
                            }
                        },
                        pin -> {},
                        (hostId, certificate) -> {},
                        hostId -> {}));
            }
            catch (Exception error) {
                throw new AssertionError(error);
            }
        });
        thread.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS));

        HostPairingUseCase.Outcome duplicate = useCase.execute(
                HostId.of("two"),
                false,
                backend(HostPairingUseCase.BackendState.PAIRED, null),
                pin -> {},
                (hostId, certificate) -> {},
                hostId -> {});
        release.countDown();
        thread.join(2000);

        assertEquals(
                HostPairingUseCase.Outcome.ALREADY_IN_PROGRESS,
                duplicate);
        assertEquals(
                HostPairingUseCase.Outcome.ALREADY_PAIRED,
                first.get());
    }

    @Test
    public void credentialFailureRollsBackRemotePairing()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        X509Certificate certificate = TestCertificates.certificate();
        AtomicBoolean rolledBack = new AtomicBoolean();
        AtomicBoolean invalidated = new AtomicBoolean();
        IOException failure = new IOException("persistence failed");
        HostPairingUseCase.Backend backend =
                new HostPairingUseCase.Backend() {
                    @Override
                    public HostPairingUseCase.BackendState getState() {
                        return HostPairingUseCase.BackendState.NOT_PAIRED;
                    }

                    @Override
                    public HostPairingUseCase.PairingAttempt pair(
                            String pin) {
                        return new HostPairingUseCase.PairingAttempt(
                                HostPairingUseCase.AttemptStatus.PAIRED,
                                certificate);
                    }

                    @Override
                    public void rollbackPairing() {
                        rolledBack.set(true);
                    }
                };

        try {
            useCase.execute(
                    HostId.of("host"),
                    false,
                    backend,
                    pin -> {},
                    (hostId, value) -> {
                        throw failure;
                    },
                    hostId -> invalidated.set(true));
            fail("Expected credential persistence failure");
        }
        catch (IOException error) {
            assertSame(failure, error);
        }

        assertTrue(rolledBack.get());
        assertFalse(invalidated.get());
    }

    @Test
    public void rollbackFailureIsSuppressedByCredentialFailure()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        IOException persistenceFailure =
                new IOException("persistence");
        IOException rollbackFailure = new IOException("rollback");
        HostPairingUseCase.Backend backend =
                new HostPairingUseCase.Backend() {
                    @Override
                    public HostPairingUseCase.BackendState getState() {
                        return HostPairingUseCase.BackendState.NOT_PAIRED;
                    }

                    @Override
                    public HostPairingUseCase.PairingAttempt pair(
                            String pin) {
                        return new HostPairingUseCase.PairingAttempt(
                                HostPairingUseCase.AttemptStatus.PAIRED,
                                TestCertificates.certificate());
                    }

                    @Override
                    public void rollbackPairing()
                            throws IOException {
                        throw rollbackFailure;
                    }
                };

        try {
            useCase.execute(
                    HostId.of("host"),
                    false,
                    backend,
                    pin -> {},
                    (hostId, certificate) -> {
                        throw persistenceFailure;
                    },
                    hostId -> {});
            fail("Expected credential persistence failure");
        }
        catch (IOException error) {
            assertSame(persistenceFailure, error);
            assertEquals(1, error.getSuppressed().length);
            assertSame(rollbackFailure, error.getSuppressed()[0]);
        }
    }

    @Test
    public void failedOperationReleasesSerializationGuard()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        HostPairingUseCase.Backend failing =
                new HostPairingUseCase.Backend() {
                    @Override
                    public HostPairingUseCase.BackendState getState()
                            throws IOException {
                        throw new IOException("network failed");
                    }

                    @Override
                    public HostPairingUseCase.PairingAttempt pair(
                            String pin) {
                        throw new AssertionError();
                    }

                    @Override
                    public void rollbackPairing() {
                        throw new AssertionError();
                    }
                };

        try {
            useCase.execute(
                    HostId.of("host"),
                    false,
                    failing,
                    pin -> {},
                    (hostId, certificate) -> {},
                    hostId -> {});
            fail("Expected backend failure");
        }
        catch (IOException expected) {
            // Expected.
        }

        assertEquals(
                HostPairingUseCase.Outcome.ALREADY_PAIRED,
                useCase.execute(
                        HostId.of("host"),
                        false,
                        backend(
                                HostPairingUseCase.BackendState.PAIRED,
                                null),
                        pin -> {},
                        (hostId, certificate) -> {},
                        hostId -> {}));
    }

    @Test
    public void cancellationBeforePairSkipsIrreversibleWork()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        AtomicBoolean pairCalled = new AtomicBoolean();
        AtomicBoolean canceled = new AtomicBoolean();
        HostPairingUseCase.Backend backend =
                new HostPairingUseCase.Backend() {
                    @Override
                    public HostPairingUseCase.BackendState getState() {
                        canceled.set(true);
                        return HostPairingUseCase.BackendState.NOT_PAIRED;
                    }

                    @Override
                    public HostPairingUseCase.PairingAttempt pair(
                            String pin) {
                        pairCalled.set(true);
                        return null;
                    }

                    @Override
                    public void rollbackPairing() {
                        throw new AssertionError();
                    }
                };

        HostPairingUseCase.Outcome outcome = useCase.execute(
                HostId.of("host"),
                false,
                backend,
                pin -> {},
                (hostId, certificate) -> {},
                hostId -> {},
                canceled::get);

        assertEquals(HostPairingUseCase.Outcome.CANCELED, outcome);
        assertFalse(pairCalled.get());
    }

    @Test
    public void cancellationAfterPairStartsStillCommitsCredential()
            throws Exception {
        HostPairingUseCase useCase = new HostPairingUseCase(
                () -> "1234");
        X509Certificate certificate = TestCertificates.certificate();
        AtomicBoolean canceled = new AtomicBoolean();
        AtomicBoolean persisted = new AtomicBoolean();
        HostPairingUseCase.Backend backend =
                new HostPairingUseCase.Backend() {
                    @Override
                    public HostPairingUseCase.BackendState getState() {
                        return HostPairingUseCase.BackendState.NOT_PAIRED;
                    }

                    @Override
                    public HostPairingUseCase.PairingAttempt pair(
                            String pin) {
                        canceled.set(true);
                        return new HostPairingUseCase.PairingAttempt(
                                HostPairingUseCase.AttemptStatus.PAIRED,
                                certificate);
                    }

                    @Override
                    public void rollbackPairing() {
                        throw new AssertionError();
                    }
                };

        HostPairingUseCase.Outcome outcome = useCase.execute(
                HostId.of("host"),
                false,
                backend,
                pin -> {},
                (hostId, value) -> persisted.set(true),
                hostId -> {},
                canceled::get);

        assertEquals(HostPairingUseCase.Outcome.PAIRED, outcome);
        assertTrue(persisted.get());
    }

    private static HostPairingUseCase.Backend backend(
            HostPairingUseCase.BackendState state,
            HostPairingUseCase.PairingAttempt attempt) {
        return new HostPairingUseCase.Backend() {
            @Override
            public HostPairingUseCase.BackendState getState() {
                return state;
            }

            @Override
            public HostPairingUseCase.PairingAttempt pair(String pin) {
                return attempt;
            }

            @Override
            public void rollbackPairing() {
                throw new AssertionError(
                        "Unexpected pairing rollback");
            }
        };
    }
}
