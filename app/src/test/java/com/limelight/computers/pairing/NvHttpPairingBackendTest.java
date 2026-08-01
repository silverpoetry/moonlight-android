package com.limelight.computers.pairing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.limelight.nvstream.http.PairingManager;

import org.junit.Test;

import java.security.cert.X509Certificate;

public final class NvHttpPairingBackendTest {
    @Test
    public void mapsEveryProtocolStateWithoutChangingSemantics() {
        X509Certificate certificate = TestCertificates.certificate();
        HostPairingUseCase.PairingAttempt paired =
                NvHttpPairingBackend.mapPairingResult(
                        PairingManager.PairState.PAIRED,
                        certificate);
        assertEquals(
                HostPairingUseCase.AttemptStatus.PAIRED,
                paired.getStatus());
        assertSame(certificate, paired.getCertificate());

        assertStatus(
                PairingManager.PairState.PIN_WRONG,
                HostPairingUseCase.AttemptStatus.PIN_WRONG);
        assertStatus(
                PairingManager.PairState.ALREADY_IN_PROGRESS,
                HostPairingUseCase.AttemptStatus.ALREADY_IN_PROGRESS);
        assertStatus(
                PairingManager.PairState.FAILED,
                HostPairingUseCase.AttemptStatus.FAILED);
        assertStatus(
                PairingManager.PairState.NOT_PAIRED,
                HostPairingUseCase.AttemptStatus.FAILED);
    }

    private static void assertStatus(
            PairingManager.PairState source,
            HostPairingUseCase.AttemptStatus expected) {
        HostPairingUseCase.PairingAttempt attempt =
                NvHttpPairingBackend.mapPairingResult(source, null);
        assertEquals(expected, attempt.getStatus());
        assertNull(attempt.getCertificate());
    }
}
