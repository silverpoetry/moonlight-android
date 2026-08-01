package com.limelight.computers.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.limelight.nvstream.http.PairingManager;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class NvHttpHostUnpairBackendTest {
    @Test
    public void mapsOnlyPairedProtocolStateToPairedDomainState()
            throws Exception {
        assertEquals(
                HostUnpairUseCase.PairingState.PAIRED,
                backendFor(PairingManager.PairState.PAIRED).getState());
        assertEquals(
                HostUnpairUseCase.PairingState.NOT_PAIRED,
                backendFor(
                        PairingManager.PairState.NOT_PAIRED).getState());
        assertEquals(
                HostUnpairUseCase.PairingState.NOT_PAIRED,
                backendFor(PairingManager.PairState.FAILED).getState());
    }

    @Test
    public void delegatesUnpairExactlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        NvHttpHostUnpairBackend backend =
                new NvHttpHostUnpairBackend(
                        new NvHttpHostUnpairBackend.UnpairTransport() {
                            @Override
                            public PairingManager.PairState getState() {
                                return PairingManager.PairState.PAIRED;
                            }

                            @Override
                            public void unpair() {
                                calls.incrementAndGet();
                            }
                        });

        backend.unpair();

        assertEquals(1, calls.get());
    }

    @Test
    public void preservesTransportFailure() throws Exception {
        IOException failure = new IOException("network");
        NvHttpHostUnpairBackend backend =
                new NvHttpHostUnpairBackend(
                        new NvHttpHostUnpairBackend.UnpairTransport() {
                            @Override
                            public PairingManager.PairState getState() {
                                return PairingManager.PairState.PAIRED;
                            }

                            @Override
                            public void unpair() throws Exception {
                                throw failure;
                            }
                        });

        try {
            backend.unpair();
            fail("Expected transport failure");
        }
        catch (IOException error) {
            assertSame(failure, error);
        }
    }

    private static NvHttpHostUnpairBackend backendFor(
            PairingManager.PairState state) {
        return new NvHttpHostUnpairBackend(
                new NvHttpHostUnpairBackend.UnpairTransport() {
                    @Override
                    public PairingManager.PairState getState() {
                        return state;
                    }

                    @Override
                    public void unpair() {
                    }
                });
    }
}
